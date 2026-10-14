package com.acooldog.toolbox.share.domain.model;

public final class BaiduMapRuntimeConfig {
    private final String runtimeValue;

    public BaiduMapRuntimeConfig(String runtimeValue) {
        this.runtimeValue = runtimeValue == null ? "" : runtimeValue.trim();
    }

    public String getRuntimeValue() {
        return runtimeValue;
    }
}
