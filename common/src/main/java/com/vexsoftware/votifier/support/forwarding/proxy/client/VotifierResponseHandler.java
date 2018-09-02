package com.vexsoftware.votifier.support.forwarding.proxy.client;

public interface VotifierResponseHandler {
    void onSuccess();

    void onFailure(Throwable error);
}
