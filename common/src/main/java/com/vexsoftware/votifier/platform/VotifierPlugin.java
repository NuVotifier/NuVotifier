package com.vexsoftware.votifier.platform;

import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface VotifierPlugin {
    AttributeKey<VotifierPlugin> KEY = AttributeKey.valueOf("votifier_plugin");

    Map<String, Key> getTokens();

    KeyPair getProtocolV1Key();

    String getVersion();

    Logger getPluginLogger();

    VotifierScheduler getScheduler();

    default boolean isDebug() {
        return false;
    }
}
