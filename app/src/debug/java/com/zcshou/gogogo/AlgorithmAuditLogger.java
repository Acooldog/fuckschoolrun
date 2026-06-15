package com.acooldog.toolbox;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class AlgorithmAuditLogger {
    private static final String KEY_ALIAS = "algorithm_test_audit_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private final Context appContext;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    AlgorithmAuditLogger(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    String append(
            @NonNull String username,
            @NonNull String module,
            @NonNull String action,
            @NonNull String input,
            @NonNull String output
    ) throws Exception {
        String digest = AlgorithmHash.sha256(module + "|" + action + "|" + input + "|" + output);
        String entry = timestampFormat.format(new Date()) + " | " + username + " | " + module + " | "
                + action + " | " + input + " | " + output + " | hash=" + digest;
        byte[] encrypted = encrypt(entry);
        File auditFile = new File(appContext.getFilesDir(), "algorithm_audit.log.enc");
        try (FileOutputStream outputStream = new FileOutputStream(auditFile, true)) {
            outputStream.write(Base64.encode(encrypted, Base64.NO_WRAP));
            outputStream.write('\n');
        }
        return digest;
    }

    @NonNull
    File auditFile() {
        return new File(appContext.getFilesDir(), "algorithm_audit.log.enc");
    }

    private byte[] encrypt(@NonNull String value) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return combined;
    }

    @NonNull
    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
