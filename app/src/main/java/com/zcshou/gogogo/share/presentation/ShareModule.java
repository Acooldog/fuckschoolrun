package com.zcshou.gogogo.share.presentation;

import android.content.Context;

import com.zcshou.gogogo.share.data.ShareApiClient;

import java.io.IOException;

public final class ShareModule {
    private static volatile ShareModule instance;

    private final ShareApiClient shareApiClient;

    private ShareModule() throws IOException {
        shareApiClient = new ShareApiClient();
    }

    public static ShareModule from(Context context) throws IOException {
        if (instance == null) {
            synchronized (ShareModule.class) {
                if (instance == null) {
                    instance = new ShareModule();
                }
            }
        }
        return instance;
    }

    public ShareApiClient shareApiClient() {
        return shareApiClient;
    }
}
