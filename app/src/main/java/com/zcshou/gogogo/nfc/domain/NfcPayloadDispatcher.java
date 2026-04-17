package com.zcshou.gogogo.nfc.domain;

import android.content.Context;

public interface NfcPayloadDispatcher {
    NfcPayloadDispatchResult dispatch(Context context, NfcPayload payload);
}
