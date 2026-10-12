package com.acooldog.toolbox;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.ui.MineComposeView;

public class MineActivity extends BaseActivity implements MineComposeView.Actions {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MineComposeView view = new MineComposeView(this);
        view.setActions(this);
        setContentView(view);
        CupertinoDockInstaller.install(this, CupertinoDockInstaller.TAB_MINE);
    }

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void openInternalLogin() {
        startActivity(new Intent(this, InternalLoginActivity.class));
    }
}
