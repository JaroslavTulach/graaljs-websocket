package org.apidesign.polyfill.websocket;

import java.util.Arrays;
import java.util.Optional;

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

    private static final String WEBSOCKET_POLYFILL_JS = "websocket-polyfill.js";

    public WebSocketPolyfill() {
    }

    public Context initialize(Context ctx) {
        Source polyfill = Source
                .newBuilder("js", WebSocketPolyfill.class.getResource(WEBSOCKET_POLYFILL_JS))
                .buildLiteral();

        ctx.eval(polyfill).execute(new PolyfillApi());

        return ctx;
    }

    private static final class PolyfillApi implements ProxyExecutable {

        PolyfillApi() {
        }

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
