package com.vexsoftware.votifier.util.standalone;

import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.util.KeyCreator;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VotifierServerBuilder {
    private final Map<String, Key> keyMap = new HashMap<>();
    private KeyPair v1Key;
    private InetSocketAddress bind;
    private VoteReceiver receiver;

    public VotifierServerBuilder addToken(String service, String token) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(token, "key");

        keyMap.put(service, KeyCreator.createKeyFrom(token));
        return this;
    }

    public VotifierServerBuilder v1Key(KeyPair v1Key) {
        this.v1Key = Objects.requireNonNull(v1Key, "v1Key");
        if (!(v1Key.getPrivate() instanceof RSAKey)) {
            throw new IllegalArgumentException("Provided key is not an RSA key.");
        }
        return this;
    }

    public VotifierServerBuilder v1KeyFolder(File file) throws Exception {
        this.v1Key = RSAIO.load(Objects.requireNonNull(file, "file"));
        return this;
    }

    public VotifierServerBuilder bind(InetSocketAddress bind) {
        this.bind = Objects.requireNonNull(bind, "bind");
        return this;
    }

    public VotifierServerBuilder receiver(VoteReceiver receiver) {
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        return this;
    }

    public StandaloneVotifierPlugin create() {
        Objects.requireNonNull(bind, "need an address to bind to");
        Objects.requireNonNull(receiver, "need a receiver for votes");
        return new StandaloneVotifierPlugin(keyMap, receiver, v1Key, bind);
    }
}
