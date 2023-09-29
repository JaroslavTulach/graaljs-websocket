package org.apidesign.polyfill.websocket;

import org.graalvm.polyglot.Context;

public final class WebsocketPolyfill {
    private WebsocketPolyfill() {
    }
    
    public static
    
    public static void main(String[] args) {
      try (var ctx = Context.create()) {
        ctx.eval("js", "print('Hello world!')");
      }
    }
}
