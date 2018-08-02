package com.vexsoftware.votifier.sponge.config;

import com.google.common.reflect.TypeToken;
import com.vexsoftware.votifier.sponge.VotifierPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;

public class ConfigLoader {

    private static VotifierPlugin plugin;
    private static SpongeConfig spongeConfig;

    public static void loadConfig(VotifierPlugin pl) {
        plugin = pl;
        if (!plugin.getConfigDir().exists()) {
            plugin.getConfigDir().mkdirs();
        }
        try {
            File file = new File(plugin.getConfigDir(), "config.yml");
            if (!file.exists()) {
                file.createNewFile();
            }
            ConfigurationLoader loader = YAMLConfigurationLoader.builder().setFile(file).build();
            ConfigurationNode config = loader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
            spongeConfig = config.getValue(TypeToken.of(SpongeConfig.class), new SpongeConfig());
            loader.save(config);
        } catch (Exception e) {
            plugin.getLogger().error("Could not load config.", e);
        }
    }

    public static SpongeConfig getSpongeConfig() {
        return spongeConfig;
    }
}
