package com.acooldog.toolbox.share.domain.model;

public final class AppClientConfig {
    private final String noticeTitle;
    private final String noticeMessage;
    private final String qqGroupNumber;
    private final String bilibiliText;
    private final String bilibiliUrl;

    public AppClientConfig(
            String noticeTitle,
            String noticeMessage,
            String qqGroupNumber,
            String bilibiliText,
            String bilibiliUrl
    ) {
        this.noticeTitle = noticeTitle == null ? "" : noticeTitle.trim();
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage.trim();
        this.qqGroupNumber = qqGroupNumber == null ? "" : qqGroupNumber.trim();
        this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
        this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
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
}
