package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;

import java.util.List;

public class PluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final VotifierPlugin plugin;

    public PluginMessagingForwardingSource(String channel, List<String> ignoredServers, VotifierPlugin plugin, VoteCache cache, int dumpRate) {
        super(channel, ignoredServers, plugin, cache, dumpRate);
        this.plugin = plugin;
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        onServerConnect(new VelocityBackendServer(plugin.getServer(), e.getServer()));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getIdentifier().equals(VelocityUtil.getId(channel))) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
