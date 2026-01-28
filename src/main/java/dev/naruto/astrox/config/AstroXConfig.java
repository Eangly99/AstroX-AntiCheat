package dev.naruto.astrox.config;

import java.util.Map;

public final class AstroXConfig {
    public final Performance performance;
    public final Checks checks;
    public final Punishments punishments;
    public final Debug debug;
    public final BStats bstats;

    public AstroXConfig(Performance performance, Checks checks, Punishments punishments, Debug debug, BStats bstats) {
        this.performance = performance;
        this.checks = checks;
        this.punishments = punishments;
        this.debug = debug;
        this.bstats = bstats;
    }

    public static AstroXConfig defaults() {
        return new AstroXConfig(
            new Performance(6, 8192, 4096, 8192),
            new Checks(
                new Speed(true, 0.45, 1.30, 0.02, 0.10, 0.91, 0.08, 0.98, 3.0, new Buffer(5.0, 0.25, 30)),
                new Fly(true, 0.06, 3.0, new Buffer(4.0, 0.25, 30)),
                new Reach(true, 3.40, 0.10, 0.60, 3.0, new Buffer(4.0, 0.20, 25)),
                new BadPackets(true, 120, 2, 3.0, new Buffer(6.0, 0.50, 10))
            ),
            new Punishments(20, 5, "notify"),
            new Debug(false),
            new BStats(true, 0, false, false, false)
        );
    }

    @SuppressWarnings("unchecked")
    public static AstroXConfig from(Map<String, Object> root) {
        if (root == null) {
            return defaults();
        }

        Map<String, Object> perf = section(root, "performance");
        Performance performance = new Performance(
            intVal(perf, "workerThreads", 6),
            intVal(perf, "maxQueuedTasks", 8192),
            intVal(section(perf, "pool"), "eventObjects", 4096),
            intVal(section(perf, "pool"), "vectors", 8192)
        );

        Map<String, Object> checksSection = section(root, "checks");
        Speed speed = new Speed(
            boolVal(section(checksSection, "speed"), "enabled", true),
            doubleVal(section(checksSection, "speed"), "maxHorizontalBase", 0.45),
            doubleVal(section(checksSection, "speed"), "sprintMultiplier", 1.30),
            doubleVal(section(checksSection, "speed"), "airAccel", 0.02),
            doubleVal(section(checksSection, "speed"), "groundAccel", 0.10),
            doubleVal(section(checksSection, "speed"), "friction", 0.91),
            doubleVal(section(checksSection, "speed"), "gravity", 0.08),
            doubleVal(section(checksSection, "speed"), "drag", 0.98),
            doubleVal(section(checksSection, "speed"), "zThreshold", 3.0),
            buffer(section(section(checksSection, "speed"), "buffer"), 5.0, 0.25, 30)
        );

        Fly fly = new Fly(
            boolVal(section(checksSection, "fly"), "enabled", true),
            doubleVal(section(checksSection, "fly"), "verticalTolerance", 0.06),
            doubleVal(section(checksSection, "fly"), "zThreshold", 3.0),
            buffer(section(section(checksSection, "fly"), "buffer"), 4.0, 0.25, 30)
        );

        Reach reach = new Reach(
            boolVal(section(checksSection, "reach"), "enabled", true),
            doubleVal(section(checksSection, "reach"), "maxReach", 3.40),
            doubleVal(section(checksSection, "reach"), "bedrockHitboxExtra", 0.10),
            doubleVal(section(checksSection, "reach"), "aimDotThreshold", 0.60),
            doubleVal(section(checksSection, "reach"), "zThreshold", 3.0),
            buffer(section(section(checksSection, "reach"), "buffer"), 4.0, 0.20, 25)
        );

        BadPackets badPackets = new BadPackets(
            boolVal(section(checksSection, "badPackets"), "enabled", true),
            intVal(section(checksSection, "badPackets"), "maxPacketsPerSecond", 120),
            intVal(section(checksSection, "badPackets"), "sleepInteractGrace", 2),
            doubleVal(section(checksSection, "badPackets"), "zThreshold", 3.0),
            buffer(section(section(checksSection, "badPackets"), "buffer"), 6.0, 0.50, 10)
        );

        Checks checks = new Checks(speed, fly, reach, badPackets);

        Map<String, Object> punishSection = section(root, "punishments");
        Punishments punishments = new Punishments(
            intVal(punishSection, "maxVl", 20),
            intVal(punishSection, "alertLevel", 5),
            stringVal(punishSection, "action", "notify")
        );

        Map<String, Object> debugSection = section(root, "debug");
        Debug debug = new Debug(boolVal(debugSection, "enabled", false));

        Map<String, Object> bstatsSection = section(root, "bstats");
        BStats bstats = new BStats(
            boolVal(bstatsSection, "enabled", true),
            intVal(bstatsSection, "serviceId", 29113),
            boolVal(bstatsSection, "logErrors", false),
            boolVal(bstatsSection, "logSentData", false),
            boolVal(bstatsSection, "logResponseStatus", false)
        );

        return new AstroXConfig(performance, checks, punishments, debug, bstats);
    }

    private static Map<String, Object> section(Map<String, Object> root, String key) {
        if (root == null) {
            return Map.of();
        }
        Object val = root.get(key);
        if (val instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static double doubleVal(Map<String, Object> root, String key, double def) {
        Object val = root.get(key);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static int intVal(Map<String, Object> root, String key, int def) {
        Object val = root.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static boolean boolVal(Map<String, Object> root, String key, boolean def) {
        Object val = root.get(key);
        if (val instanceof Boolean bool) {
            return bool;
        }
        if (val instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return def;
    }

    private static String stringVal(Map<String, Object> root, String key, String def) {
        Object val = root.get(key);
        return val != null ? String.valueOf(val) : def;
    }

    private static Buffer buffer(Map<String, Object> root, double max, double decay, int minSamples) {
        return new Buffer(
            doubleVal(root, "max", max),
            doubleVal(root, "decay", decay),
            intVal(root, "minSamples", minSamples)
        );
    }

    public record Performance(int workerThreads, int maxQueuedTasks, int poolEventObjects, int poolVectors) {}

    public record Buffer(double max, double decay, int minSamples) {}

    public record Speed(boolean enabled, double maxHorizontalBase, double sprintMultiplier, double airAccel,
                        double groundAccel, double friction, double gravity, double drag, double zThreshold, Buffer buffer) {}

    public record Fly(boolean enabled, double verticalTolerance, double zThreshold, Buffer buffer) {}

    public record Reach(boolean enabled, double maxReach, double bedrockHitboxExtra, double aimDotThreshold, double zThreshold, Buffer buffer) {}

    public record BadPackets(boolean enabled, int maxPacketsPerSecond, int sleepInteractGrace, double zThreshold, Buffer buffer) {}

    public record Checks(Speed speed, Fly fly, Reach reach, BadPackets badPackets) {}

    public record Punishments(int maxVl, int alertLevel, String action) {}

    public record Debug(boolean enabled) {}

    public record BStats(boolean enabled, int serviceId, boolean logErrors, boolean logSentData, boolean logResponseStatus) {}
}
