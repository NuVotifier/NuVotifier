package com.vexsoftware.votifier.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtil {
    public static byte[] readAllBytes(InputStream from) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buf = new byte[8192];
        int read;
        while ((read = from.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }
}
