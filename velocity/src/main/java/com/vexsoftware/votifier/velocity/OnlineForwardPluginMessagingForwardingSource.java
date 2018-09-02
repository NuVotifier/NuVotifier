package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MessageHandler;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;

import java.util.Optional;

public final class OnlineForwardPluginMessagingForwardingSource extends AbstractPluginMessagingForwardingSource {

    private final String fallbackServer;
    private final VotifierPlugin plugin;

    public OnlineForwardPluginMessagingForwardingSource(String channel, VotifierPlugin plugin, VoteCache cache, String fallbackServer) {
        super(channel, plugin, cache);
        this.fallbackServer = fallbackServer;
        this.plugin = plugin;
        plugin.getServer().getEventManager().register(plugin, this);
        plugin.getServer().getChannelRegistrar().register((source, side, identifier, data) -> MessageHandler.ForwardStatus.HANDLED, VelocityUtil.getId(channel));
    }

    @Override
    public void forward(Vote v) {
        Optional<Player> p = plugin.getServer().getPlayer(v.getUsername());
        Optional<ServerConnection> sc = p.flatMap(Player::getCurrentServer);
        if (sc.isPresent()) {
            if (forwardSpecific(new VelocityBackendServer(plugin.getServer(), sc.get().getServerInfo()), v)) {
                return;
            }
        }

        Optional<ServerInfo> fs = plugin.getServer().getServerInfo(fallbackServer);
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
}
