package com.acooldog.toolbox.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.acooldog.toolbox.R;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.share.presentation.ShareModule;

import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeMineFragment extends Fragment {
    private ExecutorService ioExecutor;
    private InternalAuthStore authStore;
    private TextView internalLoginSummaryView;

    public interface Actions {
        void onCheckUpdateClicked();

        void onSettingsClicked();

        void onInternalLoginClicked();

        void onDeveloperOptionsClicked();
    }

    public static HomeMineFragment newInstance() {
        return new HomeMineFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ioExecutor = Executors.newSingleThreadExecutor();
        if (getContext() != null) {
            authStore = new InternalAuthStore(getContext().getApplicationContext());
        }
        bindInternalLoginState(view);
        Actions actions = getActions();
        if (actions == null) {
            return;
        }
        view.findViewById(R.id.card_update_check).setOnClickListener(v -> actions.onCheckUpdateClicked());
        view.findViewById(R.id.card_settings).setOnClickListener(v -> actions.onSettingsClicked());
        view.findViewById(R.id.card_internal_login).setOnClickListener(v -> actions.onInternalLoginClicked());
        view.findViewById(R.id.card_dev).setOnClickListener(v -> actions.onDeveloperOptionsClicked());
    }

    @Override
    public void onResume() {
        super.onResume();
        bindInternalLoginState(getView());
    }

    @Override
    public void onDestroyView() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
            ioExecutor = null;
        }
        internalLoginSummaryView = null;
        super.onDestroyView();
    }

    @Nullable
    private Actions getActions() {
        if (getActivity() instanceof Actions) {
            return (Actions) getActivity();
        }
        return null;
    }

    private void bindInternalLoginState(@Nullable View root) {
        if (root == null) {
            return;
        }
        internalLoginSummaryView = root.findViewById(R.id.home_internal_login_summary);
        renderInternalLoginState();
        refreshInternalAccountProfile();
    }

    private void renderInternalLoginState() {
        if (internalLoginSummaryView == null || authStore == null) {
            return;
        }
        InternalAccountProfile profile = authStore.getProfile();
        if (profile != null && !profile.getUsername().isEmpty()) {
            internalLoginSummaryView.setText(getString(
                    R.string.home_internal_login_logged_in,
                    profile.getUsername(),
                    profile.getTesterTypeLabel()
            ));
        } else {
            internalLoginSummaryView.setText(R.string.home_internal_login_summary);
        }
    }

    private void refreshInternalAccountProfile() {
        Context context = getContext();
        if (authStore == null || ioExecutor == null || context == null || !authStore.isLoggedIn()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        String token = authStore.getToken();
        ioExecutor.execute(() -> {
            try {
                InternalAccountProfile profile = ShareModule.from(appContext)
                        .shareApiClient()
                        .getInternalAccountProfile(token);
                authStore.saveProfile(profile);
                if (isAdded()) {
                    requireActivity().runOnUiThread(this::renderInternalLoginState);
                }
            } catch (Exception ignored) {
                // Keep the cached profile if the backend is temporarily unavailable.
            }
        });
    }
}
