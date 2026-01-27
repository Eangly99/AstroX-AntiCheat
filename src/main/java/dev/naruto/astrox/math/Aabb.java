package dev.naruto.astrox.math;

public final class Aabb {
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    public Aabb set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    public boolean intersectsRay(Vec3f origin, Vec3f dir, float maxDistance) {
        float tMin = 0.0f;
        float tMax = maxDistance;

        float invX = dir.x == 0f ? Float.POSITIVE_INFINITY : 1.0f / dir.x;
        float invY = dir.y == 0f ? Float.POSITIVE_INFINITY : 1.0f / dir.y;
        float invZ = dir.z == 0f ? Float.POSITIVE_INFINITY : 1.0f / dir.z;

        float t1 = (minX - origin.x) * invX;
        float t2 = (maxX - origin.x) * invX;
        tMin = Math.max(tMin, Math.min(t1, t2));
        tMax = Math.min(tMax, Math.max(t1, t2));
        if (tMax < tMin) {
            return false;
        }

        t1 = (minY - origin.y) * invY;
        t2 = (maxY - origin.y) * invY;
        tMin = Math.max(tMin, Math.min(t1, t2));
        tMax = Math.min(tMax, Math.max(t1, t2));
        if (tMax < tMin) {
            return false;
        }

        t1 = (minZ - origin.z) * invZ;
        t2 = (maxZ - origin.z) * invZ;
        tMin = Math.max(tMin, Math.min(t1, t2));
        tMax = Math.min(tMax, Math.max(t1, t2));

        return tMax >= tMin && tMax >= 0.0f;
    }
}
