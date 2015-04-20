package com.vexsoftware.votifier.crypto;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class KeyCreator {
    public static Key createKeyFrom(String token) {
        return new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
