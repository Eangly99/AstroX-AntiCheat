package dev.naruto.astrox.check;

import dev.naruto.astrox.check.impl.BadPacketsCheck;
import dev.naruto.astrox.check.impl.FlyCheck;
import dev.naruto.astrox.check.impl.ReachCheck;
import dev.naruto.astrox.check.impl.SpeedCheck;
import dev.naruto.astrox.config.AstroXConfig;

public final class CheckManager {
    private final SpeedCheck speedCheck;
    private final FlyCheck flyCheck;
    private final ReachCheck reachCheck;
    private final BadPacketsCheck badPacketsCheck;

    public CheckManager(AstroXConfig config) {
        this.speedCheck = new SpeedCheck(config);
        this.flyCheck = new FlyCheck(config);
        this.reachCheck = new ReachCheck(config);
        this.badPacketsCheck = new BadPacketsCheck(config);
    }

    public SpeedCheck speed() {
        return speedCheck;
    }

    public FlyCheck fly() {
        return flyCheck;
    }

    public ReachCheck reach() {
        return reachCheck;
    }

    public BadPacketsCheck badPackets() {
        return badPacketsCheck;
    }
}
