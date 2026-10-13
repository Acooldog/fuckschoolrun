package com.acooldog.toolbox.ui;

public final class RouteCreateMapReveal {
    private RouteCreateMapReveal() {
    }

    public static int left(int width, float progress, boolean satellite) {
        int visibleWidth = visibleWidth(width, progress);
        return satellite ? 0 : Math.max(0, width) - visibleWidth;
    }

    public static int right(int width, float progress, boolean satellite) {
        return satellite ? visibleWidth(width, progress) : Math.max(0, width);
    }

    private static int visibleWidth(int width, float progress) {
        return Math.round(Math.max(0, width) * Math.max(0f, Math.min(1f, progress)));
    }
}
