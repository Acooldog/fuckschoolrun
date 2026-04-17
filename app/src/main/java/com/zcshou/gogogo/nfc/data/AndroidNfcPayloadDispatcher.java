package com.zcshou.gogogo.nfc.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.zcshou.gogogo.nfc.domain.NfcPayload;
import com.zcshou.gogogo.nfc.domain.NfcPayloadDispatchResult;
import com.zcshou.gogogo.nfc.domain.NfcPayloadDispatcher;

public final class AndroidNfcPayloadDispatcher implements NfcPayloadDispatcher {
    @Override
    public NfcPayloadDispatchResult dispatch(Context context, NfcPayload payload) {
        try {
            Intent ndefIntent = createNdefIntent(payload);
            context.startActivity(ndefIntent);
            return NfcPayloadDispatchResult.ndefSent();
        } catch (ActivityNotFoundException | SecurityException exception) {
            return dispatchViewFallback(context, payload, exception);
        } catch (Exception exception) {
            return NfcPayloadDispatchResult.failed(exception.getMessage());
        }
    }

    private Intent createNdefIntent(NfcPayload payload) {
        Uri uri = Uri.parse(payload.getUrl());
        NdefRecord uriRecord = NdefRecord.createUri(uri);
        NdefRecord appRecord = NdefRecord.createApplicationRecord(payload.getPackageName());
        NdefMessage message = new NdefMessage(new NdefRecord[]{uriRecord, appRecord});

        Intent intent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intent.setData(uri);
        intent.setPackage(payload.getPackageName());
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new Parcelable[]{message});
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private NfcPayloadDispatchResult dispatchViewFallback(
            Context context,
            NfcPayload payload,
            Exception firstFailure
    ) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payload.getUrl()));
        intent.setPackage(payload.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            return NfcPayloadDispatchResult.fallbackViewSent();
        } catch (ActivityNotFoundException | SecurityException secondFailure) {
            Intent genericIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(payload.getUrl()));
            genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(genericIntent);
                return NfcPayloadDispatchResult.fallbackViewSent();
            } catch (Exception finalFailure) {
                String detail = firstFailure.getMessage();
                if (detail == null || detail.trim().isEmpty()) {
                    detail = finalFailure.getMessage();
                }
                return NfcPayloadDispatchResult.failed(detail);
            }
        }
    }
}
