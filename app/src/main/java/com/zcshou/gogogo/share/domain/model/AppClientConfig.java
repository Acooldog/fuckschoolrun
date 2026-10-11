package com.acooldog.toolbox.share.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AppClientConfig {
    private final String noticeTitle;
    private final String noticeMessage;
    private final String qqGroupNumber;
    private final String bilibiliText;
    private final String bilibiliUrl;
    private final boolean forceShutdown;
    private final String forceShutdownMessage;
    private final List<StartupDialogConfig> startupDialogs;

    public AppClientConfig(
            String noticeTitle,
            String noticeMessage,
            String qqGroupNumber,
            String bilibiliText,
            String bilibiliUrl,
            boolean forceShutdown,
            String forceShutdownMessage,
            List<StartupDialogConfig> startupDialogs
    ) {
        this.noticeTitle = noticeTitle == null ? "" : noticeTitle.trim();
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage.trim();
        this.qqGroupNumber = qqGroupNumber == null ? "" : qqGroupNumber.trim();
        this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
        this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
        this.forceShutdown = forceShutdown;
        this.forceShutdownMessage = forceShutdownMessage == null ? "" : forceShutdownMessage.trim();
        this.startupDialogs = startupDialogs == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(startupDialogs));
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

    public List<StartupDialogConfig> getStartupDialogs() {
        return startupDialogs;
    }

    public long getStartupDialogRevision() {
        long revision = 0L;
        for (StartupDialogConfig dialog : startupDialogs) {
            revision = Math.max(revision, dialog.getUpdatedAt());
        }
        return revision > 0L ? revision : 1L;
    }
}
