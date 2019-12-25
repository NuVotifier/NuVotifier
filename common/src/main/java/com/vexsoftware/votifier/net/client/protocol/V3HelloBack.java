package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class V3HelloBack extends V3Message {

    private final List<String> acceptedCapabilities;

    public V3HelloBack(List<String> acceptedCapabilities) {
        super("helloback");
        this.acceptedCapabilities = acceptedCapabilities;
    }

    public List<String> getAcceptedCapabilities() {
        return acceptedCapabilities;
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = super.serialize();
        JsonArray array = new JsonArray();
        for (String s : acceptedCapabilities) {
            array.add(s);
        }
        o.add("capabilities", array);
        return o;
    }

    public static V3HelloBack decode(JsonObject o) {
        JsonArray a = o.get("capabilities").getAsJsonArray();
        List<String> c = new ArrayList<>();
        for (int i = 0; i < a.size(); i++)
            c.add(a.get(i).getAsString());
        return new V3HelloBack(c);
    }
}
