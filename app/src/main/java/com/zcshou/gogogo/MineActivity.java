package com.acooldog.toolbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.AppClientConfig;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.ui.CupertinoDialogs;
import com.acooldog.toolbox.ui.MineComposeView;
import com.acooldog.toolbox.ui.MinePageData;
import com.acooldog.toolbox.utils.GoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MineActivity extends BaseActivity implements MineComposeView.Actions {
    private ExecutorService ioExecutor;
    private InternalAuthStore authStore;
    private MineComposeView contentView;
    private volatile String currentAuthorQq = MinePageData.DEFAULT_AUTHOR_QQ;
    private volatile String currentQqGroup = MinePageData.DEFAULT_QQ_GROUP;
    private volatile String currentBilibiliUrl = MinePageData.DEFAULT_BILIBILI_URL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ioExecutor = Executors.newSingleThreadExecutor();
        authStore = new InternalAuthStore(getApplicationContext());
        contentView = new MineComposeView(this);
        contentView.setActions(this);
        setContentView(contentView);
        CupertinoDockInstaller.install(this, CupertinoDockInstaller.TAB_MINE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (contentView != null) {
            contentView.reloadData();
        }
    }

    @Override
    protected void onDestroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void openInternalLogin() {
        startActivity(new Intent(this, InternalLoginActivity.class));
    }

    @Override
    public void openTutorial() {
        com.acooldog.toolbox.tutorial.TutorialController.replay(this);
    }

    @Override
    public void loadMinePageData(MineComposeView.DataCallback callback) {
        boolean loggedIn = authStore != null && authStore.isLoggedIn();
        ioExecutor.execute(() -> {
            MinePageData fallback = MinePageData.fallback(loggedIn);
            MinePageData resolved = fallback;
            try {
                if (GoUtils.isNetworkAvailable(this)) {
                    AppClientConfig config = ShareModule.from(getApplicationContext())
                            .shareApiClient()
                            .getAppClientConfig();
                    resolved = new MinePageData(
                            loggedIn,
                            // The aggregate endpoint is defined in the handoff document;
                            // keep these fields nullable until that backend contract exists.
                            null,
                            null,
                            null,
                            null,
                            "",
                            java.util.Collections.emptyList(),
                            MinePageData.DEFAULT_AUTHOR_QQ,
                            config.getQqGroupNumber(),
                            MinePageData.DEFAULT_BILIBILI_NAME,
                            config.getBilibiliUrl(),
                            null
                    );
                }
            } catch (Exception ignored) {
                resolved = fallback;
            }
            MinePageData finalResolved = resolved;
            currentAuthorQq = finalResolved.getAuthorQq();
            currentQqGroup = finalResolved.getQqGroup();
            currentBilibiliUrl = finalResolved.getBilibiliUrl();
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    callback.onLoaded(finalResolved);
                }
            });
        });
    }

    @Override
    public void openApp(String appId) {
        Intent intent;
        switch (appId) {
            case "draw_route":
                intent = new Intent(this, HomeActivity.class);
                break;
            case "route_simulation":
                intent = new Intent(this, RouteRunActivity.class);
                break;
            case "route_management":
                intent = new Intent(this, RouteActivity.class);
                break;
            case "usage_tips":
                intent = new Intent(this, UsageTipsActivity.class);
                break;
            case "operation_tips":
                intent = new Intent(this, OperationTipsActivity.class);
                break;
            case "history":
                intent = new Intent(this, HistoryActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }

    @Override
    public void openAuthorQq() {
        CupertinoDialogs.showChoices(
                this,
                "联系作者 QQ",
                "请选择加群还是联系 QQ。",
                new String[]{"加群", "联系 QQ"},
                "取消",
                index -> {
                    if (index == 0) {
                        showGroupInstructions();
                    } else if (index == 1) {
                        copyAuthorQq();
                    }
                }
        );
    }

    @Override
    public void openAuthorBilibili(String url) {
        openExternalUrl(url);
    }

    private void showGroupInstructions() {
        String groupNumber = currentQqGroup;
        CupertinoDialogs.showChoices(
                this,
                "加群说明",
                "需要在哔哩哔哩找到相关视频，关注作者并一键三连，在私信发送“01”即可加群。\n\n"
                        + "QQ群号：" + groupNumber + "\n\n是否跳转到哔哩哔哩？",
                new String[]{"复制作者QQ", "跳转哔哩哔哩"},
                "取消",
                index -> {
                    if (index == 0) {
                        copyAuthorQq();
                    } else if (index == 1) {
                        openExternalUrl(currentBilibiliUrl);
                    }
                }
        );
    }

    private void copyAuthorQq() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("author_qq", currentAuthorQq)
            );
            GoUtils.DisplayToast(this, "已复制到剪贴板");
        }
    }

    private void openExternalUrl(String url) {
        try {
            String resolvedUrl = url == null || url.trim().isEmpty()
                    ? currentBilibiliUrl
                    : url;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)));
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "无法打开链接");
        }
    }
}
