package com.vexsoftware.votifier.velocity.event;

import com.vexsoftware.votifier.model.Vote;
import io.ibj.nuvotifier.model.NuVote;

public class VotifierEvent {
    private final NuVote vote;

    public VotifierEvent(NuVote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }

    @Override
    public String toString() {
        return "VotifierEvent{" +
                "vote=" + vote +
                '}';
    }
}
