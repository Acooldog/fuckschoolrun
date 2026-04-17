package com.zcshou.gogogo.route.domain.model;

public final class RouteSimulationConfig {
    private final double speedMetersPerSecond;
    private final int loopCount;
    private final boolean speedFloating;
    private final long tickMillis;

    public RouteSimulationConfig(double speedMetersPerSecond, int loopCount, boolean speedFloating, long tickMillis) {
        if (speedMetersPerSecond <= 0) {
            throw new IllegalArgumentException("speedMetersPerSecond must be positive");
        }
        if (loopCount <= 0) {
            throw new IllegalArgumentException("loopCount must be positive");
        }
        if (tickMillis <= 0) {
            throw new IllegalArgumentException("tickMillis must be positive");
        }
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.loopCount = loopCount;
        this.speedFloating = speedFloating;
        this.tickMillis = tickMillis;
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public boolean isSpeedFloating() {
        return speedFloating;
    }

    public long getTickMillis() {
        return tickMillis;
    }
}
