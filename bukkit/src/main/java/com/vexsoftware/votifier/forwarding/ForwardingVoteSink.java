package com.vexsoftware.votifier.forwarding;

/**
 * Represents a method at which to receive forwarded votes.
 */
public interface ForwardingVoteSink {

    /**
     * Stop or close any oustanding network interfaces. Occurs on the onDisable method of a plugin.
     */
    void halt();

}
