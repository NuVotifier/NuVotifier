package com.vexsoftware.votifier.model;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class VoteTest {
    @Test
    public void testVoteConstructFromJsonObject() {
        String serviceName = "serviceNameA";
        String username = "usernameA";
        String address = "home";
        String timestamp = "1234";
        Long localTimestamp = 1233L;

        JsonObject o = new JsonObject();
        o.addProperty("serviceName", serviceName);
        o.addProperty("username", username);
        o.addProperty("address", address);
        o.addProperty("timestamp", timestamp);
        o.addProperty("localTimestamp", localTimestamp);

        Vote v = new Vote(o);

        Assertions.assertEquals(serviceName, v.getServiceName());
        Assertions.assertEquals(username, v.getUsername());
        Assertions.assertEquals(address, v.getAddress());
        Assertions.assertEquals(timestamp, v.getTimeStamp());
        Assertions.assertEquals(localTimestamp, v.getLocalTimestamp());
    }
}
