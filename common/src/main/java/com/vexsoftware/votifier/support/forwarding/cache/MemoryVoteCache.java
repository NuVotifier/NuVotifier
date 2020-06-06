package com.vexsoftware.votifier.support.forwarding.cache;

import com.google.gson.JsonObject;
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

    protected final Map<String, Collection<VoteWithRecordedTimestamp>> voteCache;
    protected final Map<String, Collection<VoteWithRecordedTimestamp>> playerVoteCache;

    protected final ReentrantLock cacheLock = new ReentrantLock();

    private final ScheduledVotifierTask sweepTask;

    public MemoryVoteCache(VotifierPlugin p, long voteTTL) {
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
            voteCache.computeIfAbsent(server, k -> new HashSet<>()).add(new VoteWithRecordedTimestamp(v));
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCachePlayer(Vote v, String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            playerVoteCache.computeIfAbsent(player, k -> new HashSet<>()).add(new VoteWithRecordedTimestamp(v));
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evictPlayer(String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<VoteWithRecordedTimestamp> playerVotes = playerVoteCache.remove(player);
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
            Collection<VoteWithRecordedTimestamp> serverVotes = voteCache.remove(server);
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

    private void sweep(Map<String, Collection<VoteWithRecordedTimestamp>> m) {
        for (Map.Entry<String, Collection<VoteWithRecordedTimestamp>> entry : m.entrySet()) {
            Iterator<VoteWithRecordedTimestamp> vi = entry.getValue().iterator();
            while (vi.hasNext()) {
                VoteWithRecordedTimestamp v = vi.next();
                if (hasTimedOut(v)) {
                    l.warn("Purging out of date vote.", v);
                    vi.remove();
                }
            }
        }
    }

    protected boolean hasTimedOut(VoteWithRecordedTimestamp v) {
        if (voteTTL == -1) return false;

        long daysAsMillis = TimeUnit.DAYS.toMillis(voteTTL);
        return v.recorded + daysAsMillis < System.currentTimeMillis();
    }

    static class VoteWithRecordedTimestamp extends Vote {
        private final long recorded;

        VoteWithRecordedTimestamp(Vote vote) {
            super(vote);
            this.recorded = System.currentTimeMillis();
        }

        VoteWithRecordedTimestamp(JsonObject object) {
            super(object);
            if (object.has("recorded")) {
                this.recorded = object.get("recorded").getAsLong();
            } else {
                // Assign the current time, in the hope that the stale votes will eventually get purged.
                this.recorded = System.currentTimeMillis();
            }
        }

        @Override
        public JsonObject serialize() {
            JsonObject object = super.serialize();
            object.addProperty("recorded", this.recorded);
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            VoteWithRecordedTimestamp that = (VoteWithRecordedTimestamp) o;

            return recorded == that.recorded;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (int) (recorded ^ (recorded >>> 32));
            return result;
        }
    }
}
