package com.vexsoftware.votifier.bungee.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.bungee.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * @author Joseph Hirschfeld
 * @date 12/31/2015
 */
public final class OnlineForwardPluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource implements Listener {
    public OnlineForwardPluginMessagingForwardingSource(String channel, NuVotifier nuVotifier, VoteCache cache, String fallbackServer) {
        super(channel, nuVotifier, cache);
        this.fallbackServer = fallbackServer;
        ProxyServer.getInstance().getPluginManager().registerListener(nuVotifier, this);
    }

    private final String fallbackServer;

    @Override
    public void forward(Vote v) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(v.getUsername());
        if (p == null || !forwardSpecific(p.getServer().getInfo(), v)) {
            ServerInfo serverInfo = ProxyServer.getInstance().getServers().get(fallbackServer);

            // nowhere to fall back to, yet still not online. lets save this vote yet!
            if (serverInfo == null)
                attempToAddToPlayerCache(v, v.getUsername());

            else if (!forwardSpecific(serverInfo, v))
                attemptToAddToCache(v, fallbackServer);
        }
    }

    // The below is to allow us to share code since the event handlers from the parent class don't get
    // picked up.
    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equals(channel)) e.setCancelled(true);
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        handleServerConnected(e);
    }

    @EventHandler
    public void onPlayerSwitch(ServerConnectedEvent e) {
        handlePlayerSwitch(e);
    }
}
