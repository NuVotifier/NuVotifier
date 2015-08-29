package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteHandler;
import com.vexsoftware.votifier.net.VotifierSession;
import net.md_5.bungee.api.plugin.Plugin;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

/**
 * Created by tux on 4/26/15.
 */
public class Votifier extends Plugin implements VoteHandler, com.vexsoftware.votifier.VotifierPlugin {
    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public Map<String, Key> getTokens() {
        return null;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }
}
