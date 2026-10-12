package com.acooldog.toolbox;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.ui.CupertinoSettingsView;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new CupertinoSettingsView(this));
    }
}
