package com.vexsoftware.votifier.bungee.events;

import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.plugin.Event;

/**
 * {@code VotifierEvent} is a custom BungeeCord event class that is sent
 * asynchronously allowing other plugins to listen for votes.
 */
public class VotifierEvent extends Event {
    /**
     * The vote.
     **/
    private final Vote vote;

    public VotifierEvent(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }
}
