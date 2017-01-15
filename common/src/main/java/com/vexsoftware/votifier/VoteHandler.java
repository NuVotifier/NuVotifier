package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.Channel;

public interface VoteHandler {
    void onVoteReceived(Channel channel, Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception;

    void onError(Channel channel, Throwable throwable);
}
