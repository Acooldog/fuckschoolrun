package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.icons.outlined.Person
import io.github.alexzhirkevich.cupertino.section.CupertinoSection
import io.github.alexzhirkevich.cupertino.section.SectionStyle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class MineComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun openSettings()
        fun openInternalLogin()
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
    val context = LocalContext.current
    val background = Color(0xFFF2F2F7)
    val primary = Color(0xFF1C1C1E)
    val secondary = Color(0xFF8E8E93)
    val accent = Color(0xFF007AFF)
    val accentSoft = Color(0xFFE5F1FF)

    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = background
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(scaffoldPadding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.Person,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        CupertinoText(
                            text = "未登录",
                            style = TextStyle(
                                color = primary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.size(4.dp))
                        CupertinoText(
                            text = "登录后同步共享内容",
                            style = TextStyle(color = secondary, fontSize = 13.sp)
                        )
                    }
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Gearshape,
                        contentDescription = context.getString(com.acooldog.toolbox.R.string.app_settings),
                        tint = primary,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .cupertinoClickable { actions?.openSettings() }
                            .padding(4.dp)
                    )
                }
            }
            item {
                CupertinoSection(
                    modifier = Modifier.fillMaxWidth(),
                    style = SectionStyle.InsetGrouped
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MineStat(
                            modifier = Modifier.weight(1f),
                            value = "0 km",
                            label = "今日模拟",
                            primary = primary,
                            secondary = secondary
                        )
                        MineStat(
                            modifier = Modifier.weight(1f),
                            value = "0 km",
                            label = "累计模拟",
                            primary = primary,
                            secondary = secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MineStat(
    modifier: Modifier,
    value: String,
    label: String,
    primary: Color,
    secondary: Color
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CupertinoText(
            text = value,
            style = TextStyle(
                color = primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        CupertinoText(
            text = label,
            style = TextStyle(color = secondary, fontSize = 12.sp)
        )
    }
}
