package com.acooldog.toolbox.route.domain.model;

public final class RouteSimulationConfig {
    public enum Mode {
        SPEED,
        CADENCE
    }

    private final Mode mode;
    private final double speedMetersPerSecond;
    private final double cadenceStepsPerMinute;
    private final int loopCount;
    private final boolean speedFloating;
    private final long tickMillis;

    public RouteSimulationConfig(double speedMetersPerSecond, int loopCount, boolean speedFloating, long tickMillis) {
        this(Mode.SPEED, speedMetersPerSecond, 0d, loopCount, speedFloating, tickMillis);
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean speedFloating,
            long tickMillis
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (loopCount <= 0) {
            throw new IllegalArgumentException("loopCount must be positive");
        }
        if (tickMillis <= 0) {
            throw new IllegalArgumentException("tickMillis must be positive");
        }
        if (mode == Mode.SPEED && speedMetersPerSecond <= 0d) {
            throw new IllegalArgumentException("speedMetersPerSecond must be positive in speed mode");
        }
        if (mode == Mode.CADENCE && cadenceStepsPerMinute <= 0d) {
            throw new IllegalArgumentException("cadenceStepsPerMinute must be positive in cadence mode");
        }
        this.mode = mode;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.cadenceStepsPerMinute = cadenceStepsPerMinute;
        this.loopCount = loopCount;
        this.speedFloating = speedFloating;
        this.tickMillis = tickMillis;
    }

    public Mode getMode() {
        return mode;
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public double getCadenceStepsPerMinute() {
        return cadenceStepsPerMinute;
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
