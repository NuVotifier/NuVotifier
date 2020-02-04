package io.ibj.nuvotifier.multiplatform;

public interface BackendServer {
    String getName();

    boolean sendPluginMessage(String channel, byte[] data);
}
