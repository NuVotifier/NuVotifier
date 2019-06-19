package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.Channel;

public interface VoteHandler {

    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) throws Exception;

    default void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        throw new RuntimeException("Unimplemented onError handler");
    }

}
