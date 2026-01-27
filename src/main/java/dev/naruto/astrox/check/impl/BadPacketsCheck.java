package dev.naruto.astrox.check.impl;

import dev.naruto.astrox.check.Check;
import dev.naruto.astrox.check.CheckType;
import dev.naruto.astrox.check.sample.PacketSample;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.player.PlayerData;

public final class BadPacketsCheck extends Check<PacketSample> {
    private final AstroXConfig.BadPackets cfg;

    public BadPacketsCheck(AstroXConfig config) {
        super("bad_packets", CheckType.BAD_PACKETS, config);
        this.cfg = config.checks.badPackets();
    }

    @Override
    public boolean enabled() {
        return cfg.enabled();
    }

    @Override
    public void handle(PacketSample sample) {
        if (!enabled()) {
            return;
        }
        PlayerData data = sample.player;
        if (data == null) {
            return;
        }

        double value = 0.0;
        int excessRate = sample.packetsPerSecond - cfg.maxPacketsPerSecond();
        if (excessRate > 0) {
            value += excessRate;
        }
        if (sample.actionWhileSleeping) {
            value += cfg.sleepInteractGrace();
        }

        if (value <= 0.0) {
            data.badPacketsBuffer.sample(0.0);
            return;
        }

        boolean flagged = data.badPacketsBuffer.sample(value);
        if (flagged) {
            int vl = data.addViolation(id(), 1);
            if (vl % 5 == 0) {
                // placeholder for alerting/mitigation
            }
        }
    }
}
