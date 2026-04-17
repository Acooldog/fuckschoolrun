package com.zcshou.gogogo.nfc.domain;

import android.content.Context;

public final class SendNfcPayloadUseCase {
    private final NfcPayloadDispatcher dispatcher;

    public SendNfcPayloadUseCase(NfcPayloadDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public NfcPayloadDispatchResult send(Context context, NfcPayload payload) {
        return dispatcher.dispatch(context, payload);
    }
}
