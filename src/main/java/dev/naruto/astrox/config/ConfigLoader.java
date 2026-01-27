package dev.naruto.astrox.config;

import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class ConfigLoader {
    private static final Yaml YAML = new Yaml();

    private ConfigLoader() {
    }

    public static AstroXConfig load(Path dataFolder, ExtensionLogger logger) {
        Path configPath = dataFolder.resolve("config.yml");
        ensureConfigExists(configPath, logger);

        try (InputStream in = Files.newInputStream(configPath)) {
            Object loaded = YAML.load(in);
            if (loaded instanceof Map<?, ?> root) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) root;
                return AstroXConfig.from(cast);
            }
        } catch (IOException ex) {
            logger.error("Failed to read config.yml", ex);
        }

        return AstroXConfig.defaults();
    }

    private static void ensureConfigExists(Path configPath, ExtensionLogger logger) {
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yml")) {
                    if (in == null) {
                        logger.warning("Missing default config.yml in resources; using defaults only.");
                        return;
                    }
                    Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to create config.yml", ex);
        }
    }
}
