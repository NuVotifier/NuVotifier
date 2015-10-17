package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;

public interface VoteHandler {
    void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception;
    void onError(Throwable throwable);
}
