package org.apidesign.polyfill.websocket;

import java.io.InputStreamReader;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public final class WebsocketPolyfill {
    private WebsocketPolyfill() {
    }

    public static void prepare(Context ctx) {
      var code = """
      function WebSocketServer(config) {
          var wss = {
              on : function(type, callback) {
                 var ws = {
                    on : function(type, callback) {
                      print("on ws with " + type + " with " + callback);
                    }
                 }
                 print("on wss with " + type + " with " + callback);
                 ws.on("connection", callback);
              }
          };
          return wss;
      }
      """;
      var polyfill = Source.newBuilder("js", code, "websocket-polyfill.js")
          .buildLiteral();
      ctx.eval(polyfill);
    }

    public static void main(String[] args) throws Exception {
      try (
        var demo = new InputStreamReader(WebsocketPolyfill.class.getResourceAsStream("/WebsocketServerDemo.js"));
        var ctx = Context.create();
      ) {
        prepare(ctx);
        var src = Source.newBuilder("js", demo, "WebsocketServerDemo.js")
            .build();
        ctx.eval(src);
      }
    }
}
