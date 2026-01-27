package dev.naruto.astrox.check.impl;

import dev.naruto.astrox.check.Check;
import dev.naruto.astrox.check.CheckType;
import dev.naruto.astrox.check.sample.CombatSample;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.math.Aabb;
import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.ObjectPool;

public final class ReachCheck extends Check<CombatSample> {
    private static final ObjectPool<Vec3f> VEC_POOL = new ObjectPool<>(Vec3f::new, 1024);

    private final AstroXConfig.Reach cfg;

    public ReachCheck(AstroXConfig config) {
        super("reach", CheckType.COMBAT, config);
        this.cfg = config.checks.reach();
    }

    @Override
    public boolean enabled() {
        return cfg.enabled();
    }

    @Override
    public void handle(CombatSample sample) {
        if (!enabled()) {
            return;
        }
        PlayerData data = sample.player;
        if (data == null) {
            return;
        }

        Vec3f look = VEC_POOL.acquire();
        Vec3f toTarget = VEC_POOL.acquire();
        Vec3f origin = VEC_POOL.acquire();
        try {
            origin.set(sample.eyeX, sample.eyeY, sample.eyeZ);
            float yawRad = (float) Math.toRadians(sample.yaw);
            float pitchRad = (float) Math.toRadians(sample.pitch);
            look.set(
                (float) (-Math.sin(yawRad) * Math.cos(pitchRad)),
                (float) (-Math.sin(pitchRad)),
                (float) (Math.cos(yawRad) * Math.cos(pitchRad))
            ).normalize();

            float targetCenterX = sample.targetX;
            float targetCenterY = sample.targetY + (sample.targetHeight * 0.5f);
            float targetCenterZ = sample.targetZ;

            toTarget.set(
                targetCenterX - sample.eyeX,
                targetCenterY - sample.eyeY,
                targetCenterZ - sample.eyeZ
            );

            float distance = toTarget.length();
            if (distance < 0.05f) {
                return;
            }

            Vec3f toTargetDir = VEC_POOL.acquire();
            try {
                toTargetDir.set(toTarget).normalize();
                float dot = look.dot(toTargetDir);

                float maxReach = (float) cfg.maxReach() + (sample.ping * 0.002f);
                float hitboxExtra = (float) cfg.bedrockHitboxExtra();

                Aabb box = new Aabb().set(
                    sample.targetX - (sample.targetWidth * 0.5f) - hitboxExtra,
                    sample.targetY - hitboxExtra,
                    sample.targetZ - (sample.targetWidth * 0.5f) - hitboxExtra,
                    sample.targetX + (sample.targetWidth * 0.5f) + hitboxExtra,
                    sample.targetY + sample.targetHeight + hitboxExtra,
                    sample.targetZ + (sample.targetWidth * 0.5f) + hitboxExtra
                );

                boolean intersects = box.intersectsRay(origin, look, maxReach + 0.3f);
                float excess = Math.max(0f, distance - maxReach);
                if (dot < cfg.aimDotThreshold()) {
                    excess += (cfg.aimDotThreshold() - dot);
                }
                if (!intersects) {
                    excess += 0.15f;
                }

                double sampleValue = Math.max(0.0, excess);
                boolean flagged = data.reachBuffer.sample(sampleValue);
                if (flagged) {
                    int vl = data.addViolation(id(), 1);
                    if (vl % 5 == 0) {
                        // placeholder for alerting/mitigation
                    }
                }
            } finally {
                VEC_POOL.release(toTargetDir);
            }
        } finally {
            VEC_POOL.release(look);
            VEC_POOL.release(toTarget);
            VEC_POOL.release(origin);
        }
    }
}
