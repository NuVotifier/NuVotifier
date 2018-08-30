package com.vexsoftware.votifier.support.forwarding;

import com.vexsoftware.votifier.model.Vote;

/**
 * Represents a source for forwarding votes through to servers.
 */
public interface ForwardingVoteSource {

    /**
     * Forwards a vote to all servers set up to receive votes.
     *
     * @param v Vote to forward to servers
     */
    void forward(Vote v);

    /**
     * Stop or close any oustanding network interfaces. Occurs on the onDisable method of a plugin.
     */
    void halt();

}
