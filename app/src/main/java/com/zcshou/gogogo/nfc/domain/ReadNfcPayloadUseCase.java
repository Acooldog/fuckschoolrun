package com.zcshou.gogogo.nfc.domain;

import android.content.Intent;

public final class ReadNfcPayloadUseCase {
    private final AndroidNfcRecordReader recordReader;
    private final NfcPayloadParser payloadParser;

    public ReadNfcPayloadUseCase(AndroidNfcRecordReader recordReader, NfcPayloadParser payloadParser) {
        this.recordReader = recordReader;
        this.payloadParser = payloadParser;
    }

    public NfcPayload read(Intent intent) {
        String source = intent == null || intent.getAction() == null ? "" : intent.getAction();
        return payloadParser.parse(recordReader.readRecords(intent), source);
    }
}
