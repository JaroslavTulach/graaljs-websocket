package org.apidesign.polyfill.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apidesign.polyfill.Polyfill;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import io.helidon.common.buffers.BufferData;
import io.helidon.webclient.websocket.WsClient;
import io.helidon.webclient.websocket.WsClientProtocolConfig;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;

public final class WebSocketPolyfill implements ProxyExecutable, Polyfill {

    private static final String ADD_EVENT_LISTENER = "add-event-listener";
    private static final String REMOVE_EVENT_LISTENER = "remove-event-listener";
    private static final String GET_EVENT_LISTENERS = "get-event-listeners";
    private static final String SET_ON_LISTENER = "set-on-listener";
    private static final String GET_ON_LISTENER = "get-on-listener";
    private static final String NEW_WEB_SOCKET_CONECTION = "new-web-socket-connection";
    private static final String WEB_SOCKET_SEND_TEXT = "web-socket-send-text";
    private static final String WEB_SOCKET_SEND_BINARY = "web-socket-send-binary";
    private static final String WEB_SOCKET_CLOSE = "web-socket-close";
    private static final String WEB_SOCKET_TERMINATE = "web-socket-terminate";
    private static final String NEW_WEB_SOCKET_SERVER_DATA = "new-web-socket-server-data";

    private static final String WEBSOCKET_POLYFILL_JS = "websocket-polyfill.js";

    private final Map<String, Set<Value>> listeners = new HashMap<>();

    private final ExecutorService executor;

    public WebSocketPolyfill(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void initialize(Context ctx) {
        Source webSocketPolyfillJs = Source
                .newBuilder("js", WebSocketPolyfill.class.getResource(WEBSOCKET_POLYFILL_JS))
                .buildLiteral();

        ctx.eval(webSocketPolyfillJs).execute(this);
    }

    @Override
    public Object execute(Value... arguments) {
        var command = arguments[0].asString();
        System.err.println(command + " " + Arrays.toString(arguments));

        return switch (command) {
            case ADD_EVENT_LISTENER -> {
                var type = arguments[1].asString();
                var listener = arguments[2];
                yield listeners.compute(type, (k, v) -> {
                    var set = v == null ? new HashSet<Value>() : v;
                    set.add(listener);
                    return set;
                });
            }

            case REMOVE_EVENT_LISTENER -> {
                var type = arguments[1].asString();
                var listener = arguments[2];
                yield listeners.compute(type, (k, v) -> {
                    if (v == null) {
                        return v;
                    } else {
                        v.remove(listener);
                        return v.isEmpty() ? null : v;
                    }
                });
            }

            case GET_EVENT_LISTENERS -> {
                var type = arguments[1].asString();
                var set = listeners.get(type);
                yield set == null ? new Value[]{} : set.toArray();
            }

            case SET_ON_LISTENER -> {
                var type = arguments[1].asString();
                var listener = arguments[2];
                yield listeners.compute(type, (k, v) -> {
                    var set = new HashSet<Value>();
                    set.add(listener);
                    return set;
                });
            }

            case GET_ON_LISTENER -> {
                var type = arguments[1].asString();
                var set = listeners.get(type);
                yield set == null ? null : set.iterator().next();
            }

            case NEW_WEB_SOCKET_CONECTION -> {
                var urlString = arguments[1].asString();
                var protocols = arguments[2].as(String[].class);
                var handleOpen = arguments[3];
                var handleClose = arguments[4];
                var handleError = arguments[5];
                var handleMessage = arguments[6];
                var connection = new WebSocketConnection(executor, handleOpen, handleClose, handleError, handleMessage);

                URI uri = null;
                try {
                    uri = new URI(urlString);
                } catch (URISyntaxException ex) {
                    throw new IllegalStateException("Illegal URL", ex);
                }

                var protocolConfig = WsClientProtocolConfig.builder();
                if (protocols != null) {
                    protocolConfig.subProtocols(Arrays.asList(protocols));
                }

                var wsClient = WsClient.builder()
                        .addHeader("Connection", "Upgrade")
                        .protocolConfig(protocolConfig.build())
                        .build();
                wsClient.connect(uri, connection);

                yield connection;
            }

            case WEB_SOCKET_SEND_TEXT -> {
                var connection = arguments[1].as(WebSocketConnection.class);
                var data = arguments[2].asString();

                yield connection.getSession().send(data, true);
            }

            case WEB_SOCKET_SEND_BINARY -> {
                var connection = arguments[1].as(WebSocketConnection.class);
                var data = arguments[2].as(int[].class);

                var bytes = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    bytes[i] = (byte) data[i];
                }
                var bufferData = BufferData.create(bytes);

                yield connection.getSession().send(bufferData, true);
            }

            case WEB_SOCKET_TERMINATE -> {
                var connection = arguments[1].as(WebSocketConnection.class);

                yield connection.getSession().terminate();
            }

            case WEB_SOCKET_CLOSE -> {
                var connection = arguments[1].as(WebSocketConnection.class);
                var code = arguments[2].asInt();
                var reasonArgument = arguments[3];
                var reason = reasonArgument == null ? "Close" : reasonArgument.asString();

                yield connection.getSession().close(code, reason);
            }

            default ->
                throw new IllegalStateException(command);
        };
    }

    private static final class WebSocketConnection implements WsListener {

        private final ExecutorService executor;
        private final Value handleOpen;
        private final Value handleClose;
        private final Value handleError;
        private final Value handleMessage;

        private WsSession session;

        private WebSocketConnection(ExecutorService executor, Value handleOpen, Value handleClose, Value handleError, Value handleMessage) {
            this.executor = executor;
            this.handleOpen = handleOpen;
            this.handleClose = handleClose;
            this.handleError = handleError;
            this.handleMessage = handleMessage;
        }

        public WsSession getSession() {
            return session;
        }

        @Override
        public void onMessage(WsSession session, BufferData buffer, boolean last) {
            Object data = buffer.readBytes();
            executor.execute(() -> handleMessage.executeVoid(data));

            session.subProtocol();

            System.err.println("WebSocketListener.onMessageBinary\n" + buffer.debugDataHex(true));
        }

        @Override
        public void onMessage(WsSession session, String text, boolean last) {
            executor.execute(() -> handleMessage.executeVoid(text));
            System.err.println("WebSocketListener.onMessage " + text);
        }

        @Override
        public void onPing(WsSession session, BufferData buffer) {
            System.err.println("WebSocketListener.onPing " + buffer);
        }

        @Override
        public void onPong(WsSession session, BufferData buffer) {
            System.err.println("WebSocketListener.onPong " + buffer);
        }

        @Override
        public void onOpen(WsSession session) {
            this.session = session;
            executor.execute(() -> handleOpen.executeVoid());

            System.err.println("WebSocketListener.onOpen");
        }

        @Override
        public void onClose(WsSession session, int status, String reason) {
            executor.execute(() -> handleClose.executeVoid(status, reason));
            this.session = null;

            System.err.println("WebSocketListener.onClose " + status + " " + reason);
        }

        @Override
        public void onError(WsSession session, Throwable t) {
            executor.execute(() -> handleError.executeVoid(t.getMessage()));

            System.err.println("WebSocketListener.onError " + t);
        }
    }

}
