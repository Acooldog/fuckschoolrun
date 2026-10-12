package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.icons.outlined.Location
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlin.math.roundToInt

private val SurfaceShape = RoundedCornerShape(24.dp)
private val SurfaceColor = Color(0xFFF5F5F7).copy(alpha = 0.9f)
private val LabelColor = Color(0xFF1C1C1E)
private val SecondaryLabelColor = Color(0xFF6D6D72)

class CupertinoRouteCreateOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    interface Actions {
        fun onSearchChanged(query: String)
        fun onSearchSubmit(query: String)
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
    )

    private var actions: Actions? = null
    private var state by mutableStateOf(State())

    fun setActions(actions: Actions?) {
        this.actions = actions
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
        )
        invalidate()
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            RouteCreateOverlay(state = state, actions = actions)
        }
    }
}

@Composable
private fun RouteCreateOverlay(
    state: CupertinoRouteCreateOverlayView.State,
    actions: CupertinoRouteCreateOverlayView.Actions?,
) {
    val focusManager = LocalFocusManager.current
    val backdrop = rememberLayerBackdrop()
    var searchExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var gearX by remember { mutableFloatStateOf(Float.NaN) }
    var gearY by remember { mutableFloatStateOf(Float.NaN) }
    var menuHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
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
        val menuWidth = 236.dp
        val menuWidthPx = with(density) { menuWidth.toPx() }
        val maxMenuX = with(density) {
            (maxWidth - menuWidth).toPx().coerceAtLeast(0f)
        }
        val currentGearX = if (gearX.isNaN()) maxGearX else gearX.coerceIn(0f, maxGearX)
        val currentGearY = if (gearY.isNaN()) {
            defaultGearY
        } else {
            gearY.coerceIn(0f, maxGearY)
        }
        val menuX = (currentGearX + gearSizePx - menuWidthPx).coerceIn(0f, maxMenuX)
        val menuTopY = if (currentGearY - menuHeightPx - 8.dp.toPx(density) >= 0f) {
            currentGearY - menuHeightPx - 8.dp.toPx(density)
        } else {
            (currentGearY + gearSizePx + 8.dp.toPx(density))
                .coerceIn(0f, (with(density) { maxHeight.toPx() } - menuHeightPx).coerceAtLeast(0f))
        }

        LaunchedEffect(maxGearX, maxGearY) {
            if (gearX.isNaN()) {
                gearX = maxGearX
                gearY = defaultGearY
            } else {
                gearX = gearX.coerceIn(0f, maxGearX)
                gearY = gearY.coerceIn(0f, maxGearY)
            }
        }

        AnimatedVisibility(
            visible = !state.toolboxCollapsed,
            modifier = Modifier
                .offset { IntOffset(menuX.roundToInt(), menuTopY.roundToInt()) }
                .width(menuWidth)
                .zIndex(2f)
                .clip(SurfaceShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { SurfaceShape },
                    effects = {
                        vibrancy()
                        blur(20.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(SurfaceColor)
                    },
                )
                .padding(10.dp)
                .onGloballyPositioned { coordinates ->
                    menuHeightPx = coordinates.size.height
                },
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + scaleIn(),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + scaleOut(),
        ) {
            OverflowMenu(
                state = state,
                actions = actions,
                onDismiss = { actions?.onToggleToolbox() },
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(currentGearX.roundToInt(), currentGearY.roundToInt()) }
                .size(gearSize)
                .zIndex(3f)
                .pointerInput(maxGearX, maxGearY) {
                    detectDragGesturesAfterLongPress { change, dragAmount ->
                        val startX = if (gearX.isNaN()) maxGearX else gearX
                        val startY = if (gearY.isNaN()) defaultGearY else gearY
                        gearX = (startX + dragAmount.x).coerceIn(0f, maxGearX)
                        gearY = (startY + dragAmount.y).coerceIn(0f, maxGearY)
                    }
                },
        ) {
            CupertinoIconButton(
                onClick = {
                    focusManager.clearFocus()
                    actions?.onToggleToolbox()
                },
                modifier = Modifier
                    .size(gearSize)
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(26.dp)),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Gearshape,
                    contentDescription = "路线工具",
                    tint = LabelColor,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier
                    .then(
                        if (searchExpanded) {
                            Modifier
                                .fillMaxWidth(0.72f)
                                .widthIn(max = 320.dp)
                        } else {
                            Modifier.width(52.dp)
                        },
                    )
                    .animateContentSize(tween(240)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (searchExpanded) {
                    CupertinoSearchTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            actions?.onSearchChanged(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester)
                            .onFocusChanged {
                                if (!it.isFocused && searchExpanded) {
                                    searchExpanded = false
                                }
                            },
                        placeholder = {
                            CupertinoText("搜索地点")
                        },
                        textStyle = TextStyle(fontSize = 15.sp),
                    )
                    CupertinoIconButton(
                        onClick = {
                            actions?.onSearchSubmit(searchText)
                            focusManager.clearFocus()
                            searchExpanded = false
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                            contentDescription = "搜索",
                            tint = LabelColor,
                        )
                    }
                } else {
                    CupertinoIconButton(
                        onClick = { searchExpanded = true },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                            contentDescription = "展开搜索",
                            tint = LabelColor,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CupertinoButton(
                        onClick = { actions?.onSelectMapType(false) },
                        enabled = state.satelliteMap,
                    ) {
                        CupertinoText("标准")
                    }
                    CupertinoButton(
                        onClick = { actions?.onSelectMapType(true) },
                        enabled = !state.satelliteMap,
                    ) {
                        CupertinoText("卫星")
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
                onClick = { actions?.onCloseRoute() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
            ) {
                CupertinoText("闭合路线")
            }
        }

        CupertinoIconButton(
            onClick = { actions?.onLocation() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 72.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Location,
                contentDescription = "当前位置",
                tint = LabelColor,
            )
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CupertinoSwitch(
                checked = state.recordMode,
                onCheckedChange = {
                    actions?.onRecordModeChanged(it)
                },
            )
            CupertinoText(
                text = "录制路线",
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 15.sp),
            )
            CupertinoIconButton(
                onClick = { actions?.onRecordSettings() },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Gearshape,
                    contentDescription = "录制设置",
                    tint = LabelColor,
                )
            }
            CupertinoButton(
                onClick = { actions?.onRecord() },
                enabled = state.recordMode || state.recording,
            ) {
                CupertinoText(if (state.recording) "停止绘制" else "开始绘制")
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
            .fillMaxWidth()
            .height(420.dp)
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
        OverflowActionButton("保存路线", enabled = !state.recording) {
            perform { actions?.onSave() }
        }
        OverflowActionButton("清除重画", enabled = !state.recording) {
            perform { actions?.onClear() }
        }
        OverflowActionButton("分享路线", enabled = !state.recording) {
            perform { actions?.onShare() }
        }
        OverflowActionButton(
            label = nextMode,
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
        OverflowActionButton("自动加点", enabled = state.canAutoAdd) {
            perform { actions?.onAutoAdd() }
        }
        OverflowActionButton("自动减点", enabled = state.canAutoReduce) {
            perform { actions?.onAutoReduce() }
        }
        OverflowActionButton("平滑拐角", enabled = state.canSmooth) {
            perform { actions?.onSmoothCorner() }
        }
        OverflowActionButton("撤销", enabled = state.canUndo) {
            perform { actions?.onUndo() }
        }
        OverflowActionButton("重做", enabled = state.canRedo) {
            perform { actions?.onRedo() }
        }
        if (state.selectedPointIndex >= 0) {
            OverflowActionButton("保留点 ${state.selectedPointIndex + 1}") {
                perform { actions?.onKeep() }
            }
            OverflowActionButton("删除点 ${state.selectedPointIndex + 1}") {
                perform { actions?.onDelete() }
            }
        }
    }
}

@Composable
private fun OverflowActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CupertinoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun androidx.compose.ui.unit.Dp.toPx(density: androidx.compose.ui.unit.Density): Float {
    return with(density) { toPx() }
}
