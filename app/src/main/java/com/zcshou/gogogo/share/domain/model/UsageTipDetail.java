package com.acooldog.toolbox.share.domain.model;

public final class UsageTipDetail {
    private final String id;
    private final String title;
    private final String htmlContent;
    private final String plainText;
    private final String contributorQq;
    private final String authorAccountId;
    private final String authorUsername;
    private final boolean published;
    private final boolean editable;
    private final long createdAt;
    private final long updatedAt;

    public UsageTipDetail(
            String id,
            String title,
            String htmlContent,
            String plainText,
            String contributorQq,
            String authorAccountId,
            String authorUsername,
            boolean published,
            boolean editable,
            long createdAt,
            long updatedAt
    ) {
        this.id = id == null ? "" : id.trim();
        this.title = title == null ? "" : title.trim();
        this.htmlContent = htmlContent == null ? "" : htmlContent.trim();
        this.plainText = plainText == null ? "" : plainText.trim();
        this.contributorQq = contributorQq == null ? "" : contributorQq.trim();
        this.authorAccountId = authorAccountId == null ? "" : authorAccountId.trim();
        this.authorUsername = authorUsername == null ? "" : authorUsername.trim();
        this.published = published;
        this.editable = editable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public String getPlainText() {
        return plainText;
    }

    public String getContributorQq() {
        return contributorQq;
    }

    public String getAuthorAccountId() {
        return authorAccountId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isEditable() {
        return editable;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
