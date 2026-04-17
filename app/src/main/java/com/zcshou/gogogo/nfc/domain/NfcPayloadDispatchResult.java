package com.zcshou.gogogo.nfc.domain;

public final class NfcPayloadDispatchResult {
    public enum Status {
        NDEF_SENT,
        FALLBACK_VIEW_SENT,
        FAILED
    }

    private final Status status;
    private final String detail;

    private NfcPayloadDispatchResult(Status status, String detail) {
        this.status = status;
        this.detail = detail == null ? "" : detail;
    }

    public static NfcPayloadDispatchResult ndefSent() {
        return new NfcPayloadDispatchResult(Status.NDEF_SENT, "");
    }

    public static NfcPayloadDispatchResult fallbackViewSent() {
        return new NfcPayloadDispatchResult(Status.FALLBACK_VIEW_SENT, "");
    }

    public static NfcPayloadDispatchResult failed(String detail) {
        return new NfcPayloadDispatchResult(Status.FAILED, detail);
    }

    public Status getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isSuccessful() {
        return status != Status.FAILED;
    }
}
