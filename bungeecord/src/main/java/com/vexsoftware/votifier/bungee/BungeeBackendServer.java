package com.vexsoftware.votifier.bungee;

import com.google.common.base.Preconditions;
import com.vexsoftware.votifier.platform.BackendServer;
import net.md_5.bungee.api.config.ServerInfo;

public class BungeeBackendServer implements BackendServer {
    private final ServerInfo info;

    public BungeeBackendServer(ServerInfo info) {
        this.info = Preconditions.checkNotNull(info, "info");
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public boolean sendPluginMessage(String channel, byte[] data) {
        return info.sendData(channel, data, false);
    }

    @Override
    public String toString() {
        return info.getName();
    }
}
