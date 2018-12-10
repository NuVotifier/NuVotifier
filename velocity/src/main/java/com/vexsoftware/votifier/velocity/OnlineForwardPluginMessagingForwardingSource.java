package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;

import java.util.Optional;

public final class OnlineForwardPluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final String fallbackServer;
    private final VotifierPlugin plugin;
    private final ChannelIdentifier velocityChannelId;

    public OnlineForwardPluginMessagingForwardingSource(String channel, ServerFilter serverFilter, VotifierPlugin plugin, VoteCache cache, String fallbackServer, int dumpRate) {
        super(channel, serverFilter, plugin, cache, dumpRate);
        this.fallbackServer = fallbackServer;
        this.plugin = plugin;
        this.velocityChannelId = VelocityUtil.getId(channel);
        plugin.getServer().getChannelRegistrar().register(velocityChannelId);
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void forward(Vote v) {
        Optional<Player> p = plugin.getServer().getPlayer(v.getUsername());
        Optional<ServerConnection> sc = p.flatMap(Player::getCurrentServer);
        if (sc.isPresent() &&
                serverFilter.isAllowed(sc.get().getServerInfo().getName())
        ) {
            if (forwardSpecific(new VelocityBackendServer(plugin.getServer(), sc.get().getServer()), v)) {
                return;
            }
        }

        Optional<RegisteredServer> fs = plugin.getServer().getServer(fallbackServer);
        // nowhere to fall back to, yet still not online. lets save this vote yet!
        if (!fs.isPresent())
            attemptToAddToPlayerCache(v, v.getUsername());
        else if (!forwardSpecific(new VelocityBackendServer(plugin.getServer(), fs.get()), v))
            attemptToAddToCache(v, fallbackServer);
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        BackendServer server = new VelocityBackendServer(plugin.getServer(), e.getServer());
        onServerConnect(server);
        handlePlayerSwitch(server, e.getPlayer().getUsername());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getIdentifier().equals(velocityChannelId)) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
