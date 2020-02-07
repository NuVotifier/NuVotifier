package com.vexsoftware.votifier.bungee.events;

import io.ibj.nuvotifier.model.NuVote;
import net.md_5.bungee.api.plugin.Event;

/**
 * {@code VotifierEvent} is a custom BungeeCord event class that is sent
 * asynchronously allowing other plugins to listen for votes.
 */
public class VotifierEvent extends Event {
    /**
     * The vote.
     **/
    private final NuVote vote;

    public VotifierEvent(NuVote vote) {
        this.vote = vote;
    }

    public NuVote getVote() {
        return vote;
    }
}
