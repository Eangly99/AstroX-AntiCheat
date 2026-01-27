package dev.naruto.astrox;

import dev.naruto.astrox.check.CheckManager;
import dev.naruto.astrox.config.AstroXConfig;
import dev.naruto.astrox.config.ConfigLoader;
import dev.naruto.astrox.packet.PacketInterceptor;
import dev.naruto.astrox.packet.PacketListener;
import dev.naruto.astrox.packet.events.SessionDownstreamPacketEvent;
import dev.naruto.astrox.packet.events.SessionUpstreamPacketEvent;
import dev.naruto.astrox.player.PlayerDataManager;
import dev.naruto.astrox.util.StripedExecutor;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreReloadEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AstroX implements Extension {
    private AstroXConfig config;
    private PlayerDataManager players;
    private CheckManager checks;
    private PacketListener packetListener;
    private PacketInterceptor packetInterceptor;
    private ExecutorService workerPool;
    private StripedExecutor stripedExecutor;

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger().info("Starting AstroX AntiCheat...");

        this.config = ConfigLoader.load(this.dataFolder(), this.logger());
        this.players = new PlayerDataManager(config);
        this.checks = new CheckManager(config);

        SessionUpstreamPacketEvent.configurePool(config.performance.poolEventObjects());
        SessionDownstreamPacketEvent.configurePool(config.performance.poolEventObjects());

        this.workerPool = createWorkerPool(config.performance.workerThreads(), config.performance.maxQueuedTasks());
        this.stripedExecutor = new StripedExecutor(workerPool, Math.max(4, config.performance.workerThreads()));

        this.packetListener = new PacketListener(config, players, checks, stripedExecutor);
        this.packetInterceptor = new PacketInterceptor(this.eventBus());

        this.eventBus().register(packetListener);
        this.logger().info("AstroX AntiCheat initialized.");
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        if (players == null || packetInterceptor == null) {
            return;
        }
        players.getOrCreate(event.connection());
        packetInterceptor.attach(event.connection());
    }

    @Subscribe
    public void onSessionLogin(SessionLoginEvent event) {
        if (packetInterceptor == null) {
            return;
        }
        packetInterceptor.attach(event.connection());
    }

    @Subscribe
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        if (players == null || packetInterceptor == null) {
            return;
        }
        packetInterceptor.detach(event.connection());
        players.remove(event.connection());
    }

    @Subscribe
    public void onReload(GeyserPreReloadEvent event) {
        this.config = ConfigLoader.load(this.dataFolder(), this.logger());
        this.logger().info("AstroX AntiCheat config reloaded.");
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        if (workerPool != null) {
            workerPool.shutdown();
        }
    }

    private ExecutorService createWorkerPool(int threads, int queueSize) {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger idx = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AstroX-Worker-" + idx.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };

        return new ThreadPoolExecutor(
            Math.max(2, threads),
            Math.max(2, threads),
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(Math.max(1024, queueSize)),
            factory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
