package com.vexsoftware.votifier.sponge.config;

import com.google.common.reflect.TypeToken;
import com.vexsoftware.votifier.sponge.NuVotifier;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {

    private static SpongeConfig spongeConfig;

    public static void loadConfig(NuVotifier pl) {
        if (!pl.getConfigDir().exists()) {
            if (!pl.getConfigDir().mkdirs()) {
                throw new RuntimeException("Unable to create the plugin data folder " + pl.getConfigDir());
            }
        }
        try {
            File config = new File(pl.getConfigDir(), "config.yml");
            if (!config.exists() && !config.createNewFile()) {
                throw new IOException("Unable to create the config file at " + config);
            }
            ConfigurationLoader loader = YAMLConfigurationLoader.builder().setFile(config).build();
            ConfigurationNode configNode = loader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
            spongeConfig = configNode.getValue(TypeToken.of(SpongeConfig.class), new SpongeConfig());
            loader.save(configNode);
        } catch (Exception e) {
            pl.getLogger().error("Could not load config.", e);
        }
    }

    public static SpongeConfig getSpongeConfig() {
        return spongeConfig;
    }
}
