package com.vexsoftware.votifier.bungee.forwarding;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.bungee.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.bungee.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public abstract class AbstractPluginMessagingForwardingSource implements ForwardingVoteSource {

    public AbstractPluginMessagingForwardingSource(String channel, List<String> ignoredServers, NuVotifier nuVotifier, VoteCache cache) {
        ProxyServer.getInstance().registerChannel(channel);
        this.channel = channel;
        this.nuVotifier = nuVotifier;
        this.cache = cache;
        this.ignoredServers = ignoredServers;
    }

    protected AbstractPluginMessagingForwardingSource(String channel, NuVotifier nuVotifier, VoteCache voteCache) {
        this(channel, null, nuVotifier, voteCache);
    }

    protected final NuVotifier nuVotifier;
    protected final String channel;
    protected final VoteCache cache;
    protected final List<String> ignoredServers;

    @Override
    public void forward(Vote v) {
        byte[] rawData = v.serialize().toString().getBytes(StandardCharsets.UTF_8);
        for (ServerInfo s : ProxyServer.getInstance().getServers().values()) {
            if (ignoredServers != null && ignoredServers.contains(s.getName())) continue;
            if (!forwardSpecific(s, rawData)) attemptToAddToCache(v, s.getName());
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

    protected void handleServerConnected(final ServerConnectedEvent e) {
        if (cache == null) return;
        final String serverName = e.getServer().getInfo().getName();
        final Collection<Vote> cachedVotes = cache.evict(serverName);
        final Collection<Vote> failedVotes = dumpVotesToServer(cachedVotes, e.getServer().getInfo(), "server '" + serverName + "'");
        for (Vote v : failedVotes)
            cache.addToCache(v, serverName);
    }

    protected void attemptToAddToCache(Vote v, String server) {
        if (cache != null) {
            cache.addToCache(v, server);
            if (nuVotifier.isDebug())
                nuVotifier.getLogger().info("Added to forwarding cache: " + v + " -> " + server);
        } else if (nuVotifier.isDebug())
            nuVotifier.getLogger().severe("Could not immediately send vote to backend, vote lost! " + v + " -> " + server);
    }

    protected void attempToAddToPlayerCache(Vote v, String player) {
        if (cache != null) {
            cache.addToCachePlayer(v, player);
            if (nuVotifier.isDebug())
                nuVotifier.getLogger().info("Added to forwarding cache: " + v + " -> (player) " + player);
        } else if (nuVotifier.isDebug())
            nuVotifier.getLogger().severe("Could not immediately send vote to backend, vote lost! " + v + " -> (player) " + player);

    }

    // returns a collection of failed votes
    private Collection<Vote> dumpVotesToServer(Collection<Vote> cachedVotes, ServerInfo target, String identifier) {
        List<Vote> failures = new ArrayList<>();

        if (!cachedVotes.isEmpty()) {
            nuVotifier.getProxy().getScheduler().schedule(nuVotifier, new Runnable() {
                @Override
                public void run() {
                    int evicted = 0;
                    int unsuccessfulEvictions = 0;
                    for (Vote v : cachedVotes) {
                        if (forwardSpecific(target, v)) {
                            evicted++;
                        } else {
                            // Re-add to cache to send later.
                            failures.add(v);
                            unsuccessfulEvictions++;
                        }
                    }
                    if (nuVotifier.isDebug()) {
                        nuVotifier.getLogger().info("Successfully evicted " + evicted + " votes to " + identifier + ".");
                        if (unsuccessfulEvictions > 0) {
                            nuVotifier.getLogger().info("Held " + unsuccessfulEvictions + " votes for " + identifier + ".");
                        }
                    }
                }
            }, 1, TimeUnit.SECONDS);
        }
        return failures;
    }

    protected void handlePlayerSwitch(ServerConnectedEvent e) {
        if (cache == null) return;
        if (ignoredServers != null && ignoredServers.contains(e.getServer().getInfo().getName())) return;

        String playerName = e.getPlayer().getName();

        final Collection<Vote> cachedVotes = cache.evictPlayer(playerName);
        final Collection<Vote> failedVotes = dumpVotesToServer(cachedVotes, e.getServer().getInfo(), "player '" + playerName);
        for (Vote v : failedVotes)
            cache.addToCachePlayer(v, playerName);
    }
}
