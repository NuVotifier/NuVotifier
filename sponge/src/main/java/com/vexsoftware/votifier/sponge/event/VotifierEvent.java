package com.vexsoftware.votifier.sponge.event;

import com.vexsoftware.votifier.model.Vote;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class VotifierEvent extends AbstractEvent {
    private final Vote vote;
    private final Cause cause;

    public VotifierEvent(Vote vote, Cause cause) {
        this.vote = vote;
        this.cause = cause;
    }

    @Override
    public Cause getCause() {
        return cause;
    }
}
