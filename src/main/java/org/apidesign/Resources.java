package org.apidesign;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Resources {

    private Resources() {
    }

    public static String read(String resource) throws IOException {
        InputStream in = Resources.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        readInputStream(in, result);

        return result.toString(StandardCharsets.UTF_8);
    }

    private static void readInputStream(InputStream in, ByteArrayOutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        for (int length; (length = in.read(buffer)) != -1;) {
            out.write(buffer, 0, length);
        }
    }
}
