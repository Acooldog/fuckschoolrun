package com.acooldog.toolbox;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;
import com.acooldog.toolbox.algorithmkit.ConsistencyReport;
import com.acooldog.toolbox.algorithmkit.GpsTrajectoryGenerator;
import com.acooldog.toolbox.algorithmkit.GpsTrajectoryResult;
import com.acooldog.toolbox.algorithmkit.SensorConsistencyVerifier;
import com.acooldog.toolbox.algorithmkit.StepCadenceSimulator;
import com.acooldog.toolbox.algorithmkit.StepSample;
import com.acooldog.toolbox.algorithmkit.StepSimulationResult;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.utils.GoUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class AlgorithmTestLabActivity extends BaseActivity {
    private static final String PREFS_NAME = "algorithm_test_lab";
    private static final String KEY_FUNCTION_ENABLED = "function_enabled";

    private InternalAuthStore authStore;
    private AlgorithmAuditLogger auditLogger;
    private AlgorithmTestFileStore fileStore;
    private SharedPreferences preferences;

    private boolean sessionConfirmed;
    private String username = "unknown";

    private CheckBox featureSwitch;
    private TextView safetyStatusView;
    private AlgorithmChartView chartView;
    private TextView outputView;
    private TextView fileListView;

    private EditText cadenceInput;
    private EditText stepDurationInput;
    private EditText startLatitudeInput;
    private EditText startLongitudeInput;
    private EditText endLatitudeInput;
    private EditText endLongitudeInput;
    private EditText gpsSpeedInput;

    private StepSimulationResult latestStepResult;
    private GpsTrajectoryResult latestGpsResult;
    private ConsistencyReport latestConsistencyReport;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authStore = new InternalAuthStore(getApplicationContext());
        auditLogger = new AlgorithmAuditLogger(getApplicationContext());
        fileStore = new AlgorithmTestFileStore(getApplicationContext());
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        InternalAccountProfile profile = authStore.getProfile();
        if (profile != null && !TextUtils.isEmpty(profile.getUsername())) {
            username = profile.getUsername();
        }
        setTitle(R.string.algorithm_lab_title);
        setContentView(buildContentView());
        refreshSafetyStatus();
        refreshGeneratedFileList();
    }

    @NonNull
    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView noticeView = text(getString(R.string.algorithm_lab_notice), 13, "#5D4037");
        noticeView.setBackgroundColor(Color.parseColor("#FFF3E0"));
        noticeView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(noticeView, matchWrap());

        featureSwitch = new CheckBox(this);
        featureSwitch.setText(R.string.algorithm_lab_feature_switch);
        featureSwitch.setChecked(preferences.getBoolean(KEY_FUNCTION_ENABLED, false));
        featureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_FUNCTION_ENABLED, isChecked).apply();
            appendAuditQuietly("安全确认", "功能开关", "enabled=" + isChecked, "状态已记录");
            refreshSafetyStatus();
        });
        root.addView(featureSwitch, matchWrap());

        Button sessionButton = new Button(this);
        sessionButton.setText(R.string.algorithm_lab_session_confirm);
        sessionButton.setOnClickListener(v -> confirmSession());
        root.addView(sessionButton, matchWrap());

        safetyStatusView = text("", 13, "#455A64");
        root.addView(safetyStatusView, matchWrap());

        root.addView(sectionTitle(R.string.algorithm_lab_step_title), matchWrap());
        cadenceInput = input("180", InputType.TYPE_CLASS_NUMBER);
        stepDurationInput = input("300", InputType.TYPE_CLASS_NUMBER);
        root.addView(row(label(R.string.algorithm_lab_cadence_label), cadenceInput));
        root.addView(row(label(R.string.algorithm_lab_duration_label), stepDurationInput));
        Button generateStepButton = new Button(this);
        generateStepButton.setText(R.string.algorithm_lab_generate_step);
        generateStepButton.setOnClickListener(v -> confirmGenerate("步频模拟器", this::generateStepData));
        root.addView(generateStepButton, matchWrap());

        root.addView(sectionTitle(R.string.algorithm_lab_gps_title), matchWrap());
        startLatitudeInput = input("36.667662", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        startLongitudeInput = input("117.027707", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        endLatitudeInput = input("36.669000", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        endLongitudeInput = input("117.030000", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        gpsSpeedInput = input("4.0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(row(label(R.string.algorithm_lab_start_latitude), startLatitudeInput));
        root.addView(row(label(R.string.algorithm_lab_start_longitude), startLongitudeInput));
        root.addView(row(label(R.string.algorithm_lab_end_latitude), endLatitudeInput));
        root.addView(row(label(R.string.algorithm_lab_end_longitude), endLongitudeInput));
        root.addView(row(label(R.string.algorithm_lab_speed_label), gpsSpeedInput));
        Button generateGpsButton = new Button(this);
        generateGpsButton.setText(R.string.algorithm_lab_generate_gps);
        generateGpsButton.setOnClickListener(v -> confirmGenerate("GPS轨迹生成器", this::generateGpsData));
        root.addView(generateGpsButton, matchWrap());

        root.addView(sectionTitle(R.string.algorithm_lab_consistency_title), matchWrap());
        Button verifyConsistencyButton = new Button(this);
        verifyConsistencyButton.setText(R.string.algorithm_lab_generate_consistency);
        verifyConsistencyButton.setOnClickListener(v -> confirmGenerate("传感器一致性验证", this::generateConsistencyReport));
        root.addView(verifyConsistencyButton, matchWrap());

        chartView = new AlgorithmChartView(this);
        root.addView(chartView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240)));

        outputView = text(R.string.algorithm_lab_output_empty, 13, "#263238");
        outputView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        outputView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(outputView, matchWrap());

        root.addView(sectionTitle(R.string.algorithm_lab_export_title), matchWrap());
        root.addView(exportRow(
                button(R.string.algorithm_lab_save_step_json, v -> saveLatest("step", "json", latestStepResult == null ? null : latestStepResult.toJson(), "步频模拟器")),
                button(R.string.algorithm_lab_save_step_csv, v -> saveLatest("step", "csv", latestStepResult == null ? null : latestStepResult.toCsv(), "步频模拟器"))
        ));
        root.addView(exportRow(
                button(R.string.algorithm_lab_save_gps_json, v -> saveLatest("gps", "json", latestGpsResult == null ? null : latestGpsResult.toJson(), "GPS轨迹生成器")),
                button(R.string.algorithm_lab_save_gps_nmea, v -> saveLatest("gps", "nmea", latestGpsResult == null ? null : latestGpsResult.toNmeaGprmc(), "GPS轨迹生成器"))
        ));
        root.addView(exportRow(
                button(R.string.algorithm_lab_save_gps_gpx, v -> saveLatest("gps", "gpx", latestGpsResult == null ? null : latestGpsResult.toGpx(), "GPS轨迹生成器")),
                button(R.string.algorithm_lab_save_gps_kml, v -> saveLatest("gps", "kml", latestGpsResult == null ? null : latestGpsResult.toKml(), "GPS轨迹生成器"))
        ));
        root.addView(exportRow(
                button(R.string.algorithm_lab_save_report_json, v -> saveLatest("consistency", "json", latestConsistencyReport == null ? null : latestConsistencyReport.toJson(), "传感器一致性验证")),
                button(R.string.algorithm_lab_save_report_csv, v -> saveLatest("consistency", "csv", latestConsistencyReport == null ? null : latestConsistencyReport.toCsv(), "传感器一致性验证"))
        ));

        root.addView(sectionTitle(R.string.algorithm_lab_data_management_title), matchWrap());
        fileListView = text("", 12, "#455A64");
        fileListView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        fileListView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(fileListView, matchWrap());
        return scrollView;
    }

    private void confirmSession() {
        if (!BuildConfig.DEBUG || !BuildConfig.ENABLE_ALGORITHM_TEST || !isInternalTester()) {
            GoUtils.DisplayToast(this, getString(R.string.algorithm_lab_safety_failed));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.algorithm_lab_session_confirm)
                .setMessage(R.string.algorithm_lab_session_confirm_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    sessionConfirmed = true;
                    appendAuditQuietly("安全确认", "会话确认", "user=" + username, "会话已确认");
                    refreshSafetyStatus();
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void confirmGenerate(@NonNull String module, @NonNull Runnable action) {
        if (!checkSafety()) {
            GoUtils.DisplayToast(this, getString(R.string.algorithm_lab_safety_failed));
            refreshSafetyStatus();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.algorithm_lab_operation_confirm_title)
                .setMessage(getString(R.string.algorithm_lab_operation_confirm_message, module))
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> action.run())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void generateStepData() {
        try {
            int cadence = parseInt(cadenceInput, "步频");
            int duration = parseInt(stepDurationInput, "运动时间");
            latestStepResult = new StepCadenceSimulator().generate(cadence, duration);
            String json = latestStepResult.toJson();
            String dataHash = AlgorithmHash.sha256(json);
            renderStepChart(latestStepResult);
            outputView.setText(getString(
                    R.string.algorithm_lab_step_output,
                    latestStepResult.getSamples().size(),
                    latestStepResult.getTotalSteps(),
                    dataHash
            ));
            appendAuditQuietly(
                    "步频模拟器",
                    "生成数据",
                    "步频=" + cadence + ",时长=" + duration + "s",
                    latestStepResult.outputSummary() + ",hash=" + dataHash
            );
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void generateGpsData() {
        try {
            double startLatitude = parseDouble(startLatitudeInput, "起点纬度");
            double startLongitude = parseDouble(startLongitudeInput, "起点经度");
            double endLatitude = parseDouble(endLatitudeInput, "终点纬度");
            double endLongitude = parseDouble(endLongitudeInput, "终点经度");
            double speed = parseDouble(gpsSpeedInput, "运动速度");
            latestGpsResult = new GpsTrajectoryGenerator().generate(
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    speed
            );
            String nmea = latestGpsResult.toNmeaGprmc();
            String dataHash = AlgorithmHash.sha256(nmea);
            renderGpsChart(latestGpsResult);
            outputView.setText(getString(
                    R.string.algorithm_lab_gps_output,
                    latestGpsResult.getPoints().size(),
                    latestGpsResult.getDistanceMeters(),
                    dataHash
            ));
            appendAuditQuietly(
                    "GPS轨迹生成器",
                    "生成数据",
                    String.format(Locale.US, "起点=%.6f,%.6f,终点=%.6f,%.6f,速度=%.2f",
                            startLatitude, startLongitude, endLatitude, endLongitude, speed),
                    latestGpsResult.outputSummary() + ",hash=" + dataHash
            );
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void generateConsistencyReport() {
        try {
            if (latestStepResult == null || latestGpsResult == null) {
                throw new IllegalArgumentException(getString(R.string.algorithm_lab_need_step_and_gps));
            }
            latestConsistencyReport = new SensorConsistencyVerifier().verify(latestStepResult, latestGpsResult);
            String json = latestConsistencyReport.toJson();
            String dataHash = AlgorithmHash.sha256(json);
            renderConsistencyChart(latestConsistencyReport);
            outputView.setText(getString(
                    R.string.algorithm_lab_consistency_output,
                    latestConsistencyReport.getScore(),
                    latestConsistencyReport.getStrideMeters(),
                    latestConsistencyReport.getVerdict(),
                    dataHash
            ));
            appendAuditQuietly(
                    "传感器一致性验证",
                    "生成报告",
                    "步频数据点=" + latestStepResult.getSamples().size() + ",轨迹点=" + latestGpsResult.getPoints().size(),
                    latestConsistencyReport.outputSummary() + ",hash=" + dataHash
            );
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void saveLatest(
            @NonNull String prefix,
            @NonNull String extension,
            @Nullable String content,
            @NonNull String module
    ) {
        if (TextUtils.isEmpty(content)) {
            GoUtils.DisplayToast(this, getString(R.string.algorithm_lab_no_data_to_save));
            return;
        }
        if (!checkSafety()) {
            GoUtils.DisplayToast(this, getString(R.string.algorithm_lab_safety_failed));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.algorithm_lab_save_confirm_title)
                .setMessage(R.string.algorithm_lab_save_confirm_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    try {
                        File file = fileStore.save(prefix, extension, content);
                        String dataHash = AlgorithmHash.sha256(content);
                        appendAuditQuietly(
                                module,
                                "保存数据",
                                "format=" + extension,
                                "file=" + file.getName() + ",bytes=" + file.length() + ",hash=" + dataHash
                        );
                        refreshGeneratedFileList();
                        GoUtils.DisplayToast(this, getString(R.string.algorithm_lab_save_success, file.getName()));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, exception.getMessage());
                    }
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private boolean checkSafety() {
        return BuildConfig.DEBUG
                && BuildConfig.ENABLE_ALGORITHM_TEST
                && isInternalTester()
                && featureSwitch != null
                && featureSwitch.isChecked()
                && sessionConfirmed;
    }

    private boolean isInternalTester() {
        return authStore != null && authStore.isLoggedIn();
    }

    private void refreshSafetyStatus() {
        String status = getString(
                R.string.algorithm_lab_safety_status,
                BuildConfig.DEBUG ? "true" : "false",
                BuildConfig.ENABLE_ALGORITHM_TEST ? "true" : "false",
                isInternalTester() ? username : getString(R.string.algorithm_lab_not_logged_in),
                featureSwitch != null && featureSwitch.isChecked() ? "true" : "false",
                sessionConfirmed ? "true" : "false"
        );
        safetyStatusView.setText(status);
    }

    private void refreshGeneratedFileList() {
        List<File> files = fileStore.listGeneratedFiles();
        if (files.isEmpty()) {
            fileListView.setText(R.string.algorithm_lab_file_empty);
            return;
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(files.size(), 20);
        for (int index = 0; index < limit; index++) {
            File file = files.get(index);
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(file.getName()).append(" | ").append(file.length()).append(" bytes");
        }
        builder.append('\n').append(getString(R.string.algorithm_lab_audit_file, auditLogger.auditFile().getName()));
        fileListView.setText(builder.toString());
    }

    private void renderStepChart(@NonNull StepSimulationResult result) {
        List<Float> acceleration = new ArrayList<>();
        List<Float> steps = new ArrayList<>();
        int stride = Math.max(1, result.getSamples().size() / 180);
        for (int index = 0; index < result.getSamples().size(); index += stride) {
            StepSample sample = result.getSamples().get(index);
            acceleration.add((float) sample.getAccelerationZ());
            steps.add((float) sample.getStepCount());
        }
        chartView.setSeries(getString(R.string.algorithm_lab_step_chart_label), acceleration, steps);
    }

    private void renderGpsChart(@NonNull GpsTrajectoryResult result) {
        List<Float> latitudes = new ArrayList<>();
        List<Float> speeds = new ArrayList<>();
        int stride = Math.max(1, result.getPoints().size() / 180);
        for (int index = 0; index < result.getPoints().size(); index += stride) {
            latitudes.add((float) result.getPoints().get(index).getLatitude());
            speeds.add((float) result.getPoints().get(index).getSpeedMetersPerSecond());
        }
        chartView.setSeries(getString(R.string.algorithm_lab_gps_chart_label), latitudes, speeds);
    }

    private void renderConsistencyChart(@NonNull ConsistencyReport report) {
        List<Float> metrics = new ArrayList<>();
        metrics.add((float) report.getScore());
        metrics.add((float) (report.getStrideMeters() * 50f));
        metrics.add((float) (report.getGpsAverageSpeed() * 20f));
        metrics.add((float) (report.getStepDerivedSpeed() * 20f));
        chartView.setSeries(getString(R.string.algorithm_lab_consistency_chart_label), metrics, new ArrayList<>());
    }

    private void appendAuditQuietly(
            @NonNull String module,
            @NonNull String action,
            @NonNull String input,
            @NonNull String output
    ) {
        try {
            auditLogger.append(username, module, action, input, output);
        } catch (Exception ignored) {
            // Keep algorithm generation usable even if local audit encryption is unavailable.
        }
    }

    private int parseInt(@NonNull EditText input, @NonNull String label) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "无效");
        }
    }

    private double parseDouble(@NonNull EditText input, @NonNull String label) {
        try {
            return Double.parseDouble(input.getText().toString().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "无效");
        }
    }

    private TextView sectionTitle(int resId) {
        TextView view = text(resId, 16, "#1F2A33");
        view.setGravity(Gravity.START);
        view.setPadding(0, dp(18), 0, dp(6));
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView label(int resId) {
        TextView view = text(resId, 14, "#455A64");
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private EditText input(@NonNull String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setText(value);
        return editText;
    }

    private Button button(int resId, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(resId);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout row(@NonNull View label, @NonNull View input) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout exportRow(@NonNull Button left, @NonNull Button right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private TextView text(int resId, int sp, String color) {
        return text(getString(resId), sp, color);
    }

    private TextView text(@NonNull String value, int sp, String color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.parseColor(color));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
