package com.zcshou.gogogo.route.domain.model;

import android.text.TextUtils;

public final class RouteShareInfo {
    public static final RouteShareInfo NONE = new RouteShareInfo("", false, false, false, 0L);

    private final String shareId;
    private final boolean shared;
    private final boolean privacyMode;
    private final boolean downloadedFromShared;
    private final long sharedAt;

    public RouteShareInfo(
            String shareId,
            boolean shared,
            boolean privacyMode,
            boolean downloadedFromShared,
            long sharedAt
    ) {
        this.shareId = shareId == null ? "" : shareId.trim();
        this.shared = shared;
        this.privacyMode = privacyMode;
        this.downloadedFromShared = downloadedFromShared;
        this.sharedAt = sharedAt;
    }

    public String getShareId() {
        return shareId;
    }

    public boolean isShared() {
        return shared || !TextUtils.isEmpty(shareId);
    }

    public boolean isPrivacyMode() {
        return privacyMode;
    }

    public boolean isDownloadedFromShared() {
        return downloadedFromShared;
    }

    public long getSharedAt() {
        return sharedAt;
    }

    public boolean shouldMaskMapForSimulation() {
        return privacyMode && downloadedFromShared;
    }
}
