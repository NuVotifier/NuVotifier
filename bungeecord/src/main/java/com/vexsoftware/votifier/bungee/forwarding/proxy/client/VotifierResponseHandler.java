package com.vexsoftware.votifier.bungee.forwarding.proxy.client;

public interface VotifierResponseHandler {
    void onSuccess();

    void onFailure(Throwable error);
}
