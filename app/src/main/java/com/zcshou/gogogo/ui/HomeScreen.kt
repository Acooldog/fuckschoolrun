package com.acooldog.toolbox.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

private val PageBackground = Color(0xFFF2F2F7)
private val Surface = Color.White
private val PrimaryText = Color(0xFF1C1C1E)
private val SecondaryText = Color(0xFF8E8E93)
private val Separator = Color(0xFFD1D1D6)
private val Accent = Color(0xFF007AFF)
private val AccentSoft = Color(0xFFE5F1FF)

@Composable
fun HomeScreen(actions: HomeComposeView.Actions?) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val backdrop = rememberLayerBackdrop()

    CupertinoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                when (selectedTab) {
                    0 -> DrawRouteHome(actions)
                    1 -> SimulationHome(actions)
                    2 -> RoutesHome(actions)
                    else -> MineHome(actions)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                LiquidDock(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
private fun DrawRouteHome(actions: HomeComposeView.Actions?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(
                eyebrow = "路线工作台",
                title = "绘制路线",
                subtitle = "从地图开始，创建一条属于你的路线。"
            )
        }
        item {
            RouteCanvasPreview()
        }
        item {
            PrimaryAction(
                icon = Icons.Outlined.AddLocationAlt,
                title = "开始绘制路线",
                subtitle = "搜索地点或直接在地图上添加路径点",
                onClick = { actions?.openDrawRoute() }
            )
        }
        item {
            SectionLabel("最近使用")
        }
        item {
            GroupedAction(
                icon = Icons.Outlined.FolderOpen,
                title = "打开路线管理",
                subtitle = "查看本地路线和共享路线",
                onClick = { actions?.openRoutes() }
            )
        }
    }
}

@Composable
private fun SimulationHome(actions: HomeComposeView.Actions?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(
                eyebrow = "路线工作台",
                title = "路线模拟",
                subtitle = "选择本地路线或共享路线，继续上次的模拟设置。"
            )
        }
        item {
            StatSurface(
                value = "待选择",
                label = "当前路线",
                accent = Accent
            )
        }
        item {
            PrimaryAction(
                icon = Icons.Outlined.DirectionsRun,
                title = "进入路线模拟",
                subtitle = "共享路线也可以在模拟页直接选择",
                onClick = { actions?.openSimulation() }
            )
        }
        item {
            GroupedAction(
                icon = Icons.Outlined.Tune,
                title = "模拟参数",
                subtitle = "速度、步频、循环次数和自然变化",
                onClick = { actions?.openSimulation() }
            )
        }
    }
}

@Composable
private fun RoutesHome(actions: HomeComposeView.Actions?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(
                eyebrow = "路线工作台",
                title = "路线",
                subtitle = "管理本地路线、共享路线和导入的路线文件。"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSurface(
                    modifier = Modifier.weight(1f),
                    value = "本地",
                    label = "已保存路线",
                    accent = Accent
                )
                StatSurface(
                    modifier = Modifier.weight(1f),
                    value = "共享",
                    label = "在线路线",
                    accent = Color(0xFF34C759)
                )
            }
        }
        item {
            PrimaryAction(
                icon = Icons.Outlined.Route,
                title = "打开路线管理",
                subtitle = "编辑、删除、分享或直接开始模拟",
                onClick = { actions?.openRoutes() }
            )
        }
    }
}

@Composable
private fun MineHome(actions: HomeComposeView.Actions?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "未登录",
                        style = TextStyle(
                            color = PrimaryText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = "登录后同步你的共享内容",
                        style = TextStyle(color = SecondaryText, fontSize = 13.sp)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    tint = PrimaryText,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .cupertinoClickable { actions?.openSettings() }
                        .padding(4.dp)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSurface(
                    modifier = Modifier.weight(1f),
                    value = "0 km",
                    label = "今日模拟",
                    accent = Accent
                )
                StatSurface(
                    modifier = Modifier.weight(1f),
                    value = "0 km",
                    label = "累计模拟",
                    accent = Color(0xFFFF9F0A)
                )
            }
        }
        item {
            GroupedAction(
                icon = Icons.Outlined.Person,
                title = "账号登录",
                subtitle = "登录内部账号或查看当前状态",
                onClick = { actions?.openInternalLogin() }
            )
        }
        item {
            GroupedAction(
                icon = Icons.Outlined.Update,
                title = "检查更新",
                subtitle = "查看当前版本和更新说明",
                onClick = { actions?.checkForUpdates() }
            )
        }
        item {
            GroupedAction(
                icon = Icons.Outlined.AutoAwesome,
                title = "开发者选项",
                subtitle = "仅在需要调试时使用",
                onClick = { actions?.openDeveloperOptions() }
            )
        }
    }
}

@Composable
private fun Header(eyebrow: String, title: String, subtitle: String) {
    Column {
        BasicText(
            text = eyebrow.uppercase(),
            style = TextStyle(
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(5.dp))
        BasicText(
            text = title,
            style = TextStyle(
                color = PrimaryText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(5.dp))
        BasicText(
            text = subtitle,
            style = TextStyle(color = SecondaryText, fontSize = 15.sp)
        )
    }
}

@Composable
private fun RouteCanvasPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE4EDF3))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.72f)
                cubicTo(
                    size.width * 0.28f,
                    size.height * 0.18f,
                    size.width * 0.56f,
                    size.height * 0.88f,
                    size.width * 0.86f,
                    size.height * 0.28f
                )
            }
            drawPath(
                path = path,
                color = Accent,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(Accent, radius = 12.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.72f))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.72f))
            drawCircle(Accent, radius = 12.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.28f))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.28f))
        }
        BasicText(
            text = "路线预览",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            style = TextStyle(color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun PrimaryAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Accent)
            .cupertinoClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(3.dp))
            BasicText(subtitle, style = TextStyle(color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp))
        }
        Icon(Icons.Outlined.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GroupedAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Separator.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .cupertinoClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(3.dp))
            BasicText(subtitle, style = TextStyle(color = SecondaryText, fontSize = 12.sp))
        }
        BasicText("›", style = TextStyle(color = SecondaryText, fontSize = 24.sp))
    }
}

@Composable
private fun StatSurface(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Separator.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.height(12.dp))
        BasicText(value, style = TextStyle(color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        BasicText(label, style = TextStyle(color = SecondaryText, fontSize = 12.sp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = SecondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun LiquidDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop
) {
    val tabs = listOf(
        DockTab("绘制", Icons.Outlined.AddLocationAlt),
        DockTab("模拟", Icons.Outlined.DirectionsRun),
        DockTab("路线", Icons.Outlined.Route),
        DockTab("我的", Icons.Outlined.Person)
    )

    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(68.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    vibrancy()
                    blur(14.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.7f))
                }
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) AccentSoft else Color.Transparent)
                    .cupertinoClickable { onTabSelected(index) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) Accent else SecondaryText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = tab.label,
                    style = TextStyle(
                        color = if (selected) Accent else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

private data class DockTab(
    val label: String,
    val icon: ImageVector
)
