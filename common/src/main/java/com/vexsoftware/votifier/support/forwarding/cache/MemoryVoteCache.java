package com.vexsoftware.votifier.support.forwarding.cache;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class MemoryVoteCache implements VoteCache {

    private final LoggingAdapter l;
    private final long voteTTL;

    protected final Multimap<String, Vote> voteCache;
    protected final Multimap<String, Vote> playerVoteCache;

    protected final ReentrantLock cacheLock = new ReentrantLock();

    private final ScheduledVotifierTask sweepTask;

    public MemoryVoteCache(int initialSize, VotifierPlugin p, long voteTTL) {
        voteCache = HashMultimap.create(initialSize, 10); // 10 is a complete guess. I have no idea .-.
        playerVoteCache = HashMultimap.create();

        this.voteTTL = voteTTL;

        this.l = p.getPluginLogger();
        this.sweepTask = p.getScheduler().repeatOnPool(this::sweep, 12, 12, TimeUnit.HOURS);
    }

    @Override
    public Collection<String> getCachedServers() {
        cacheLock.lock();
        try {
            return ImmutableList.copyOf(voteCache.keySet());
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCache(Vote v, String server) {
        if (server == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            voteCache.put(server, v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCachePlayer(Vote v, String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            playerVoteCache.put(player, v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evictPlayer(String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            return new HashSet<>(playerVoteCache.removeAll(player));
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evict(String server) {
        if (server == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            return new HashSet<>(voteCache.removeAll(server));
        } finally {
            cacheLock.unlock();
        }
    }

    public void sweep() {
        cacheLock.lock();
        try {
            sweep(voteCache);
            sweep(playerVoteCache);
        } finally {
            cacheLock.unlock();
        }
    }

    private void sweep(Multimap<?, Vote> m) {
        Iterator<Vote> vi = m.values().iterator();
        while (vi.hasNext()) {
            Vote v = vi.next();
            if (hasTimedOut(v)) {
                l.warn("Purging out of date vote.", v);
                vi.remove();
            }
        }
    }

    protected boolean hasTimedOut(Vote v) {
        if (voteTTL == -1) return false;
        // scale voteTTL to milliseconds
        return v.getLocalTimestamp() + voteTTL * 24 * 60 * 60 * 1000 < System.currentTimeMillis();
    }
}
