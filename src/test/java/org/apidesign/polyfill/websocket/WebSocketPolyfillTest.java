package org.apidesign.polyfill.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebSocketPolyfillTest {

    private static CompletableFuture<Context> futureContext;
    private static ExecutorService executor;

    public WebSocketPolyfillTest() {
    }

    @BeforeClass
    public static void prepareContext() throws Exception {
        var b = Context.newBuilder("js")
                .allowIO(IOAccess.ALL);
        var chromePort = Integer.getInteger("inspectPort", -1);
        if (chromePort > 0) {
            b.option("inspect", ":" + chromePort);
        }
        executor = Executors.newSingleThreadExecutor();
        futureContext = WebSocketPolyfill.prepare(b::build, executor);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        executor.close();
        futureContext.get().close();
    }

    @Test
    public void allTests() throws Exception {
        var allTest = WebSocketPolyfillTest.class.getResource("/all-tests.js");
        assertNotNull("Generated tests found", allTest);
        var code = Source.newBuilder("js", allTest)
                .mimeType("application/javascript+module")
                .build();

        futureContext
                .thenAcceptAsync(ctx -> {
                    ctx.eval("js", """
                    globalThis.importScripts = function() {
                        debugger;
                    };
                    globalThis.process= {
                        env : "none"
                    };
                    globalThis.__vitest_worker__ = {
                        config : {},
                        environment : { name : "Graal.js" }
                    };
                    globalThis.location = "${l}";
                    """.replace("${l}", allTest.toExternalForm()));

                    ctx.eval(code);
                }, executor)
                .get();
    }
}
