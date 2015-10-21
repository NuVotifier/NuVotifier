package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.vexsoftware.votifier.model.Vote;

import java.util.*;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class MemoryVoteCache implements VoteCache {

    public MemoryVoteCache(int initialSize){
        voteCache = new HashMap<>(initialSize);
    }

    private final Map<String, Collection<Vote>> voteCache;

    @Override
    public Collection<String> getCachedServers() {
        return voteCache.keySet();
    }

    @Override
    public void addToCache(Vote v, String server) {
        Collection<Vote> voteCollection = voteCache.get(server);
        if(voteCollection == null){
            voteCollection = new LinkedHashSet<>();
            voteCache.put(server,voteCollection);
        }
        voteCollection.add(v);
    }

    @Override
    public Collection<Vote> evict(String server) {
        return voteCache.remove(server);
    }

    @Override
    public boolean hasVotes(String server) {
        return voteCache.containsKey(server);
    }
}
