package dev.naruto.astrox.player;

import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.util.RateTracker;
import dev.naruto.astrox.util.ZScoreBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    private final UUID playerId;
    private final String xuid;
    private final String bedrockUsername;

    private final Vec3f lastPosition = new Vec3f();
    private final Vec3f lastVelocity = new Vec3f();

    private volatile int ping;
    private volatile boolean onGround;
    private volatile boolean sleeping;
    private volatile boolean flying;
    private volatile boolean sprinting;
    private volatile boolean sneaking;
    private volatile float yaw;
    private volatile float pitch;
    private volatile long lastTick;
    private volatile long lastPacketTimeNs;

    private final RateTracker packetRate = new RateTracker(1000);

    private final Map<String, Integer> violationLevels = new HashMap<>();

    public final ZScoreBuffer speedBuffer;
    public final ZScoreBuffer flyBuffer;
    public final ZScoreBuffer reachBuffer;
    public final ZScoreBuffer badPacketsBuffer;

    public PlayerData(UUID playerId, String xuid, String bedrockUsername,
                      ZScoreBuffer speedBuffer, ZScoreBuffer flyBuffer,
                      ZScoreBuffer reachBuffer, ZScoreBuffer badPacketsBuffer) {
        this.playerId = playerId;
        this.xuid = xuid;
        this.bedrockUsername = bedrockUsername;
        this.speedBuffer = speedBuffer;
        this.flyBuffer = flyBuffer;
        this.reachBuffer = reachBuffer;
        this.badPacketsBuffer = badPacketsBuffer;
    }

    public UUID playerId() {
        return playerId;
    }

    public String xuid() {
        return xuid;
    }

    public String bedrockUsername() {
        return bedrockUsername;
    }

    public int ping() {
        return ping;
    }

    public void ping(int ping) {
        this.ping = ping;
    }

    public boolean onGround() {
        return onGround;
    }

    public void onGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean sleeping() {
        return sleeping;
    }

    public void sleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    public boolean flying() {
        return flying;
    }

    public void flying(boolean flying) {
        this.flying = flying;
    }

    public boolean sprinting() {
        return sprinting;
    }

    public void sprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean sneaking() {
        return sneaking;
    }

    public void sneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public long lastTick() {
        return lastTick;
    }

    public void lastTick(long lastTick) {
        this.lastTick = lastTick;
    }

    public long lastPacketTimeNs() {
        return lastPacketTimeNs;
    }

    public void lastPacketTimeNs(long lastPacketTimeNs) {
        this.lastPacketTimeNs = lastPacketTimeNs;
    }

    public Vec3f lastPosition() {
        return lastPosition;
    }

    public Vec3f lastVelocity() {
        return lastVelocity;
    }

    public void updatePosition(float x, float y, float z, Vec3f outDelta) {
        synchronized (lastPosition) {
            outDelta.set(x - lastPosition.x, y - lastPosition.y, z - lastPosition.z);
            lastPosition.set(x, y, z);
        }
    }

    public void updateVelocity(float vx, float vy, float vz) {
        synchronized (lastVelocity) {
            lastVelocity.set(vx, vy, vz);
        }
    }

    public Vec3f snapshotVelocity(Vec3f out) {
        synchronized (lastVelocity) {
            return out.set(lastVelocity);
        }
    }

    public int hitPacketRate() {
        return packetRate.hit();
    }

    public int addViolation(String checkId, int amount) {
        synchronized (violationLevels) {
            int current = violationLevels.getOrDefault(checkId, 0);
            int next = current + amount;
            violationLevels.put(checkId, next);
            return next;
        }
    }

    public int violationLevel(String checkId) {
        synchronized (violationLevels) {
            return violationLevels.getOrDefault(checkId, 0);
        }
    }
}
