package com.zcshou.gogogo.nfc.domain;

import android.text.TextUtils;

import java.util.List;

public final class NfcPayloadParser {
    public NfcPayload parse(List<NfcRecordData> records, String source) {
        if (records == null || records.isEmpty()) {
            return NfcPayload.EMPTY;
        }

        String url = "";
        String packageName = "";
        for (NfcRecordData record : records) {
            if (record == null) {
                continue;
            }
            if (TextUtils.isEmpty(url) && !TextUtils.isEmpty(record.getUri())) {
                url = record.getUri();
            }
            if (TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(record.getPackageName())) {
                packageName = record.getPackageName();
            }
            if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(packageName)) {
                break;
            }
        }

        return new NfcPayload(url, packageName, source);
    }
}
