package dev.naruto.astrox.util;

import org.geysermc.geyser.api.extension.ExtensionLogger;

public final class DebugLogger {
    private final ExtensionLogger logger;
    private volatile boolean enabled;

    public DebugLogger(ExtensionLogger logger, boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void debug(String message) {
        if (enabled) {
            logger.info("[Debug] " + message);
        }
    }
}
