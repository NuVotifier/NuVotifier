package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.vexsoftware.votifier.platform.BackendServer;

import java.util.Optional;

class VelocityBackendServer implements BackendServer {
    private final ProxyServer server;
    private final ServerInfo info;

    VelocityBackendServer(ProxyServer server, ServerInfo info) {
        this.server = server;
        this.info = info;
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public boolean sendPluginMessage(String channel, byte[] data) {
        Optional<ServerConnection> connection = server.getAllPlayers().stream()
                .map(p -> p.getCurrentServer().filter(s -> s.getServerInfo().equals(info)))
                .filter(Optional::isPresent)
                .findAny()
                .flatMap(o -> o);
        if (connection.isPresent()) {
            connection.get().sendPluginMessage(VelocityUtil.getId(channel), data);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return info.getName();
    }
}
