package dev.naruto.astrox.packet.events;

import dev.naruto.astrox.util.ObjectPool;
import dev.naruto.astrox.util.Poolable;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.connection.GeyserConnection;

public final class SessionUpstreamPacketEvent implements Event, Poolable {
    private static volatile ObjectPool<SessionUpstreamPacketEvent> POOL =
        new ObjectPool<>(SessionUpstreamPacketEvent::new, 4096);

    private GeyserConnection connection;
    private BedrockPacket packet;
    private boolean cancelled;
    private long timeNanos;

    public static void configurePool(int size) {
        POOL = new ObjectPool<>(SessionUpstreamPacketEvent::new, size);
    }

    public static SessionUpstreamPacketEvent acquire() {
        return POOL.acquire();
    }

    public void release() {
        POOL.release(this);
    }

    public SessionUpstreamPacketEvent init(GeyserConnection connection, BedrockPacket packet, long timeNanos) {
        this.connection = connection;
        this.packet = packet;
        this.timeNanos = timeNanos;
        return this;
    }

    public GeyserConnection connection() {
        return connection;
    }

    public BedrockPacket packet() {
        return packet;
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
        cancelled = false;
        timeNanos = 0L;
    }
}
