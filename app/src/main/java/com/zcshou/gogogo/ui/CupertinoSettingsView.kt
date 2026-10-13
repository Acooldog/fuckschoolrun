package com.acooldog.toolbox.ui

import com.acooldog.toolbox.tutorial.tutorialAnchor
import com.acooldog.toolbox.tutorial.tutorialNamedAnchor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.preference.PreferenceManager
import com.acooldog.toolbox.R
import com.acooldog.toolbox.utils.GoUtils
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class CupertinoSettingsView(
    context: Context
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        CupertinoTheme {
            SettingsContent(
                context = context,
                onBack = { (context as? android.app.Activity)?.finish() }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    context: Context,
    onBack: () -> Unit
) {
    val preferences = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val resources = context.resources
    val primaryText = Color(0xFF1C1C1E)
    val secondaryText = Color(0xFF6D6D72)
    val pageBackground = Color(0xFFF2F2F7)

    var walk by remember {
        mutableStateOf(
            preferences.getString("setting_walk", resources.getString(R.string.setting_walk_default))
                ?: resources.getString(R.string.setting_walk_default)
        )
    }
    var run by remember {
        mutableStateOf(
            preferences.getString("setting_run", resources.getString(R.string.setting_run_default))
                ?: resources.getString(R.string.setting_run_default)
        )
    }
    var bike by remember {
        mutableStateOf(
            preferences.getString("setting_bike", resources.getString(R.string.setting_bike_default))
                ?: resources.getString(R.string.setting_bike_default)
        )
    }
    var randomOffset by remember {
        mutableStateOf(preferences.getBoolean("setting_random_offset", false))
    }
    var latitudeOffset by remember {
        mutableStateOf(
            preferences.getString(
                "setting_lat_max_offset",
                resources.getString(R.string.setting_random_offset_default)
            ) ?: resources.getString(R.string.setting_random_offset_default)
        )
    }
    var longitudeOffset by remember {
        mutableStateOf(
            preferences.getString(
                "setting_lon_max_offset",
                resources.getString(R.string.setting_random_offset_default)
            ) ?: resources.getString(R.string.setting_random_offset_default)
        )
    }
    var logOff by remember {
        mutableStateOf(preferences.getBoolean("setting_log_off", false))
    }
    var historyDays by remember {
        mutableStateOf(
            preferences.getString("setting_pos_history", resources.getString(R.string.history_expiration))
                ?: resources.getString(R.string.history_expiration)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        CupertinoTopAppBar(
            title = {
                CupertinoText(
                    text = context.getString(R.string.app_settings),
                    style = TextStyle(color = primaryText, fontSize = 18.sp)
                )
            },
            navigationIcon = {
                CupertinoIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp).tutorialAnchor("settings.back", onClick = onBack)
                ) {
                    androidx.compose.material.Icon(
                        CupertinoIcons.Outlined.ChevronBackward,
                        contentDescription = context.getString(R.string.app_settings)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(context.getString(R.string.setting_group_move), primaryText) {
                SettingInput(
                    label = context.getString(R.string.setting_walk),
                    value = walk,
                    onValueChange = {
                        walk = it
                        preferences.edit().putString("setting_walk", it).apply()
                    }
                )
                SettingInput(
                    label = context.getString(R.string.setting_run),
                    value = run,
                    onValueChange = {
                        run = it
                        preferences.edit().putString("setting_run", it).apply()
                    }
                )
                SettingInput(
                    label = context.getString(R.string.setting_bike),
                    value = bike,
                    onValueChange = {
                        bike = it
                        preferences.edit().putString("setting_bike", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_location), primaryText) {
                SettingSwitch(
                    label = context.getString(R.string.setting_random_offset),
                    summary = context.getString(R.string.setting_random_offset_summary),
                    checked = randomOffset,
                    onCheckedChange = {
                        randomOffset = it
                        preferences.edit().putBoolean("setting_random_offset", it).apply()
                    }
                )
                SettingInput(
                    label = context.getString(R.string.setting_lat_max_offset),
                    value = latitudeOffset,
                    onValueChange = {
                        latitudeOffset = it
                        preferences.edit().putString("setting_lat_max_offset", it).apply()
                    }
                )
                SettingInput(
                    label = context.getString(R.string.setting_lon_max_offset),
                    value = longitudeOffset,
                    onValueChange = {
                        longitudeOffset = it
                        preferences.edit().putString("setting_lon_max_offset", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_group_log), primaryText) {
                SettingSwitch(
                    label = context.getString(R.string.setting_log_off),
                    checked = logOff,
                    onCheckedChange = {
                        logOff = it
                        preferences.edit().putBoolean("setting_log_off", it).apply()
                    }
                )
                SettingInput(
                    label = context.getString(R.string.setting_pos_history),
                    value = historyDays,
                    onValueChange = {
                        historyDays = it
                        preferences.edit().putString("setting_pos_history", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_group_about), primaryText) {
                SettingInfo(
                    context.getString(R.string.setting_version),
                    GoUtils.getVersionName(context)
                )
                SettingInfo(
                    context.getString(R.string.setting_author),
                    context.getString(R.string.app_author)
                )
                SettingButton(context.getString(R.string.setting_user_agreement)) {
                    CupertinoDialogs.showDocument(
                        context as android.app.Activity,
                        context.getString(R.string.app_agreement),
                        context.getString(R.string.app_agreement_content),
                        context.getString(R.string.legal_document_close)
                    )
                }
                SettingButton(context.getString(R.string.setting_disclaimer)) {
                    CupertinoDialogs.showDocument(
                        context as android.app.Activity,
                        context.getString(R.string.app_privacy),
                        context.getString(R.string.app_privacy_content),
                        context.getString(R.string.legal_document_close)
                    )
                }
                SettingButton(context.getString(R.string.setting_license_protocol)) {
                    openLink(context, context.getString(R.string.setting_license_protocol_link))
                }
                SettingButton(context.getString(R.string.setting_reference_link)) {
                    openLink(context, context.getString(R.string.setting_reference_repo_link_summary))
                }
                SettingButton(context.getString(R.string.setting_project_link)) {
                    openLink(context, context.getString(R.string.setting_project_link_summary))
                }
                SettingButton(context.getString(R.string.setting_bilibili_link)) {
                    openLink(context, context.getString(R.string.app_bilibili_url))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    textColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(color = textColor, fontSize = 15.sp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
        )
        CupertinoTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.widthIn(min = 132.dp, max = 168.dp).tutorialNamedAnchor(label, "settings"),
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(label, style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp))
            if (!summary.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                BasicText(summary, style = TextStyle(color = Color(0xFF6D6D72), fontSize = 12.sp))
            }
        }
        CupertinoSwitch(checked = checked, onCheckedChange = onCheckedChange,
            modifier = Modifier.tutorialNamedAnchor(label, "settings", onClick = { onCheckedChange(!checked) }))
    }
}

@Composable
private fun SettingInfo(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(label, style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp))
        BasicText(value, style = TextStyle(color = Color(0xFF6D6D72), fontSize = 14.sp))
    }
}

@Composable
private fun SettingButton(label: String, onClick: () -> Unit) {
    CupertinoButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().tutorialNamedAnchor(label, "settings", onClick = onClick)
    ) {
        BasicText(label, style = TextStyle(color = Color(0xFF007AFF), fontSize = 15.sp))
    }
}

private fun openLink(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
