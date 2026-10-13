package com.acooldog.toolbox.ui

import com.acooldog.toolbox.tutorial.tutorialAnchor

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class MineComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun openSettings()
        fun openInternalLogin()
        fun openTutorial()
    }

    var actions: Actions? = null

    @Composable
    override fun Content() {
        CupertinoTheme {
            MineScreen(actions)
        }
    }
}

@Composable
private fun MineScreen(actions: MineComposeView.Actions?) {
    val background = Color(0xFFF2F2F7)
    val surface = Color.White
    val primary = Color(0xFF1C1C1E)
    val secondary = Color(0xFF8E8E93)
    val accent = Color(0xFF007AFF)
    val accentSoft = Color(0xFFE5F1FF)
    val separator = Color(0xFFD1D1D6)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 18.dp,
            bottom = 116.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialAnchor("mine.login", onClick = { actions?.openInternalLogin() })
                    .cupertinoClickable { actions?.openInternalLogin() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "未登录",
                        style = TextStyle(
                            color = primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.size(4.dp))
                    BasicText(
                        text = "登录后同步共享内容",
                        style = TextStyle(color = secondary, fontSize = 13.sp)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    tint = primary,
                    modifier = Modifier
                        .size(28.dp)
                        .tutorialAnchor("mine.settings", onClick = { actions?.openSettings() })
                        .clip(CircleShape)
                        .cupertinoClickable { actions?.openSettings() }
                        .padding(4.dp)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MineStat(
                    modifier = Modifier.weight(1f),
                    value = "0 km",
                    label = "今日模拟",
                    accent = accent,
                    surface = surface,
                    primary = primary,
                    secondary = secondary,
                    separator = separator
                )
                MineStat(
                    modifier = Modifier.weight(1f),
                    value = "0 km",
                    label = "累计模拟",
                    accent = Color(0xFFFF9F0A),
                    surface = surface,
                    primary = primary,
                    secondary = secondary,
                    separator = separator
                )
            }
        }
        item {
            MineAction(
                key = "mine.settings",
                title = "应用设置",
                subtitle = "管理应用偏好和运行选项",
                icon = Icons.Outlined.Settings,
                onClick = { actions?.openSettings() },
                surface = surface,
                primary = primary,
                secondary = secondary,
                accent = accent,
                accentSoft = accentSoft,
                separator = separator
            )
        }
        item {
            MineAction(
                key = "mine.update",
                title = "版本更新",
                subtitle = "更新检查由应用设置统一管理",
                icon = Icons.Outlined.Update,
                onClick = { actions?.openSettings() },
                surface = surface,
                primary = primary,
                secondary = secondary,
                accent = accent,
                accentSoft = accentSoft,
                separator = separator
            )
        }
        item {
            MineAction(
                key = "mine.tutorial", title = "使用教程", subtitle = "重新查看",
                icon = Icons.Outlined.HelpOutline, onClick = { actions?.openTutorial() },
                surface = surface, primary = primary, secondary = secondary, accent = accent,
                accentSoft = accentSoft, separator = separator
            )
        }
    }
}

@Composable
private fun MineStat(
    modifier: Modifier,
    value: String,
    label: String,
    accent: Color,
    surface: Color,
    primary: Color,
    secondary: Color,
    separator: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .border(1.dp, separator.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.size(12.dp))
        BasicText(
            text = value,
            style = TextStyle(color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.size(4.dp))
        BasicText(text = label, style = TextStyle(color = secondary, fontSize = 12.sp))
    }
}

@Composable
private fun MineAction(
    key: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    surface: Color,
    primary: Color,
    secondary: Color,
    accent: Color,
    accentSoft: Color,
    separator: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tutorialAnchor(key, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .border(1.dp, separator.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .cupertinoClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = TextStyle(color = primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.size(3.dp))
            BasicText(text = subtitle, style = TextStyle(color = secondary, fontSize = 12.sp))
        }
        BasicText(text = "›", style = TextStyle(color = secondary, fontSize = 24.sp))
    }
}
