package com.acooldog.toolbox.share.domain.model;

public final class EmailVerificationState {
    private final boolean accepted;
    private final boolean canSend;
    private final int cooldownSeconds;
    private final long expiresAt;

    public EmailVerificationState(boolean accepted, boolean canSend, int cooldownSeconds, long expiresAt) {
        this.accepted = accepted;
        this.canSend = canSend;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.expiresAt = Math.max(0L, expiresAt);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean canSend() {
        return canSend;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
