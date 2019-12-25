package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class V3VoteAck extends V3Message {

    private final JsonElement id;

    public V3VoteAck(JsonElement id) {
        super("voteack");
        this.id = id;
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = super.serialize();
        o.add("id", id);
        return o;
    }
}
