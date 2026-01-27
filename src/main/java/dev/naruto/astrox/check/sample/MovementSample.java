package dev.naruto.astrox.check.sample;

import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.Poolable;

public final class MovementSample implements Poolable {
    public PlayerData player;
    public long tick;
    public int dtTicks;
    public int ping;

    public float posX;
    public float posY;
    public float posZ;
    public float deltaX;
    public float deltaY;
    public float deltaZ;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean sprinting;
    public boolean jumping;
    public boolean flying;
    public boolean gliding;
    public boolean sneaking;

    public float horizontalDistance() {
        return (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    @Override
    public void reset() {
        player = null;
        tick = 0L;
        dtTicks = 0;
        ping = 0;
        posX = posY = posZ = 0f;
        deltaX = deltaY = deltaZ = 0f;
        yaw = pitch = 0f;
        onGround = false;
        sprinting = false;
        jumping = false;
        flying = false;
        gliding = false;
        sneaking = false;
    }
}
