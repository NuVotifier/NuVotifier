package io.ibj.nuvotifier.model;

import com.vexsoftware.votifier.model.Vote;

/**
 * This is the NuVote object. It extends the old 'Vote' object. The old 'Vote' object is frozen - i.e. no changes will be
 * made to it. API additions to Vote happen in this file.
 */
public class NuVote extends Vote {
    private NuVote(String serviceName, String username, String address, String timeStamp) {
        super(serviceName, username, address, timeStamp);
    }

    public static NuVote createLegacy(String serviceName, String username, String address, String timeStamp) {
        return new NuVote(serviceName, username, address, timeStamp);
    }
}
