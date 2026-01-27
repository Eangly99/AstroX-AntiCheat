package dev.naruto.astrox.check;

import dev.naruto.astrox.config.AstroXConfig;

public abstract class Check<T> {
    private final String id;
    private final CheckType type;
    protected final AstroXConfig config;

    protected Check(String id, CheckType type, AstroXConfig config) {
        this.id = id;
        this.type = type;
        this.config = config;
    }

    public String id() {
        return id;
    }

    public CheckType type() {
        return type;
    }

    public abstract boolean enabled();

    public abstract void handle(T sample);
}
