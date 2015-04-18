package com.vexsoftware.votifier;

import java.math.BigInteger;
import java.security.SecureRandom;

public class TokenUtil {
    private TokenUtil() {

    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String newToken() {
        return new BigInteger(130, RANDOM).toString(32);
    }
}
