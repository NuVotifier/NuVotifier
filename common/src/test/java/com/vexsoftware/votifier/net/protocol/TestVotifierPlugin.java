package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.util.KeyCreator;

import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class TestVotifierPlugin implements VotifierPlugin {
    public static final TestVotifierPlugin I = new TestVotifierPlugin();

    public static TestVotifierPlugin getI() {
        return I;
    }

    private final Map<String, Key> keyMap = new HashMap<>();
    private final KeyPair keyPair;

    public TestVotifierPlugin() {
        try {
            keyPair = RSAKeygen.generate(2048); // TODO: Remove this
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        keyMap.put("default", KeyCreator.createKeyFrom("test"));
    }

    @Override
    public Map<String, Key> getTokens() {
        return keyMap;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public String getVersion() {
        return "2.2.1";
    }
}
