package com.zcshou.gogogo.share.domain.model;

public final class SharedRouteSummary {
    private final String id;
    private final String name;
    private final boolean privacyMode;
    private final int pointCount;
    private final long createdAt;

    public SharedRouteSummary(String id, String name, boolean privacyMode, int pointCount, long createdAt) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.privacyMode = privacyMode;
        this.pointCount = pointCount;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPrivacyMode() {
        return privacyMode;
    }

    public int getPointCount() {
        return pointCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
