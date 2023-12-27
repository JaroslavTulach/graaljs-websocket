package org.apidesign.polyfill.websocket;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apidesign.Resources;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import io.helidon.common.buffers.BufferData;
import io.helidon.http.Headers;
import io.helidon.http.HttpPrologue;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.websocket.WsRouting;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
import io.helidon.websocket.WsUpgradeException;

public final class WebSocketPolyfill {

    private static final String NEW_WEB_SOCKET_SERVER_DATA = "new-web-socket-server-data";
    private static final String SET_INTERVAL = "set-interval";
    private static final String CLEAR_INTERVAL = "clear-interval";
    private static final String SET_TIMEOUT = "set-timeout";
    private static final String CLEAR_TIMEOUT = "clear-timeout";

    private static final String WEBSOCKET_POLYFILL_JS_FILE = "websocket-polyfill.js";
    private static final String WEBSOCKET_POLYFILL_JS_CODE;

    static {
        try {
            WEBSOCKET_POLYFILL_JS_CODE = Resources.read(WEBSOCKET_POLYFILL_JS_FILE);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read " + WEBSOCKET_POLYFILL_JS_FILE + " resource.", ex);
        }
    }

    private WebSocketPolyfill() {
    }

    public static void prepare(Context ctx, Timers timers) {
        Source polyfill = Source.newBuilder("js", WEBSOCKET_POLYFILL_JS_CODE, WEBSOCKET_POLYFILL_JS_FILE).buildLiteral();
        ctx.eval(polyfill).execute(new ProxyExecutable() {
            @Override
            public Object execute(Value... arguments) {
                var command = arguments[1].asString();
                System.err.println(command + " " + Arrays.toString(arguments));
                return switch (arguments[0].isNull() ? null : arguments[0].asHostObject()) {
                    case null -> {
                        switch (command) {
                            case NEW_WEB_SOCKET_SERVER_DATA -> {
                                var port = arguments[2].getMember("port").asInt();
                                yield new WebSocketServerData(port);
                            }
                            case SET_INTERVAL -> {
                                var func = arguments[2].as(Consumer.class);
                                var delay = arguments[3].asLong();
                                var args = arguments[4].as(Object[].class);
                                yield timers.setInterval(func, delay, args);
                            }
                            case CLEAR_INTERVAL -> {
                                var intervalId = arguments[2].as(UUID.class);
                                timers.clearInterval(intervalId);
                                yield null;
                            }
                            case SET_TIMEOUT -> {
                                var func = arguments[2].as(Consumer.class);
                                var delay = arguments[3].asLong();
                                var args = arguments[4].as(Object[].class);
                                yield timers.setTimeout(func, delay, args);
                            }
                            case CLEAR_TIMEOUT -> {
                                var timeoutId = arguments[2].as(UUID.class);
                                timers.clearTimeout(timeoutId);
                                yield null;
                            }
                            default ->
                                throw new IllegalStateException(command);
                        }
                    }
                    case WebSocketServerData webSocketServerData ->
                        switch (command) {
                            case "connection" ->
                                webSocketServerData.onConnect(arguments[2]);
                            default ->
                                throw new IllegalStateException(command);
                        };
                    case WebSocketData webSocketData ->
                        switch (command) {
                            case "send" ->
                                webSocketData.send(arguments[2]);
                            case "error" ->
                                webSocketData.error = arguments[2];
                            case "message" ->
                                webSocketData.message = arguments[2];
                            default ->
                                throw new IllegalStateException(command);
                        };
                    default ->
                        throw new IllegalStateException(command);
                };
            }
        });
    }

    private static final class WebSocketServerData {

        private final int port;
        private WebServer server;

        WebSocketServerData(int port) {
            this.port = port;
        }

        private WebSocketData onConnect(Value onConnect) {
            var data = new WebSocketData(this, onConnect);
            if (server == null) {
                var b = WebServer.builder().port(port);
                b.addRouting(
                        WsRouting.builder().endpoint("/", data)
                );
                this.server = b.build();
                this.server.start();
            }
            return data;
        }
    }

    private static final class WebSocketData implements WsListener {

        private final Value onConnect;
        private final WebSocketServerData webSocketServerData;
        Value error;
        Value message;
        private WsSession session;

        private WebSocketData(WebSocketServerData webSocketServerData, Value onConnect) {
            this.webSocketServerData = webSocketServerData;
            this.onConnect = onConnect;
        }

        WebSocketData send(Value data) {
            if (session != null) {
                session.send(data.asString(), true);
            }
            return this;
        }

        @Override
        public Optional<Headers> onHttpUpgrade(HttpPrologue prologue, Headers headers) throws WsUpgradeException {
            return Optional.empty();
        }

        @Override
        public void onOpen(WsSession session) {
            this.session = session;
        }

        @Override
        public void onError(WsSession session, Throwable t) {
        }

        @Override
        public void onClose(WsSession session, int status, String reason) {
            if (this.session == session) {
                this.session = null;
            }
        }

        @Override
        public void onPong(WsSession session, BufferData buffer) {
        }

        @Override
        public void onPing(WsSession session, BufferData buffer) {
        }

        @Override
        public void onMessage(WsSession session, BufferData buffer, boolean last) {
            System.err.println("got msg: " + buffer);
        }

        @Override
        public void onMessage(WsSession session, String text, boolean last) {
            if (message != null) {
                message.execute(text, last);
            }
        }

    }
}
