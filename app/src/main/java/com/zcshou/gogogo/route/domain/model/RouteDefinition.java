package com.zcshou.gogogo.route.domain.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteDefinition {
    private final String id;
    private final String name;
    private final long createdAt;
    private final long updatedAt;
    private final List<RoutePoint> points;
    private final File file;
    private final RouteShareInfo shareInfo;

    public RouteDefinition(String id, String name, long createdAt, long updatedAt, List<RoutePoint> points, File file) {
        this(id, name, createdAt, updatedAt, points, file, RouteShareInfo.NONE);
    }

    public RouteDefinition(
            String id,
            String name,
            long createdAt,
            long updatedAt,
            List<RoutePoint> points,
            File file,
            RouteShareInfo shareInfo
    ) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        this.file = file;
        this.shareInfo = shareInfo == null ? RouteShareInfo.NONE : shareInfo;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<RoutePoint> getPoints() {
        return points;
    }

    public File getFile() {
        return file;
    }

    public RouteShareInfo getShareInfo() {
        return shareInfo;
    }

    public boolean isSharedRoute() {
        return shareInfo.isShared();
    }

    public boolean isPrivacyProtected() {
        return shareInfo.isPrivacyMode();
    }

    public boolean isDownloadedFromShared() {
        return shareInfo.isDownloadedFromShared();
    }

    public boolean shouldMaskMapForSimulation() {
        return shareInfo.shouldMaskMapForSimulation();
    }

    public boolean hasEnoughPoints() {
        return points.size() >= 2;
    }
}
