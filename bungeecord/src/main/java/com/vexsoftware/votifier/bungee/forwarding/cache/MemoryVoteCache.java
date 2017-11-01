package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.google.common.collect.ImmutableList;
import com.vexsoftware.votifier.model.Vote;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class MemoryVoteCache implements VoteCache {

    public MemoryVoteCache(int initialSize) {
        voteCache = new HashMap<>(initialSize);
        this.playerVoteCache = new HashMap<>();
    }

    protected final Map<String, Collection<Vote>> voteCache;

    protected final Map<String, Collection<Vote>> playerVoteCache;

    protected final ReentrantLock cacheLock = new ReentrantLock();

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
            Collection<Vote> voteCollection = voteCache.get(server);
            if (voteCollection == null) {
                voteCollection = new ArrayList<>();
                voteCache.put(server, voteCollection);
            }
            voteCollection.add(v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void addToCachePlayer(Vote v, String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<Vote> voteCollection = playerVoteCache.get(player);
            if (voteCollection == null) {
                voteCollection = new ArrayList<>();
                playerVoteCache.put(player, voteCollection);
            }
            voteCollection.add(v);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evictPlayer(String player) {
        if (player == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<Vote> fromCollection = playerVoteCache.remove(player);
            return fromCollection != null ? fromCollection : ImmutableList.of();
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Collection<Vote> evict(String server) {
        if (server == null) throw new NullPointerException();
        cacheLock.lock();
        try {
            Collection<Vote> fromCollection = voteCache.remove(server);
            return fromCollection != null ? fromCollection : ImmutableList.of();
        } finally {
            cacheLock.unlock();
        }
    }
}
