package com.vexsoftware.votifier.bungee.events;

import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.md_5.bungee.api.plugin.Cancellable;

/**
 * {@code VotifierEvent} is a custom BungeeCord event class that is sent
 * asynchronously allowing other plugins to listen for votes.
 */
public class VotifierEvent extends AsyncEvent<VotifierEvent> implements Cancellable {
    /**
     * The vote.
     **/
    private final Vote vote;

    private boolean cancelled;

    public VotifierEvent(Vote vote, Callback<VotifierEvent> done) {
        super(done);
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
