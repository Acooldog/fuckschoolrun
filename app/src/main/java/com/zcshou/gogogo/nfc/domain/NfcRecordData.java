package com.zcshou.gogogo.nfc.domain;

public final class NfcRecordData {
    private final String uri;
    private final String packageName;

    public NfcRecordData(String uri, String packageName) {
        this.uri = uri == null ? "" : uri.trim();
        this.packageName = packageName == null ? "" : packageName.trim();
    }

    public String getUri() {
        return uri;
    }

    public String getPackageName() {
        return packageName;
    }
}
