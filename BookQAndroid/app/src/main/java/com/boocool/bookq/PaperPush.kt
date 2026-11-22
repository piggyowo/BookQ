package com.boocool.bookq

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val PaperEnterEase = CubicBezierEasing(0.22f, 0.76f, 0.16f, 1f)
private val PaperExitEase = CubicBezierEasing(0.36f, 0.08f, 0.18f, 1f)

/**
 * 全局纸张推页容器。
 * 底栏页面只需要维护一个可选二级页状态；进入、返回、根页退让和左缘拖回都由这里统一处理。
 */
@Composable
fun <T : Any> PaperPushHost(
    page: T?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    root: @Composable () -> Unit,
    detail: @Composable (T) -> Unit
) {
    var lastPage by remember { mutableStateOf<T?>(page) }
    var retained by remember { mutableStateOf(page != null) }
    val motion = remember { Animatable(if (page != null) 0f else 1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page) {
        if (page != null) {
            lastPage = page
            retained = true
            motion.animateTo(0f, tween(340, easing = PaperEnterEase))
        } else if (retained) {
            motion.animateTo(1f, tween(280, easing = PaperExitEase))
            retained = false
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val progress = (1f - motion.value).coerceIn(0f, 1f)

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -maxWidth * 0.14f * progress
                        scaleX = 1f - 0.028f * progress
                        scaleY = 1f - 0.028f * progress
                        alpha = 1f - 0.10f * progress
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                root()
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f * progress))
            )

            if (retained) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = motion.value * maxWidth }
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Palette.Paper)
                    ) {
                        lastPage?.let { detail(it) }
                    }

                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(22.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .align(Alignment.CenterStart)
                            .background(Palette.Gold.copy(alpha = 0.18f))
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(26.dp)
                            .align(Alignment.CenterStart)
                            .pointerInput(maxWidth, retained) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (motion.value > 0.28f) {
                                            scope.launch {
                                                motion.animateTo(1f, tween(280, easing = PaperExitEase))
                                                onDismiss()
                                            }
                                        } else {
                                            scope.launch {
                                                motion.animateTo(0f, tween(280, easing = PaperExitEase))
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { change, amount ->
                                        if (motion.value < 0.02f) {
                                            change.consume()
                                            scope.launch {
                                                motion.snapTo(
                                                    (motion.value + amount / maxWidth).coerceIn(0f, 1f)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}
