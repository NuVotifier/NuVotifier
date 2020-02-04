package io.ibj.nuvotifier.platform;

import io.ibj.nuvotifier.model.NuVote;

public interface VoteHandler {
    void onVote(NuVote vote);

    default void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        throw new RuntimeException("Unimplemented onError handler");
    }

}
