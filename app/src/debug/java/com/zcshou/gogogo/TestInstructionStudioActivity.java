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
import com.acooldog.toolbox.algorithmkit.instruction.dsl.BasicTestInstruction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.SafetyLevel;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstructionDsl;
import com.acooldog.toolbox.algorithmkit.instruction.executor.ExecutionMode;
import com.acooldog.toolbox.algorithmkit.instruction.executor.SafeExecutionEngine;
import com.acooldog.toolbox.algorithmkit.instruction.executor.ScenarioExecutionReport;
import com.acooldog.toolbox.algorithmkit.instruction.validator.InstructionValidator;
import com.acooldog.toolbox.algorithmkit.instruction.validator.ValidationResult;
import com.acooldog.toolbox.algorithmkit.scenario.ScenarioSerializer;
import com.acooldog.toolbox.algorithmkit.scenario.ScenarioTemplateLibrary;
import com.acooldog.toolbox.algorithmkit.scenario.TestScenario;
import com.acooldog.toolbox.algorithmkit.security.audit.EnhancedAuditLogger;
import com.acooldog.toolbox.algorithmkit.security.watermark.WatermarkMetadata;
import com.acooldog.toolbox.algorithmkit.security.watermark.WatermarkSystem;
import com.acooldog.toolbox.algorithmkit.validation.anticheat.AntiCheatValidator;
import com.acooldog.toolbox.algorithmkit.validation.anticheat.ValidationReport;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.utils.GoUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class TestInstructionStudioActivity extends BaseActivity {
    private static final String PREFS_NAME = "test_instruction_studio";
    private static final String KEY_FUNCTION_ENABLED = "function_enabled";

    private InternalAuthStore authStore;
    private AlgorithmAuditLogger auditLogger;
    private AlgorithmTestFileStore fileStore;
    private SharedPreferences preferences;
    private final InstructionValidator validator = new InstructionValidator();
    private final EnhancedAuditLogger verifiableAuditLogger = new EnhancedAuditLogger();
    private final WatermarkSystem watermarkSystem = new WatermarkSystem();

    private boolean sessionConfirmed;
    private String username = "unknown";
    private int selectedIndex = -1;
    private ExecutionMode executionMode = ExecutionMode.MODE_VALIDATION_ONLY;

    private CheckBox featureSwitch;
    private TextView safetyStatusView;
    private EditText scenarioIdInput;
    private EditText scenarioNameInput;
    private EditText scenarioCategoryInput;
    private EditText searchInput;
    private EditText parameterInput;
    private TextView syntaxStatusView;
    private TextView propertyView;
    private TextView outputView;
    private InstructionTimelineView timelineView;

    private final List<TestInstruction> instructions = new ArrayList<>();
    private final List<TestScenario> templates = new ArrayList<>();
    private TestAction selectedAction = TestAction.GENERATE_STEP_FREQUENCY;

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
        templates.addAll(new ScenarioTemplateLibrary().defaults());
        templates.addAll(loadRawTemplates());
        setTitle(R.string.test_studio_title);
        setContentView(buildContentView());
        startNewScenario();
        refreshSafetyStatus();
        if (getIntent() != null && getIntent().getBooleanExtra("open_library", false)) {
            safetyStatusView.post(this::showTemplatePicker);
        }
    }

    @NonNull
    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = vertical();
        int padding = dp(14);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView notice = text(getString(R.string.test_studio_notice), 13, "#5D4037");
        notice.setBackgroundColor(Color.parseColor("#FFF3E0"));
        notice.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(notice, matchWrap());

        root.addView(toolbar(), matchWrap());
        featureSwitch = new CheckBox(this);
        featureSwitch.setText(R.string.test_studio_feature_switch);
        featureSwitch.setChecked(preferences.getBoolean(KEY_FUNCTION_ENABLED, false));
        featureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_FUNCTION_ENABLED, isChecked).apply();
            appendAuditQuietly("测试指令工作室", "功能开关", "enabled=" + isChecked, "状态已记录");
            refreshSafetyStatus();
        });
        root.addView(featureSwitch, matchWrap());
        Button sessionButton = button(R.string.test_studio_session_confirm, v -> confirmSession());
        root.addView(sessionButton, matchWrap());
        safetyStatusView = text("", 12, "#455A64");
        root.addView(safetyStatusView, matchWrap());

        root.addView(sectionTitle(R.string.test_studio_scenario_props), matchWrap());
        scenarioIdInput = input("custom_scenario", InputType.TYPE_CLASS_TEXT);
        scenarioNameInput = input("自定义测试场景", InputType.TYPE_CLASS_TEXT);
        scenarioCategoryInput = input("custom", InputType.TYPE_CLASS_TEXT);
        root.addView(row(label(R.string.test_studio_scenario_id), scenarioIdInput));
        root.addView(row(label(R.string.test_studio_scenario_name), scenarioNameInput));
        root.addView(row(label(R.string.test_studio_scenario_category), scenarioCategoryInput));

        root.addView(sectionTitle(R.string.test_studio_instruction_library), matchWrap());
        searchInput = input("", InputType.TYPE_CLASS_TEXT);
        searchInput.setHint(R.string.test_studio_search_hint);
        root.addView(searchInput, matchWrap());
        root.addView(actionLibrary(), matchWrap());

        parameterInput = input("", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        parameterInput.setSingleLine(false);
        parameterInput.setMinLines(3);
        parameterInput.setHint(R.string.test_studio_param_hint);
        root.addView(parameterInput, matchWrap());
        root.addView(row(
                button(R.string.test_studio_add_instruction, v -> addSelectedInstruction()),
                button(R.string.test_studio_update_instruction, v -> updateSelectedInstruction())
        ));
        root.addView(row(
                button(R.string.test_studio_move_up, v -> moveSelected(-1)),
                button(R.string.test_studio_move_down, v -> moveSelected(1))
        ));
        root.addView(button(R.string.test_studio_delete_instruction, v -> deleteSelected()), matchWrap());

        root.addView(sectionTitle(R.string.test_studio_timeline), matchWrap());
        timelineView = new InstructionTimelineView(this);
        timelineView.setSelectionListener(index -> {
            selectedIndex = index;
            populateSelectedInstruction();
            refreshEditor();
        });
        root.addView(timelineView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210)));

        syntaxStatusView = text("", 12, "#455A64");
        root.addView(syntaxStatusView, matchWrap());
        propertyView = text("", 12, "#455A64");
        propertyView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        propertyView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(propertyView, matchWrap());

        root.addView(sectionTitle(R.string.test_studio_safety_settings), matchWrap());
        root.addView(row(
                button(R.string.test_studio_mode_validation, v -> setMode(ExecutionMode.MODE_VALIDATION_ONLY)),
                button(R.string.test_studio_mode_simulation, v -> setMode(ExecutionMode.MODE_SIMULATION))
        ));
        root.addView(row(
                button(R.string.test_studio_mode_sandbox, v -> setMode(ExecutionMode.MODE_SANDBOX)),
                button(R.string.test_studio_mode_recording, v -> setMode(ExecutionMode.MODE_RECORDING))
        ));
        root.addView(button(R.string.test_studio_run, v -> confirmAndRun()), matchWrap());
        root.addView(button(R.string.test_studio_anticheat_report, v -> generateAntiCheatReport()), matchWrap());

        outputView = text(getString(R.string.test_studio_output_empty), 12, "#263238");
        outputView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        outputView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(outputView, matchWrap());
        return scrollView;
    }

    private LinearLayout toolbar() {
        LinearLayout toolbar = vertical();
        toolbar.addView(row(
                button(R.string.test_studio_new, v -> startNewScenario()),
                button(R.string.test_studio_templates, v -> showTemplatePicker())
        ));
        toolbar.addView(row(
                button(R.string.test_studio_save_json, v -> saveScenario("json")),
                button(R.string.test_studio_save_yaml, v -> saveScenario("yaml"))
        ));
        toolbar.addView(row(
                button(R.string.test_studio_export_dsl, v -> saveScenario("dsl")),
                button(R.string.test_studio_validate_syntax, v -> validateScenarioOnly())
        ));
        return toolbar;
    }

    private LinearLayout actionLibrary() {
        LinearLayout container = vertical();
        addActionSection(container, R.string.test_studio_category_generation, TestAction.GENERATE_STEP_FREQUENCY, TestAction.GENERATE_GPS_TRAJECTORY, TestAction.GENERATE_SENSOR_DATA);
        addActionSection(container, R.string.test_studio_category_system, TestAction.CHECK_DEVELOPER_OPTIONS, TestAction.CHECK_MOCK_LOCATION);
        addActionSection(container, R.string.test_studio_category_validation, TestAction.VALIDATE_SENSOR_CONSISTENCY, TestAction.VALIDATE_MOTION_PATTERN, TestAction.VALIDATE_LOCATION_DRIFT, TestAction.ASSERT_EXPECTED_RESULT);
        addActionSection(container, R.string.test_studio_category_control, TestAction.WAIT_FOR_DURATION, TestAction.SET_TEST_CONDITION, TestAction.SIMULATE_GPS_JUMP);
        return container;
    }

    private void addActionSection(LinearLayout container, int titleRes, TestAction... actions) {
        container.addView(sectionTitle(titleRes), matchWrap());
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        for (TestAction action : actions) {
            Button actionButton = new Button(this);
            actionButton.setAllCaps(false);
            actionButton.setText(action.name());
            actionButton.setOnClickListener(v -> {
                selectedAction = action;
                parameterInput.setText(defaultParamsFor(action));
                appendAuditQuietly("测试指令工作室", "选择指令", action.name(), "已选择");
            });
            row.addView(actionButton, matchWrap());
        }
        container.addView(row, matchWrap());
    }

    private void confirmSession() {
        if (!BuildConfig.DEBUG || !BuildConfig.ENABLE_ALGORITHM_TEST || !isInternalTester()) {
            GoUtils.DisplayToast(this, getString(R.string.test_studio_safety_failed));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_studio_session_confirm)
                .setMessage(R.string.test_studio_session_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    sessionConfirmed = true;
                    appendAuditQuietly("测试指令工作室", "会话确认", "user=" + username, "会话已确认");
                    refreshSafetyStatus();
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void addSelectedInstruction() {
        try {
            BasicTestInstruction instruction = buildInstructionFromEditor("instruction_" + (instructions.size() + 1));
            instructions.add(instruction);
            selectedIndex = instructions.size() - 1;
            appendAuditQuietly("测试指令工作室", "添加指令", instruction.getAction().name(), "index=" + selectedIndex);
            refreshEditor();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void updateSelectedInstruction() {
        if (selectedIndex < 0 || selectedIndex >= instructions.size()) {
            GoUtils.DisplayToast(this, getString(R.string.test_studio_no_instruction_selected));
            return;
        }
        try {
            BasicTestInstruction instruction = buildInstructionFromEditor(instructions.get(selectedIndex).getId());
            instructions.set(selectedIndex, instruction);
            appendAuditQuietly("测试指令工作室", "更新指令", instruction.getAction().name(), "index=" + selectedIndex);
            refreshEditor();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private BasicTestInstruction buildInstructionFromEditor(String id) {
        Map<String, Object> params = parseParameters(parameterInput.getText() == null ? "" : parameterInput.getText().toString());
        SafetyLevel safetyLevel = safetyFor(selectedAction);
        long delay = longParam(params, "delayAfter", 0L);
        params.remove("delayAfter");
        return new BasicTestInstruction(id, selectedAction, params, delay, safetyLevel);
    }

    private void moveSelected(int direction) {
        int target = selectedIndex + direction;
        if (selectedIndex < 0 || target < 0 || target >= instructions.size()) {
            return;
        }
        Collections.swap(instructions, selectedIndex, target);
        selectedIndex = target;
        refreshEditor();
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= instructions.size()) {
            return;
        }
        instructions.remove(selectedIndex);
        selectedIndex = Math.min(selectedIndex, instructions.size() - 1);
        refreshEditor();
    }

    private void startNewScenario() {
        scenarioIdInput.setText("custom_scenario");
        scenarioNameInput.setText("自定义测试场景");
        scenarioCategoryInput.setText("custom");
        instructions.clear();
        selectedIndex = -1;
        parameterInput.setText(defaultParamsFor(selectedAction));
        refreshEditor();
        appendAuditQuietly("测试指令工作室", "新建场景", "custom_scenario", "已清空编辑区");
    }

    private void showTemplatePicker() {
        String[] names = new String[templates.size()];
        for (int index = 0; index < templates.size(); index++) {
            names[index] = templates.get(index).getName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_studio_templates)
                .setItems(names, (dialog, which) -> applyTemplate(templates.get(which)))
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void applyTemplate(TestScenario scenario) {
        scenarioIdInput.setText(scenario.getId());
        scenarioNameInput.setText(scenario.getName());
        scenarioCategoryInput.setText(scenario.getCategory());
        instructions.clear();
        instructions.addAll(scenario.getInstructions());
        selectedIndex = instructions.isEmpty() ? -1 : 0;
        populateSelectedInstruction();
        refreshEditor();
        appendAuditQuietly("测试指令工作室", "加载模板", scenario.getId(), "instructions=" + instructions.size());
    }

    private void validateScenarioOnly() {
        ScenarioExecutionReport report = new SafeExecutionEngine().execute(buildScenario(), ExecutionMode.MODE_VALIDATION_ONLY);
        outputView.setText(report.toTextReport());
        appendAuditQuietly("测试指令工作室", "语法检查", buildScenario().getId(), "success=" + report.isSuccess());
        refreshEditor();
    }

    private void confirmAndRun() {
        if (!checkSafety()) {
            GoUtils.DisplayToast(this, getString(R.string.test_studio_safety_failed));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_studio_run_confirm_title)
                .setMessage(getString(R.string.test_studio_run_confirm_message, executionMode.name()))
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> runScenario())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void runScenario() {
        TestScenario scenario = buildScenario();
        ScenarioExecutionReport report = new SafeExecutionEngine().execute(scenario, executionMode);
        outputView.setText(report.toTextReport());
        String input = ScenarioSerializer.toJson(scenario);
        String output = report.toTextReport();
        for (TestInstruction instruction : scenario.getInstructions()) {
            verifiableAuditLogger.createLog(username, instruction, input, output).verify();
        }
        appendAuditQuietly("测试指令工作室", "运行场景", "mode=" + executionMode.name() + ",scenario=" + scenario.getId(), "success=" + report.isSuccess() + ",hash=" + AlgorithmHash.sha256(output));
    }

    private void generateAntiCheatReport() {
        TestScenario scenario = buildScenario();
        ValidationReport report = new AntiCheatValidator().validateTestCase(scenario);
        outputView.setText(report.toText());
        appendAuditQuietly("反作弊验证器", "生成报告", scenario.getId(), "riskScore=" + report.getRiskScore());
    }

    private void saveScenario(String format) {
        if (!checkSafety()) {
            GoUtils.DisplayToast(this, getString(R.string.test_studio_safety_failed));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_studio_save_confirm_title)
                .setMessage(R.string.test_studio_save_confirm_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    try {
                        TestScenario scenario = buildScenario();
                        String content;
                        if ("yaml".equals(format)) {
                            content = ScenarioSerializer.toYaml(scenario);
                        } else if ("dsl".equals(format)) {
                            content = TestInstructionDsl.toDsl(scenario);
                        } else {
                            content = ScenarioSerializer.toJson(scenario);
                        }
                        WatermarkMetadata metadata = new WatermarkMetadata(
                                username,
                                scenario.getId(),
                                System.currentTimeMillis(),
                                System.currentTimeMillis() + (7L * 24L * 60L * 60L * 1000L)
                        );
                        byte[] watermarked = watermarkSystem.embedWatermark(content.getBytes(StandardCharsets.UTF_8), metadata);
                        fileStore.saveBytes("scenario_" + scenario.getId(), format, watermarked);
                        appendAuditQuietly("测试指令工作室", "保存场景", "format=" + format + ",scenario=" + scenario.getId(), "bytes=" + watermarked.length + ",hash=" + AlgorithmHash.sha256(new String(watermarked, StandardCharsets.UTF_8)));
                        GoUtils.DisplayToast(this, getString(R.string.test_studio_save_success));
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, exception.getMessage());
                    }
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void setMode(ExecutionMode mode) {
        executionMode = mode;
        refreshEditor();
    }

    private void populateSelectedInstruction() {
        if (selectedIndex < 0 || selectedIndex >= instructions.size()) {
            return;
        }
        TestInstruction instruction = instructions.get(selectedIndex);
        selectedAction = instruction.getAction();
        parameterInput.setText(paramsToLines(instruction));
    }

    private void refreshEditor() {
        timelineView.setInstructions(instructions, selectedIndex);
        StringBuilder syntax = new StringBuilder();
        boolean accepted = true;
        for (TestInstruction instruction : instructions) {
            ValidationResult result = validator.validate(instruction);
            accepted = accepted && result.isAccepted();
            syntax.append(instruction.getId()).append(": ").append(result.getMessage()).append('\n');
        }
        syntaxStatusView.setText(accepted ? getString(R.string.test_studio_syntax_ok) : syntax.toString());
        propertyView.setText(getString(
                R.string.test_studio_property_summary,
                executionMode.name(),
                instructions.size(),
                selectedIndex < 0 ? "-" : instructions.get(selectedIndex).getAction().name()
        ));
    }

    private TestScenario buildScenario() {
        return new TestScenario(
                textOf(scenarioIdInput, "custom_scenario"),
                textOf(scenarioNameInput, "自定义测试场景"),
                "",
                textOf(scenarioCategoryInput, "custom"),
                instructions
        );
    }

    private List<TestScenario> loadRawTemplates() {
        List<TestScenario> loaded = new ArrayList<>();
        try (InputStream stream = getResources().openRawResource(R.raw.scenario_templates)) {
            String raw = readFully(stream);
            org.json.JSONObject root = new org.json.JSONObject(raw);
            org.json.JSONArray array = root.optJSONArray("scenario_templates");
            if (array == null) {
                return loaded;
            }
            for (int index = 0; index < array.length(); index++) {
                org.json.JSONObject item = array.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                loaded.add(parseTemplate(item));
            }
        } catch (Exception ignored) {
            // Built-in Java templates still keep the studio usable.
        }
        return loaded;
    }

    private TestScenario parseTemplate(org.json.JSONObject item) {
        List<TestInstruction> parsedInstructions = new ArrayList<>();
        org.json.JSONArray array = item.optJSONArray("instructions");
        if (array != null) {
            for (int index = 0; index < array.length(); index++) {
                org.json.JSONObject instructionObject = array.optJSONObject(index);
                if (instructionObject == null) {
                    continue;
                }
                TestAction action = TestAction.valueOf(instructionObject.optString("action", "WAIT_FOR_DURATION"));
                Map<String, Object> params = new LinkedHashMap<>();
                org.json.JSONObject paramObject = instructionObject.optJSONObject("params");
                if (paramObject != null) {
                    java.util.Iterator<String> keys = paramObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        params.put(key, paramObject.opt(key));
                    }
                }
                long delay = instructionObject.optLong("delayAfter", 0L);
                parsedInstructions.add(new BasicTestInstruction("template_" + index, action, params, delay, safetyFor(action)));
            }
        }
        return new TestScenario(
                item.optString("id", "template_" + System.currentTimeMillis()),
                item.optString("name", "模板场景"),
                item.optString("description", ""),
                item.optString("category", "template"),
                parsedInstructions
        );
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
        safetyStatusView.setText(getString(
                R.string.test_studio_safety_status,
                BuildConfig.DEBUG ? "true" : "false",
                BuildConfig.ENABLE_ALGORITHM_TEST ? "true" : "false",
                isInternalTester() ? username : getString(R.string.algorithm_lab_not_logged_in),
                featureSwitch != null && featureSwitch.isChecked() ? "true" : "false",
                sessionConfirmed ? "true" : "false"
        ));
    }

    private Map<String, Object> parseParameters(String raw) {
        Map<String, Object> params = new LinkedHashMap<>();
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int index = trimmed.indexOf('=');
            if (index <= 0) {
                throw new IllegalArgumentException("参数格式应为 key=value: " + trimmed);
            }
            params.put(trimmed.substring(0, index).trim(), coerce(trimmed.substring(index + 1).trim()));
        }
        return params;
    }

    private String defaultParamsFor(TestAction action) {
        switch (action) {
            case GENERATE_STEP_FREQUENCY:
                return "base_spm=180\nduration=300";
            case GENERATE_GPS_TRAJECTORY:
                return "start_lat=36.667662\nstart_lon=117.027707\nend_lat=36.669000\nend_lon=117.030000\nspeed_mps=4.0";
            case SIMULATE_GPS_JUMP:
                return "jump_distance=1000\nduration=5";
            case WAIT_FOR_DURATION:
                return "delayAfter=1000";
            case SET_TEST_CONDITION:
                return "name=baseline\nvalue=normal";
            case ASSERT_EXPECTED_RESULT:
                return "expected=pass";
            default:
                return "";
        }
    }

    private String paramsToLines(TestInstruction instruction) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : instruction.getParameters().entrySet()) {
            builder.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        if (instruction.getDelayAfter() > 0L) {
            builder.append("delayAfter=").append(instruction.getDelayAfter()).append('\n');
        }
        return builder.toString();
    }

    private SafetyLevel safetyFor(TestAction action) {
        switch (action) {
            case CHECK_DEVELOPER_OPTIONS:
            case CHECK_MOCK_LOCATION:
                return SafetyLevel.READ_ONLY_MONITOR;
            default:
                return SafetyLevel.SANDBOX_EXECUTION;
        }
    }

    private Object coerce(String raw) {
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private long longParam(Map<String, Object> params, String key, long fallback) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String textOf(EditText editText, String fallback) {
        String value = editText.getText() == null ? "" : editText.getText().toString().trim();
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String readFully(InputStream stream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString("UTF-8");
    }

    private void appendAuditQuietly(@NonNull String module, @NonNull String action, @NonNull String input, @NonNull String output) {
        try {
            auditLogger.append(username, module, action, input, output);
        } catch (Exception ignored) {
            // UI remains usable if the local encrypted audit backend is unavailable.
        }
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView sectionTitle(int resId) {
        TextView view = text(getString(resId), 16, "#1F2A33");
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(14), 0, dp(6));
        return view;
    }

    private TextView label(int resId) {
        TextView view = text(getString(resId), 13, "#455A64");
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(int resId, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(resId);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout row(@NonNull View left, @NonNull View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private EditText input(String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setText(value);
        return editText;
    }

    private TextView text(String value, int sp, String color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.parseColor(color));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
