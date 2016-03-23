package com.vexsoftware.votifier.util.standalone;

import com.vexsoftware.votifier.model.Vote;

public interface VoteReceiver {
    void onVote(Vote vote) throws Exception;
    void onException(Throwable throwable);
}
