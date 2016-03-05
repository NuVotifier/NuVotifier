package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSA;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class VoteUtil {
    public static byte[] encodePOJOv1(Vote vote) throws Exception {
        return encodePOJOv1(vote, TestVotifierPlugin.getI().getProtocolV1Key().getPublic());
    }

    public static byte[] encodePOJOv1(Vote vote, PublicKey key) throws Exception {
        List<String> ordered = new ArrayList<>();
        ordered.add("VOTE");
        ordered.add(vote.getServiceName());
        ordered.add(vote.getUsername());
        ordered.add(vote.getAddress());
        ordered.add(vote.getTimeStamp());

        StringBuilder builder = new StringBuilder();
        for (String s : ordered) {
            builder.append(s).append('\n'); // naive join needed!
        }

        return RSA.encrypt(builder.toString().getBytes(StandardCharsets.UTF_8), key);
    }
}
