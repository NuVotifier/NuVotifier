package com.vexsoftware.votifier.velocity.event;

import com.velocitypowered.api.event.ResultedEvent;
import com.vexsoftware.votifier.model.Vote;

public class VotifierEvent implements ResultedEvent<ResultedEvent.GenericResult> {
    private final Vote vote;
    private GenericResult result;

    public VotifierEvent(Vote vote) {
        this.vote = vote;
        this.result = GenericResult.allowed();
    }

    public Vote getVote() {
        return vote;
    }

    @Override
    public GenericResult getResult() {
        return this.result;
    }

    @Override
    public void setResult(GenericResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "VotifierEvent{" +
                "vote=" + vote +
                '}';
    }
}
