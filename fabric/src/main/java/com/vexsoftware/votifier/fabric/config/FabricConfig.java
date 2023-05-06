package com.vexsoftware.votifier.fabric.config;

import com.vexsoftware.votifier.util.TokenUtil;

import java.util.Collections;
import java.util.Map;

public class FabricConfig {

    public String host = "0.0.0.0";

    public int port = 8192;

    public boolean debug = true;

    public boolean disableV1Protocol = false;

    public Map<String, String> tokens = Collections.singletonMap("default", TokenUtil.newToken());

    public Forwarding forwarding = new Forwarding();

    public static class Forwarding {

        public String method = "none";

        public PluginMessaging pluginMessaging = new PluginMessaging();

        public static class PluginMessaging {

            public String channel = "nuvotifier:votes";

        }
    }

}
