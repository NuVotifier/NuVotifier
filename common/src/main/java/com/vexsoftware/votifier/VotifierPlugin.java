package com.vexsoftware.votifier;

import io.netty.util.AttributeKey;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

public interface VotifierPlugin {
    AttributeKey<VotifierPlugin> KEY = AttributeKey.valueOf("votifier_plugin");

    Map<String, Key> getTokens();

    KeyPair getProtocolV1Key();

    String getVersion();
}
