package com.zcshou.gogogo.share.domain.model;

import com.zcshou.gogogo.route.domain.model.RoutePoint;
import com.zcshou.gogogo.route.domain.model.RouteShareInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SharedRoutePayload {
    private final String id;
    private final String name;
    private final boolean privacyMode;
    private final long createdAt;
    private final List<RoutePoint> points;

    public SharedRoutePayload(String id, String name, boolean privacyMode, long createdAt, List<RoutePoint> points) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.privacyMode = privacyMode;
        this.createdAt = createdAt;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
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

    public long getCreatedAt() {
        return createdAt;
    }

    public List<RoutePoint> getPoints() {
        return points;
    }

    public RouteShareInfo toShareInfo(boolean downloadedFromShared) {
        return new RouteShareInfo(id, true, privacyMode, downloadedFromShared, createdAt);
    }
}
