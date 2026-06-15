package com.acooldog.toolbox.share.domain.model;

public final class InternalLoginResult {
    private final String token;
    private final InternalAccountProfile account;

    public InternalLoginResult(String token, InternalAccountProfile account) {
        this.token = token == null ? "" : token.trim();
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public InternalAccountProfile getAccount() {
        return account;
    }
}
