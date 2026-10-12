package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import androidx.compose.ui.platform.AbstractComposeView

class CupertinoSimulationSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    interface Actions {
        fun onModeChanged(cadence: Boolean)
        fun onSpeedChanged(value: String)
        fun onCadenceChanged(value: String)
        fun onLiveRouteEditChanged(enabled: Boolean)
        fun onRatioChanged(value: String)
        fun onStepsPerMeterChanged(value: String)
        fun onLoopChanged(value: String)
        fun onNetworkSimulationChanged(enabled: Boolean)
        fun onSatelliteChanged(value: Int)
        fun onSignalQualityChanged(value: Int)
        fun onHdopChanged(value: Float)
        fun onUpdateIntervalChanged(value: Int)
        fun onDynamicIntensityChanged(enabled: Boolean)
        fun onIntensityRangeChanged(value: String)
        fun onIntensityFrequencyChanged(value: Float)
        fun onPathVariationChanged(enabled: Boolean)
        fun onPathAmplitudeChanged(value: String)
        fun onAltitudeVariationChanged(enabled: Boolean)
        fun onAltitudeBaseChanged(value: String)
        fun onAltitudeRangeChanged(value: String)
        fun onAltitudeHeightChanged(value: String)
        fun onAltitudeProbabilityChanged(value: Float)
        fun onFloatingWindowChanged(enabled: Boolean)
        fun onFloatingScaleChanged(value: Float)
        fun onFloatingButtonSizeChanged(value: Float)
        fun onPickRingtone()
        fun onUpload()
        fun onDownload()
    }

    data class State(
        val currentRoute: String = "当前路线：未选择",
        val cadenceMode: Boolean = false,
        val speed: String = "15",
        val cadence: String = "",
        val liveRouteEdit: Boolean = false,
        val ratio: String = "1",
        val stepsPerMeter: String = "1",
        val loopCount: String = "100",
        val networkSimulation: Boolean = false,
        val satellites: Int = 8,
        val signalQuality: Int = 0,
        val hdop: Float = 1.5f,
        val updateInterval: Int = 1,
        val dynamicIntensity: Boolean = false,
        val intensityRange: String = "2.0",
        val intensityFrequency: Float = 0.35f,
        val pathVariation: Boolean = false,
        val pathAmplitude: String = "1.0",
        val altitudeVariation: Boolean = false,
        val altitudeBase: String = "55.0",
        val altitudeRange: String = "0.6",
        val altitudeHeight: String = "170",
        val altitudeProbability: Float = 0.35f,
        val floatingWindow: Boolean = false,
        val floatingScale: Float = 0.58f,
        val floatingButtonSize: Float = 44f,
        val ringtone: String = "默认提示音"
    )

    private var actions: Actions? = null
    private var state by mutableStateOf(State())

    fun setActions(actions: Actions?) {
        this.actions = actions
    }

    fun getCurrentState(): State = state

    fun setRingtone(value: String) {
        state = state.copy(ringtone = value)
    }

    fun render(
        currentRoute: String,
        cadenceMode: Boolean,
        speed: String,
        cadence: String,
        liveRouteEdit: Boolean,
        ratio: String,
        stepsPerMeter: String,
        loopCount: String,
        networkSimulation: Boolean,
        satellites: Int,
        signalQuality: Int,
        hdop: Float,
        updateInterval: Int,
        dynamicIntensity: Boolean,
        intensityRange: String,
        intensityFrequency: Float,
        pathVariation: Boolean,
        pathAmplitude: String,
        altitudeVariation: Boolean,
        altitudeBase: String,
        altitudeRange: String,
        altitudeHeight: String,
        altitudeProbability: Float,
        floatingWindow: Boolean,
        floatingScale: Float,
        floatingButtonSize: Float,
        ringtone: String
    ) {
        state = State(
            currentRoute,
            cadenceMode,
            speed,
            cadence,
            liveRouteEdit,
            ratio,
            stepsPerMeter,
            loopCount,
            networkSimulation,
            satellites,
            signalQuality,
            hdop,
            updateInterval,
            dynamicIntensity,
            intensityRange,
            intensityFrequency,
            pathVariation,
            pathAmplitude,
            altitudeVariation,
            altitudeBase,
            altitudeRange,
            altitudeHeight,
            altitudeProbability,
            floatingWindow,
            floatingScale,
            floatingButtonSize,
            ringtone
        )
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            SimulationSettingsContent(
                initial = state,
                actions = actions,
                onStateChanged = { state = it }
            )
        }
    }
}

@Composable
private fun SimulationSettingsContent(
    initial: CupertinoSimulationSettingsView.State,
    actions: CupertinoSimulationSettingsView.Actions?,
    onStateChanged: (CupertinoSimulationSettingsView.State) -> Unit
) {
    var state by remember(initial) { mutableStateOf(initial) }

    LaunchedEffect(state) {
        onStateChanged(state)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BasicText("当前路线", style = SectionTitle)
        BasicText(initial.currentRoute, style = BodyText)
        ToggleRow(
            label = "模拟方式",
            checked = state.cadenceMode,
            onCheckedChange = {
                state = state.copy(cadenceMode = it)
                actions?.onModeChanged(it)
            },
            checkedLabel = "步频",
            uncheckedLabel = "速度"
        )
        InputRow("速度", state.speed, "km/h") {
            state = state.copy(speed = it)
            actions?.onSpeedChanged(it)
        }
        InputRow("步频", state.cadence, "步/分钟") {
            state = state.copy(cadence = it)
            actions?.onCadenceChanged(it)
        }
        ToggleRow(
            label = "实时路线编辑",
            checked = state.liveRouteEdit,
            onCheckedChange = {
                state = state.copy(liveRouteEdit = it)
                actions?.onLiveRouteEditChanged(it)
            }
        )

        SectionTitleText("联动参数")
        InputRow("路线比例", state.ratio, "倍") {
            state = state.copy(ratio = it)
            actions?.onRatioChanged(it)
        }
        InputRow("每米步数", state.stepsPerMeter, "步") {
            state = state.copy(stepsPerMeter = it)
            actions?.onStepsPerMeterChanged(it)
        }
        InputRow("循环次数", state.loopCount, "次") {
            state = state.copy(loopCount = it)
            actions?.onLoopChanged(it)
        }

        SectionTitleText("NMEA 模拟")
        ToggleRow("网络模拟", state.networkSimulation, {
            state = state.copy(networkSimulation = it)
            actions?.onNetworkSimulationChanged(it)
        })
        SliderRow("卫星数量", state.satellites.toFloat(), 1f..12f, "${state.satellites}") {
            val value = it.toInt().coerceIn(1, 12)
            state = state.copy(satellites = value)
            actions?.onSatelliteChanged(value)
        }
        ChoiceRow(
            "信号质量",
            listOf("强", "中", "弱"),
            state.signalQuality
        ) {
            state = state.copy(signalQuality = it)
            actions?.onSignalQualityChanged(it)
        }
        SliderRow("HDOP", state.hdop, 0f..3f, "%.1f".format(state.hdop)) {
            state = state.copy(hdop = it)
            actions?.onHdopChanged(it)
        }
        SliderRow("更新间隔", state.updateInterval.toFloat(), 1f..20f, "${state.updateInterval}") {
            val value = it.toInt().coerceIn(1, 20)
            state = state.copy(updateInterval = value)
            actions?.onUpdateIntervalChanged(value)
        }

        SectionTitleText("动态变化")
        ToggleRow("速度浮动", state.dynamicIntensity, {
            state = state.copy(dynamicIntensity = it)
            actions?.onDynamicIntensityChanged(it)
        })
        if (state.dynamicIntensity) {
            InputRow("浮动范围", state.intensityRange, "km/h") {
                state = state.copy(intensityRange = it)
                actions?.onIntensityRangeChanged(it)
            }
            SliderRow(
                "浮动频率",
                state.intensityFrequency,
                0f..1f,
                "%.0f%%".format(state.intensityFrequency * 100f)
            ) {
                state = state.copy(intensityFrequency = it)
                actions?.onIntensityFrequencyChanged(it)
            }
        }

        SectionTitleText("自然路线")
        ToggleRow("路径扰动", state.pathVariation, {
            state = state.copy(pathVariation = it)
            actions?.onPathVariationChanged(it)
        })
        if (state.pathVariation) {
            InputRow("路径幅度", state.pathAmplitude, "米") {
                state = state.copy(pathAmplitude = it)
                actions?.onPathAmplitudeChanged(it)
            }
        }

        SectionTitleText("海拔变化")
        ToggleRow("海拔扰动", state.altitudeVariation, {
            state = state.copy(altitudeVariation = it)
            actions?.onAltitudeVariationChanged(it)
        })
        if (state.altitudeVariation) {
            InputRow("基础海拔", state.altitudeBase, "米") {
                state = state.copy(altitudeBase = it)
                actions?.onAltitudeBaseChanged(it)
            }
            InputRow("变化范围", state.altitudeRange, "米") {
                state = state.copy(altitudeRange = it)
                actions?.onAltitudeRangeChanged(it)
            }
            InputRow("变化高度", state.altitudeHeight, "厘米") {
                state = state.copy(altitudeHeight = it)
                actions?.onAltitudeHeightChanged(it)
            }
            SliderRow(
                "触发概率",
                state.altitudeProbability,
                0f..1f,
                "%.0f%%".format(state.altitudeProbability * 100f)
            ) {
                state = state.copy(altitudeProbability = it)
                actions?.onAltitudeProbabilityChanged(it)
            }
        }

        SectionTitleText("悬浮窗")
        ToggleRow("启用悬浮窗", state.floatingWindow, {
            state = state.copy(floatingWindow = it)
            actions?.onFloatingWindowChanged(it)
        })
        if (state.floatingWindow) {
            SliderRow(
                "缩放比例",
                state.floatingScale,
                0.1f..1f,
                "%.0f%%".format(state.floatingScale * 100f)
            ) {
                state = state.copy(floatingScale = it)
                actions?.onFloatingScaleChanged(it)
            }
            SliderRow(
                "按钮大小",
                state.floatingButtonSize,
                28f..100f,
                "${state.floatingButtonSize.toInt()}dp"
            ) {
                state = state.copy(floatingButtonSize = it)
                actions?.onFloatingButtonSizeChanged(it)
            }
        }

        SectionTitleText("提醒与共享")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(state.ringtone, style = BodyText)
            CupertinoButton(onClick = { actions?.onPickRingtone() }) {
                BasicText("选择提示音")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CupertinoButton(
                onClick = { actions?.onUpload() },
                modifier = Modifier.weight(1f)
            ) {
                BasicText("上传设置")
            }
            CupertinoButton(
                onClick = { actions?.onDownload() },
                modifier = Modifier.weight(1f)
            ) {
                BasicText("下载设置")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionTitleText(text: String) {
    BasicText(text, style = SectionTitle, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InputRow(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            label,
            style = BodyText,
            modifier = Modifier
                .widthIn(min = 92.dp)
                .weight(0.9f)
        )
        CupertinoTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1.1f)
                .widthIn(min = 116.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp)
        )
        BasicText(
            unit,
            style = BodyText,
            modifier = Modifier.widthIn(min = 44.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedLabel: String? = null,
    uncheckedLabel: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            if (checked && checkedLabel != null) "$label：$checkedLabel"
            else if (!checked && uncheckedLabel != null) "$label：$uncheckedLabel"
            else label,
            style = BodyText
        )
        CupertinoSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    choices: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = BodyText)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            choices.forEachIndexed { index, choice ->
                CupertinoButton(
                    onClick = { onSelected(index) },
                    enabled = index != selected,
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(choice)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(label, style = BodyText)
            BasicText(valueLabel, style = BodyText)
        }
        CupertinoSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val SectionTitle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold
)

private val BodyText = TextStyle(
    fontSize = 14.sp
)
