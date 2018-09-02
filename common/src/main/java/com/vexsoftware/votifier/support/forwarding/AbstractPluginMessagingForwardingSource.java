package com.vexsoftware.votifier.support.forwarding;

import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.ProxyVotifierPlugin;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.support.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.model.Vote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class AbstractPluginMessagingForwardingSource implements ForwardingVoteSource {

    public AbstractPluginMessagingForwardingSource(String channel, List<String> ignoredServers, ProxyVotifierPlugin plugin, VoteCache cache) {
        this.channel = channel;
        this.plugin = plugin;
        this.cache = cache;
        this.ignoredServers = ignoredServers;
    }

    protected AbstractPluginMessagingForwardingSource(String channel, ProxyVotifierPlugin plugin, VoteCache voteCache) {
        this(channel, null, plugin, voteCache);
    }

    protected final ProxyVotifierPlugin plugin;
    protected final String channel;
    protected final VoteCache cache;
    protected final List<String> ignoredServers;

    @Override
    public void forward(Vote v) {
        byte[] rawData = v.serialize().toString().getBytes(StandardCharsets.UTF_8);
        for (BackendServer server : plugin.getAllBackendServers()) {
            if (ignoredServers != null && ignoredServers.contains(server.getName())) continue;
            if (!forwardSpecific(server, rawData)) attemptToAddToCache(v, server.getName());
        }
    }

    protected boolean forwardSpecific(BackendServer connection, Vote vote) {
        byte[] rawData = vote.serialize().toString().getBytes(StandardCharsets.UTF_8);
        return connection.sendPluginMessage(channel, rawData);
    }

    private boolean forwardSpecific(BackendServer connection, byte[] data) {
        return connection.sendPluginMessage(channel, data);
    }

    @Override
    public void halt() {
        if (cache instanceof FileVoteCache) {
            try {
                ((FileVoteCache) cache).halt();
            } catch (IOException e) {
                plugin.getPluginLogger().error("Unable to save cached votes, votes will be lost.", e);
            }
        }
    }

    protected void onServerConnect(BackendServer server) {
        if (cache == null) return;
        final Collection<Vote> cachedVotes = cache.evict(server.getName());
        dumpVotesToServer(cachedVotes, server, "server '" + server + "'", failedVotes -> {
            for (Vote v : failedVotes)
                cache.addToCache(v, server.getName());
        });
    }

    protected void attemptToAddToCache(Vote v, String server) {
        if (cache != null) {
            cache.addToCache(v, server);
            if (plugin.isDebug())
                plugin.getPluginLogger().info("Added to forwarding cache: " + v + " -> " + server);
        } else if (plugin.isDebug())
            plugin.getPluginLogger().error("Could not immediately send vote to backend, vote lost! " + v + " -> " + server);
    }

    protected void attemptToAddToPlayerCache(Vote v, String player) {
        if (cache != null) {
            cache.addToCachePlayer(v, player);
            if (plugin.isDebug())
                plugin.getPluginLogger().info("Added to forwarding cache: " + v + " -> (player) " + player);
        } else if (plugin.isDebug())
            plugin.getPluginLogger().error("Could not immediately send vote to backend, vote lost! " + v + " -> (player) " + player);

    }

    // returns a collection of failed votes
    private void dumpVotesToServer(Collection<Vote> cachedVotes, BackendServer target, String identifier, Consumer<List<Vote>> cb) {
        List<Vote> failures = new ArrayList<>();

        if (!cachedVotes.isEmpty()) {
            plugin.getScheduler().delayedOnPool(() -> {
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
                if (plugin.isDebug()) {
                    plugin.getPluginLogger().info("Successfully evicted " + evicted + " votes to " + identifier + ".");
                    if (unsuccessfulEvictions > 0) {
                        plugin.getPluginLogger().info("Held " + unsuccessfulEvictions + " votes for " + identifier + ".");
                    }
                }
                cb.accept(failures);
            }, 3, TimeUnit.SECONDS);
        } else {
            cb.accept(failures);
        }
    }

    protected void handlePlayerSwitch(BackendServer server, String playerName) {
        if (cache == null) return;
        if (ignoredServers != null && ignoredServers.contains(server.getName())) return;

        final Collection<Vote> cachedVotes = cache.evictPlayer(playerName);
        dumpVotesToServer(cachedVotes, server, "player '" + playerName + "'", failedVotes -> {
            for (Vote v : failedVotes)
                cache.addToCachePlayer(v, playerName);
        });
    }
}
