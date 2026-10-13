package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoDivider
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
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
private val AuthDivider = Color(0xFFE5E5EA)

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
        loggedIn: Boolean
    ) {
        state = state.copy(
            status = status,
            username = username,
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
    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AuthBackground,
        topBar = {
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
                        modifier = Modifier.size(44.dp)
                    ) {
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.ChevronBackward,
                            contentDescription = "返回",
                            tint = AuthAccent
                        )
                    }
                },
                actions = {}
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuthBackground)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AuthFieldGroup {
                AuthField(
                    value = state.username,
                    placeholder = "账号",
                    onValueChange = { onStateChanged(state.copy(username = it)) }
                )
                AuthDivider()
                AuthField(
                    value = state.password,
                    placeholder = "密码",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(password = it)) }
                )
            }

            if (state.status.isNotEmpty()) {
                CupertinoText(
                    text = state.status,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(color = AuthSecondary, fontSize = 13.sp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CupertinoButton(
                    onClick = { actions?.onLogin() },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f)
                ) {
                    CupertinoText(if (state.loading) "登录中…" else "登录")
                }
                CupertinoButton(
                    onClick = { actions?.onRegister() },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                    colors = CupertinoButtonDefaults.plainButtonColors()
                ) {
                    CupertinoText(
                        "注册账号",
                        style = TextStyle(color = AuthAccent)
                    )
                }
            }

            if (state.loggedIn) {
                CupertinoButton(
                    onClick = { actions?.onLogout() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CupertinoButtonDefaults.plainButtonColors()
                ) {
                    CupertinoText(
                        "退出登录",
                        style = TextStyle(color = AuthAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFieldGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AuthSurface),
        verticalArrangement = Arrangement.Center
    ) {
        content()
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
    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AuthBackground,
        topBar = {
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
                        modifier = Modifier.size(44.dp)
                    ) {
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.ChevronBackward,
                            contentDescription = "返回",
                            tint = AuthAccent
                        )
                    }
                },
                actions = {}
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuthBackground)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AuthFieldGroup {
                AuthField(
                    value = state.email,
                    placeholder = "邮箱",
                    onValueChange = { onStateChanged(state.copy(email = it)) }
                )
                AuthDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthField(
                        value = state.code,
                        placeholder = "验证码",
                        onValueChange = { onStateChanged(state.copy(code = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    CupertinoButton(
                        onClick = { actions?.onSendCode() },
                        enabled = state.sendCodeEnabled,
                        modifier = Modifier
                            .widthIn(min = 108.dp)
                            .padding(end = 12.dp),
                        colors = CupertinoButtonDefaults.plainButtonColors()
                    ) {
                        CupertinoText(
                            text = state.sendCodeLabel,
                            style = TextStyle(
                                color = if (state.sendCodeEnabled) AuthAccent else AuthAccent.copy(alpha = 0.45f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                AuthDivider()
                AuthField(
                    value = state.password,
                    placeholder = "密码（至少 8 位）",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(password = it)) }
                )
                AuthDivider()
                AuthField(
                    value = state.confirmPassword,
                    placeholder = "确认密码",
                    password = true,
                    onValueChange = { onStateChanged(state.copy(confirmPassword = it)) }
                )
            }

            if (state.status.isNotEmpty()) {
                CupertinoText(
                    text = state.status,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(color = AuthSecondary, fontSize = 13.sp)
                )
            }

            CupertinoButton(
                onClick = { actions?.onRegister() },
                enabled = state.registerEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                CupertinoText("注册并登录")
            }
        }
    }
}

@Composable
private fun AuthDivider() {
    CupertinoDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = AuthDivider
    )
}

@Composable
private fun AuthField(
    value: String,
    placeholder: String,
    password: Boolean = false,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    CupertinoTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        placeholder = {
            CupertinoText(
                placeholder,
                style = TextStyle(color = AuthSecondary, fontSize = 16.sp)
            )
        },
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        textStyle = TextStyle(color = AuthPrimary, fontSize = 16.sp)
    )
}
