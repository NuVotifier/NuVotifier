package com.vexsoftware.votifier.platform;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import io.netty.util.AttributeKey;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

public interface VotifierPlugin extends VoteHandler {
    AttributeKey<VotifierPlugin> KEY = AttributeKey.valueOf("votifier_plugin");

    Map<String, Key> getTokens();

    KeyPair getProtocolV1Key();

    LoggingAdapter getPluginLogger();

    VotifierScheduler getScheduler();

    default boolean isDebug() {
        return false;
    }
}
