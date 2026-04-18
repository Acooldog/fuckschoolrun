package com.acooldog.toolbox.update;

public final class GiteeReleaseInfo {
    private final String tagName;
    private final String releaseName;
    private final String changelog;
    private final String downloadUrl;

    public GiteeReleaseInfo(String tagName, String releaseName, String changelog, String downloadUrl) {
        this.tagName = tagName == null ? "" : tagName.trim();
        this.releaseName = releaseName == null ? "" : releaseName.trim();
        this.changelog = changelog == null ? "" : changelog.trim();
        this.downloadUrl = downloadUrl == null ? "" : downloadUrl.trim();
    }

    public String getTagName() {
        return tagName;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean hasDownloadUrl() {
        return !downloadUrl.isEmpty();
    }
}
