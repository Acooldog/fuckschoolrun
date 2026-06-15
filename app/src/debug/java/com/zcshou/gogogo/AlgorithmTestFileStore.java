package com.acooldog.toolbox;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class AlgorithmTestFileStore {
    private final Context appContext;
    private final SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    AlgorithmTestFileStore(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    File save(@NonNull String prefix, @NonNull String extension, @NonNull String content) throws Exception {
        return saveBytes(prefix, extension, content.getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    File saveBytes(@NonNull String prefix, @NonNull String extension, @NonNull byte[] content) throws Exception {
        File directory = directory();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建测试数据目录");
        }
        String safePrefix = prefix.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        File file = new File(directory, safePrefix + "_" + nameFormat.format(new Date()) + "." + extension);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content);
        }
        return file;
    }

    @NonNull
    List<File> listGeneratedFiles() {
        File directory = directory();
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        Arrays.sort(files, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return new ArrayList<>(Arrays.asList(files));
    }

    @NonNull
    File directory() {
        return new File(appContext.getFilesDir(), "algorithm-test-cases");
    }
}
