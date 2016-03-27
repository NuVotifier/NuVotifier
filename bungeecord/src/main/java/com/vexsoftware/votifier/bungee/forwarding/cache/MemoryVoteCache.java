package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.google.common.collect.ImmutableSet;
import com.vexsoftware.votifier.model.Vote;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class MemoryVoteCache implements VoteCache {

    public MemoryVoteCache(int initialSize) {
        voteCache = new HashMap<>(initialSize);
    }

    protected final Map<String, Collection<Vote>> voteCache;

    protected final ReentrantLock cacheLock = new ReentrantLock();

    @Override
    public Collection<String> getCachedServers() {
        cacheLock.lock();
        try {
            return ImmutableSet.copyOf(voteCache.keySet());
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCache(Vote v, String server) {
        cacheLock.lock();
        try {
            Collection<Vote> voteCollection = voteCache.get(server);
            if (voteCollection == null) {
                voteCollection = new LinkedHashSet<>();
                voteCache.put(server, voteCollection);
            }
            voteCollection.add(v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evict(String server) {
        cacheLock.lock();
        try {
            Collection<Vote> fromCollection = voteCache.remove(server);
            return fromCollection != null ? ImmutableSet.copyOf(fromCollection) : ImmutableSet.<Vote>of();
        } finally {
            cacheLock.unlock();
        }
    }
}
