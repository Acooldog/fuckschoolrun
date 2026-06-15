package com.acooldog.toolbox.share.domain.model;

public final class InternalSoftwareName {
    private final String id;
    private final String name;
    private final String status;

    public InternalSoftwareName(String id, String name, String status) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.status = status == null ? "" : status.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
