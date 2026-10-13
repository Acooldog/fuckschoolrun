package com.acooldog.toolbox.ui

import com.acooldog.toolbox.tutorial.tutorialAnchor

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoDivider
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.icons.outlined.Location
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val SurfaceShape = RoundedCornerShape(24.dp)
private val SurfaceColor = Color(0xFFF5F5F7).copy(alpha = 0.9f)
private val LabelColor = Color(0xFF1C1C1E)
private val SecondaryLabelColor = Color(0xFF6D6D72)
private const val ROUTE_CREATE_PREFERENCES = "route_create_prefs"
private const val GEAR_X_FRACTION_KEY = "gear_x_fraction"
private const val GEAR_Y_FRACTION_KEY = "gear_y_fraction"
private const val GEAR_EDGE_KEY = "gear_edge"
private const val GEAR_EDGE_NONE = 0
private const val GEAR_EDGE_START = -1
private const val GEAR_EDGE_END = 1
private const val GEAR_EDGE_THRESHOLD_DP = 12
private const val GEAR_EDGE_SNAP_DELAY_MS = 3000L

class CupertinoRouteCreateOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    interface Actions {
        fun onSearchOpened()
        fun onSearchChanged(query: String)
        fun onSearchSubmit(query: String)
        fun onSearchResultSelected(latitude: Double, longitude: Double): Boolean
        fun onSelectMapType(satellite: Boolean)
        fun onSave()
        fun onClear()
        fun onShare()
        fun onRecord()
        fun onRecordSettings()
        fun onLocation()
        fun onCloseRoute()
        fun onToggleToolbox()
        fun onDrawTool()
        fun onEditTool()
        fun onAutoAdd()
        fun onAutoReduce()
        fun onSmoothCorner()
        fun onUndo()
        fun onRedo()
        fun onKeep()
        fun onDelete()
        fun onRecordModeChanged(enabled: Boolean)
    }

    data class State(
        val recordMode: Boolean = false,
        val recording: Boolean = false,
        val toolboxCollapsed: Boolean = true,
        val selectedTool: Int = 0,
        val selectedPointIndex: Int = -1,
        val canEdit: Boolean = false,
        val canAutoAdd: Boolean = false,
        val canAutoReduce: Boolean = false,
        val canSmooth: Boolean = false,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val canCloseRoute: Boolean = false,
        val hint: String = "",
        val satelliteMap: Boolean = false,
        val searchResults: List<SearchResult> = emptyList(),
    )

    data class SearchResult(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
    )

    private var actions: Actions? = null
    private var state by mutableStateOf(State())
    private val searchSession = RouteCreateSearchSession()

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun collapseSearch() {
        searchSession.collapse()
    }

    fun render(
        recordMode: Boolean,
        recording: Boolean,
        toolboxCollapsed: Boolean,
        selectedTool: Int,
        selectedPointIndex: Int,
        canEdit: Boolean,
        canAutoAdd: Boolean,
        canAutoReduce: Boolean,
        canSmooth: Boolean,
        canUndo: Boolean,
        canRedo: Boolean,
        canCloseRoute: Boolean,
        hint: String,
        satelliteMap: Boolean,
        searchResults: List<SearchResult>,
    ) {
        state = State(
            recordMode = recordMode,
            recording = recording,
            toolboxCollapsed = toolboxCollapsed,
            selectedTool = selectedTool,
            selectedPointIndex = selectedPointIndex,
            canEdit = canEdit,
            canAutoAdd = canAutoAdd,
            canAutoReduce = canAutoReduce,
            canSmooth = canSmooth,
            canUndo = canUndo,
            canRedo = canRedo,
            canCloseRoute = canCloseRoute,
            hint = hint,
            satelliteMap = satelliteMap,
            searchResults = searchResults,
        )
        invalidate()
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            RouteCreateOverlay(
                state = state,
                actions = actions,
                searchSession = searchSession,
            )
        }
    }
}

@Composable
private fun RouteCreateOverlay(
    state: CupertinoRouteCreateOverlayView.State,
    actions: CupertinoRouteCreateOverlayView.Actions?,
    searchSession: RouteCreateSearchSession,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val backdrop = rememberLayerBackdrop()
    val gearPreferences = remember(context) {
        context.getSharedPreferences(ROUTE_CREATE_PREFERENCES, Context.MODE_PRIVATE)
    }
    val searchExpanded = searchSession.expanded
    var searchText by remember { mutableStateOf("") }
    val searchScrollState = rememberScrollState()
    val searchFocusRequester = remember { FocusRequester() }
    var gearX by remember { mutableFloatStateOf(Float.NaN) }
    var gearY by remember { mutableFloatStateOf(Float.NaN) }
    var gearDragging by remember { mutableStateOf(false) }
    val gearInteractionSource = remember { MutableInteractionSource() }
    val gearPressed by gearInteractionSource.collectIsPressedAsState()
    var gearSnappedToEdge by remember { mutableIntStateOf(GEAR_EDGE_NONE) }
    var edgeSnapArmed by remember { mutableStateOf(false) }

    fun collapseSearch() {
        searchSession.collapse()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
            delay(260)
            actions?.onSearchOpened()
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 100.dp),
    ) {
        val density = LocalDensity.current
        val gearSize = 52.dp
        val gearSizePx = with(density) { gearSize.toPx() }
        val maxGearX = with(density) {
            (maxWidth - gearSize).toPx().coerceAtLeast(0f)
        }
        val maxGearY = with(density) {
            (maxHeight - gearSize).toPx().coerceAtLeast(0f)
        }
        val defaultGearY = (
            maxGearY - with(density) { 72.dp.toPx() }
        ).coerceAtLeast(0f)
        val dockWidth = (maxWidth - 16.dp).coerceAtLeast(0.dp)
        val recordingContainerWidth = dockWidth * 0.8f
        val menuWidth = maxWidth.coerceAtMost(236.dp)
        val menuHeight = maxHeight.coerceAtMost(440.dp)
        val menuWidthPx = with(density) { menuWidth.toPx() }
        val menuHeightPx = with(density) { menuHeight.toPx() }
        val gearHalfSizePx = gearSizePx / 2f
        val horizontalInsetPx = with(density) { 12.dp.toPx() }
        val edgeThresholdPx = with(density) { GEAR_EDGE_THRESHOLD_DP.dp.toPx() }
        val maxMenuX = with(density) {
            (maxWidth - menuWidth).toPx().coerceAtLeast(0f)
        }
        val currentGearX = when {
            gearSnappedToEdge == GEAR_EDGE_START -> -gearHalfSizePx - horizontalInsetPx
            gearSnappedToEdge == GEAR_EDGE_END -> maxGearX + gearHalfSizePx + horizontalInsetPx
            gearX.isNaN() -> maxGearX
            else -> gearX.coerceIn(0f, maxGearX)
        }
        val currentGearY = if (gearY.isNaN()) {
            defaultGearY
        } else {
            gearY.coerceIn(0f, maxGearY)
        }
        fun edgeForPosition(x: Float): Int = routeCreateGearEdge(x, maxGearX, edgeThresholdPx)
        fun persistGearPosition() {
            val savedX = if (gearX.isNaN()) maxGearX else gearX.coerceIn(0f, maxGearX)
            val savedY = if (gearY.isNaN()) defaultGearY else gearY.coerceIn(0f, maxGearY)
            gearPreferences.edit()
                .putFloat(
                    GEAR_X_FRACTION_KEY,
                    if (maxGearX > 0f) savedX / maxGearX else 0f,
                )
                .putFloat(
                    GEAR_Y_FRACTION_KEY,
                    if (maxGearY > 0f) savedY / maxGearY else 0f,
                )
                .putInt(GEAR_EDGE_KEY, gearSnappedToEdge)
                .apply()
        }
        val menuX = (currentGearX + gearSizePx - menuWidthPx).coerceIn(0f, maxMenuX)
        val menuGapPx = 8.dp.toPx(density)
        val menuTopY = routeCreateMenuTopY(
            currentGearY,
            gearSizePx,
            menuHeightPx,
            with(density) { maxHeight.toPx() },
            menuGapPx,
        )
        val menuOrigin = TransformOrigin(
            pivotFractionX = ((currentGearX + gearHalfSizePx - menuX) / menuWidthPx)
                .coerceIn(0f, 1f),
            pivotFractionY = if (menuTopY < currentGearY) 1f else 0f,
        )

        LaunchedEffect(maxGearX, maxGearY) {
            if (gearX.isNaN()) {
                val savedX = gearPreferences.getFloat(GEAR_X_FRACTION_KEY, Float.NaN)
                val savedY = gearPreferences.getFloat(GEAR_Y_FRACTION_KEY, Float.NaN)
                if (!savedX.isNaN() && !savedY.isNaN()) {
                    gearX = (savedX * maxGearX).coerceIn(0f, maxGearX)
                    gearY = (savedY * maxGearY).coerceIn(0f, maxGearY)
                    gearSnappedToEdge = gearPreferences
                        .getInt(GEAR_EDGE_KEY, GEAR_EDGE_NONE)
                        .coerceIn(GEAR_EDGE_START, GEAR_EDGE_END)
                } else {
                    gearX = maxGearX
                    gearY = defaultGearY
                    gearSnappedToEdge = GEAR_EDGE_NONE
                }
            } else {
                gearX = gearX.coerceIn(0f, maxGearX)
                gearY = gearY.coerceIn(0f, maxGearY)
            }
        }

        val edgeCandidate = if (
            gearSnappedToEdge == GEAR_EDGE_NONE &&
            !gearX.isNaN()
        ) {
            edgeForPosition(gearX)
        } else {
            GEAR_EDGE_NONE
        }

        LaunchedEffect(
            edgeSnapArmed,
            edgeCandidate,
            gearDragging,
            gearPressed,
            gearSnappedToEdge,
            state.toolboxCollapsed,
        ) {
            if (
                !edgeSnapArmed ||
                edgeCandidate == GEAR_EDGE_NONE ||
                gearDragging ||
                gearPressed ||
                gearSnappedToEdge != GEAR_EDGE_NONE ||
                !state.toolboxCollapsed
            ) {
                return@LaunchedEffect
            }
            val candidate = edgeCandidate
            delay(GEAR_EDGE_SNAP_DELAY_MS)
            val currentX = if (gearX.isNaN()) maxGearX else gearX
            if (
                edgeSnapArmed &&
                !gearDragging &&
                !gearPressed &&
                state.toolboxCollapsed &&
                gearSnappedToEdge == GEAR_EDGE_NONE &&
                edgeForPosition(currentX) == candidate
            ) {
                gearSnappedToEdge = candidate
                edgeSnapArmed = false
                gearX = if (candidate == GEAR_EDGE_START) 0f else maxGearX
                persistGearPosition()
            }
        }

        AnimatedVisibility(
            visible = !state.toolboxCollapsed,
            modifier = Modifier
                .offset { IntOffset(menuX.roundToInt(), menuTopY.roundToInt()) }
                .width(menuWidth)
                .zIndex(2f),
            enter = fadeIn(tween(180)) + scaleIn(
                animationSpec = tween(180),
                initialScale = 0.92f,
                transformOrigin = menuOrigin,
            ),
            exit = fadeOut(tween(140)) + scaleOut(
                animationSpec = tween(140),
                targetScale = 0.92f,
                transformOrigin = menuOrigin,
            ),
        ) {
            Box(
                Modifier
                    .height(menuHeight)
                    .clip(SurfaceShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { SurfaceShape },
                        effects = {
                            vibrancy()
                            blur(20.dp.toPx())
                        },
                        onDrawSurface = { drawRect(SurfaceColor) },
                    )
                    .padding(10.dp),
            ) {
                OverflowMenu(
                    state = state,
                    actions = actions,
                    onDismiss = {
                        collapseSearch()
                        actions?.onToggleToolbox()
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(currentGearX.roundToInt(), currentGearY.roundToInt()) }
                .size(gearSize)
                .zIndex(3f)
                .pointerInput(maxGearX, maxGearY) {
                    detectDragGestures(
                        onDragStart = {
                            collapseSearch()
                            gearDragging = true
                            edgeSnapArmed = false
                            if (gearSnappedToEdge != GEAR_EDGE_NONE) {
                                gearX = if (gearSnappedToEdge == GEAR_EDGE_START) 0f else maxGearX
                                gearSnappedToEdge = GEAR_EDGE_NONE
                            }
                        },
                        onDragEnd = {
                            gearDragging = false
                            edgeSnapArmed = edgeForPosition(
                                if (gearX.isNaN()) maxGearX else gearX,
                            ) != GEAR_EDGE_NONE
                            persistGearPosition()
                        },
                        onDragCancel = {
                            gearDragging = false
                            edgeSnapArmed = false
                            persistGearPosition()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val startX = if (gearX.isNaN()) maxGearX else gearX
                        val startY = if (gearY.isNaN()) defaultGearY else gearY
                        gearX = (startX + dragAmount.x).coerceIn(0f, maxGearX)
                        gearY = (startY + dragAmount.y).coerceIn(0f, maxGearY)
                    }
                },
        ) {
            CupertinoIconButton(
                onClick = {
                    collapseSearch()
                    edgeSnapArmed = false
                    if (gearSnappedToEdge != GEAR_EDGE_NONE) {
                        gearX = if (gearSnappedToEdge == GEAR_EDGE_START) 0f else maxGearX
                        gearSnappedToEdge = GEAR_EDGE_NONE
                        persistGearPosition()
                    }
                    actions?.onToggleToolbox()
                },
                interactionSource = gearInteractionSource,
                colors = CupertinoButtonDefaults.plainButtonColors(),
                modifier = Modifier
                    .size(gearSize)
                    .tutorialAnchor("draw.tools", onClick = { collapseSearch(); actions?.onToggleToolbox() })
                    .background(
                        Color.White.copy(
                            alpha = if (gearDragging || gearPressed || !state.toolboxCollapsed) {
                                0.90f
                            } else {
                                0.20f
                            },
                        ),
                        RoundedCornerShape(26.dp),
                    ),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Gearshape,
                    contentDescription = "路线工具",
                    tint = LabelColor.copy(
                        alpha = if (gearDragging || gearPressed || !state.toolboxCollapsed) 1f else 0.30f,
                    ),
                )
            }
        }

        val searchWidth by animateDpAsState(
            targetValue = if (searchExpanded) maxWidth else 52.dp,
            animationSpec = tween(260),
            label = "route-search-width",
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(searchWidth)
                .zIndex(4f)
                .clip(RoundedCornerShape(22.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(22.dp) },
                    effects = {
                        vibrancy()
                        blur(18.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(SurfaceColor)
                    },
                )
                .animateContentSize(tween(260))
                .padding(4.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (searchExpanded) {
                        CupertinoTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                actions?.onSearchChanged(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .tutorialAnchor("draw.search_input")
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged {
                                    searchSession.onFocusChanged(it.hasFocus)
                                },
                            placeholder = {
                                CupertinoText(
                                    "搜索地点",
                                    style = TextStyle(color = SecondaryLabelColor),
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp),
                        )
                        CupertinoIconButton(
                            onClick = { actions?.onSearchSubmit(searchText) },
                            modifier = Modifier.size(44.dp)
                                .tutorialAnchor("draw.search_submit", onClick = { actions?.onSearchSubmit(searchText) }),
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                                contentDescription = "搜索",
                                tint = LabelColor,
                            )
                        }
                    } else {
                        CupertinoIconButton(
                            onClick = { searchSession.open() },
                            modifier = Modifier.size(44.dp)
                                .tutorialAnchor("draw.search_open", onClick = { searchSession.open() }),
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                                contentDescription = "展开搜索",
                                tint = LabelColor,
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = searchExpanded && state.searchResults.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 236.dp)
                            .verticalScroll(searchScrollState),
                    ) {
                        state.searchResults.forEachIndexed { index, result ->
                            CupertinoButton(
                                onClick = {
                                    val added = actions?.onSearchResultSelected(
                                        result.latitude,
                                        result.longitude,
                                    ) == true
                                    if (added) {
                                        collapseSearch()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tutorialAnchor("draw.search_result")
                                    .height(58.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    CupertinoText(
                                        text = result.name.ifBlank { result.address },
                                        modifier = Modifier.fillMaxWidth(),
                                        style = TextStyle(
                                            color = LabelColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    )
                                    if (result.address.isNotBlank() && result.address != result.name) {
                                        CupertinoText(
                                            text = result.address,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                            style = TextStyle(
                                                color = SecondaryLabelColor,
                                                fontSize = 12.sp,
                                            ),
                                        )
                                    }
                                }
                            }
                            if (index < state.searchResults.lastIndex) {
                                CupertinoDivider(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val mapSelection by animateFloatAsState(
                    targetValue = if (state.satelliteMap) 1f else 0f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "route-map-type-selection",
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(3.dp),
                ) {
                    val tabWidth = 58.dp
                    Box(
                        modifier = Modifier
                            .offset(x = (tabWidth + 2.dp) * mapSelection)
                            .width(tabWidth)
                            .height(36.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFE4E4E7)),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(false, true).forEach { satellite ->
                            CupertinoButton(
                                onClick = {
                                    collapseSearch()
                                    actions?.onSelectMapType(satellite)
                                },
                                modifier = Modifier.width(tabWidth).height(36.dp)
                                    .tutorialAnchor(if (satellite) "draw.map_satellite" else "draw.map_standard",
                                        onClick = { collapseSearch(); actions?.onSelectMapType(satellite) }),
                                colors = CupertinoButtonDefaults.plainButtonColors(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) {
                                CupertinoText(
                                    text = if (satellite) "卫星" else "标准",
                                    style = TextStyle(
                                        color = if (state.satelliteMap == satellite) {
                                            Color.Black
                                        } else {
                                            LabelColor.copy(alpha = 0.72f)
                                        },
                                        fontSize = 14.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
                if (state.hint.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CupertinoText(
                        text = state.hint,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        if (state.canCloseRoute) {
            CupertinoButton(
                onClick = {
                    collapseSearch()
                    actions?.onCloseRoute()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .tutorialAnchor("draw.close_route", onClick = { collapseSearch(); actions?.onCloseRoute() })
                    .padding(bottom = 64.dp),
            ) {
                CupertinoText("闭合路线")
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset {
                    IntOffset(
                        0,
                        (with(density) { maxHeight.toPx() } * 0.10f).roundToInt(),
                    )
                }
                .padding(end = 4.dp)
                .size(48.dp)
                .zIndex(2f)
                .clip(RoundedCornerShape(24.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(18.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(SurfaceColor)
                    },
                )
                .padding(2.dp),
        ) {
            CupertinoIconButton(
                onClick = {
                    collapseSearch()
                    actions?.onLocation()
                },
                modifier = Modifier.fillMaxSize()
                    .tutorialAnchor("draw.location", onClick = { collapseSearch(); actions?.onLocation() }),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Location,
                    contentDescription = "当前位置",
                    tint = LabelColor,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(recordingContainerWidth)
                .clip(SurfaceShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { SurfaceShape },
                    effects = {
                        vibrancy()
                        blur(18.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(SurfaceColor)
                    },
                )
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    CupertinoText(
                        text = "录制路线",
                        style = TextStyle(fontSize = 10.sp),
                    )
                    CupertinoSwitch(
                        modifier = Modifier.tutorialAnchor("draw.record_mode",
                            onClick = { actions?.onRecordModeChanged(!state.recordMode) }),
                        checked = state.recordMode,
                        onCheckedChange = {
                            collapseSearch()
                            actions?.onRecordModeChanged(it)
                        },
                    )
                }
            }
            CupertinoIconButton(
                onClick = {
                    collapseSearch()
                    actions?.onRecordSettings()
                },
                modifier = Modifier.size(42.dp)
                    .tutorialAnchor("draw.record_settings", onClick = { collapseSearch(); actions?.onRecordSettings() }),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Gearshape,
                    contentDescription = "录制设置",
                    tint = LabelColor,
                )
            }
            CupertinoButton(
                onClick = {
                    collapseSearch()
                    actions?.onRecord()
                },
                enabled = state.recordMode || state.recording,
                modifier = Modifier
                    .height(34.dp)
                    .tutorialAnchor("draw.record", enabled = state.recordMode || state.recording)
                    .padding(horizontal = 2.dp),
            ) {
                CupertinoText(
                    text = if (state.recording) "停止绘制" else "开始绘制",
                    style = TextStyle(fontSize = 13.sp),
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    state: CupertinoRouteCreateOverlayView.State,
    actions: CupertinoRouteCreateOverlayView.Actions?,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val currentMode = if (state.selectedTool == 0) "绘制" else "编辑"
    val nextMode = if (state.selectedTool == 0) "编辑" else "绘制"
    fun perform(action: () -> Unit) {
        onDismiss()
        action()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CupertinoText(
            text = "当前模式: $currentMode",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(
                color = SecondaryLabelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        OverflowActionButton("保存路线", key = "draw.save", enabled = !state.recording) {
            perform { actions?.onSave() }
        }
        OverflowActionButton("清除重画", key = "draw.clear", enabled = !state.recording) {
            perform { actions?.onClear() }
        }
        OverflowActionButton("分享路线", key = "draw.share", enabled = !state.recording) {
            perform { actions?.onShare() }
        }
        OverflowActionButton(
            label = nextMode,
            key = "draw.mode",
            enabled = state.selectedTool != 0 || state.canEdit,
        ) {
            perform {
                if (state.selectedTool == 0) {
                    actions?.onEditTool()
                } else {
                    actions?.onDrawTool()
                }
            }
        }
        OverflowActionButton("自动加点", key = "draw.auto_add", enabled = state.canAutoAdd) {
            perform { actions?.onAutoAdd() }
        }
        OverflowActionButton("自动减点", key = "draw.auto_reduce", enabled = state.canAutoReduce) {
            perform { actions?.onAutoReduce() }
        }
        OverflowActionButton("平滑拐角", key = "draw.smooth", enabled = state.canSmooth) {
            perform { actions?.onSmoothCorner() }
        }
        OverflowActionButton("撤销", key = "draw.undo", enabled = state.canUndo) {
            perform { actions?.onUndo() }
        }
        OverflowActionButton("重做", key = "draw.redo", enabled = state.canRedo) {
            perform { actions?.onRedo() }
        }
        if (state.selectedPointIndex >= 0) {
            OverflowActionButton("保留点 ${state.selectedPointIndex + 1}", key = "draw.keep") {
                perform { actions?.onKeep() }
            }
            OverflowActionButton("删除点 ${state.selectedPointIndex + 1}", key = "draw.delete") {
                perform { actions?.onDelete() }
            }
        }
    }
}

@Composable
private fun OverflowActionButton(
    label: String,
    key: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().tutorialAnchor(key, enabled, onClick),
    ) {
        CupertinoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun routeCreateGearEdge(x: Float, maxX: Float, threshold: Float): Int = when {
    x <= threshold -> GEAR_EDGE_START
    x >= maxX - threshold -> GEAR_EDGE_END
    else -> GEAR_EDGE_NONE
}

internal class RouteCreateSearchSession {
    var expanded by mutableStateOf(false)
        private set
    private var hadFocus = false

    fun open() {
        hadFocus = false
        expanded = true
    }

    fun collapse() {
        expanded = false
        hadFocus = false
    }

    fun onFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            hadFocus = true
        } else if (hadFocus) {
            collapse()
        }
    }
}

internal fun routeCreateMenuTopY(
    gearY: Float,
    gearSize: Float,
    menuHeight: Float,
    viewportHeight: Float,
    gap: Float,
): Float {
    val above = gearY - menuHeight - gap
    return if (above >= 0f) {
        above
    } else {
        (gearY + gearSize + gap).coerceIn(0f, (viewportHeight - menuHeight).coerceAtLeast(0f))
    }
}

private fun androidx.compose.ui.unit.Dp.toPx(density: androidx.compose.ui.unit.Density): Float {
    return with(density) { toPx() }
}
