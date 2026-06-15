package com.acooldog.toolbox.route.domain.model;

public final class RouteSimulationConfig {
    public static final double DEFAULT_INTENSITY_VARIATION_RANGE_METERS_PER_SECOND = 2.0d;
    public static final double DEFAULT_INTENSITY_VARIATION_FREQUENCY = 0.35d;
    public static final double DEFAULT_PATH_VARIATION_AMPLITUDE_METERS = 1.0d;
    public static final double DEFAULT_ALTITUDE_BASE_METERS = 55d;
    public static final double DEFAULT_ALTITUDE_VARIATION_RANGE_METERS = 0.6d;
    public static final double DEFAULT_ALTITUDE_VARIATION_HEIGHT_CENTIMETERS = 170d;
    public static final double DEFAULT_ALTITUDE_VARIATION_PROBABILITY = 0.35d;
    public static final double DEFAULT_LINK_RATIO_NUMERATOR = 1.0d;
    public static final double DEFAULT_STEPS_PER_METER = 1.0d;

    public enum Mode {
        SPEED,
        CADENCE
    }

    private final Mode mode;
    private final double speedMetersPerSecond;
    private final double cadenceStepsPerMinute;
    private final int loopCount;
    private final boolean dynamicIntensityEnabled;
    private final double intensityVariationRangeMetersPerSecond;
    private final double intensityVariationFrequency;
    private final boolean naturalPathVariationEnabled;
    private final double pathVariationAmplitudeMeters;
    private final boolean naturalAltitudeVariationEnabled;
    private final double altitudeBaseMeters;
    private final double altitudeVariationRangeMeters;
    private final double altitudeVariationHeightCentimeters;
    private final double altitudeVariationProbability;
    private final double linkRatioNumerator;
    private final double stepsPerMeter;
    private final long tickMillis;

    public RouteSimulationConfig(double speedMetersPerSecond, int loopCount, boolean speedFloating, long tickMillis) {
        this(
                Mode.SPEED,
                speedMetersPerSecond,
                0d,
                loopCount,
                speedFloating,
                DEFAULT_INTENSITY_VARIATION_RANGE_METERS_PER_SECOND,
                DEFAULT_INTENSITY_VARIATION_FREQUENCY,
                false,
                0d,
                false,
                DEFAULT_ALTITUDE_BASE_METERS,
                0d,
                0d,
                0d,
                DEFAULT_LINK_RATIO_NUMERATOR,
                DEFAULT_STEPS_PER_METER,
                tickMillis
        );
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean speedFloating,
            long tickMillis
    ) {
        this(
                mode,
                speedMetersPerSecond,
                cadenceStepsPerMinute,
                loopCount,
                speedFloating,
                DEFAULT_INTENSITY_VARIATION_RANGE_METERS_PER_SECOND,
                DEFAULT_INTENSITY_VARIATION_FREQUENCY,
                false,
                0d,
                false,
                DEFAULT_ALTITUDE_BASE_METERS,
                0d,
                0d,
                0d,
                DEFAULT_LINK_RATIO_NUMERATOR,
                DEFAULT_STEPS_PER_METER,
                tickMillis
        );
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean dynamicIntensityEnabled,
            double intensityVariationRangeMetersPerSecond,
            double intensityVariationFrequency,
            boolean naturalPathVariationEnabled,
            double pathVariationAmplitudeMeters,
            long tickMillis
    ) {
        this(
                mode,
                speedMetersPerSecond,
                cadenceStepsPerMinute,
                loopCount,
                dynamicIntensityEnabled,
                intensityVariationRangeMetersPerSecond,
                intensityVariationFrequency,
                naturalPathVariationEnabled,
                pathVariationAmplitudeMeters,
                false,
                DEFAULT_ALTITUDE_BASE_METERS,
                0d,
                0d,
                0d,
                DEFAULT_LINK_RATIO_NUMERATOR,
                DEFAULT_STEPS_PER_METER,
                tickMillis
        );
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean dynamicIntensityEnabled,
            double intensityVariationRangeMetersPerSecond,
            double intensityVariationFrequency,
            boolean naturalPathVariationEnabled,
            double pathVariationAmplitudeMeters,
            double linkRatioNumerator,
            double stepsPerMeter,
            long tickMillis
    ) {
        this(
                mode,
                speedMetersPerSecond,
                cadenceStepsPerMinute,
                loopCount,
                dynamicIntensityEnabled,
                intensityVariationRangeMetersPerSecond,
                intensityVariationFrequency,
                naturalPathVariationEnabled,
                pathVariationAmplitudeMeters,
                false,
                DEFAULT_ALTITUDE_BASE_METERS,
                0d,
                0d,
                0d,
                linkRatioNumerator,
                stepsPerMeter,
                tickMillis
        );
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean dynamicIntensityEnabled,
            double intensityVariationRangeMetersPerSecond,
            double intensityVariationFrequency,
            boolean naturalPathVariationEnabled,
            double pathVariationAmplitudeMeters,
            boolean naturalAltitudeVariationEnabled,
            double altitudeVariationRangeMeters,
            double altitudeVariationHeightCentimeters,
            double altitudeVariationProbability,
            double linkRatioNumerator,
            double stepsPerMeter,
            long tickMillis
    ) {
        this(
                mode,
                speedMetersPerSecond,
                cadenceStepsPerMinute,
                loopCount,
                dynamicIntensityEnabled,
                intensityVariationRangeMetersPerSecond,
                intensityVariationFrequency,
                naturalPathVariationEnabled,
                pathVariationAmplitudeMeters,
                naturalAltitudeVariationEnabled,
                DEFAULT_ALTITUDE_BASE_METERS,
                altitudeVariationRangeMeters,
                altitudeVariationHeightCentimeters,
                altitudeVariationProbability,
                linkRatioNumerator,
                stepsPerMeter,
                tickMillis
        );
    }

    public RouteSimulationConfig(
            Mode mode,
            double speedMetersPerSecond,
            double cadenceStepsPerMinute,
            int loopCount,
            boolean dynamicIntensityEnabled,
            double intensityVariationRangeMetersPerSecond,
            double intensityVariationFrequency,
            boolean naturalPathVariationEnabled,
            double pathVariationAmplitudeMeters,
            boolean naturalAltitudeVariationEnabled,
            double altitudeBaseMeters,
            double altitudeVariationRangeMeters,
            double altitudeVariationHeightCentimeters,
            double altitudeVariationProbability,
            double linkRatioNumerator,
            double stepsPerMeter,
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
        if (intensityVariationRangeMetersPerSecond < 0d) {
            throw new IllegalArgumentException("intensityVariationRangeMetersPerSecond must not be negative");
        }
        if (intensityVariationFrequency < 0d || intensityVariationFrequency > 1d) {
            throw new IllegalArgumentException("intensityVariationFrequency must be between 0 and 1");
        }
        if (pathVariationAmplitudeMeters < 0d) {
            throw new IllegalArgumentException("pathVariationAmplitudeMeters must not be negative");
        }
        if (altitudeBaseMeters < 0d) {
            throw new IllegalArgumentException("altitudeBaseMeters must not be negative");
        }
        if (altitudeVariationRangeMeters < 0d) {
            throw new IllegalArgumentException("altitudeVariationRangeMeters must not be negative");
        }
        if (altitudeVariationHeightCentimeters < 0d) {
            throw new IllegalArgumentException("altitudeVariationHeightCentimeters must not be negative");
        }
        if (altitudeVariationProbability < 0d || altitudeVariationProbability > 1d) {
            throw new IllegalArgumentException("altitudeVariationProbability must be between 0 and 1");
        }
        if (linkRatioNumerator <= 0d) {
            throw new IllegalArgumentException("linkRatioNumerator must be positive");
        }
        if (stepsPerMeter <= 0d) {
            throw new IllegalArgumentException("stepsPerMeter must be positive");
        }
        this.mode = mode;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.cadenceStepsPerMinute = cadenceStepsPerMinute;
        this.loopCount = loopCount;
        this.dynamicIntensityEnabled = dynamicIntensityEnabled;
        this.intensityVariationRangeMetersPerSecond = intensityVariationRangeMetersPerSecond;
        this.intensityVariationFrequency = intensityVariationFrequency;
        this.naturalPathVariationEnabled = naturalPathVariationEnabled;
        this.pathVariationAmplitudeMeters = pathVariationAmplitudeMeters;
        this.naturalAltitudeVariationEnabled = naturalAltitudeVariationEnabled;
        this.altitudeBaseMeters = altitudeBaseMeters;
        this.altitudeVariationRangeMeters = altitudeVariationRangeMeters;
        this.altitudeVariationHeightCentimeters = altitudeVariationHeightCentimeters;
        this.altitudeVariationProbability = altitudeVariationProbability;
        this.linkRatioNumerator = linkRatioNumerator;
        this.stepsPerMeter = stepsPerMeter;
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

    public boolean isDynamicIntensityEnabled() {
        return dynamicIntensityEnabled;
    }

    public boolean isSpeedFloating() {
        return dynamicIntensityEnabled;
    }

    public double getIntensityVariationRangeMetersPerSecond() {
        return intensityVariationRangeMetersPerSecond;
    }

    public double getIntensityVariationFrequency() {
        return intensityVariationFrequency;
    }

    public boolean isNaturalPathVariationEnabled() {
        return naturalPathVariationEnabled;
    }

    public double getPathVariationAmplitudeMeters() {
        return pathVariationAmplitudeMeters;
    }

    public boolean isNaturalAltitudeVariationEnabled() {
        return naturalAltitudeVariationEnabled;
    }

    public double getAltitudeBaseMeters() {
        return altitudeBaseMeters;
    }

    public double getAltitudeVariationRangeMeters() {
        return altitudeVariationRangeMeters;
    }

    public double getAltitudeVariationHeightCentimeters() {
        return altitudeVariationHeightCentimeters;
    }

    public double getAltitudeVariationProbability() {
        return altitudeVariationProbability;
    }

    public double getLinkRatioNumerator() {
        return linkRatioNumerator;
    }

    public double getStepsPerMeter() {
        return stepsPerMeter;
    }

    public long getTickMillis() {
        return tickMillis;
    }
}
