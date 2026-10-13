package com.acooldog.toolbox.ui;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MinePageData {
    public static final String DEFAULT_AUTHOR_QQ = "2622138410";
    public static final String DEFAULT_QQ_GROUP = "951300122";
    public static final String DEFAULT_BILIBILI_NAME = "牢大_科比";
    public static final String DEFAULT_BILIBILI_URL =
            "https://space.bilibili.com/1492911803";
    public static final String DEFAULT_DISCLAIMER =
            "本软件为免费开源软件。如果是购买软件，或者花钱买教程才得到的软件，请立即退款；"
                    + "如果得到了软件但是对方要求你付费，请勿付费。\n"
                    + "作者qq: 2622138410 作者bilibili: 牢大_科比 作者github名字: Acooldog";

    private final boolean loggedIn;
    private final String todaySimulationKm;
    private final String totalSimulationKm;
    private final String uploadedRouteCount;
    private final String helpedUserCount;
    private final String preferredRouteName;
    private final List<MineRoutePoint> preferredRoutePoints;
    private final String authorQq;
    private final String qqGroup;
    private final String bilibiliName;
    private final String bilibiliUrl;
    private final String disclaimerText;

    public MinePageData(
            boolean loggedIn,
            String todaySimulationKm,
            String totalSimulationKm,
            String uploadedRouteCount,
            String helpedUserCount,
            String preferredRouteName,
            @NonNull List<MineRoutePoint> preferredRoutePoints,
            String authorQq,
            String qqGroup,
            String bilibiliName,
            String bilibiliUrl,
            String disclaimerText
    ) {
        this.loggedIn = loggedIn;
        this.todaySimulationKm = normalize(todaySimulationKm);
        this.totalSimulationKm = normalize(totalSimulationKm);
        this.uploadedRouteCount = normalize(uploadedRouteCount);
        this.helpedUserCount = normalize(helpedUserCount);
        this.preferredRouteName = normalize(preferredRouteName);
        this.preferredRoutePoints = Collections.unmodifiableList(
                preferredRoutePoints == null
                        ? Collections.emptyList()
                        : new ArrayList<>(preferredRoutePoints)
        );
        this.authorQq = fallback(authorQq, DEFAULT_AUTHOR_QQ);
        this.qqGroup = fallback(qqGroup, DEFAULT_QQ_GROUP);
        this.bilibiliName = fallback(bilibiliName, DEFAULT_BILIBILI_NAME);
        this.bilibiliUrl = fallback(bilibiliUrl, DEFAULT_BILIBILI_URL);
        this.disclaimerText = fallback(disclaimerText, DEFAULT_DISCLAIMER);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getTodaySimulationKm() {
        return todaySimulationKm;
    }

    public String getTotalSimulationKm() {
        return totalSimulationKm;
    }

    public String getUploadedRouteCount() {
        return uploadedRouteCount;
    }

    public String getHelpedUserCount() {
        return helpedUserCount;
    }

    public String getPreferredRouteName() {
        return preferredRouteName;
    }

    public List<MineRoutePoint> getPreferredRoutePoints() {
        return preferredRoutePoints;
    }

    public String getAuthorQq() {
        return authorQq;
    }

    public String getQqGroup() {
        return qqGroup;
    }

    public String getBilibiliName() {
        return bilibiliName;
    }

    public String getBilibiliUrl() {
        return bilibiliUrl;
    }

    public String getDisclaimerText() {
        return disclaimerText;
    }

    public static MinePageData fallback(boolean loggedIn) {
        return new MinePageData(
                loggedIn,
                null,
                null,
                null,
                null,
                "",
                Collections.emptyList(),
                DEFAULT_AUTHOR_QQ,
                DEFAULT_QQ_GROUP,
                DEFAULT_BILIBILI_NAME,
                DEFAULT_BILIBILI_URL,
                DEFAULT_DISCLAIMER
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String fallback(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }
}
