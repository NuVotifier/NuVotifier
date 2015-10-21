package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.vexsoftware.votifier.model.Vote;

import java.util.Collection;

/**
 * Represents a method of caching votes for forwarding to a server that it was not previously capable of sending.
 */
public interface VoteCache {

    /**
     * Returns the names of all server in which the cache is holding votes for
     *
     * @return Collection of all cached servers
     */
    Collection<String> getCachedServers();

    /**
     * Adds a vote to the vote cache for later sending
     *
     * @param v      Vote to add to cache
     * @param server Server to add vote under
     */
    void addToCache(Vote v, String server);

    /**
     * Evicts all votes from the vote cache and returns a collection of vote objects
     *
     * @param server Server name of which to evict the votes from the cache
     * @return A collection of all votes which were previously in the cache under the server
     */
    Collection<Vote> evict(String server);

    /**
     * Returns whether the cache has votes present within it ready for evicting
     *
     * @param server Server name of server to check
     * @return Whether the passed server has votes ready for eviction
     */
    boolean hasVotes(String server);
}
