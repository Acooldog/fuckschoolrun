package com.acooldog.toolbox.ui

import com.acooldog.toolbox.tutorial.tutorialAnchor
import com.acooldog.toolbox.tutorial.tutorialNamedAnchor

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

private val AuthBackground = Color(0xFFF2F2F7)
private val AuthSurface = Color.White
private val AuthPrimary = Color(0xFF1C1C1E)
private val AuthSecondary = Color(0xFF6D6D72)
private val AuthAccent = Color(0xFF007AFF)

class CupertinoInternalLoginView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun onBack()
        fun onLogin()
        fun onLogout()
        fun onRegister()
    }

    data class State(
        val status: String = "",
        val username: String = "",
        val password: String = "",
        val deviceId: String = "",
        val deviceName: String = "",
        val loggedIn: Boolean = false,
        val loading: Boolean = false
    )

    private var actions: Actions? = null
    private var state by mutableStateOf(State())

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun getUsername(): String = state.username.trim()

    fun getPassword(): String = state.password.trim()

    fun setUsername(value: String) {
        state = state.copy(username = value)
    }

    fun setPassword(value: String) {
        state = state.copy(password = value)
    }

    fun setStatus(value: String) {
        state = state.copy(status = value)
    }

    fun setLoading(value: Boolean) {
        state = state.copy(loading = value)
    }

    fun render(
        status: String,
        username: String,
        deviceId: String,
        deviceName: String,
        loggedIn: Boolean
    ) {
        state = state.copy(
            status = status,
            username = username,
            deviceId = deviceId,
            deviceName = deviceName,
            loggedIn = loggedIn
        )
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            InternalLoginContent(state, actions) { updated ->
                state = updated
            }
        }
    }
}

@Composable
private fun InternalLoginContent(
    state: CupertinoInternalLoginView.State,
    actions: CupertinoInternalLoginView.Actions?,
    onStateChanged: (CupertinoInternalLoginView.State) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CupertinoTopAppBar(
            title = {
                CupertinoText(
                    "登录",
                    style = TextStyle(color = AuthPrimary, fontSize = 18.sp)
                )
            },
            navigationIcon = {
                CupertinoIconButton(
                    onClick = { actions?.onBack() },
                    modifier = Modifier.size(44.dp).tutorialAnchor("login.back", onClick = { actions?.onBack() })
                ) {
                    Icon(
                        CupertinoIcons.Outlined.ChevronBackward,
                        contentDescription = "返回",
                        tint = AuthAccent
                    )
                }
            },
            actions = {}
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(
                "内部账号",
                style = TextStyle(color = AuthPrimary, fontSize = 28.sp)
            )
            BasicText(
                "登录后可同步共享内容和账号状态。",
                style = TextStyle(color = AuthSecondary, fontSize = 14.sp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuthSurface)
            ) {
                AuthField(
                    value = state.username,
                    placeholder = "账号",
                    onValueChange = { onStateChanged(state.copy(username = it)) }
                )
                AuthField(
                    value = state.password,
                    placeholder = "密码",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(password = it)) }
                )
            }

            DeviceInfo(
                deviceId = state.deviceId,
                deviceName = state.deviceName
            )

            if (state.status.isNotEmpty()) {
                BasicText(
                    state.status,
                    style = TextStyle(color = AuthSecondary, fontSize = 13.sp)
                )
            }

            CupertinoButton(
                onClick = { actions?.onLogin() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().tutorialAnchor("login.submit", !state.loading)
            ) {
                BasicText(if (state.loading) "登录中…" else "登录")
            }
            CupertinoButton(
                onClick = { actions?.onRegister() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().tutorialAnchor("login.register", !state.loading,
                    onClick = { actions?.onRegister() })
            ) {
                BasicText("注册账号")
            }
            if (state.loggedIn) {
                CupertinoButton(
                    onClick = { actions?.onLogout() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().tutorialAnchor("login.logout", !state.loading)
                ) {
                    BasicText("退出登录")
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    placeholder: String,
    password: Boolean = false,
    onValueChange: (String) -> Unit
) {
    CupertinoTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .tutorialNamedAnchor(placeholder, if (placeholder == "账号" || placeholder == "密码") "login" else "register")
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { CupertinoText(placeholder, style = TextStyle(color = AuthSecondary)) },
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        textStyle = TextStyle(color = AuthPrimary, fontSize = 16.sp)
    )
}

@Composable
private fun DeviceInfo(deviceId: String, deviceName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AuthSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        BasicText("设备", style = TextStyle(color = AuthPrimary, fontSize = 15.sp))
        BasicText(deviceName, style = TextStyle(color = AuthSecondary, fontSize = 13.sp))
        BasicText(deviceId, style = TextStyle(color = AuthSecondary, fontSize = 12.sp))
    }
}

class CupertinoEmailRegisterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun onBack()
        fun onSendCode()
        fun onRegister()
    }

    data class State(
        val email: String = "",
        val code: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val status: String = "",
        val sendCodeEnabled: Boolean = true,
        val sendCodeLabel: String = "获取验证码",
        val registerEnabled: Boolean = true
    )

    private var actions: Actions? = null
    private var state by mutableStateOf(State())

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun getEmail(): String = state.email.trim()
    fun getCode(): String = state.code.trim()
    fun getPassword(): String = state.password.trim()
    fun getConfirmPassword(): String = state.confirmPassword.trim()

    fun setEmail(value: String) {
        state = state.copy(email = value)
    }

    fun setStatus(value: String) {
        state = state.copy(status = value)
    }

    fun setSendCodeEnabled(value: Boolean) {
        state = state.copy(sendCodeEnabled = value)
    }

    fun setSendCodeLabel(value: String) {
        state = state.copy(sendCodeLabel = value)
    }

    fun setRegisterEnabled(value: Boolean) {
        state = state.copy(registerEnabled = value)
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            EmailRegisterContent(state, actions) { updated ->
                state = updated
            }
        }
    }
}

@Composable
private fun EmailRegisterContent(
    state: CupertinoEmailRegisterView.State,
    actions: CupertinoEmailRegisterView.Actions?,
    onStateChanged: (CupertinoEmailRegisterView.State) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CupertinoTopAppBar(
            title = {
                CupertinoText(
                    "注册",
                    style = TextStyle(color = AuthPrimary, fontSize = 18.sp)
                )
            },
            navigationIcon = {
                CupertinoIconButton(
                    onClick = { actions?.onBack() },
                    modifier = Modifier.size(44.dp).tutorialAnchor("register.back", onClick = { actions?.onBack() })
                ) {
                    Icon(
                        CupertinoIcons.Outlined.ChevronBackward,
                        contentDescription = "返回",
                        tint = AuthAccent
                    )
                }
            },
            actions = {}
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText("注册账号", style = TextStyle(color = AuthPrimary, fontSize = 28.sp))
            BasicText(
                "使用邮箱创建账号，注册完成后自动登录。",
                style = TextStyle(color = AuthSecondary, fontSize = 14.sp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuthSurface)
            ) {
                AuthField(
                    value = state.email,
                    placeholder = "邮箱",
                    onValueChange = { onStateChanged(state.copy(email = it)) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthField(
                        value = state.code,
                        placeholder = "验证码",
                        onValueChange = { onStateChanged(state.copy(code = it)) }
                    )
                    CupertinoButton(
                        onClick = { actions?.onSendCode() },
                        enabled = state.sendCodeEnabled,
                        modifier = Modifier.widthIn(min = 104.dp).tutorialAnchor("register.code", state.sendCodeEnabled)
                    ) {
                        BasicText(state.sendCodeLabel)
                    }
                }
                AuthField(
                    value = state.password,
                    placeholder = "密码（至少 8 位）",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(password = it)) }
                )
                AuthField(
                    value = state.confirmPassword,
                    placeholder = "确认密码",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(confirmPassword = it)) }
                )
            }

            if (state.status.isNotEmpty()) {
                BasicText(
                    state.status,
                    style = TextStyle(color = AuthSecondary, fontSize = 13.sp)
                )
            }
            CupertinoButton(
                onClick = { actions?.onRegister() },
                enabled = state.registerEnabled,
                modifier = Modifier.fillMaxWidth().tutorialAnchor("register.submit", state.registerEnabled)
            ) {
                BasicText("注册并登录")
            }
        }
    }
}
