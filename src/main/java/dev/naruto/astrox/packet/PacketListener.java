package dev.naruto.astrox.packet;

import dev.naruto.astrox.check.CheckManager;
import dev.naruto.astrox.check.sample.CombatSample;
import dev.naruto.astrox.check.sample.MovementSample;
import dev.naruto.astrox.check.sample.PacketSample;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.math.Vec3f;
import dev.naruto.astrox.packet.events.SessionDownstreamPacketEvent;
import dev.naruto.astrox.packet.events.SessionUpstreamPacketEvent;
import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.player.PlayerDataManager;
import dev.naruto.astrox.util.DebugLogger;
import dev.naruto.astrox.util.ObjectPool;
import dev.naruto.astrox.util.StripedExecutor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

public final class PacketListener {
    private final AstroXConfig config;
    private final PlayerDataManager players;
    private final CheckManager checks;
    private final StripedExecutor executor;
    private final DebugLogger debug;

    private final ObjectPool<MovementSample> movementPool;
    private final ObjectPool<CombatSample> combatPool;
    private final ObjectPool<PacketSample> packetPool;
    private final ObjectPool<Vec3f> vecPool;

    public PacketListener(AstroXConfig config, PlayerDataManager players, CheckManager checks, StripedExecutor executor, DebugLogger debug) {
        this.config = config;
        this.players = players;
        this.checks = checks;
        this.executor = executor;
        this.debug = debug;
        this.movementPool = new ObjectPool<>(MovementSample::new, config.performance.poolEventObjects());
        this.combatPool = new ObjectPool<>(CombatSample::new, config.performance.poolEventObjects());
        this.packetPool = new ObjectPool<>(PacketSample::new, config.performance.poolEventObjects());
        this.vecPool = new ObjectPool<>(Vec3f::new, config.performance.poolVectors());
    }

    @Subscribe
    public void onUpstream(SessionUpstreamPacketEvent event) {
        GeyserConnection connection = event.connection();
        PlayerData data = players.getOrCreate(connection);
        long stripeKey = stripeKey(connection);
        data.ping(connection.ping());
        data.lastPacketTimeNs(event.timeNanos());

        int rate = data.hitPacketRate();
        if (rate > config.checks.badPackets().maxPacketsPerSecond()) {
            if (debug.enabled()) {
                debug.debug("Packet rate exceeded: " + rate + " pps for " + data.bedrockUsername());
            }
            PacketSample sample = packetPool.acquire();
            sample.player = data;
            sample.packetsPerSecond = rate;
            executor.execute(stripeKey, () -> {
                try {
                    checks.badPackets().handle(sample);
                } finally {
                    packetPool.release(sample);
                }
            });
        }

        if (event.packet() instanceof PlayerActionPacket action) {
            if (action.getAction() == PlayerActionType.START_SLEEP) {
                data.sleeping(true);
            } else if (action.getAction() == PlayerActionType.STOP_SLEEP) {
                data.sleeping(false);
            } else if (action.getAction() == PlayerActionType.START_SPRINT) {
                data.sprinting(true);
            } else if (action.getAction() == PlayerActionType.STOP_SPRINT) {
                data.sprinting(false);
            } else if (action.getAction() == PlayerActionType.START_SNEAK) {
                data.sneaking(true);
            } else if (action.getAction() == PlayerActionType.STOP_SNEAK) {
                data.sneaking(false);
            } else if (action.getAction() == PlayerActionType.START_FLYING) {
                data.flying(true);
            } else if (action.getAction() == PlayerActionType.STOP_FLYING) {
                data.flying(false);
            }
        }

        if (event.packet() instanceof MovePlayerPacket move) {
            data.onGround(move.isOnGround());
            Vector3f pos = move.getPosition();
            Vec3f delta = vecPool.acquire();
            try {
                data.updatePosition(pos.getX(), pos.getY(), pos.getZ(), delta);
                MovementSample sample = movementPool.acquire();
                sample.player = data;
                sample.tick = move.getTick();
                sample.dtTicks = computeDt(data, move.getTick());
                sample.ping = data.ping();
                sample.posX = pos.getX();
                sample.posY = pos.getY();
                sample.posZ = pos.getZ();
                sample.deltaX = delta.x;
                sample.deltaY = delta.y;
                sample.deltaZ = delta.z;
                sample.yaw = move.getRotation().getY();
                sample.pitch = move.getRotation().getX();
                sample.onGround = data.onGround();
                sample.sprinting = data.sprinting();
                sample.sneaking = data.sneaking();
                sample.flying = data.flying();

                executor.execute(stripeKey, () -> {
                    try {
                        checks.speed().handle(sample);
                        checks.fly().handle(sample);
                        data.updateVelocity(sample.deltaX / sample.dtTicks, sample.deltaY / sample.dtTicks, sample.deltaZ / sample.dtTicks);
                    } finally {
                        movementPool.release(sample);
                    }
                });
            } finally {
                vecPool.release(delta);
            }
        }

        if (event.packet() instanceof PlayerAuthInputPacket auth) {
            Vector3f pos = auth.getPosition();
            Vector3f rot = auth.getRotation();
            data.rotation(rot.getY(), rot.getX());

            boolean sprinting = auth.getInputData().contains(PlayerAuthInputData.SPRINTING)
                || auth.getInputData().contains(PlayerAuthInputData.START_SPRINTING);
            boolean sneaking = auth.getInputData().contains(PlayerAuthInputData.SNEAKING)
                || auth.getInputData().contains(PlayerAuthInputData.START_SNEAKING);
            boolean jumping = auth.getInputData().contains(PlayerAuthInputData.JUMPING)
                || auth.getInputData().contains(PlayerAuthInputData.START_JUMPING);
            boolean gliding = auth.getInputData().contains(PlayerAuthInputData.START_GLIDING);
            boolean flying = auth.getInputData().contains(PlayerAuthInputData.START_FLYING)
                || auth.getInputData().contains(PlayerAuthInputData.ASCEND);

            data.sprinting(sprinting);
            data.sneaking(sneaking);
            if (auth.getInputData().contains(PlayerAuthInputData.START_FLYING)) {
                data.flying(true);
            } else if (auth.getInputData().contains(PlayerAuthInputData.STOP_FLYING)) {
                data.flying(false);
            }

            Vec3f delta = vecPool.acquire();
            try {
                data.updatePosition(pos.getX(), pos.getY(), pos.getZ(), delta);
                MovementSample sample = movementPool.acquire();
                sample.player = data;
                sample.tick = auth.getTick();
                sample.dtTicks = computeDt(data, auth.getTick());
                sample.ping = data.ping();
                sample.posX = pos.getX();
                sample.posY = pos.getY();
                sample.posZ = pos.getZ();
                sample.deltaX = delta.x;
                sample.deltaY = delta.y;
                sample.deltaZ = delta.z;
                sample.yaw = rot.getY();
                sample.pitch = rot.getX();
                sample.onGround = data.onGround();
                sample.sprinting = sprinting;
                sample.sneaking = sneaking;
                sample.jumping = jumping;
                sample.flying = data.flying() || flying;
                sample.gliding = gliding;

                executor.execute(stripeKey, () -> {
                    try {
                        checks.speed().handle(sample);
                        checks.fly().handle(sample);
                        data.updateVelocity(sample.deltaX / sample.dtTicks, sample.deltaY / sample.dtTicks, sample.deltaZ / sample.dtTicks);
                    } finally {
                        movementPool.release(sample);
                    }
                });
            } finally {
                vecPool.release(delta);
            }
        }

        if (event.packet() instanceof InventoryTransactionPacket tx
            && tx.getTransactionType() == InventoryTransactionType.ITEM_USE_ON_ENTITY) {
            if (data.sleeping()) {
                if (debug.enabled()) {
                    debug.debug("Interact while sleeping: " + data.bedrockUsername());
                }
                PacketSample sample = packetPool.acquire();
                sample.player = data;
                sample.packetsPerSecond = 0;
                sample.actionWhileSleeping = true;
                executor.execute(stripeKey, () -> {
                    try {
                        checks.badPackets().handle(sample);
                    } finally {
                        packetPool.release(sample);
                    }
                });
                return;
            }

            if (connection instanceof GeyserSession session) {
                Entity target = session.getEntityCache().getEntityByGeyserId(tx.getRuntimeEntityId());
                if (target != null) {
                    CombatSample sample = combatPool.acquire();
                    sample.player = data;
                    sample.tick = data.lastTick();
                    sample.ping = data.ping();
                    sample.targetId = tx.getRuntimeEntityId();
                    sample.targetX = target.getPosition().getX();
                    sample.targetY = target.getPosition().getY();
                    sample.targetZ = target.getPosition().getZ();
                    sample.targetWidth = target.getBoundingBoxWidth();
                    sample.targetHeight = target.getBoundingBoxHeight();

                    float eyeHeight = data.sneaking() ? 1.54f : 1.62f;
                    sample.eyeX = data.lastPosition().x;
                    sample.eyeY = data.lastPosition().y + eyeHeight;
                    sample.eyeZ = data.lastPosition().z;
                    sample.yaw = data.yaw();
                    sample.pitch = data.pitch();

                    executor.execute(stripeKey, () -> {
                        try {
                            checks.reach().handle(sample);
                        } finally {
                            combatPool.release(sample);
                        }
                    });
                }
            }
        }
    }

    @Subscribe
    public void onDownstream(SessionDownstreamPacketEvent event) {
        // Reserved for downstream correlation (server-driven state, server-side lag compensation)
    }

    private int computeDt(PlayerData data, long tick) {
        long last = data.lastTick();
        data.lastTick(tick);
        if (last == 0L || tick <= last) {
            return 1;
        }
        long dt = tick - last;
        if (dt > 10) {
            return 10;
        }
        return (int) dt;
    }

    private long stripeKey(GeyserConnection connection) {
        var key = players.keyFor(connection);
        return key.getMostSignificantBits() ^ key.getLeastSignificantBits();
    }
}
