package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;

public class PluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final VotifierPlugin plugin;
    private final ChannelIdentifier velocityChannelId;

    public PluginMessagingForwardingSource(String channel, ServerFilter serverFilter, VotifierPlugin plugin, VoteCache cache, int dumpRate) {
        super(channel, serverFilter, plugin, cache, dumpRate);
        this.plugin = plugin;
        this.velocityChannelId = VelocityUtil.getId(channel);
        plugin.getServer().getChannelRegistrar().register(velocityChannelId);
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        onServerConnect(new VelocityBackendServer(plugin.getServer(), e.getServer()));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getIdentifier().equals(velocityChannelId)) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
