package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

class CupertinoRingtoneOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    interface Listener {
        fun onPreview(index: Int)
        fun onSelect(index: Int)
    }

    private var options by mutableStateOf(emptyList<String>())
    private var listener: Listener? = null

    fun setOptions(options: Array<String>) {
        this.options = options.toList()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    @Composable
    override fun Content() {
        CupertinoTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, label ->
                    RingtoneOptionRow(
                        label = label,
                        onPreview = { listener?.onPreview(index) },
                        onSelect = { listener?.onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RingtoneOptionRow(
    label: String,
    onPreview: () -> Unit,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color(0xFF1C1C1E), fontSize = 15.sp)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CupertinoButton(onClick = onPreview) {
                BasicText("试听", style = TextStyle(color = Color(0xFF007AFF), fontSize = 14.sp))
            }
            CupertinoButton(onClick = onSelect) {
                BasicText("选择", style = TextStyle(color = Color(0xFF007AFF), fontSize = 14.sp))
            }
        }
    }
}
