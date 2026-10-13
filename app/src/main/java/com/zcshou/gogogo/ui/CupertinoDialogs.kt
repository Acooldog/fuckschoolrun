package com.acooldog.toolbox.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.Orientation
import io.github.alexzhirkevich.cupertino.AlertDialogActionsScope
import io.github.alexzhirkevich.cupertino.CupertinoActionSheet
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoCheckBox
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.cancel
import io.github.alexzhirkevich.cupertino.default
import io.github.alexzhirkevich.cupertino.destructive
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import java.util.function.BooleanSupplier

object CupertinoDialogs {
    class DialogHandle internal constructor(
        private val onDismissAction: () -> Unit
    ) {
        private var dismissed = false

        fun dismiss() {
            if (dismissed) return
            dismissed = true
            onDismissAction()
        }
    }

    interface ChoiceListener {
        fun onChoice(index: Int)
    }

    interface TextInputListener {
        fun onConfirm(value: String)
    }

    interface Listener {
        fun onLegalConfirmed()
        fun onLegalRejected()
        fun onLegalCancelled()
        fun onOpenDocument(title: String, content: String)
        fun onEntryPrimary()
        fun onCopyGroup()
        fun onOpenBilibili()
    }

    @JvmStatic
    fun showLegalConsent(
        activity: Activity,
        title: String,
        message: String,
        agreementTitle: String,
        agreementContent: String,
        privacyTitle: String,
        privacyContent: String,
        checkboxLabel: String,
        cancelLabel: String,
        confirmLabel: String,
        listener: Listener
    ) {
        show(activity) { dialog ->
            var checked by remember { mutableStateOf(false) }
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { listener.onLegalCancelled() },
                buttonsOrientation = Orientation.Vertical,
                content = {
                    CupertinoText(message, modifier = Modifier.fillMaxWidth(), style = BodyStyle)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .cupertinoClickable { checked = !checked }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CupertinoCheckBox(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        CupertinoText(checkboxLabel, style = BodyStyle)
                    }
                    Spacer(Modifier.height(10.dp))
                    DialogActionRow(agreementTitle) {
                        listener.onOpenDocument(agreementTitle, agreementContent)
                    }
                    Spacer(Modifier.height(6.dp))
                    DialogActionRow(privacyTitle) {
                        listener.onOpenDocument(privacyTitle, privacyContent)
                    }
                    Spacer(Modifier.height(6.dp))
                    DialogActionRow(confirmLabel, enabled = checked) {
                        if (checked) {
                            dialog.dismiss()
                            listener.onLegalConfirmed()
                        } else {
                            listener.onLegalRejected()
                        }
                    }
                },
                actions = {
                    cancel(
                        onClick = {
                            dialog.dismiss()
                            listener.onLegalCancelled()
                        }
                    ) { CupertinoText(cancelLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showDocument(activity: Activity, title: String, content: String, closeLabel: String) {
        show(activity) { dialog ->
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { dialog.dismiss() },
                content = {
                    CupertinoText(
                        text = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        style = BodyStyle
                    )
                },
                actions = {
                    default(onClick = { dialog.dismiss() }) { CupertinoText(closeLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showEntryNotice(
        activity: Activity,
        title: String,
        message: String,
        primaryLabel: String,
        groupLabel: String,
        bilibiliLabel: String,
        listener: Listener
    ) {
        show(activity) { dialog ->
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = {},
                buttonsOrientation = Orientation.Vertical,
                content = {
                    CupertinoText(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp)
                            .verticalScroll(rememberScrollState()),
                        style = BodyStyle
                    )
                    if (groupLabel.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        DialogActionRow(groupLabel) {
                            listener.onCopyGroup()
                        }
                    }
                    if (bilibiliLabel.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        DialogActionRow(bilibiliLabel) {
                            listener.onOpenBilibili()
                        }
                    }
                },
                actions = {
                    default(
                        onClick = {
                            dialog.dismiss()
                            listener.onEntryPrimary()
                        }
                    ) { CupertinoText(primaryLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showReleaseUpdate(
        activity: Activity,
        title: String,
        version: String,
        changelog: String,
        downloadLabel: String,
        dismissLabel: String,
        onDownload: Runnable,
        onDismiss: Runnable
    ) {
        show(activity) { dialog ->
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { onDismiss.run() },
                content = {
                    CupertinoText(version, style = EmphasisStyle)
                    Spacer(Modifier.height(8.dp))
                    CupertinoText(
                        text = changelog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        style = BodyStyle
                    )
                },
                actions = {
                    cancel(
                        onClick = {
                            dialog.dismiss()
                            onDismiss.run()
                        }
                    ) { CupertinoText(dismissLabel) }
                    default(
                        onClick = {
                            dialog.dismiss()
                            onDownload.run()
                        }
                    ) { CupertinoText(downloadLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showConfirm(
        activity: Activity,
        title: String,
        message: String,
        cancelLabel: String,
        confirmLabel: String,
        onCancel: Runnable?,
        onConfirm: Runnable
    ) {
        show(activity) { dialog ->
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { onCancel?.run() },
                content = {
                    CupertinoText(message, modifier = Modifier.fillMaxWidth(), style = BodyStyle)
                },
                actions = {
                    destructive(
                        onClick = {
                            dialog.dismiss()
                            onCancel?.run()
                        }
                    ) { CupertinoText(cancelLabel) }
                    default(
                        onClick = {
                            dialog.dismiss()
                            onConfirm.run()
                        }
                    ) { CupertinoText(confirmLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showChoices(
        activity: Activity,
        title: String,
        message: String?,
        options: Array<String>,
        cancelLabel: String,
        listener: ChoiceListener
    ) {
        show(activity) { dialog ->
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { dialog.dismiss() },
                buttonsOrientation = Orientation.Vertical,
                content = {
                    if (!message.isNullOrEmpty()) {
                        CupertinoText(
                            text = message,
                            modifier = Modifier.fillMaxWidth(),
                            style = BodyStyle
                        )
                        if (options.size > 2) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    if (options.size > 2) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.forEachIndexed { index, option ->
                                DialogActionRow(option) {
                                    dialog.dismiss()
                                    listener.onChoice(index)
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (options.size <= 2) {
                        options.forEachIndexed { index, option ->
                            default(
                                onClick = {
                                    dialog.dismiss()
                                    listener.onChoice(index)
                                }
                            ) { CupertinoText(option) }
                        }
                    }
                    cancel(
                        onClick = { dialog.dismiss() }
                    ) { CupertinoText(cancelLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showInput(
        activity: Activity,
        title: String,
        message: String?,
        initialValue: String,
        hint: String,
        cancelLabel: String,
        confirmLabel: String,
        listener: TextInputListener
    ) {
        showInput(
            activity,
            title,
            message,
            initialValue,
            hint,
            cancelLabel,
            confirmLabel,
            false,
            listener
        )
    }

    @JvmStatic
    fun showInput(
        activity: Activity,
        title: String,
        message: String?,
        initialValue: String,
        hint: String,
        cancelLabel: String,
        confirmLabel: String,
        numericInput: Boolean,
        listener: TextInputListener
    ) {
        show(activity) { dialog ->
            var value by remember { mutableStateOf(initialValue) }
            DialogShell(
                dialog = dialog,
                title = title,
                onDismiss = { dialog.dismiss() },
                content = {
                    val inputFocusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(numericInput) {
                        if (numericInput) {
                            withFrameNanos { }
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
                    if (!message.isNullOrEmpty()) {
                        CupertinoText(
                            message,
                            modifier = Modifier.fillMaxWidth(),
                            style = BodyStyle
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    CupertinoTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (numericInput) {
                                    Modifier
                                        .heightIn(min = 44.dp)
                                        .focusRequester(inputFocusRequester)
                                } else {
                                    Modifier
                                }
                            ),
                        placeholder = { CupertinoText(hint) },
                        singleLine = true,
                        keyboardOptions = if (numericInput) {
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            )
                        } else {
                            KeyboardOptions.Default
                        },
                        textStyle = BodyStyle
                    )
                },
                actions = {
                    cancel(onClick = { dialog.dismiss() }) { CupertinoText(cancelLabel) }
                    default(
                        onClick = {
                            dialog.dismiss()
                            listener.onConfirm(value)
                        }
                    ) { CupertinoText(confirmLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showCustom(
        activity: Activity,
        title: String,
        contentView: View,
        cancelLabel: String,
        confirmLabel: String,
        confirmListener: BooleanSupplier,
        cancelListener: Runnable?
    ): DialogHandle {
        return showHost(activity) { dialog ->
            var visible by remember { mutableStateOf(true) }
            CupertinoActionSheet(
                visible = visible,
                onDismissRequest = {
                    visible = false
                    dialog.dismiss()
                    cancelListener?.run()
                },
                title = { CupertinoText(title) },
                content = {
                    AndroidView(
                        factory = {
                            (contentView.parent as? ViewGroup)?.removeView(contentView)
                            contentView
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                    )
                },
                buttons = {
                    cancel(
                        onClick = {
                            visible = false
                            dialog.dismiss()
                            cancelListener?.run()
                        }
                    ) { CupertinoText(cancelLabel) }
                    default(
                        onClick = {
                            if (confirmListener.asBoolean) {
                                visible = false
                                dialog.dismiss()
                            }
                        }
                    ) { CupertinoText(confirmLabel) }
                }
            )
        }
    }

    @JvmStatic
    fun showContent(
        activity: Activity,
        title: String,
        contentView: View,
        closeLabel: String
    ): DialogHandle {
        return showContent(activity, title, contentView, closeLabel, null)
    }

    @JvmStatic
    fun showContent(
        activity: Activity,
        title: String,
        contentView: View,
        closeLabel: String,
        onDismiss: Runnable?
    ): DialogHandle {
        return showHost(activity) { dialog ->
            var visible by remember { mutableStateOf(true) }
            CupertinoActionSheet(
                visible = visible,
                onDismissRequest = {
                    visible = false
                    dialog.dismiss()
                    onDismiss?.run()
                },
                title = { CupertinoText(title) },
                content = {
                    AndroidView(
                        factory = {
                            (contentView.parent as? ViewGroup)?.removeView(contentView)
                            contentView
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                    )
                },
                buttons = {
                    default(
                        onClick = {
                            visible = false
                            dialog.dismiss()
                            onDismiss?.run()
                        }
                    ) { CupertinoText(closeLabel) }
                }
            )
        }
    }

    private fun show(activity: Activity, content: @Composable (DialogHandle) -> Unit) {
        showHost(activity, content)
    }

    private fun showHost(
        activity: Activity,
        content: @Composable (DialogHandle) -> Unit
    ): DialogHandle {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val host = android.widget.FrameLayout(activity)
        host.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val handle = DialogHandle { root?.removeView(host) }
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                CupertinoTheme {
                    content(handle)
                }
            }
        }
        host.addView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root?.addView(host)
        return handle
    }
}

private val PrimaryText = Color(0xFF1C1C1E)
private val BodyStyle = TextStyle(color = PrimaryText, fontSize = 15.sp, lineHeight = 22.sp)
private val EmphasisStyle = TextStyle(
    color = PrimaryText,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold
)

@Composable
private fun DialogActionRow(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) {
                    Modifier.cupertinoClickable(onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CupertinoText(
            text = label,
            style = if (enabled) {
                BodyStyle
            } else {
                BodyStyle.copy(color = PrimaryText.copy(alpha = 0.45f))
            }
        )
    }
}

@Composable
private fun DialogShell(
    dialog: CupertinoDialogs.DialogHandle,
    title: String,
    onDismiss: () -> Unit,
    buttonsOrientation: Orientation = Orientation.Horizontal,
    content: @Composable () -> Unit,
    actions: AlertDialogActionsScope.() -> Unit
) {
    CupertinoAlertDialog(
        onDismissRequest = {
            dialog.dismiss()
            onDismiss()
        },
        title = {
            CupertinoText(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    color = PrimaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            )
        },
        message = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        buttonsOrientation = buttonsOrientation,
        buttons = actions
    )
}
