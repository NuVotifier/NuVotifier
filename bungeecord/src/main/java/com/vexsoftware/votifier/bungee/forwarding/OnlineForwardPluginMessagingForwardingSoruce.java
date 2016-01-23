package com.vexsoftware.votifier.bungee.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.bungee.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * @author Joseph Hirschfeld
 * @date 12/31/2015
 */
public final class OnlineForwardPluginMessagingForwardingSoruce extends PluginMessagingForwardingSource {
    public OnlineForwardPluginMessagingForwardingSoruce(String channel, NuVotifier nuVotifier, VoteCache cache, String fallbackServer) {
        super(channel, nuVotifier, cache);
        this.fallbackServer = fallbackServer;
    }

    private final String fallbackServer;

    @Override
    public void forward(Vote v) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(v.getUsername());
        if (p == null || !forwardSpecific(p.getServer().getInfo(), v)) {
            ServerInfo serverInfo = ProxyServer.getInstance().getServers().get(fallbackServer);
            if (serverInfo == null || !forwardSpecific(serverInfo, v)) {
                attemptToAddToCache(v, fallbackServer);
            }
        }
    }
}
