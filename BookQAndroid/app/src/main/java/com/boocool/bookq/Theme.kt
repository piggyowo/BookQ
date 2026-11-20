package com.boocool.bookq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

object Palette {
    val Ink = Color(0xFF16232F)
    val Ink2 = Color(0xFF44525F)
    val Muted = Color(0xFF7C8794)
    val Paper = Color(0xFFF5F0E7)
    val Card = Color(0xFFFFFDF8)
    val Line = Color(0xFFE5DCCB)
    val Line2 = Color(0xFFEFE7D8)
    val Primary = Color(0xFF1A455C)
    val Primary2 = Color(0xFF2A6B80)
    val Accent = Color(0xFFB9613C)
    val Gold = Color(0xFFBD9350)
    val Green = Color(0xFF1C7A5B)
    val GreenBg = Color(0xFFE4F2EA)
    val Red = Color(0xFFAB3A2C)
    val RedBg = Color(0xFFFAE8E3)
    val Amber = Color(0xFF8A6A2F)
    val AmberLine = Color(0xFFD9BD8C)
    val AmberBg = Color(0xFFFBF3E3)
    val Track = Color(0xFFEEE7D9)
    val Cream = Color(0xFFF6EFE2)
    val SoftPaper = Color(0xFFFDFAF3)
    val SelectedBg = Color(0xFFEFF5F7)
    val AnsweredBg = Color(0xFFEAF1F3)
    val ChipBg = Color(0xFFFAF6EE)
    val InkSoft = Color(0xFF6A5A3C)
    val Number = Color(0xFFD8CBB3)
}

fun serif(size: Int, weight: FontWeight = FontWeight.Bold): TextStyle =
    TextStyle(fontFamily = FontFamily.Serif, fontSize = size.sp, fontWeight = weight, color = Palette.Ink)

fun sans(size: Int, weight: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size.sp, fontWeight = weight, color = Palette.Ink)

@Composable
fun BookQTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Palette.Primary,
            onPrimary = Color.White,
            background = Palette.Paper,
            onBackground = Palette.Ink,
            surface = Palette.Card,
            onSurface = Palette.Ink
        )
    ) {
        content()
    }
}

@Composable
fun PaperBackground() {
    Box(Modifier.fillMaxSize().background(Palette.Paper)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Palette.Paper.copy(alpha = 0.95f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 720.dp.toPx()
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE8EEF0).copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = 620.dp.toPx()
                )
            )
            val step = 4.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(Palette.Ink.copy(alpha = 0.022f), Offset(x, 0f), Offset(x, size.height), 0.5f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Palette.Ink.copy(alpha = 0.022f), Offset(0f, y), Offset(size.width, y), 0.5f)
                y += step
            }
        }
    }
}

@Composable
fun FabricGrid(modifier: Modifier = Modifier, spacing: Dp = 5.dp, light: Boolean = false) {
    Canvas(modifier.fillMaxSize()) {
        val step = spacing.toPx()
        val color = (if (light) Color.White else Palette.Ink).copy(alpha = if (light) 0.045f else 0.022f)
        var x = 0f
        while (x <= size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), 0.5f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), 0.5f)
            y += step
        }
    }
}

@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .shadow(14.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF152B3A), Color(0xFF1C455C), Color(0xFF25596A)),
                    start = Offset(0f, 0f),
                    end = Offset(1200f, 900f)
                )
            )
    ) {
        FabricGrid(spacing = 5.dp, light = true)
        content()
    }
}

@Composable
fun BrandMark(size: Dp = 42.dp) {
    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Palette.Primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(horizontal = size * 0.26f),
            verticalArrangement = Arrangement.spacedBy(size * 0.1f)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Palette.Cream)
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = size * 0.14f, end = size * 0.14f)
                .size(5.dp)
                .background(Palette.Gold, CircleShape)
        )
    }
}

@Composable
fun PillLabel(text: String, color: Color = Palette.Primary, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = sans(12, FontWeight.Medium).copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(Palette.Track)
            .padding(horizontal = 11.dp, vertical = 4.dp)
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = serif(19), modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                style = sans(12, FontWeight.Normal).copy(color = if (onTrailing != null) Palette.Primary else Palette.Muted),
                modifier = if (onTrailing != null) Modifier.clickable { onTrailing() } else Modifier
            )
        }
    }
}

@Composable
fun ProgressTrack(
    value: Double,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    low: Boolean = false
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = Palette.Track, size = size, cornerRadius = radius)
        val progress = max(0.0, value.coerceAtMost(1.0)).toFloat()
        if (progress > 0f) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = if (low) listOf(Color(0xFFC98A6B), Palette.Accent)
                    else listOf(Palette.Primary2, Palette.Primary)
                ),
                size = Size(size.width * progress, size.height),
                cornerRadius = radius
            )
        }
    }
}

@Composable
fun RingProgress(
    value: Double,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    lineWidth: Dp = 7.dp,
    foreground: Color = Palette.Cream,
    background: Color = Color.White.copy(alpha = 0.18f)
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(background, style = Stroke(width = lineWidth.toPx()))
            drawArc(
                color = foreground,
                startAngle = -90f,
                sweepAngle = 360f * max(0.0, value.coerceAtMost(1.0)).toFloat(),
                useCenter = false,
                style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            "${(value * 100).roundToInt()}%",
            style = TextStyle(fontFamily = FontFamily.Serif, fontSize = (size.value * 0.25f).sp, fontWeight = FontWeight.Bold, color = Palette.Cream)
        )
    }
}

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    padding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier.shadow(4.dp, shape),
        shape = shape,
        color = Palette.Card,
        border = BorderStroke(1.dp, Palette.Line)
    ) {
        Box(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Primary,
            contentColor = Color.White,
            disabledContainerColor = Palette.Muted,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = sans(15, FontWeight.SemiBold).copy(color = Color.White))
    }
}

@Composable
fun CreamPillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Palette.Cream)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = sans(14, FontWeight.SemiBold).copy(color = Palette.Primary))
    }
}

@Composable
fun GhostPillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        style = sans(14, FontWeight.Medium),
        modifier = modifier
            .clip(CircleShape)
            .background(Palette.Card.copy(alpha = 0.85f))
            .border(1.dp, Palette.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

@Composable
fun ChipButton(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        title,
        style = sans(13, FontWeight.Medium).copy(color = if (selected) Color.White else Palette.Ink2),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Palette.Primary else Palette.ChipBg)
            .border(1.dp, if (selected) Palette.Primary else Palette.Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    )
}

@Composable
fun BottomTabBar(
    selected: AppTab,
    bankCount: Int,
    wrongCount: Int,
    onSelect: (AppTab) -> Unit
) {
    Surface(
        color = Palette.Card.copy(alpha = 0.98f),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Palette.Line)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            AppTab.entries.forEach { tab ->
                val active = selected == tab
                val icon = when (tab) {
                    AppTab.HOME -> Icons.Outlined.Home
                    AppTab.LIBRARY -> Icons.Outlined.LibraryBooks
                    AppTab.SETTINGS -> Icons.Outlined.Tune
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(38.dp, 30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) Palette.Primary.copy(alpha = 0.10f) else Color.Transparent)
                        )
                        Icon(
                            icon,
                            contentDescription = tab.title,
                            tint = if (active) Palette.Primary else Palette.Muted,
                            modifier = Modifier.size(19.dp)
                        )
                        if (tab == AppTab.LIBRARY && bankCount > 0) {
                            Text(
                                "$bankCount",
                                style = sans(9, FontWeight.SemiBold).copy(color = Color.White),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Palette.Accent, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (tab == AppTab.HOME && wrongCount > 0) {
                            Text(
                                "$wrongCount",
                                style = sans(9, FontWeight.SemiBold).copy(color = Color.White),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Palette.Accent, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.title,
                        style = sans(11, if (active) FontWeight.SemiBold else FontWeight.Normal).copy(
                            color = if (active) Palette.Primary else Palette.Muted
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ToastBar(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text(
            message,
            style = sans(13, FontWeight.Normal).copy(color = Color.White),
            modifier = Modifier
                .padding(bottom = 110.dp)
                .shadow(12.dp, CircleShape)
                .background(Color(0xEF17242F), CircleShape)
                .padding(horizontal = 20.dp, vertical = 11.dp)
        )
    }
}

fun formatDuration(seconds: Long): String {
    val total = max(0L, seconds)
    return "%02d:%02d".format(total / 60, total % 60)
}

fun formatDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.CHINA)
    return fmt.format(java.util.Date(millis))
}

fun rateLabel(rate: Int?): String = if (rate == null) "未练习" else "正确率 $rate%"
