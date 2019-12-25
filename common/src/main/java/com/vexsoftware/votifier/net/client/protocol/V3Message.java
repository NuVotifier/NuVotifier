package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonObject;

public class V3Message {

    private final String type;

    public V3Message(String type) {
        this.type = type;
    }

    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        return o;
    }
}
