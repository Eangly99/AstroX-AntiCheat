package dev.naruto.astrox.math;

import dev.naruto.astrox.util.Poolable;

public final class Vec3f implements Poolable {
    public float x;
    public float y;
    public float z;

    public Vec3f() {
        this(0f, 0f, 0f);
    }

    public Vec3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vec3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3f set(Vec3f other) {
        return set(other.x, other.y, other.z);
    }

    public Vec3f add(Vec3f other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    public Vec3f sub(Vec3f other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
    }

    public Vec3f mul(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public float dot(Vec3f other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Vec3f normalize() {
        float len = length();
        if (len > 1.0E-6f) {
            mul(1.0f / len);
        }
        return this;
    }

    @Override
    public void reset() {
        x = 0f;
        y = 0f;
        z = 0f;
    }
}
