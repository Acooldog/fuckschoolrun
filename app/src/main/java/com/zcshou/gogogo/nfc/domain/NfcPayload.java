package com.zcshou.gogogo.nfc.domain;

import android.text.TextUtils;

public final class NfcPayload {
    public static final NfcPayload EMPTY = new NfcPayload("", "", "");

    private final String url;
    private final String packageName;
    private final String source;

    public NfcPayload(String url, String packageName, String source) {
        this.url = url == null ? "" : url.trim();
        this.packageName = packageName == null ? "" : packageName.trim();
        this.source = source == null ? "" : source.trim();
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

    public boolean hasUrl() {
        return !TextUtils.isEmpty(url);
    }

    public boolean hasPackageName() {
        return !TextUtils.isEmpty(packageName);
    }

    public boolean isEmpty() {
        return !hasUrl() && !hasPackageName();
    }
}
