package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoDivider
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.icons.outlined.Person
import io.github.alexzhirkevich.cupertino.section.CupertinoSection
import io.github.alexzhirkevich.cupertino.section.SectionScope
import io.github.alexzhirkevich.cupertino.section.SectionStyle
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class MineComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    interface Actions {
        fun openSettings()
        fun openInternalLogin()
        fun loadMinePageData(callback: DataCallback)
        fun openApp(appId: String)
        fun openAuthorQq()
        fun openAuthorBilibili(url: String)
    }

    interface DataCallback {
        fun onLoaded(data: MinePageData)
    }

    var actions: Actions? = null
    private var reloadSignal by mutableIntStateOf(0)

    fun reloadData() {
        reloadSignal++
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            MineScreen(actions = actions, reloadSignal = reloadSignal)
        }
    }
}

private val MineBackground = Color(0xFFF2F2F7)
private val MinePrimary = Color(0xFF1C1C1E)
private val MineSecondary = Color(0xFF8E8E93)
private val MineSeparator = Color(0xFFD1D1D6)
private val MineAccent = Color(0xFF007AFF)
private val MineAccentSoft = Color(0xFFE5F1FF)
private val MineMapBackground = Color(0xFFE7EEF2)

private data class MineApp(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val MineApps = listOf(
    MineApp("draw_route", "绘制路线", Icons.Outlined.LocationOn),
    MineApp("route_simulation", "路线模拟", Icons.Outlined.DirectionsRun),
    MineApp("route_management", "路线管理", Icons.Outlined.Route),
    MineApp("usage_tips", "使用技巧", Icons.Outlined.Book),
    MineApp("operation_tips", "操作说明", CupertinoIcons.Outlined.Gearshape),
    MineApp("history", "历史记录", Icons.Outlined.History)
)

@Composable
private fun MineScreen(actions: MineComposeView.Actions?, reloadSignal: Int) {
    var loading by remember { mutableStateOf(true) }
    var pageData by remember { mutableStateOf<MinePageData?>(null) }

    LaunchedEffect(actions, reloadSignal) {
        if (actions == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        actions.loadMinePageData(
            object : MineComposeView.DataCallback {
                override fun onLoaded(data: MinePageData) {
                    pageData = data
                    loading = false
                }
            }
        )
    }

    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MineBackground,
        topBar = {
            CupertinoTopAppBar(
                title = {
                    CupertinoText(
                        text = "我的",
                        style = TextStyle(color = MinePrimary, fontSize = 18.sp)
                    )
                },
                navigationIcon = {},
                actions = {
                    CupertinoIconButton(
                        onClick = { actions?.openSettings() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.Gearshape,
                            contentDescription = "设置",
                            tint = MinePrimary
                        )
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MineBackground)
                .padding(scaffoldPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ProfileHeader(
                    isLoggedIn = pageData?.isLoggedIn == true,
                    onClick = { actions?.openInternalLogin() }
                )
            }
            item {
                MineCard(title = "应用列表") {
                    AppGrid(onOpenApp = { appId -> actions?.openApp(appId) })
                }
            }
            item {
                MineCard(title = "模拟统计") {
                    SimulationStatsCardContent(loading = loading, data = pageData)
                }
            }
            item {
                MineCard(title = "偏好路线") {
                    FavoriteRouteCardContent(loading = loading, data = pageData)
                }
            }
            item {
                MineCard(title = "路线共享统计") {
                    RouteSharingStatsCardContent(loading = loading, data = pageData)
                }
            }
            item {
                MineCard(title = "联系作者") {
                    AuthorContactCardContent(
                        loading = loading,
                        data = pageData,
                        onQqClick = { actions?.openAuthorQq() },
                        onBilibiliClick = {
                            actions?.openAuthorBilibili(
                                pageData?.bilibiliUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?: MinePageData.DEFAULT_BILIBILI_URL
                            )
                        }
                    )
                }
            }
            item {
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    CupertinoText(
                        text = pageData?.disclaimerText
                            ?.takeIf { it.isNotBlank() }
                            ?: MinePageData.DEFAULT_DISCLAIMER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = TextStyle(
                            color = MineSecondary,
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(isLoggedIn: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .cupertinoClickable(onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MineAccentSoft),
            contentAlignment = Alignment.Center
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.Person,
                contentDescription = null,
                tint = MineAccent,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            CupertinoText(
                text = if (isLoggedIn) "已登录" else "未登录",
                style = TextStyle(
                    color = MinePrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(4.dp))
            CupertinoText(
                text = "登录后解锁更多功能",
                style = TextStyle(color = MineSecondary, fontSize = 13.sp)
            )
        }
    }
}

@Composable
private fun MineCard(
    title: String,
    content: @Composable SectionScope.() -> Unit
) {
    CupertinoSection(
        modifier = Modifier.fillMaxWidth(),
        style = SectionStyle.InsetGrouped,
        contentPadding = PaddingValues(0.dp),
        dividerPadding = PaddingValues(horizontal = 12.dp),
            title = {
                CupertinoText(
                    text = title,
                    style = TextStyle(
                        color = MineSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    ) {
        content()
    }
}

@Composable
private fun SectionScope.AppGrid(onOpenApp: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MineApps.chunked(4).forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowApps.forEach { app ->
                    AppGridItem(
                        app = app,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenApp(app.id) }
                    )
                }
                repeat(4 - rowApps.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: MineApp,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CupertinoIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MineAccentSoft)
        ) {
            CupertinoIcon(
                imageVector = app.icon,
                contentDescription = app.label,
                tint = MineAccent,
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        CupertinoText(
            text = app.label,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                color = MinePrimary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun SectionScope.SimulationStatsCardContent(
    loading: Boolean,
    data: MinePageData?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SimulationStat(
            value = resolveStatValue(
                loading = loading,
                loggedIn = data?.isLoggedIn == true,
                value = data?.todaySimulationKm
            ),
            label = "今日模拟",
            modifier = Modifier.weight(1f)
        )
        CupertinoVerticalDivider(height = 64.dp)
        SimulationStat(
            value = resolveStatValue(
                loading = loading,
                loggedIn = data?.isLoggedIn == true,
                value = data?.totalSimulationKm
            ),
            label = "累计模拟",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SimulationStat(
    value: String?,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (value == null) {
            LoadingIndicator()
        } else {
            CupertinoText(
                text = value,
                style = TextStyle(
                    color = if (value == "登录可见") MineSecondary else MinePrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        CupertinoText(
            text = label,
            style = TextStyle(color = MineSecondary, fontSize = 13.sp)
        )
    }
}

@Composable
private fun SectionScope.FavoriteRouteCardContent(
    loading: Boolean,
    data: MinePageData?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoutePreviewMap(
            points = data?.preferredRoutePoints.orEmpty(),
            loading = loading
        )
        CupertinoDivider(modifier = Modifier.fillMaxWidth())
        CupertinoText(
            text = if (loading) {
                "正在获取偏好路线"
            } else {
                data?.preferredRouteName
                    ?.takeIf { it.isNotBlank() }
                    ?: "暂无偏好路线"
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            style = TextStyle(
                color = MinePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun RoutePreviewMap(
    points: List<MineRoutePoint>,
    loading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MineMapBackground)
            .border(
                width = 1.dp,
                color = MineSeparator.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val roadColor = Color.White.copy(alpha = 0.9f)
            val minorRoadColor = Color.White.copy(alpha = 0.55f)
            for (index in 1..5) {
                val x = size.width * index / 6f
                drawLine(
                    color = minorRoadColor,
                    start = Offset(x, 0f),
                    end = Offset(x + size.width * 0.12f, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
                val y = size.height * index / 6f
                drawLine(
                    color = roadColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y - size.height * 0.08f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            if (points.size >= 2) {
                val minLatitude = points.minOf { it.latitude }
                val maxLatitude = points.maxOf { it.latitude }
                val minLongitude = points.minOf { it.longitude }
                val maxLongitude = points.maxOf { it.longitude }
                val latitudeSpan = (maxLatitude - minLatitude).coerceAtLeast(0.000001)
                val longitudeSpan = (maxLongitude - minLongitude).coerceAtLeast(0.000001)
                val routePath = Path()

                points.forEachIndexed { index, point ->
                    val x = size.width * (
                        (point.longitude - minLongitude) / longitudeSpan
                    ).toFloat().coerceIn(0.08f, 0.92f)
                    val y = size.height * (
                        1f - ((point.latitude - minLatitude) / latitudeSpan).toFloat()
                    ).coerceIn(0.12f, 0.88f)
                    if (index == 0) {
                        routePath.moveTo(x, y)
                    } else {
                        routePath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = routePath,
                    color = MineAccent,
                    style = Stroke(
                        width = 7.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawCircle(
                    color = MineAccent,
                    radius = 10.dp.toPx(),
                    center = routePathStart(points, size.width, size.height)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = routePathStart(points, size.width, size.height)
                )
                drawCircle(
                    color = MineAccent,
                    radius = 10.dp.toPx(),
                    center = routePathEnd(points, size.width, size.height)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = routePathEnd(points, size.width, size.height)
                )
            }
        }
        if (loading) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (points.size < 2) {
            CupertinoText(
                text = "暂无偏好路线",
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(color = MineSecondary, fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun SectionScope.RouteSharingStatsCardContent(
    loading: Boolean,
    data: MinePageData?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SharingStat(
            value = resolveDataValue(loading, data?.uploadedRouteCount),
            label = "上传路线",
            modifier = Modifier.weight(1f)
        )
        CupertinoVerticalDivider(height = 64.dp)
        SharingStat(
            value = resolveDataValue(loading, data?.helpedUserCount),
            label = "帮助过的人",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SharingStat(
    value: String?,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (value == null) {
            LoadingIndicator()
        } else {
            CupertinoText(
                text = value,
                style = TextStyle(
                    color = if (value == "暂无数据") MineSecondary else MinePrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        CupertinoText(
            text = label,
            style = TextStyle(color = MineSecondary, fontSize = 13.sp)
        )
    }
}

@Composable
private fun SectionScope.AuthorContactCardContent(
    loading: Boolean,
    data: MinePageData?,
    onQqClick: () -> Unit,
    onBilibiliClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MineAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.acooldog.toolbox.R.mipmap.ic_launcher),
                        contentDescription = "软件图标",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(6.dp))
                CupertinoText(
                    text = "SchoolRun",
                    style = TextStyle(
                        color = MinePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        CupertinoVerticalDivider(height = 82.dp)
        ContactAction(
            label = "联系作者 QQ",
            value = if (loading) null else {
                data?.authorQq ?: MinePageData.DEFAULT_AUTHOR_QQ
            },
            icon = CupertinoIcons.Outlined.Person,
            modifier = Modifier.weight(1f),
            enabled = !loading,
            onClick = onQqClick
        )
        ContactAction(
            label = "联系作者哔哩哔哩",
            value = if (loading) null else {
                data?.bilibiliName ?: MinePageData.DEFAULT_BILIBILI_NAME
            },
            icon = CupertinoIcons.Outlined.Gearshape,
            modifier = Modifier.weight(1f),
            enabled = !loading,
            onClick = onBilibiliClick
        )
    }
}

@Composable
private fun ContactAction(
    label: String,
    value: String?,
    icon: ImageVector,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(96.dp),
        colors = CupertinoButtonDefaults.plainButtonColors(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = label,
                tint = MineAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(5.dp))
            CupertinoText(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    color = MinePrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(3.dp))
            if (value == null) {
                LoadingIndicator()
            } else {
                CupertinoText(
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        color = MineSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun CupertinoVerticalDivider(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        CupertinoDivider(
            modifier = Modifier
                .width(height)
                .height(1.dp)
                .rotate(90f)
        )
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(22.dp),
        color = MineAccent,
        strokeWidth = 2.5.dp
    )
}

private fun resolveStatValue(
    loading: Boolean,
    loggedIn: Boolean,
    value: String?
): String? {
    if (loading) {
        return null
    }
    if (!loggedIn) {
        return "登录可见"
    }
    return value?.takeIf { it.isNotBlank() } ?: "暂无数据"
}

private fun resolveDataValue(loading: Boolean, value: String?): String? {
    if (loading) {
        return null
    }
    return value?.takeIf { it.isNotBlank() } ?: "暂无数据"
}

private fun routePathStart(
    points: List<MineRoutePoint>,
    width: Float,
    height: Float
): Offset {
    return routePointOffset(points.first(), points, width, height)
}

private fun routePathEnd(
    points: List<MineRoutePoint>,
    width: Float,
    height: Float
): Offset {
    return routePointOffset(points.last(), points, width, height)
}

private fun routePointOffset(
    point: MineRoutePoint,
    points: List<MineRoutePoint>,
    width: Float,
    height: Float
): Offset {
    val minLatitude = points.minOf { it.latitude }
    val maxLatitude = points.maxOf { it.latitude }
    val minLongitude = points.minOf { it.longitude }
    val maxLongitude = points.maxOf { it.longitude }
    val latitudeSpan = (maxLatitude - minLatitude).coerceAtLeast(0.000001)
    val longitudeSpan = (maxLongitude - minLongitude).coerceAtLeast(0.000001)
    val x = width * (
        (point.longitude - minLongitude) / longitudeSpan
    ).toFloat().coerceIn(0.08f, 0.92f)
    val y = height * (
        1f - ((point.latitude - minLatitude) / latitudeSpan).toFloat()
    ).coerceIn(0.12f, 0.88f)
    return Offset(x, y)
}
