package com.vexsoftware.votifier.support.forwarding.cache;

import com.vexsoftware.votifier.model.Vote;

import java.util.Collection;
import java.util.Collections;

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
     * Adds a vote to the vote cache for later sending, specifically keyed to a player. This is for the instance when the
     * player is rewarded, however is not online any servers able to recieve said vote.
     *
     * @param v      Vote to add to the cache
     * @param player Server to add vote under
     */
    default void addToCachePlayer(Vote v, String player) {
    }

    /**
     * Evicts all votes from the vote cache and returns a collection of vote objects
     *
     * @param server Server name of which to evict the votes from the cache
     * @return A collection of all votes which were previously in the cache under the server
     */
    Collection<Vote> evict(String server);

    /**
     * Evicts all votes from the vote cache for a specific player not assigned to a server and returns a collection of
     * vote objects
     *
     * @param player Player name of the votes to evict from the cache
     * @return A collection of all votes which were previously in the cache under the player
     */
    default Collection<Vote> evictPlayer(String player) {
        return Collections.EMPTY_LIST;
    }
}
