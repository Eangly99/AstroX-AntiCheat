package dev.naruto.astrox.packet;

import dev.naruto.astrox.packet.events.SessionDownstreamPacketEvent;
import dev.naruto.astrox.packet.events.SessionUpstreamPacketEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.ExtensionEventBus;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketInterceptor {
    private final ExtensionEventBus eventBus;
    private final Map<UUID, SessionListener> downstreamListeners = new ConcurrentHashMap<>();

    public PacketInterceptor(ExtensionEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void attach(GeyserConnection connection) {
        if (!(connection instanceof GeyserSession session)) {
            return;
        }
        attachUpstream(connection, session);
        attachDownstream(connection, session);
    }

    public void detach(GeyserConnection connection) {
        if (!(connection instanceof GeyserSession session)) {
            return;
        }
        UUID key = connection.javaUuid();
        if (key != null) {
            SessionListener listener = downstreamListeners.remove(key);
            if (listener != null && session.getDownstream() != null) {
                session.getDownstream().getSession().removeListener(listener);
            }
        }
    }

    private void attachUpstream(GeyserConnection connection, GeyserSession session) {
        BedrockPacketHandler current = session.getUpstream().getSession().getPacketHandler();
        if (current != null && Proxy.isProxyClass(current.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(current);
            if (handler instanceof UpstreamProxyHandler) {
                return;
            }
        }

        BedrockPacketHandler proxy = (BedrockPacketHandler) Proxy.newProxyInstance(
            BedrockPacketHandler.class.getClassLoader(),
            new Class<?>[]{BedrockPacketHandler.class},
            new UpstreamProxyHandler(connection, current, eventBus)
        );

        session.getUpstream().getSession().setPacketHandler(proxy);
    }

    private void attachDownstream(GeyserConnection connection, GeyserSession session) {
        if (session.getDownstream() == null) {
            return;
        }
        UUID key = connection.javaUuid();
        if (key == null || downstreamListeners.containsKey(key)) {
            return;
        }
        DownstreamListener listener = new DownstreamListener(connection, eventBus);
        session.getDownstream().getSession().addListener(listener);
        downstreamListeners.put(key, listener);
    }

    private static final class UpstreamProxyHandler implements InvocationHandler {
        private final GeyserConnection connection;
        private final BedrockPacketHandler delegate;
        private final ExtensionEventBus eventBus;

        private UpstreamProxyHandler(GeyserConnection connection, BedrockPacketHandler delegate, ExtensionEventBus eventBus) {
            this.connection = connection;
            this.delegate = delegate;
            this.eventBus = eventBus;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && args.length == 1 && args[0] instanceof BedrockPacket packet
                && method.getName().equals("handle")) {
                SessionUpstreamPacketEvent event = SessionUpstreamPacketEvent.acquire()
                    .init(connection, packet, System.nanoTime());
                try {
                    eventBus.fire(event);
                    if (event.cancelled()) {
                        return PacketSignal.HANDLED;
                    }
                } finally {
                    event.release();
                }
            }

            if (delegate == null) {
                return PacketSignal.UNHANDLED;
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }

    private static final class DownstreamListener implements SessionListener {
        private final GeyserConnection connection;
        private final ExtensionEventBus eventBus;

        private DownstreamListener(GeyserConnection connection, ExtensionEventBus eventBus) {
            this.connection = connection;
            this.eventBus = eventBus;
        }

        @Override
        public void packetReceived(Session session, Packet packet) {
            SessionDownstreamPacketEvent event = SessionDownstreamPacketEvent.acquire()
                .init(connection, packet, SessionDownstreamPacketEvent.Direction.INBOUND, System.nanoTime());
            try {
                eventBus.fire(event);
            } finally {
                event.release();
            }
        }

        @Override
        public void packetSending(PacketSendingEvent event) {
            SessionDownstreamPacketEvent down = SessionDownstreamPacketEvent.acquire()
                .init(connection, event.getPacket(), SessionDownstreamPacketEvent.Direction.OUTBOUND, System.nanoTime());
            try {
                eventBus.fire(down);
                if (down.cancelled()) {
                    event.setCancelled(true);
                }
            } finally {
                down.release();
            }
        }

        @Override
        public void packetSent(Session session, Packet packet) {
            // no-op
        }

        @Override
        public void packetError(PacketErrorEvent event) {
            // no-op
        }

        @Override
        public void connected(ConnectedEvent event) {
            // no-op
        }

        @Override
        public void disconnecting(DisconnectingEvent event) {
            // no-op
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            // no-op
        }
    }
}
