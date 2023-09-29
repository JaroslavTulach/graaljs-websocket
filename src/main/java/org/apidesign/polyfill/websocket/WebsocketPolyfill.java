package org.apidesign.polyfill.websocket;

import org.graalvm.polyglot.Context;

public final class WebsocketPolyfill {
    private WebsocketPolyfill() {
    }
    
    public static void main(String[] args) {
      try (org.graalvm.polyglot.Context ctx = Context.create()) {
        ctx.eval("js", "print('Hello world!')");
      }
    }
}
