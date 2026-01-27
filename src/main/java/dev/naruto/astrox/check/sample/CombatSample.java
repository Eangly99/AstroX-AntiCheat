package dev.naruto.astrox.check.sample;

import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.Poolable;

public final class CombatSample implements Poolable {
    public PlayerData player;
    public long tick;
    public int ping;

    public long targetId;
    public float targetX;
    public float targetY;
    public float targetZ;
    public float targetWidth;
    public float targetHeight;

    public float eyeX;
    public float eyeY;
    public float eyeZ;
    public float yaw;
    public float pitch;

    @Override
    public void reset() {
        player = null;
        tick = 0L;
        ping = 0;
        targetId = 0L;
        targetX = targetY = targetZ = 0f;
        targetWidth = targetHeight = 0f;
        eyeX = eyeY = eyeZ = 0f;
        yaw = pitch = 0f;
    }
}
