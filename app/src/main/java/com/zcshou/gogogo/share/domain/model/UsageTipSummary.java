package com.acooldog.toolbox.share.domain.model;

public final class UsageTipSummary {
    private final String id;
    private final String title;
    private final String excerpt;
    private final String contributorQq;
    private final String authorUsername;
    private final boolean published;
    private final boolean editable;
    private final long createdAt;
    private final long updatedAt;

    public UsageTipSummary(
            String id,
            String title,
            String excerpt,
            String contributorQq,
            String authorUsername,
            boolean published,
            boolean editable,
            long createdAt,
            long updatedAt
    ) {
        this.id = id == null ? "" : id.trim();
        this.title = title == null ? "" : title.trim();
        this.excerpt = excerpt == null ? "" : excerpt.trim();
        this.contributorQq = contributorQq == null ? "" : contributorQq.trim();
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

    public String getExcerpt() {
        return excerpt;
    }

    public String getContributorQq() {
        return contributorQq;
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
