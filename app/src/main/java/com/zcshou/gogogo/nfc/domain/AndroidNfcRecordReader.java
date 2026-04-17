package com.zcshou.gogogo.nfc.domain;

import android.content.Intent;

import java.util.List;

public interface AndroidNfcRecordReader {
    List<NfcRecordData> readRecords(Intent intent);
}
