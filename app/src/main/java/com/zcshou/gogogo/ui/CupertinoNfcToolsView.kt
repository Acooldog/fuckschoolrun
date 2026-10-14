package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.AbstractComposeView
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class CupertinoNfcToolsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun onBack()
        fun onCopyUrl(value: String)
        fun onCopyPackage(value: String)
        fun onSave()
        fun onLoadLocal()
        fun onMock()
        fun onShare()
        fun onDownload()
    }

    private data class State(
        val hardwareStatus: String = "",
        val hardwareWarning: Boolean = false,
        val url: String = "",
        val packageName: String = "",
        val status: String = "",
        val statusWarning: Boolean = false
    )

    private var state by mutableStateOf(State())
    private var actions: Actions? = null

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun setHardwareStatus(value: String, warning: Boolean) {
        state = state.copy(hardwareStatus = value, hardwareWarning = warning)
    }

    fun setUrl(value: String) {
        state = state.copy(url = value)
    }

    fun getUrl(): String = state.url.trim()

    fun setPackageName(value: String) {
        state = state.copy(packageName = value)
    }

    fun getPackageName(): String = state.packageName.trim()

    fun setStatus(value: String, warning: Boolean) {
        state = state.copy(status = value, statusWarning = warning)
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            CupertinoScaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color(0xFFF2F2F7),
                topBar = {
                    CupertinoTopAppBar(
                        title = {
                            CupertinoText(
                                "NFC",
                                style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 18.sp)
                            )
                        },
                        navigationIcon = {
                            CupertinoIconButton(
                                onClick = { actions?.onBack() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                CupertinoIcon(
                                    CupertinoIcons.Outlined.ChevronBackward,
                                    contentDescription = "Back",
                                    tint = Color(0xFF007AFF)
                                )
                            }
                        },
                        actions = {}
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F7))
                        .padding(paddingValues)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NfcSection {
                        CupertinoText(
                            state.hardwareStatus,
                            style = TextStyle(
                                color = if (state.hardwareWarning) Color(0xFFFF3B30) else Color(0xFF34C759),
                                fontSize = 13.sp
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        CupertinoText(
                            "统一配置",
                            style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 17.sp)
                        )
                        Spacer(Modifier.height(6.dp))
                        CupertinoText(
                            "读取真实 NFC 或使用下面的模拟数据启动路线。",
                            style = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp)
                        )
                        Spacer(Modifier.height(12.dp))
                        CupertinoTextField(
                            value = state.url,
                            onValueChange = { state = state.copy(url = it) },
                            placeholder = { CupertinoText("链接") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CupertinoButton(
                            onClick = { actions?.onCopyUrl(getUrl()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CupertinoText("复制链接")
                        }
                        CupertinoTextField(
                            value = state.packageName,
                            onValueChange = { state = state.copy(packageName = it) },
                            placeholder = { CupertinoText("应用包名") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CupertinoButton(
                            onClick = { actions?.onCopyPackage(getPackageName()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CupertinoText("复制包名")
                        }
                        if (state.status.isNotEmpty()) {
                            CupertinoText(
                                state.status,
                                style = TextStyle(
                                    color = if (state.statusWarning) Color(0xFFFF3B30) else Color(0xFF34C759),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    CupertinoButton(
                        onClick = { actions?.onMock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CupertinoText("模拟 NFC")
                    }

                    NfcSection {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CupertinoButton(
                                onClick = { actions?.onSave() },
                                modifier = Modifier.weight(1f)
                            ) {
                                CupertinoText("保存")
                            }
                            CupertinoButton(
                                onClick = { actions?.onLoadLocal() },
                                modifier = Modifier.weight(1f)
                            ) {
                                CupertinoText("本地配置")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CupertinoButton(
                                onClick = { actions?.onShare() },
                                modifier = Modifier.weight(1f)
                            ) {
                                CupertinoText("共享 NFC")
                            }
                            CupertinoButton(
                                onClick = { actions?.onDownload() },
                                modifier = Modifier.weight(1f)
                            ) {
                                CupertinoText("下载共享")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
