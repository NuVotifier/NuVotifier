package com.vexsoftware.votifier.bungee.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.bungee.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.bungee.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class BungeePluginMessagingForwardingSource implements ForwardingVoteSource, Listener {

    public BungeePluginMessagingForwardingSource(String channel, NuVotifier nuVotifier, VoteCache cache) {
        ProxyServer.getInstance().registerChannel(channel);
        ProxyServer.getInstance().getPluginManager().registerListener(nuVotifier, this);
        this.channel = channel;
        this.nuVotifier = nuVotifier;
        this.cache = cache;
    }

    private final NuVotifier nuVotifier;
    private final String channel;
    private final VoteCache cache;

    @Override
    public void forward(Vote v) {
        byte[] rawData = v.serialize().toString().getBytes(StandardCharsets.UTF_8);
        for (ServerInfo s : ProxyServer.getInstance().getServers().values()) {
            if (!forwardSpecific(s, rawData)) {
                if (cache != null) {
                    cache.addToCache(v, s.getName());
                    if (nuVotifier.isDebug())
                        nuVotifier.getLogger().info("Added to forwarding cache: " + v.toString() + " -> " + s.getName());
                } else if (nuVotifier.isDebug())
                    nuVotifier.getLogger().severe("Could not immediately send vote to backend, vote lost! " + v.toString() + " -> " + s.getName());
            }
        }
    }

    protected boolean forwardSpecific(ServerInfo connection, Vote vote) {
        byte[] rawData = vote.serialize().toString().getBytes(StandardCharsets.UTF_8);
        return connection.sendData(channel, rawData, false);
    }

    private boolean forwardSpecific(ServerInfo connection, byte[] data) {
        return connection.sendData(channel, data, false);
    }

    @Override
    public void halt() {
        ProxyServer.getInstance().unregisterChannel(channel);

        if (cache != null && cache instanceof FileVoteCache) {
            try {
                ((FileVoteCache) cache).halt();
            } catch (IOException e) {
                nuVotifier.getLogger().log(Level.SEVERE, "Unable to save cached votes, votes will be lost.", e);
            }
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equals(channel)) e.setCancelled(true);
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent e) { //Attempt to resend any votes that were previously cached.
        if (cache == null) return;
        final String serverName = e.getServer().getInfo().getName();
        final Collection<Vote> cachedVotes = cache.evict(serverName);
        if (!cachedVotes.isEmpty()) {
            nuVotifier.getProxy().getScheduler().schedule(nuVotifier, new Runnable() {
                @Override
                public void run() {
                    int evicted = 0;
                    int unsuccessfulEvictions = 0;
                    for (Vote v : cachedVotes) {
                        if (forwardSpecific(e.getServer().getInfo(), v)) {
                            evicted++;
                        } else {
                            // Re-add to cache to send later.
                            cache.addToCache(v, serverName);
                            unsuccessfulEvictions++;
                        }
                    }
                    if (nuVotifier.isDebug()) {
                        nuVotifier.getLogger().info("Successfully evicted " + evicted + " votes to server '" + serverName + "'.");
                        if (unsuccessfulEvictions > 0) {
                            nuVotifier.getLogger().info("Held " + unsuccessfulEvictions + " votes for server '" + serverName + "'.");
                        }
                    }
                }
            }, 1, TimeUnit.SECONDS);
        }
    }
}
