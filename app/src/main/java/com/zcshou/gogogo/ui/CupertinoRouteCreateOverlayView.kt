package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoSearchTextField
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Location
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import androidx.compose.material.Icon
import androidx.compose.foundation.text.BasicText

class CupertinoRouteCreateOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
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
        satelliteMap: Boolean
    ) {
        state = state.copy(
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
            satelliteMap = satelliteMap
        )
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            RouteCreateOverlay(state, actions)
        }
    }

    data class State(
        val recordMode: Boolean = false,
        val recording: Boolean = false,
        val toolboxCollapsed: Boolean = false,
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
        val satelliteMap: Boolean = false
    )
}

@Composable
private fun RouteCreateOverlay(
    state: CupertinoRouteCreateOverlayView.State,
    actions: CupertinoRouteCreateOverlayView.Actions?
) {
    var searchExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val toolboxWidth by animateDpAsState(
        targetValue = if (state.toolboxCollapsed) 52.dp else 214.dp,
        animationSpec = tween(240),
        label = "create-toolbox-width"
    )

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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
                        }
                    )
                    .animateContentSize(tween(240)),
                verticalAlignment = Alignment.CenterVertically
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
                        placeholder = { BasicText("搜索地点") },
                        textStyle = TextStyle(fontSize = 15.sp)
                    )
                    CupertinoIconButton(
                        onClick = {
                            actions?.onSearchSubmit(searchText)
                            focusManager.clearFocus()
                            searchExpanded = false
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            CupertinoIcons.Outlined.MagnifyingGlass,
                            contentDescription = "搜索"
                        )
                    }
                } else {
                    CupertinoIconButton(
                        onClick = { searchExpanded = true },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            CupertinoIcons.Outlined.MagnifyingGlass,
                            contentDescription = "展开搜索"
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CupertinoButton(
                        onClick = { actions?.onSelectMapType(false) },
                        enabled = state.satelliteMap
                    ) {
                        BasicText("标准")
                    }
                    CupertinoButton(
                        onClick = { actions?.onSelectMapType(true) },
                        enabled = !state.satelliteMap
                    ) {
                        BasicText("卫星")
                    }
                }
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = state.hint,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 68.dp)
                .width(toolboxWidth)
                .fillMaxHeight(0.58f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = !state.toolboxCollapsed) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionButton("保存路线", enabled = !state.recording) { actions?.onSave() }
                    ActionButton("清除重画", enabled = !state.recording) { actions?.onClear() }
                    ActionButton("分享路线", enabled = !state.recording) { actions?.onShare() }
                    ActionButton("绘制", enabled = state.selectedTool != 0) { actions?.onDrawTool() }
                    ActionButton("编辑", enabled = state.canEdit) { actions?.onEditTool() }
                    ActionButton("自动加点", enabled = state.canAutoAdd) { actions?.onAutoAdd() }
                    ActionButton("自动减点", enabled = state.canAutoReduce) { actions?.onAutoReduce() }
                    ActionButton("平滑拐角", enabled = state.canSmooth) { actions?.onSmoothCorner() }
                    ActionButton("撤销", enabled = state.canUndo) { actions?.onUndo() }
                    ActionButton("重做", enabled = state.canRedo) { actions?.onRedo() }
                    if (state.selectedPointIndex >= 0) {
                        ActionButton("保留点 ${state.selectedPointIndex + 1}") { actions?.onKeep() }
                        ActionButton("删除点 ${state.selectedPointIndex + 1}") { actions?.onDelete() }
                    }
                }
            }
            CupertinoIconButton(
                onClick = { actions?.onToggleToolbox() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    if (state.toolboxCollapsed) CupertinoIcons.Outlined.ChevronForward
                    else CupertinoIcons.Outlined.ChevronBackward,
                    contentDescription = "收起工具栏"
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(330.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CupertinoSwitch(
                checked = state.recordMode,
                onCheckedChange = { actions?.onRecordModeChanged(it) }
            )
            BasicText(
                text = "绘制路线",
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Color.White, fontSize = 14.sp)
            )
            CupertinoIconButton(
                onClick = { actions?.onRecordSettings() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(CupertinoIcons.Outlined.Gearshape, contentDescription = "录制设置")
            }
            CupertinoButton(
                onClick = { actions?.onRecord() },
                enabled = state.recordMode || state.recording
            ) {
                BasicText(if (state.recording) "停止绘制" else "开始绘制")
            }
        }

        if (state.canCloseRoute) {
            CupertinoButton(
                onClick = { actions?.onCloseRoute() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            ) {
                BasicText("闭合路线")
            }
        }

        CupertinoIconButton(
            onClick = { actions?.onLocation() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 66.dp)
                .size(48.dp)
        ) {
            Icon(CupertinoIcons.Outlined.Location, contentDescription = "当前位置")
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicText(label)
    }
}
