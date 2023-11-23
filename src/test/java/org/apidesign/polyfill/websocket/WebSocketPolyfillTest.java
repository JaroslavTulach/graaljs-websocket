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

    public WebSocketPolyfillTest() {
    }

    @BeforeClass
    public static void prepareContext() throws Exception {
        ctx = Context.newBuilder("js")
            .allowIO(IOAccess.ALL)
            .build();
        WebSocketPolyfill.prepare(ctx);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ctx.close();
    }

    @Test
    public void allTests() throws Exception {
        var allTest = WebSocketPolyfillTest.class.getResource("/all-tests.js");
        assertNotNull("Generated tests found", allTest);
        var code = Source.newBuilder("js", allTest)
             .mimeType("application/javascript+module")
             .build();
        ctx.eval(code);
    }
}
