package dev.naruto.astrox.check.sample;

import dev.naruto.astrox.player.PlayerData;
import dev.naruto.astrox.util.Poolable;

public final class PacketSample implements Poolable {
    public PlayerData player;
    public int packetsPerSecond;
    public boolean actionWhileSleeping;

    @Override
    public void reset() {
        player = null;
        packetsPerSecond = 0;
        actionWhileSleeping = false;
    }
}
