package com.acooldog.toolbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.FsrBindingState;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.utils.GoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FsrBindingActivity extends BaseActivity {
    public static final String EXTRA_COMMAND = "fsr_binding_command";
    public static final String EXTRA_JOB_ID = "fsr_binding_job_id";

    private InternalAuthStore authStore;
    private ExecutorService ioExecutor;
    private TextView commandView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button copyButton;
    private String command = "";
    private boolean destroyed;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollStatus();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fsr_binding);

        authStore = new InternalAuthStore(getApplicationContext());
        ioExecutor = Executors.newSingleThreadExecutor();
        commandView = findViewById(R.id.fsr_binding_command);
        statusView = findViewById(R.id.fsr_binding_status);
        progressBar = findViewById(R.id.fsr_binding_progress);
        copyButton = findViewById(R.id.fsr_binding_copy);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_fsr_binding);
        toolbar.setNavigationOnClickListener(v -> finish());
        copyButton.setOnClickListener(v -> copyCommand());

        command = getIntent().getStringExtra(EXTRA_COMMAND);
        if (command == null) {
            command = "";
        }
        commandView.setText(command);
        if (TextUtils.isEmpty(command)) {
            loadInitialState();
        } else {
            statusView.setText(R.string.fsr_binding_waiting);
            handler.post(pollRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacks(pollRunnable);
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void loadInitialState() {
        String token = authStore.getToken();
        if (TextUtils.isEmpty(token)) {
            statusView.setText(R.string.fsr_binding_session_expired);
            return;
        }
        statusView.setText(R.string.fsr_binding_loading);
        ioExecutor.execute(() -> {
            try {
                FsrBindingState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getFsrBindingStatus(token, true);
                runOnUiThread(() -> applyState(state));
            } catch (Exception exception) {
                runOnUiThread(() -> statusView.setText(
                        getString(R.string.fsr_binding_failed, safeMessage(exception))
                ));
            }
        });
    }

    private void pollStatus() {
        if (destroyed) {
            return;
        }
        String token = authStore.getToken();
        if (TextUtils.isEmpty(token)) {
            statusView.setText(R.string.fsr_binding_session_expired);
            return;
        }
        ioExecutor.execute(() -> {
            try {
                FsrBindingState state = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getFsrBindingStatus(token, false);
                if (!destroyed) {
                    runOnUiThread(() -> applyState(state));
                }
            } catch (Exception exception) {
                if (!destroyed) {
                    runOnUiThread(() -> {
                        statusView.setText(getString(
                                R.string.fsr_binding_failed,
                                safeMessage(exception)
                        ));
                        handler.postDelayed(pollRunnable, 3000L);
                    });
                }
            }
        });
    }

    private void applyState(FsrBindingState state) {
        if (state == null) {
            statusView.setText(R.string.fsr_binding_waiting);
            handler.postDelayed(pollRunnable, 2000L);
            return;
        }
        if (!TextUtils.isEmpty(state.getCommand())) {
            command = state.getCommand();
            commandView.setText(command);
        }
        String stateValue = state.getStatus();
        if ("bound".equalsIgnoreCase(stateValue)) {
            progressBar.setVisibility(View.GONE);
            statusView.setText(R.string.fsr_binding_success);
            copyButton.setEnabled(false);
            return;
        }
        if ("failed".equalsIgnoreCase(stateValue)) {
            progressBar.setVisibility(View.GONE);
            statusView.setText(TextUtils.isEmpty(state.getMessage())
                    ? getString(R.string.fsr_binding_failed, "")
                    : state.getMessage());
            handler.postDelayed(pollRunnable, 5000L);
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        if (state.getQueuePosition() > 0) {
            statusView.setText(getString(
                    R.string.fsr_binding_queue_position,
                    state.getQueuePosition()
            ));
        } else if ("processing".equalsIgnoreCase(stateValue)) {
            statusView.setText(R.string.fsr_binding_processing);
        } else {
            statusView.setText(R.string.fsr_binding_waiting);
        }
        handler.postDelayed(pollRunnable, 2000L);
    }

    private void copyCommand() {
        if (TextUtils.isEmpty(command)) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("FSR binding command", command));
        GoUtils.DisplayToast(this, getString(R.string.fsr_binding_copied));
    }

    private String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        return TextUtils.isEmpty(message) ? "network error" : message;
    }
}
