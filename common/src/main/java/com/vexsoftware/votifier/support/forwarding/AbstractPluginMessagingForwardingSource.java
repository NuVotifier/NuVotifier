package com.vexsoftware.votifier.support.forwarding;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.ProxyVotifierPlugin;
import com.vexsoftware.votifier.support.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AbstractPluginMessagingForwardingSource implements ForwardingVoteSource {

    public AbstractPluginMessagingForwardingSource(String channel, ServerFilter serverFilter, ProxyVotifierPlugin plugin, VoteCache cache, int dumpRate) {
        this.channel = channel;
        this.plugin = plugin;
        this.cache = cache;
        this.serverFilter = serverFilter;
        this.dumpRate = dumpRate;
    }

    protected AbstractPluginMessagingForwardingSource(String channel, ProxyVotifierPlugin plugin, VoteCache voteCache, int dumpRate) {
        this(channel, null, plugin, voteCache, dumpRate);
    }

    protected final ProxyVotifierPlugin plugin;
    protected final String channel;
    protected final VoteCache cache;
    protected final ServerFilter serverFilter;
    private final int dumpRate;

    @Override
    public void forward(Vote v) {
        byte[] rawData = v.serialize().toString().getBytes(StandardCharsets.UTF_8);
        for (BackendServer server : plugin.getAllBackendServers()) {
            if (!serverFilter.isAllowed(server.getName())) continue;
            if (!forwardSpecific(server, rawData)) attemptToAddToCache(v, server.getName());
        }
    }

    protected boolean forwardSpecific(BackendServer connection, Vote vote) {
        byte[] rawData = vote.serialize().toString().getBytes(StandardCharsets.UTF_8);
        return forwardSpecific(connection, rawData);
    }

    protected boolean forwardSpecific(BackendServer connection, Collection<Vote> votes) {
        StringBuilder data = new StringBuilder();
        for (Vote v : votes) {
            data.append(v.serialize().toString());
        }

        return forwardSpecific(connection, data.toString().getBytes(StandardCharsets.UTF_8));
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
    private void dumpVotesToServer(Collection<Vote> cachedVotes, BackendServer target, String identifier, Consumer<Collection<Vote>> cb) {
        dumpVotesToServer(cachedVotes, target, identifier, 0, cb);
    }
    private void dumpVotesToServer(Collection<Vote> cachedVotes, BackendServer target, String identifier, int evictedAlready, Consumer<Collection<Vote>> cb) {
        if (!cachedVotes.isEmpty()) {
            plugin.getScheduler().delayedOnPool(() -> {
                int evicted = 0;
                Iterator<Vote> vi = cachedVotes.iterator();
                Collection<Vote> chunk = new ArrayList<>(dumpRate);
                while (vi.hasNext() && evicted < dumpRate) {
                    chunk.add(vi.next());
                    vi.remove();
                }

                if (forwardSpecific(target, chunk)) {
                    evicted += chunk.size();

                    if (evicted >= dumpRate && !cachedVotes.isEmpty()) {
                        // if we evicted everything we could but still need to evict more
                        dumpVotesToServer(cachedVotes, target, identifier, evictedAlready + evicted, cb);
                        return;
                    }
                } else {
                    // so since our forwarding failed, break like we are done
                    cachedVotes.addAll(chunk);
                }

                if (plugin.isDebug()) {
                    plugin.getPluginLogger().info("Successfully evicted " + (evictedAlready + evicted) + " votes to " + identifier + ".");
                    if (!cachedVotes.isEmpty()) {
                        plugin.getPluginLogger().info("Held " + cachedVotes.size() + " votes for " + identifier + ".");
                    }
                }

                // cachedVotes contains any votes which have yet to be evicted
                cb.accept(cachedVotes);
            }, 1, TimeUnit.SECONDS);
        } else {
            cb.accept(cachedVotes);
        }
    }

    protected void handlePlayerSwitch(BackendServer server, String playerName) {
        if (cache == null) return;
        if (!serverFilter.isAllowed(server.getName())) return;

        final Collection<Vote> cachedVotes = cache.evictPlayer(playerName);
        dumpVotesToServer(cachedVotes, server, "player '" + playerName + "'", failedVotes -> {
            for (Vote v : failedVotes)
                cache.addToCachePlayer(v, playerName);
        });
    }
}
