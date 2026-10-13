package com.acooldog.toolbox.tutorial

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.*
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun TutorialOverlay(activity: Activity) {
    val document = TutorialController.config ?: return
    CupertinoTheme {
        if (TutorialController.offering) {
            CupertinoAlertDialog(
                onDismissRequest = { TutorialController.decline() },
                title = { CupertinoText(document.title) },
                message = { CupertinoText(document.entryMessage) },
                buttons = {
                    cancel(onClick = { TutorialController.decline() }) { CupertinoText("暂不需要") }
                    default(onClick = { TutorialController.accept() }) { CupertinoText("开始教程") }
                },
            )
            return@CupertinoTheme
        }
        if (!TutorialController.running) return@CupertinoTheme
        val step = document.steps.getOrNull(TutorialController.index) ?: return@CupertinoTheme
        BackHandler { TutorialController.exit() }
        if (step.element.key == "dialog") {
            CupertinoAlertDialog(
                onDismissRequest = { TutorialController.exit() },
                title = { CupertinoText(document.title) },
                message = {
                    Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                        CupertinoText(step.text, style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp))
                        CupertinoButton(onClick = { TutorialController.next() }, enabled = !TutorialController.busy) {
                            CupertinoText(if (TutorialController.busy) "请稍候" else "下一步")
                        }
                    }
                },
                buttons = { cancel(onClick = { TutorialController.exit() }) { CupertinoText("结束教程") } },
            )
        } else {
            HighlightStep(activity, step)
        }
    }
}

@Composable
private fun HighlightStep(activity: Activity, step: TutorialStep) {
    var anchor by remember(step.id) { mutableStateOf<TutorialAnchor?>(null) }
    LaunchedEffect(activity, step.id) {
        while (true) {
            anchor = TutorialController.anchor(activity, step.element)
            TutorialController.alignWindow(activity, anchor)
            delay(80)
        }
    }
    val pulse by rememberInfiniteTransition(label = "tutorial-breath").animateFloat(
        initialValue = 0.25f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "tutorial-glow",
    )
    val rootLocation = TutorialController.overlayLocation()
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.10f))
        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}) {
        val density = LocalDensity.current
        val margin = with(density) { 16.dp.toPx() }
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val bounds = anchor?.bounds?.translate(Offset(-rootLocation[0].toFloat(), -rootLocation[1].toFloat()))
            ?.intersect(Rect(0f, 0f, width, height))?.takeUnless { it.isEmpty }
        if (bounds != null) {
            Canvas(Modifier.fillMaxSize()) {
                for (layer in 10 downTo 1) {
                    val expansion = (layer * 1.3f + 2f).dp.toPx()
                    drawRoundRect(Color(0xFFFF9500).copy(alpha = pulse * (11 - layer) / 100f),
                        topLeft = Offset(bounds.left - expansion, bounds.top - expansion),
                        size = Size(bounds.width + expansion * 2, bounds.height + expansion * 2),
                        cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(3.dp.toPx()))
                }
                drawRoundRect(Color(0xFFFF9500).copy(alpha = pulse),
                    Offset(bounds.left - 2.dp.toPx(), bounds.top - 2.dp.toPx()),
                    Size(bounds.width + 4.dp.toPx(), bounds.height + 4.dp.toPx()),
                    CornerRadius(6.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
        var bubbleHeight by remember(step.id) { mutableStateOf(0) }
        val bubbleWidth = (maxWidth - 32.dp).coerceAtMost(340.dp)
        val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
        val x = ((bounds?.center?.x ?: width / 2) - bubbleWidthPx / 2)
            .coerceIn(margin, (width - bubbleWidthPx - margin).coerceAtLeast(margin))
        val below = (bounds?.bottom ?: margin) + margin
        val desiredY = if (below + bubbleHeight <= height - margin) below
            else (bounds?.top ?: height) - bubbleHeight - margin
        val y = desiredY.coerceIn(margin, (height - bubbleHeight - margin).coerceAtLeast(margin))
        Column(Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(bubbleWidth).heightIn(max = (maxHeight - 32.dp).coerceAtMost(360.dp))
            .onSizeChanged { bubbleHeight = it.height }
            .background(Color(0xCC55565A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFDDDEE1).copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .padding(14.dp)) {
            CupertinoText("${TutorialController.index + 1} / ${TutorialController.config?.steps?.size}",
                style = TextStyle(color = Color(0xFFD9D9DE), fontSize = 12.sp))
            Spacer(Modifier.height(8.dp))
            CupertinoText(if (bounds == null) "${step.text}\n\n等待控件：${step.element.name}" else step.text,
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                style = TextStyle(color = Color.White, fontSize = 15.sp, lineHeight = 22.sp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CupertinoButton(onClick = { TutorialController.exit() },
                    colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText("结束", style = TextStyle(color = Color.White)) }
                if (bounds == null) {
                    CupertinoButton(onClick = { TutorialController.retry() }) { CupertinoText("重试") }
                } else {
                    CupertinoButton(onClick = { TutorialController.next() },
                        enabled = !TutorialController.busy && (!step.autoClick || anchor?.enabled == true)) {
                        CupertinoText(if (TutorialController.busy) "请稍候" else "下一步")
                    }
                }
            }
        }
    }
}
