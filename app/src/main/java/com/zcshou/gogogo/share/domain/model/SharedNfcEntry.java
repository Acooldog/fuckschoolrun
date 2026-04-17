package com.zcshou.gogogo.share.domain.model;

import com.zcshou.gogogo.nfc.domain.NfcPayload;

public final class SharedNfcEntry {
    private final String id;
    private final String name;
    private final String url;
    private final String packageName;
    private final String source;
    private final long createdAt;

    public SharedNfcEntry(String id, String name, String url, String packageName, String source, long createdAt) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.url = url == null ? "" : url.trim();
        this.packageName = packageName == null ? "" : packageName.trim();
        this.source = source == null ? "" : source.trim();
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSource() {
        return source;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public NfcPayload toPayload() {
        return new NfcPayload(url, packageName, source);
    }
}
