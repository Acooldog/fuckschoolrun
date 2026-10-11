package com.acooldog.toolbox.share.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AppClientConfig {
    private final String noticeTitle;
    private final String noticeMessage;
    private final String qqGroupNumber;
    private final String bilibiliText;
    private final String bilibiliUrl;
    private final boolean forceShutdown;
    private final String forceShutdownMessage;
    private final String minSupportedVersion;
    private final boolean startupDialogsConfigured;
    private final List<StartupDialogConfig> startupDialogs;

    public AppClientConfig(
            String noticeTitle,
            String noticeMessage,
            String qqGroupNumber,
            String bilibiliText,
            String bilibiliUrl,
            boolean forceShutdown,
            String forceShutdownMessage,
            String minSupportedVersion,
            boolean startupDialogsConfigured,
            List<StartupDialogConfig> startupDialogs
    ) {
        this.noticeTitle = noticeTitle == null ? "" : noticeTitle.trim();
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage.trim();
        this.qqGroupNumber = qqGroupNumber == null ? "" : qqGroupNumber.trim();
        this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
        this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
        this.forceShutdown = forceShutdown;
        this.forceShutdownMessage = forceShutdownMessage == null ? "" : forceShutdownMessage.trim();
        this.minSupportedVersion = minSupportedVersion == null ? "" : minSupportedVersion.trim();
        this.startupDialogsConfigured = startupDialogsConfigured;
        List<StartupDialogConfig> sortedDialogs = startupDialogs == null
                ? new ArrayList<>()
                : new ArrayList<>(startupDialogs);
        sortedDialogs.sort(
                Comparator.comparingInt(StartupDialogConfig::getDisplayOrder)
                        .thenComparingInt(StartupDialogConfig::getCycleIndex)
                        .thenComparing(StartupDialogConfig::getId)
        );
        this.startupDialogs = Collections.unmodifiableList(sortedDialogs);
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public String getQqGroupNumber() {
        return qqGroupNumber;
    }

    public String getBilibiliText() {
        return bilibiliText;
    }

    public String getBilibiliUrl() {
        return bilibiliUrl;
    }

    public boolean isForceShutdown() {
        return forceShutdown;
    }

    public String getForceShutdownMessage() {
        return forceShutdownMessage;
    }

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }

    public boolean isStartupDialogsConfigured() {
        return startupDialogsConfigured;
    }

    public List<StartupDialogConfig> getStartupDialogs() {
        return startupDialogs;
    }

    public long getStartupDialogRevision() {
        if (startupDialogsConfigured && startupDialogs.isEmpty()) {
            return 0L;
        }
        long revision = 0L;
        for (StartupDialogConfig dialog : startupDialogs) {
            revision = Math.max(revision, dialog.getUpdatedAt());
        }
        return revision > 0L ? revision : 1L;
    }
}
