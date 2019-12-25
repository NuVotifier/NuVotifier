package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonObject;

public class V3Error extends V3Message {

    private final String error;
    private final boolean goodbye;

    public V3Error(String error, boolean goodbye) {
        super("error");
        this.error = error;
        this.goodbye = goodbye;
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = super.serialize();
        o.addProperty("error", error);
        o.addProperty("goodbye", goodbye);
        return o;
    }

    public static V3Error decode(JsonObject o) {
        return new V3Error(o.get("error").getAsString(), o.get("goodbye").getAsBoolean());
    }
}
