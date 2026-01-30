package dev.naruto.astrox.player;

import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.util.ZScoreBuffer;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataManager {
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final AstroXConfig config;

    public PlayerDataManager(AstroXConfig config) {
        this.config = config;
    }

    public UUID keyFor(GeyserConnection connection) {
        UUID javaUuid = connection.javaUuid();
        if (javaUuid != null) {
            return javaUuid;
        }
        String xuid = connection.xuid();
        if (xuid != null && !xuid.isEmpty()) {
            return UUID.nameUUIDFromBytes(("xuid:" + xuid).getBytes(StandardCharsets.UTF_8));
        }
        return UUID.nameUUIDFromBytes(("bedrock:" + connection.bedrockUsername()).getBytes(StandardCharsets.UTF_8));
    }

    public PlayerData getOrCreate(GeyserConnection connection) {
        UUID key = keyFor(connection);
        synchronized (players) {
            PlayerData existing = players.get(key);
            if (existing != null) {
                return existing;
            }
            PlayerData created = createPlayerData(connection, key);
            players.put(key, created);
            return created;
        }
    }

    public PlayerData get(GeyserConnection connection) {
        UUID key = keyFor(connection);
        synchronized (players) {
            return players.get(key);
        }
    }

    public void remove(GeyserConnection connection) {
        UUID key = keyFor(connection);
        synchronized (players) {
            players.remove(key);
        }
    }

    public Map<UUID, PlayerData> snapshot() {
        synchronized (players) {
            return Map.copyOf(players);
        }
    }

    private PlayerData createPlayerData(GeyserConnection connection, UUID key) {
        AstroXConfig.Buffer speedBuffer = config.checks.speed().buffer();
        AstroXConfig.Buffer flyBuffer = config.checks.fly().buffer();
        AstroXConfig.Buffer reachBuffer = config.checks.reach().buffer();
        AstroXConfig.Buffer badBuffer = config.checks.badPackets().buffer();

        return new PlayerData(
            key,
            connection.xuid(),
            connection.bedrockUsername(),
            new ZScoreBuffer(config.checks.speed().zThreshold(), speedBuffer.max(), speedBuffer.decay(), speedBuffer.minSamples()),
            new ZScoreBuffer(config.checks.fly().zThreshold(), flyBuffer.max(), flyBuffer.decay(), flyBuffer.minSamples()),
            new ZScoreBuffer(config.checks.reach().zThreshold(), reachBuffer.max(), reachBuffer.decay(), reachBuffer.minSamples()),
            new ZScoreBuffer(config.checks.badPackets().zThreshold(), badBuffer.max(), badBuffer.decay(), badBuffer.minSamples())
        );
    }
}
