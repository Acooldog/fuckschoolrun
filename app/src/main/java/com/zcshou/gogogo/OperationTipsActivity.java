package com.zcshou.gogogo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.appbar.MaterialToolbar;

public class OperationTipsActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation_tips);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_operation_tips);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
