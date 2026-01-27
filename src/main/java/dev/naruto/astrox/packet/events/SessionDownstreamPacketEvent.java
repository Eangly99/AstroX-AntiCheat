package dev.naruto.astrox.packet.events;

import dev.naruto.astrox.util.ObjectPool;
import dev.naruto.astrox.util.Poolable;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.mcprotocollib.network.packet.Packet;

public final class SessionDownstreamPacketEvent implements Event, Poolable {
    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    private static volatile ObjectPool<SessionDownstreamPacketEvent> POOL =
        new ObjectPool<>(SessionDownstreamPacketEvent::new, 4096);

    private GeyserConnection connection;
    private Packet packet;
    private Direction direction;
    private boolean cancelled;
    private long timeNanos;

    public static void configurePool(int size) {
        POOL = new ObjectPool<>(SessionDownstreamPacketEvent::new, size);
    }

    public static SessionDownstreamPacketEvent acquire() {
        return POOL.acquire();
    }

    public void release() {
        POOL.release(this);
    }

    public SessionDownstreamPacketEvent init(GeyserConnection connection, Packet packet, Direction direction, long timeNanos) {
        this.connection = connection;
        this.packet = packet;
        this.direction = direction;
        this.timeNanos = timeNanos;
        return this;
    }

    public GeyserConnection connection() {
        return connection;
    }

    public Packet packet() {
        return packet;
    }

    public Direction direction() {
        return direction;
    }

    public long timeNanos() {
        return timeNanos;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public void reset() {
        connection = null;
        packet = null;
        direction = null;
        cancelled = false;
        timeNanos = 0L;
    }
}
