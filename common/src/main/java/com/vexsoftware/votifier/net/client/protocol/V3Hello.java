package com.vexsoftware.votifier.net.client.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class V3Hello extends V3Message {
    private final String token;
    private final int protocolVersion;
    private final List<String> capabilities;

    public V3Hello(String token, int protocolVersion, List<String> capabilities) {
        super("hello");
        this.token = token;
        this.protocolVersion = protocolVersion;
        this.capabilities = capabilities;
    }

    public String getToken() {
        return token;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = super.serialize();
        o.addProperty("token", token);
        JsonArray a = new JsonArray();
        for (String c : capabilities)
            a.add(c);
        o.add("capabilities", a);
        o.addProperty("protocolVersion", protocolVersion);
        return o;
    }
}
