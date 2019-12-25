package com.vexsoftware.votifier.net.client;

import com.vexsoftware.votifier.net.client.protocol.V3Error;
import com.vexsoftware.votifier.net.client.protocol.V3Vote;

public interface Votifier3Hook {
    void onError(Throwable t);

    void onHangup();

    void onRemoteError(V3Error e);

    void onVote(V3Vote v);
}
