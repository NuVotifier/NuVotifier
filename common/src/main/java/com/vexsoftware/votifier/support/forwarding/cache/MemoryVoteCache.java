package com.vexsoftware.votifier.support.forwarding.cache;

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

    protected final Map<String, Collection<Vote>> voteCache;
    protected final Map<String, Collection<Vote>> playerVoteCache;

    protected final ReentrantLock cacheLock = new ReentrantLock();

    private final ScheduledVotifierTask sweepTask;

    public MemoryVoteCache(int initialSize, VotifierPlugin p, long voteTTL) {
        voteCache = new HashMap<>();
        playerVoteCache = new HashMap<>();

        this.voteTTL = voteTTL;

        this.l = p.getPluginLogger();
        this.sweepTask = p.getScheduler().repeatOnPool(this::sweep, 12, 12, TimeUnit.HOURS);
    }

    @Override
    public Collection<String> getCachedServers() {
        cacheLock.lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(voteCache.keySet()));
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCache(Vote v, String server) {
        if (server == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            voteCache.computeIfAbsent(server, k -> new HashSet<>()).add(v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCachePlayer(Vote v, String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            playerVoteCache.computeIfAbsent(player, k -> new HashSet<>()).add(v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evictPlayer(String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<Vote> playerVotes = playerVoteCache.remove(player);
            if (playerVotes != null) {
                return new HashSet<>(playerVotes);
            } else {
                return Collections.emptySet();
            }
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evict(String server) {
        if (server == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<Vote> serverVotes = voteCache.remove(server);
            if (serverVotes != null) {
                return new HashSet<>(serverVotes);
            } else {
                return Collections.emptySet();
            }
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

    private void sweep(Map<String, Collection<Vote>> m) {
        for (Map.Entry<String, Collection<Vote>> entry : m.entrySet()) {
            Iterator<Vote> vi = entry.getValue().iterator();
            while (vi.hasNext()) {
                Vote v = vi.next();
                if (hasTimedOut(v)) {
                    l.warn("Purging out of date vote.", v);
                    vi.remove();
                }
            }
        }
    }

    protected boolean hasTimedOut(Vote v) {
        if (voteTTL == -1) return false;
        // TODO: FIX FOR 3.0 RELEASE
        // scale voteTTL to milliseconds
        return false;
        //return v.getLocalTimestamp() + voteTTL * 24 * 60 * 60 * 1000 < System.currentTimeMillis();
    }
}
