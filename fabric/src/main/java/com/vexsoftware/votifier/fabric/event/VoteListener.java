package com.vexsoftware.votifier.fabric.event;

import com.vexsoftware.votifier.model.Vote;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface VoteListener {

    Event<VoteListener> EVENT = EventFactory.createArrayBacked(VoteListener.class, (voteListeners) -> (vote) -> {
        for (VoteListener voteListener : voteListeners) {
            voteListener.onVote(vote);
        }
    });

    void onVote(Vote vote);

}
