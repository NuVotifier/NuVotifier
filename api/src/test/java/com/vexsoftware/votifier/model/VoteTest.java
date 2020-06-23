package com.vexsoftware.votifier.model;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VoteTest {
    @Test
    public void testVoteConstructFromJsonObject() {

        String serviceName = "serviceNameA";
        String username = "usernameA";
        String address = "home";
        String timestamp = "1234";

        JsonObject o = new JsonObject();
        o.addProperty("serviceName", serviceName);
        o.addProperty("username", username);
        o.addProperty("address", address);
        o.addProperty("timestamp", timestamp);

        Vote v = new Vote(o);

        assertEquals(serviceName, v.getServiceName());
        assertEquals(username, v.getUsername());
        assertEquals(address, v.getAddress());
        assertEquals(timestamp, v.getTimeStamp());
    }

    @Test
    public void testVoteConstructFromJsonObjectLongTS() {

        String serviceName = "serviceNameA";
        String username = "usernameA";
        String address = "home";

        JsonObject o = new JsonObject();
        o.addProperty("serviceName", serviceName);
        o.addProperty("username", username);
        o.addProperty("address", address);
        o.addProperty("timestamp", 1234);

        Vote v = new Vote(o);

        assertEquals(serviceName, v.getServiceName());
        assertEquals(username, v.getUsername());
        assertEquals(address, v.getAddress());
        assertEquals("1234", v.getTimeStamp());
    }

    @Test
    public void testVoteSerialize() {
        Vote v = new Vote("service", "username", "address", "1234");
        JsonObject o = v.serialize();

        assertEquals(4, o.size());
        assertEquals("service", o.get("serviceName").getAsString());
        assertEquals("username", o.get("username").getAsString());
        assertEquals("address", o.get("address").getAsString());
        assertEquals("1234", o.get("timestamp").getAsString());
    }

    @Test
    public void testVoteEquals() {
        Vote v1 = new Vote("service", "username", "address", "1234");
        assertEquals(v1, v1);
        assertEquals(v1.hashCode(), new Vote("service", "username", "address", "1234").hashCode());
        assertEquals(v1, new Vote("service", "username", "address", "1234"));
        assertNotEquals(v1, new Object());
        assertNotEquals(v1, null);
        assertNotEquals(v1, new Vote("service2", "username", "address", "1234"));
        assertNotEquals(v1, new Vote("service", "username1", "address", "1234"));
        assertNotEquals(v1, new Vote("service", "username", "address1", "1234"));
        assertNotEquals(v1, new Vote("service", "username", "address", "12345"));
    }
}
