package dev.naruto.astrox.check.impl;

import dev.naruto.astrox.check.Check;
import dev.naruto.astrox.check.CheckType;
import dev.naruto.astrox.check.sample.MovementSample;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.ObjectPool;

public final class FlyCheck extends Check<MovementSample> {
    private static final ObjectPool<Vec3f> VEC_POOL = new ObjectPool<>(Vec3f::new, 1024);

    private final AstroXConfig.Fly cfg;
    private final AstroXConfig.Speed speedCfg;

    public FlyCheck(AstroXConfig config) {
        super("fly", CheckType.MOVEMENT, config);
        this.cfg = config.checks.fly();
        this.speedCfg = config.checks.speed();
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
        if (sample.onGround || sample.flying || sample.gliding) {
            return;
        }

        Vec3f prevVel = VEC_POOL.acquire();
        try {
            data.snapshotVelocity(prevVel);
            float expectedVy = (prevVel.y - (float) speedCfg.gravity()) * (float) speedCfg.drag();
            float actualVy = sample.deltaY / sample.dtTicks;

            float excess = actualVy - expectedVy - (float) cfg.verticalTolerance();
            double sampleValue = Math.max(0.0, excess);

            boolean flagged = data.flyBuffer.sample(sampleValue);
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
