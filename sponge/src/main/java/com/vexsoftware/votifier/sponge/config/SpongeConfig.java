package com.vexsoftware.votifier.sponge.config;

import com.vexsoftware.votifier.util.TokenUtil;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;

import java.util.Collections;
import java.util.Map;

@ConfigSerializable
public class SpongeConfig {

    @Setting(comment = "The IP to listen to. Use 0.0.0.0 if you wish to listen to all interfaces on your server. (All IP addresses)\n" +
            "This defaults to the IP you have configured your server to listen on, or 0.0.0.0 if you have not configured this.")
    public String host = Sponge.getServer().getBoundAddress().isPresent() ? Sponge.getServer().getBoundAddress().get().getAddress().getHostAddress() : "0.0.0.0";

    @Setting(comment = "Port to listen for new votes on")
    public int port = 8192;

    @Setting(comment = "Whether or not to print debug messages. In a production system, this should be set to false.\n" +
            "This is useful when initially setting up NuVotifier to ensure votes are being delivered.")
    public boolean debug = true;

    @Setting( value = "disable-v1-protocol", comment = "Setting this option to true will disable handling of Protocol v1 packets. While the old protocol is not secure, this\n" +
            "option is currently not recommended as most voting sites only support the old protocol at present. However, if you are\n" +
            "using NuVotifier's proxy forwarding mechanism, enabling this option will increase your server's security.")
    public boolean disableV1Protocol = false;

    @Setting(comment = "All tokens, labeled by the serviceName of each server list.\n" +
            "Default token for all server lists, if another isn't supplied.")
    public Map<String, String> tokens = Collections.singletonMap("default", TokenUtil.newToken());

    @Setting(comment = "Configuration section for all vote forwarding to NuVotifier")
    public Forwarding forwarding = new Forwarding();

    @ConfigSerializable
    public static class Forwarding {

        @Setting(comment = "Sets whether to set up a remote method for fowarding. Supported methods:\n" +
                "- none - Does not set up a forwarding method.\n" +
                "- pluginMessaging - Sets up plugin messaging")
        public String method = "none";

        @Setting
        public PluginMessaging pluginMessaging = new PluginMessaging();

        @ConfigSerializable
        public static class PluginMessaging {

            @Setting
            public String channel = "nuvotifier:votes";
        }
    }
}
