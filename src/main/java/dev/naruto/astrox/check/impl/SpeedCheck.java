package dev.naruto.astrox.check.impl;

import dev.naruto.astrox.check.Check;
import dev.naruto.astrox.check.CheckType;
import dev.naruto.astrox.check.sample.MovementSample;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.engine.MovementPredictor;
import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.ObjectPool;

public final class SpeedCheck extends Check<MovementSample> {
    private static final ObjectPool<Vec3f> VEC_POOL = new ObjectPool<>(Vec3f::new, 2048);

    private final MovementPredictor predictor = new MovementPredictor();
    private final AstroXConfig.Speed cfg;

    public SpeedCheck(AstroXConfig config) {
        super("speed", CheckType.MOVEMENT, config);
        this.cfg = config.checks.speed();
    }

    @Override
    public boolean enabled() {
        return cfg.enabled();
    }

    @Override
    public void handle(MovementSample sample) {
        if (!enabled()) {
            return;
        }
        PlayerData data = sample.player;
        if (data == null || sample.dtTicks <= 0) {
            return;
        }
        if (sample.flying) {
            return;
        }

        Vec3f prevVel = VEC_POOL.acquire();
        try {
            data.snapshotVelocity(prevVel);

            float predicted = predictor.predictHorizontalDistance(
                sample,
                prevVel,
                cfg.maxHorizontalBase(),
                cfg.sprintMultiplier(),
                cfg.groundAccel(),
                cfg.airAccel(),
                cfg.friction()
            );

            float actual = sample.horizontalDistance();
            float pingComp = sample.ping * 0.0006f;
            float allowed = predicted + pingComp + 0.02f;
            float excess = actual - allowed;

            double sampleValue = Math.max(0.0, excess);
            boolean flagged = data.speedBuffer.sample(sampleValue);
            if (flagged) {
                int vl = data.addViolation(id(), 1);
                if (vl % 5 == 0) {
                    // placeholder for alerting/mitigation
                }
            }

        } finally {
            VEC_POOL.release(prevVel);
        }
    }
}
