package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;

public class V3Vote extends V3Message {

    private final Vote v;
    private final JsonElement voteId;

    public V3Vote(Vote v, JsonElement voteId) {
        super("vote");
        this.v = v;
        this.voteId = voteId;
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = super.serialize();
        o.add("vote", v.serialize());
        o.add("id", voteId);
        return o;
    }

    public static V3Vote decode(JsonObject o) {
        return new V3Vote(new Vote(o.get("vote").getAsJsonObject()), o.get("voteId"));
    }
}
