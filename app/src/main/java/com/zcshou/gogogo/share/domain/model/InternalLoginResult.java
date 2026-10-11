package com.acooldog.toolbox.share.domain.model;

public final class InternalLoginResult {
    private final String token;
    private final InternalAccountProfile account;
    private final FsrBindingState fsrBinding;

    public InternalLoginResult(String token, InternalAccountProfile account) {
        this(token, account, null);
    }

    public InternalLoginResult(
            String token,
            InternalAccountProfile account,
            FsrBindingState fsrBinding
    ) {
        this.token = token == null ? "" : token.trim();
        this.account = account;
        this.fsrBinding = fsrBinding;
    }

    public String getToken() {
        return token;
    }

    public InternalAccountProfile getAccount() {
        return account;
    }

    public FsrBindingState getFsrBinding() {
        return fsrBinding;
    }
}
