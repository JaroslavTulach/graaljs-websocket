package org.apidesign.polyfill.websocket;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

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
        var b = Context.newBuilder("js")
            .allowIO(IOAccess.ALL)
            .allowExperimentalOptions(true)
            .option("js.commonjs-require", "true")
            .option("js.commonjs-require-cwd", commonJsRoot);
        var chromePort = Integer.getInteger("inspectPort", -1);
        if (chromePort > 0) {
            b.option("inspect", ":" + chromePort);
        }
        try (
            var executor = Executors.newSingleThreadExecutor();
        ) {
            var futureContext = WebSocketPolyfill.prepare(b::build, executor);
            var src = Source.newBuilder("js", demo)
                    .mimeType("application/javascript+module")
                    .build();
            futureContext.thenAcceptAsync(ctx -> ctx.eval(src), executor);
            System.out.println("Press enter to exit");
            System.in.read();
        }
    }


}
