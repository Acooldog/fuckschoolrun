package com.acooldog.toolbox.config;

import android.text.TextUtils;

import com.acooldog.toolbox.nfc.domain.NfcPayload;

public final class SavedNfcConfig {
    private final String name;
    private final String url;
    private final String packageName;
    private final String source;

    public SavedNfcConfig(String name, String url, String packageName, String source) {
        this.name = name == null ? "" : name.trim();
        this.url = url == null ? "" : url.trim();
        this.packageName = packageName == null ? "" : packageName.trim();
        this.source = source == null ? "" : source.trim();
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

    public boolean isComplete() {
        return !TextUtils.isEmpty(name)
                && !TextUtils.isEmpty(url)
                && !TextUtils.isEmpty(packageName);
    }

    public NfcPayload toPayload() {
        return new NfcPayload(url, packageName, source);
    }
}
