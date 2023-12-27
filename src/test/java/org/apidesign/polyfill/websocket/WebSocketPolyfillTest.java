package org.apidesign.polyfill.websocket;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebSocketPolyfillTest {
    private static Context ctx;
    private static Timers timers;

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
        ctx = b.build();
        timers = new Timers();
        WebSocketPolyfill.prepare(ctx, timers);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        timers.close();
        ctx.close();
    }

    @Test
    public void allTests() throws Exception {
        var allTest = WebSocketPolyfillTest.class.getResource("/all-tests.js");
        assertNotNull("Generated tests found", allTest);

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

        var code = Source.newBuilder("js", allTest)
             .mimeType("application/javascript+module")
             .build();
        ctx.eval(code);
    }
}
