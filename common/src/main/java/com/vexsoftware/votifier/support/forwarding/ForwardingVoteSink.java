package com.vexsoftware.votifier.support.forwarding;

/**
 * Represents a method at which to receive forwarded votes.
 */
public interface ForwardingVoteSink {

    /**
     * Stop or close any oustanding network interfaces. Occurs on the onDisable method of a plugin.
     */
    void halt();

}
