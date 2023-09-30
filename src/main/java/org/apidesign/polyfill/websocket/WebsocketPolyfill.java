package org.apidesign.polyfill.websocket;

import io.helidon.common.buffers.BufferData;
import io.helidon.http.Headers;
import io.helidon.http.HttpPrologue;
import java.util.Arrays;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.websocket.WsRouting;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
import io.helidon.websocket.WsUpgradeException;
import java.util.Optional;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class WebsocketPolyfill {
    private WebsocketPolyfill() {
    }

    public static void prepare(Context ctx) {
        var code = """
        (function (jvm) {
            globalThis.WebSocketServer = function(config) {
                var webSocketServerData = jvm(null, "", config);
                var wss = {
                    on : function(type, callback) {
                       var webSocketData = jvm(webSocketServerData, type, callback);
                       var ws = {
                          on : function(type, callback) {
                            jvm(webSocketData, type, callback);
                          },
                          send : function(msg) {
                            jvm(webSocketData, "send", msg);
                          }
                       }
                       callback(ws);
                    }
                };
                return wss;
            };
        })
        """;
        var polyfill = Source.newBuilder("js", code, "websocket-polyfill.js")
                .buildLiteral();
        ctx.eval(polyfill).execute(new ProxyExecutable() {
            @Override
            public Object execute(Value... arguments) {
                var command = arguments[1].asString();
                System.err.println(command + " " + Arrays.toString(arguments));
                return switch (arguments[0].isNull() ? null : arguments[0].asHostObject()) {
                    case null -> {
                        var port = arguments[2].getMember("port").asInt();
                        yield new WebSocketServerData(port);
                    }
                    case WebSocketServerData webSocketServerData ->
                        switch (command) {
                            case "connection" -> webSocketServerData.onConnect(arguments[2]);
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

    public static void main(String[] args) throws Exception {
        var demo = WebsocketPolyfill.class.getResource("/WebsocketServerDemo.js");
        try (var ctx = Context.create()) {
            prepare(ctx);
            var src = Source.newBuilder("js", demo)
                    .build();
            ctx.eval(src);
            System.in.read();
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
                b.addRouting(WsRouting.builder().endpoint("/", data).build());
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
