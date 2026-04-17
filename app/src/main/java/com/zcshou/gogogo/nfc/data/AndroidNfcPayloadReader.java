package com.zcshou.gogogo.nfc.data;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.zcshou.gogogo.nfc.domain.AndroidNfcRecordReader;
import com.zcshou.gogogo.nfc.domain.NfcRecordData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AndroidNfcPayloadReader implements AndroidNfcRecordReader {
    private static final byte[] ANDROID_APP_RECORD_TYPE =
            "android.com:pkg".getBytes(StandardCharsets.US_ASCII);

    @Override
    public List<NfcRecordData> readRecords(Intent intent) {
        List<NfcRecordData> records = new ArrayList<>();
        if (intent == null) {
            return records;
        }

        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null) {
            return records;
        }

        for (Parcelable rawMessage : rawMessages) {
            if (!(rawMessage instanceof NdefMessage)) {
                continue;
            }
            for (NdefRecord record : ((NdefMessage) rawMessage).getRecords()) {
                records.add(new NfcRecordData(extractUri(record), extractPackageName(record)));
            }
        }
        return records;
    }

    private String extractUri(NdefRecord record) {
        try {
            Uri uri = record.toUri();
            return uri == null ? "" : uri.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractPackageName(NdefRecord record) {
        if (record.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return "";
        }
        if (!Arrays.equals(record.getType(), ANDROID_APP_RECORD_TYPE)) {
            return "";
        }
        return new String(record.getPayload(), StandardCharsets.UTF_8);
    }
}
