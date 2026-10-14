package com.acooldog.toolbox.ui

import com.acooldog.toolbox.tutorial.tutorialAnchor
import com.acooldog.toolbox.tutorial.tutorialNamedAnchor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.acooldog.toolbox.R
import com.acooldog.toolbox.utils.GoUtils
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.section.CupertinoSection
import io.github.alexzhirkevich.cupertino.section.SectionItem
import io.github.alexzhirkevich.cupertino.section.SectionScope
import io.github.alexzhirkevich.cupertino.section.SectionStyle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class CupertinoSettingsView(
    context: Context,
    private val onCheckUpdate: Runnable
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        CupertinoTheme {
            SettingsContent(
                context = context,
                onBack = { (context as? android.app.Activity)?.finish() },
                onCheckUpdate = { onCheckUpdate.run() }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    context: Context,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val preferences = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val resources = context.resources
    val primaryText = Color(0xFF1C1C1E)
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

    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pageBackground,
        topBar = {
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
                        modifier = Modifier
                            .size(44.dp)
                            .tutorialAnchor("settings.back", onClick = onBack)
                    ) {
                        CupertinoIcon(
                            CupertinoIcons.Outlined.ChevronBackward,
                            contentDescription = "返回"
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
                .background(pageBackground)
                .padding(contentPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(context.getString(R.string.setting_group_move), primaryText) {
                SectionInput(
                    label = context.getString(R.string.setting_walk),
                    value = walk,
                    onValueChange = {
                        walk = it
                        preferences.edit().putString("setting_walk", it).apply()
                    }
                )
                SectionInput(
                    label = context.getString(R.string.setting_run),
                    value = run,
                    onValueChange = {
                        run = it
                        preferences.edit().putString("setting_run", it).apply()
                    }
                )
                SectionInput(
                    label = context.getString(R.string.setting_bike),
                    value = bike,
                    onValueChange = {
                        bike = it
                        preferences.edit().putString("setting_bike", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_location), primaryText) {
                SectionSwitch(
                    label = context.getString(R.string.setting_random_offset),
                    summary = context.getString(R.string.setting_random_offset_summary),
                    checked = randomOffset,
                    onCheckedChange = {
                        randomOffset = it
                        preferences.edit().putBoolean("setting_random_offset", it).apply()
                    }
                )
                SectionInput(
                    label = context.getString(R.string.setting_lat_max_offset),
                    value = latitudeOffset,
                    onValueChange = {
                        latitudeOffset = it
                        preferences.edit().putString("setting_lat_max_offset", it).apply()
                    }
                )
                SectionInput(
                    label = context.getString(R.string.setting_lon_max_offset),
                    value = longitudeOffset,
                    onValueChange = {
                        longitudeOffset = it
                        preferences.edit().putString("setting_lon_max_offset", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_group_log), primaryText) {
                SectionSwitch(
                    label = context.getString(R.string.setting_log_off),
                    checked = logOff,
                    onCheckedChange = {
                        logOff = it
                        preferences.edit().putBoolean("setting_log_off", it).apply()
                    }
                )
                SectionInput(
                    label = context.getString(R.string.setting_pos_history),
                    value = historyDays,
                    onValueChange = {
                        historyDays = it
                        preferences.edit().putString("setting_pos_history", it).apply()
                    }
                )
            }

            SettingsSection(context.getString(R.string.setting_group_about), primaryText) {
                SectionInfo(
                    label = context.getString(R.string.setting_version),
                    value = GoUtils.getVersionName(context)
                )
                SectionLink(context.getString(R.string.home_update_title), onClick = onCheckUpdate)
                SectionInfo(
                    label = context.getString(R.string.setting_author),
                    value = context.getString(R.string.app_author)
                )
                SectionLink(context.getString(R.string.setting_user_agreement)) {
                    CupertinoDialogs.showDocument(
                        context as android.app.Activity,
                        context.getString(R.string.app_agreement),
                        context.getString(R.string.app_agreement_content),
                        context.getString(R.string.legal_document_close)
                    )
                }
                SectionLink(context.getString(R.string.setting_disclaimer)) {
                    CupertinoDialogs.showDocument(
                        context as android.app.Activity,
                        context.getString(R.string.app_privacy),
                        context.getString(R.string.app_privacy_content),
                        context.getString(R.string.legal_document_close)
                    )
                }
                SectionLink(context.getString(R.string.setting_license_protocol)) {
                    openLink(context, context.getString(R.string.setting_license_protocol_link))
                }
                SectionLink(context.getString(R.string.setting_reference_link)) {
                    openLink(context, context.getString(R.string.setting_reference_repo_link_summary))
                }
                SectionLink(context.getString(R.string.setting_project_link)) {
                    openLink(context, context.getString(R.string.setting_project_link_summary))
                }
                SectionLink(context.getString(R.string.setting_bilibili_link)) {
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
    content: @Composable SectionScope.() -> Unit
) {
    CupertinoSection(
        modifier = Modifier.fillMaxWidth(),
        style = SectionStyle.InsetGrouped,
        contentPadding = PaddingValues(0.dp),
        dividerPadding = PaddingValues(horizontal = 12.dp),
        title = {
            CupertinoText(title, style = TextStyle(color = textColor, fontSize = 14.sp))
        }
    ) {
        content()
    }
}

@Composable
private fun SectionScope.SectionInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    SectionItem(
        modifier = Modifier.tutorialNamedAnchor(label, "settings"),
        title = {
            CupertinoText(
                label,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    color = Color(0xFF1C1C1E),
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier.width(112.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CupertinoTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF1C1C1E),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.End
                    )
                )
            }
        }
    )
}

@Composable
private fun SectionScope.SectionSwitch(
    label: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SectionItem(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CupertinoText(
                    label,
                    style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
                )
                if (!summary.isNullOrEmpty()) {
                    CupertinoText(
                        summary,
                        style = TextStyle(color = Color(0xFF6D6D72), fontSize = 12.sp)
                    )
                }
            }
        },
        trailingContent = {
            CupertinoSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.tutorialNamedAnchor(
                    label,
                    "settings",
                    onClick = { onCheckedChange(!checked) }
                )
            )
        }
    )
}

@Composable
private fun SectionScope.SectionInfo(label: String, value: String) {
    SectionItem(
        title = {
            CupertinoText(
                label,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier.width(112.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CupertinoText(
                    value,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        color = Color(0xFF6D6D72),
                        fontSize = 14.sp,
                        textAlign = TextAlign.End
                    )
                )
            }
        }
    )
}

@Composable
private fun SectionScope.SectionLink(label: String, onClick: () -> Unit) {
    SectionItem(
        modifier = Modifier
            .tutorialNamedAnchor(label, "settings", onClick = onClick)
            .cupertinoClickable(onClick),
        title = {
            CupertinoText(
                label,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
            )
        },
        trailingContent = {
            CupertinoIcon(
                CupertinoIcons.Outlined.ChevronForward,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

private fun openLink(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
