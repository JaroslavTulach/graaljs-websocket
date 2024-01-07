package org.apidesign;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apidesign.polyfill.Polyfill;
import org.apidesign.polyfill.crypto.CryptoPolyfill;
import org.apidesign.polyfill.timers.TimersPolyfill;
import org.apidesign.polyfill.websocket.WebSocketPolyfill;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

public class WebSocket {

    private WebSocket() {
    }

    public static void initializePolyfill(Context ctx, ExecutorService executor) {
        Polyfill[] components = new Polyfill[]{
            new TimersPolyfill(executor),
            new CryptoPolyfill(),
            new WebSocketPolyfill(executor)
        };

        for (Polyfill component : components) {
            component.initialize(ctx);
        }
    }

    public static void main(String[] args) throws Exception {
        var path = "/all-y-websocket.js";
        var demo = WebSocket.class.getResource(path);
        if (demo == null) {
            throw new IOException("Cannot find " + path);
        }
        var commonJsRoot = new File(demo.toURI()).getParent();

        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.EXPLICIT)
                .allowArrayAccess(true)
                .build();

        var b = Context.newBuilder("js")
                .allowIO(IOAccess.ALL)
                .allowHostAccess(hostAccess)
                .allowExperimentalOptions(true)
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", commonJsRoot);
        var chromePort = Integer.getInteger("inspectPort", -1);
        if (chromePort > 0) {
            b.option("inspect", ":" + chromePort);
        }
        try (var executor = Executors.newSingleThreadExecutor()) {
            var demoJs = Source.newBuilder("js", demo)
                    .mimeType("application/javascript+module")
                    .build();

            CompletableFuture
                    .supplyAsync(b::build, executor)
                    .thenAcceptAsync(ctx -> {
                        initializePolyfill(ctx, executor);
                        ctx.eval(demoJs);
                    }, executor)
                    .get();

            System.out.println("Press enter to exit");
            System.in.read();
        }
    }

}
