package dev.naruto.astrox.engine;

import dev.naruto.astrox.check.sample.MovementSample;
import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.util.ObjectPool;

public final class MovementPredictor {
    private static final ObjectPool<Vec3f> VEC_POOL = new ObjectPool<>(Vec3f::new, 2048);

    public float predictHorizontalDistance(MovementSample sample, Vec3f prevVelocity,
                                           double maxBase, double sprintMultiplier,
                                           double groundAccel, double airAccel, double friction) {
        float maxSpeed = (float) (maxBase * (sample.sprinting ? sprintMultiplier : 1.0));
        float accel = sample.onGround ? (float) groundAccel : (float) airAccel;
        float frictionFactor = (float) friction;

        Vec3f moveDir = VEC_POOL.acquire();
        try {
            if (Math.abs(sample.deltaX) > 1.0E-4f || Math.abs(sample.deltaZ) > 1.0E-4f) {
                moveDir.set(sample.deltaX, 0f, sample.deltaZ).normalize();
            } else {
                float yawRad = (float) Math.toRadians(sample.yaw);
                moveDir.set((float) -Math.sin(yawRad), 0f, (float) Math.cos(yawRad)).normalize();
            }

            float velX = prevVelocity.x;
            float velZ = prevVelocity.z;
            float distance = 0f;

            int ticks = Math.max(1, sample.dtTicks);
            for (int i = 0; i < ticks; i++) {
                velX *= frictionFactor;
                velZ *= frictionFactor;

                velX += moveDir.x * accel;
                velZ += moveDir.z * accel;

                float horiz = (float) Math.sqrt(velX * velX + velZ * velZ);
                if (horiz > maxSpeed) {
                    float scale = maxSpeed / horiz;
                    velX *= scale;
                    velZ *= scale;
                }

                distance += (float) Math.sqrt(velX * velX + velZ * velZ);
            }

            return distance;
        } finally {
            VEC_POOL.release(moveDir);
        }
    }
}
