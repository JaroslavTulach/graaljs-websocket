package org.apidesign.polyfill.websocket;

import java.io.File;
import java.io.IOException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

public class Main {
    private Main() {
    }


    public static void main(String[] args) throws Exception {
        var path = "/all-y-websocket.js";
        var demo = WebSocketPolyfill.class.getResource(path);
        if (demo == null) {
            throw new IOException("Cannot find " + path);
        }
        var commonJsRoot = new File(demo.toURI()).getParent();
        try (
            var ctx = Context.newBuilder("js")
                .allowIO(IOAccess.ALL)
                .allowExperimentalOptions(true)
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", commonJsRoot)
                .build()
        ) {
            WebSocketPolyfill.prepare(ctx);
            var src = Source.newBuilder("js", demo)
                    .mimeType("application/javascript+module")
                    .build();
            ctx.eval(src);
            System.out.println("Press enter to exit");
            System.in.read();
        }
    }


}
