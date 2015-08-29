package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.model.Vote;

public interface VoteHandler {
    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception;
    void onError(Throwable throwable);
}
