package dev.naruto.astrox.util;

public final class ZScoreBuffer {
    private long count;
    private double mean;
    private double m2;
    private double buffer;

    private final double zThreshold;
    private final double maxBuffer;
    private final double decay;
    private final int minSamples;

    public ZScoreBuffer(double zThreshold, double maxBuffer, double decay, int minSamples) {
        this.zThreshold = zThreshold;
        this.maxBuffer = maxBuffer;
        this.decay = decay;
        this.minSamples = Math.max(5, minSamples);
    }

    public boolean sample(double value) {
        // Welford's online algorithm
        count++;
        double delta = value - mean;
        mean += delta / count;
        double delta2 = value - mean;
        m2 += delta * delta2;

        if (count < minSamples) {
            return false;
        }

        double variance = count > 1 ? (m2 / (count - 1)) : 0.0;
        double stdDev = Math.sqrt(Math.max(variance, 1.0E-9));
        double z = (value - mean) / stdDev;

        if (z > zThreshold) {
            buffer += 1.0;
        } else {
            buffer = Math.max(0.0, buffer - decay);
        }

        return buffer >= maxBuffer;
    }

    public void reset() {
        count = 0;
        mean = 0.0;
        m2 = 0.0;
        buffer = 0.0;
    }
}
