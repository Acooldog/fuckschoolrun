package com.acooldog.toolbox.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.res.painterResource
import com.acooldog.toolbox.R
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import kotlinx.coroutines.launch

class WelcomeComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    @Composable
    override fun Content() {
        CupertinoTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            setImageResource(R.drawable.welcome_cover)
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                val logoAlpha = remember { Animatable(0f) }
                val logoScale = remember { Animatable(0.82f) }
                val titleAlpha = remember { Animatable(0f) }
                val titleOffset = remember { Animatable(36f) }
                val subtitleAlpha = remember { Animatable(0f) }
                val subtitleOffset = remember { Animatable(48f) }

                LaunchedEffect(Unit) {
                    launch {
                        logoAlpha.animateTo(1f, tween(durationMillis = 1000))
                    }
                    launch {
                        logoScale.animateTo(1f, tween(durationMillis = 1000))
                    }
                    launch {
                        titleAlpha.animateTo(1f, tween(durationMillis = 1000))
                    }
                    launch {
                        titleOffset.animateTo(0f, tween(durationMillis = 1000))
                    }
                    launch {
                        kotlinx.coroutines.delay(150)
                        subtitleAlpha.animateTo(1f, tween(durationMillis = 850))
                        subtitleOffset.animateTo(0f, tween(durationMillis = 850))
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    AndroidView(
                        factory = { context ->
                            android.widget.ImageView(context).apply {
                                setImageResource(R.mipmap.ic_launcher)
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        modifier = Modifier
                            .size(112.dp)
                            .graphicsLayer {
                                alpha = logoAlpha.value
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                            },
                    )
                    Spacer(Modifier.height(24.dp))
                    BasicText(
                        text = "牢大肘击校园跑",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.graphicsLayer {
                            alpha = titleAlpha.value
                            translationY = titleOffset.value
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        text = "路线模拟 · NFC 工具 · 联动测试",
                        style = TextStyle(
                            color = Color(0xFFF5F5F5),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.graphicsLayer {
                            alpha = subtitleAlpha.value
                            translationY = subtitleOffset.value
                        }
                    )
                }
            }
        }
    }
}
