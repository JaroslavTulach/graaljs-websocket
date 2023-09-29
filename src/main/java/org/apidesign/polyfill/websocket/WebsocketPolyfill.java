package org.apidesign.polyfill.websocket;

import java.io.InputStreamReader;
import java.util.Arrays;
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
                          }
                       }
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
                            case "connection" ->
                                new WebSocketData();
                            default ->
                                throw new IllegalStateException(command);
                        };
                    case WebSocketData webSocketData ->
                        switch (command) {
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
        try (
                var demo = new InputStreamReader(WebsocketPolyfill.class.getResourceAsStream("/WebsocketServerDemo.js")); var ctx = Context.create();) {
            prepare(ctx);
            var src = Source.newBuilder("js", demo, "WebsocketServerDemo.js")
                    .build();
            ctx.eval(src);
            System.in.read();
        }
    }

    private static final class WebSocketServerData {

        private final int port;

        WebSocketServerData(int port) {
            this.port = port;
        }
    }

    private static final class WebSocketData {

        Value error;
        Value message;
    }
}
