package com.vexsoftware.votifier.platform;

public interface BackendServer {
    String getName();

    boolean sendPluginMessage(String channel, byte[] data);
}
