package com.vexsoftware.votifier.fabric.config;

import com.vexsoftware.votifier.fabric.NuVotifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigLoader {

    private static FabricConfig fabricConfig;
    private static Yaml yaml;

    public static void loadConfig(NuVotifier pl) {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);

        Representer representer = new Representer(options);
        representer.addClassTag(FabricConfig.class, Tag.MAP);
        yaml = new Yaml(representer, options);

        if (!pl.getConfigDir().exists()) {
            if (!pl.getConfigDir().mkdirs()) {
                throw new RuntimeException("Unable to create the plugin data folder " + pl.getConfigDir());
            }
        }
        try {
            File config = new File(pl.getConfigDir(), "nuvotifier.yml");
            if (!config.exists()) {
                if (!config.createNewFile()) {
                    throw new IOException("Unable to create the config file at " + config);
                }
                FileWriter fileWriter = new FileWriter(config);
                // Save the default config
                yaml.dump(new FabricConfig(), fileWriter);
                fileWriter.flush();
            }
            fabricConfig = yaml.loadAs(Files.newInputStream(config.toPath()), FabricConfig.class);
        } catch (Exception e) {
            NuVotifier.LOGGER.error("Could not load config.", e);
        }
    }

    public static FabricConfig getFabricConfig() {
        return fabricConfig;
    }


}
