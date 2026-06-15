package com.acooldog.toolbox.share.domain.model;

public final class SharedSimulationConfigEntry {
    private final String id;
    private final String name;
    private final String mode;
    private final double speed;
    private final double cadence;
    private final int loopCount;
    private final boolean dynamicIntensityEnabled;
    private final double intensityVariationRange;
    private final double intensityVariationFrequency;
    private final boolean naturalPathVariationEnabled;
    private final double pathVariationAmplitude;
    private final boolean naturalAltitudeVariationEnabled;
    private final double altitudeBaseMeters;
    private final double altitudeVariationRange;
    private final double altitudeVariationHeightCentimeters;
    private final double altitudeVariationProbability;
    private final double linkRatioNumerator;
    private final double stepsPerMeter;
    private final String authorName;
    private final long createdAt;
    private final long updatedAt;

    public SharedSimulationConfigEntry(
            String id,
            String name,
            String mode,
            double speed,
            double cadence,
            int loopCount,
            boolean dynamicIntensityEnabled,
            double intensityVariationRange,
            double intensityVariationFrequency,
            boolean naturalPathVariationEnabled,
            double pathVariationAmplitude,
            boolean naturalAltitudeVariationEnabled,
            double altitudeBaseMeters,
            double altitudeVariationRange,
            double altitudeVariationHeightCentimeters,
            double altitudeVariationProbability,
            double linkRatioNumerator,
            double stepsPerMeter,
            String authorName,
            long createdAt,
            long updatedAt
    ) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.mode = mode == null ? "" : mode.trim();
        this.speed = speed;
        this.cadence = cadence;
        this.loopCount = loopCount;
        this.dynamicIntensityEnabled = dynamicIntensityEnabled;
        this.intensityVariationRange = intensityVariationRange;
        this.intensityVariationFrequency = intensityVariationFrequency;
        this.naturalPathVariationEnabled = naturalPathVariationEnabled;
        this.pathVariationAmplitude = pathVariationAmplitude;
        this.naturalAltitudeVariationEnabled = naturalAltitudeVariationEnabled;
        this.altitudeBaseMeters = altitudeBaseMeters;
        this.altitudeVariationRange = altitudeVariationRange;
        this.altitudeVariationHeightCentimeters = altitudeVariationHeightCentimeters;
        this.altitudeVariationProbability = altitudeVariationProbability;
        this.linkRatioNumerator = linkRatioNumerator;
        this.stepsPerMeter = stepsPerMeter;
        this.authorName = authorName == null ? "" : authorName.trim();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMode() {
        return mode;
    }

    public double getSpeed() {
        return speed;
    }

    public double getCadence() {
        return cadence;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public boolean isDynamicIntensityEnabled() {
        return dynamicIntensityEnabled;
    }

    public double getIntensityVariationRange() {
        return intensityVariationRange;
    }

    public double getIntensityVariationFrequency() {
        return intensityVariationFrequency;
    }

    public boolean isNaturalPathVariationEnabled() {
        return naturalPathVariationEnabled;
    }

    public double getPathVariationAmplitude() {
        return pathVariationAmplitude;
    }

    public boolean isNaturalAltitudeVariationEnabled() {
        return naturalAltitudeVariationEnabled;
    }

    public double getAltitudeBaseMeters() {
        return altitudeBaseMeters;
    }

    public double getAltitudeVariationRange() {
        return altitudeVariationRange;
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

    public String getAuthorName() {
        return authorName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
