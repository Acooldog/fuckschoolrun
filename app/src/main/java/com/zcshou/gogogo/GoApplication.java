package com.acooldog.toolbox;

import android.app.Application;
import android.content.Context;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.ConsolePrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator;

import java.io.File;

public class GoApplication extends Application {
    public static final String APP_NAME = "LaodaSchoolRun";
    public static final String LOG_FILE_NAME = APP_NAME + ".log";
    private static final long MAX_TIME = 1000 * 60 * 60 * 24 * 3;
    private static final Object BAIDU_SDK_LOCK = new Object();
    private static volatile boolean baiduSdkInitialized;

    @Override
    public void onCreate() {
        super.onCreate();
        initXlog();
    }

    public static boolean isBaiduSdkInitialized() {
        return baiduSdkInitialized;
    }

    public static boolean initializeBaiduSdk(Context context, String runtimeValue) {
        if (baiduSdkInitialized) {
            return true;
        }
        if (context == null || runtimeValue == null || runtimeValue.trim().isEmpty()) {
            return false;
        }
        synchronized (BAIDU_SDK_LOCK) {
            if (baiduSdkInitialized) {
                return true;
            }
            try {
                Context applicationContext = context.getApplicationContext();
                SDKInitializer.setAgreePrivacy(applicationContext, true);
                LocationClient.setAgreePrivacy(true);
                SDKInitializer.setApiKey(runtimeValue.trim());
                SDKInitializer.initialize(applicationContext);
                SDKInitializer.setCoordType(CoordType.BD09LL);
                baiduSdkInitialized = true;
                return true;
            } catch (RuntimeException exception) {
                XLog.e("ERROR initialize map runtime");
                return false;
            }
        }
    }

    private void initXlog() {
        File logPath = getExternalFilesDir("Logs");
        if (logPath == null) {
            return;
        }
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)
                .tag(APP_NAME)
                .enableThreadInfo()
                .enableStackTrace(2)
                .enableBorder()
                .build();
        Printer consolePrinter = new ConsolePrinter();
        Printer filePrinter = new FilePrinter
                .Builder(logPath.getPath())
                .fileNameGenerator(new ChangelessFileNameGenerator(LOG_FILE_NAME))
                .backupStrategy(new NeverBackupStrategy())
                .cleanStrategy(new FileLastModifiedCleanStrategy(MAX_TIME))
                .build();
        XLog.init(config, consolePrinter, filePrinter);
    }
}
