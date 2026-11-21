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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

data class BookQColors(
    val isDark: Boolean,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val paper: Color,
    val card: Color,
    val line: Color,
    val line2: Color,
    val primary: Color,
    val primary2: Color,
    val accent: Color,
    val gold: Color,
    val green: Color,
    val greenBg: Color,
    val red: Color,
    val redBg: Color,
    val amber: Color,
    val amberLine: Color,
    val amberBg: Color,
    val track: Color,
    val cream: Color,
    val softPaper: Color,
    val selectedBg: Color,
    val answeredBg: Color,
    val chipBg: Color,
    val inkSoft: Color,
    val number: Color,
    val hero1: Color,
    val hero2: Color,
    val hero3: Color,
    val glow1: Color,
    val glow2: Color
)

object BookQPalettes {
    val IndigoLight = BookQColors(
        isDark = false,
        ink = Color(0xFF16232F), ink2 = Color(0xFF44525F), muted = Color(0xFF7C8794),
        paper = Color(0xFFF5F0E7), card = Color(0xFFFFFDF8), line = Color(0xFFE5DCCB), line2 = Color(0xFFEFE7D8),
        primary = Color(0xFF1A455C), primary2 = Color(0xFF2A6B80), accent = Color(0xFFB9613C), gold = Color(0xFFBD9350),
        green = Color(0xFF1C7A5B), greenBg = Color(0xFFE4F2EA), red = Color(0xFFAB3A2C), redBg = Color(0xFFFAE8E3),
        amber = Color(0xFF8A6A2F), amberLine = Color(0xFFD9BD8C), amberBg = Color(0xFFFBF3E3),
        track = Color(0xFFEEE7D9), cream = Color(0xFFF6EFE2), softPaper = Color(0xFFFDFAF3),
        selectedBg = Color(0xFFEFF5F7), answeredBg = Color(0xFFEAF1F3), chipBg = Color(0xFFFAF6EE),
        inkSoft = Color(0xFF6A5A3C), number = Color(0xFFD8CBB3),
        hero1 = Color(0xFF152B3A), hero2 = Color(0xFF1C455C), hero3 = Color(0xFF25596A),
        glow1 = Color(0xFFBD9350), glow2 = Color(0xFF2A6B80)
    )

    val RustLight = BookQColors(
        isDark = false,
        ink = Color(0xFF2B211C), ink2 = Color(0xFF5C493F), muted = Color(0xFF806F63),
        paper = Color(0xFFF6EDE0), card = Color(0xFFFFF9EF), line = Color(0xFFE6D2BD), line2 = Color(0xFFF0E1CE),
        primary = Color(0xFF8D3F2F), primary2 = Color(0xFFB45C43), accent = Color(0xFF20505D), gold = Color(0xFFC18A4A),
        green = Color(0xFF39775A), greenBg = Color(0xFFE6F0E7), red = Color(0xFFA83A2A), redBg = Color(0xFFFAE8E3),
        amber = Color(0xFF8A6A2F), amberLine = Color(0xFFD9BD8C), amberBg = Color(0xFFFBF3E3),
        track = Color(0xFFEEE0CF), cream = Color(0xFFFBF1DF), softPaper = Color(0xFFFFFCF6),
        selectedBg = Color(0xFFF7EDE5), answeredBg = Color(0xFFF2E9DF), chipBg = Color(0xFFFAEFE1),
        inkSoft = Color(0xFF7A4E32), number = Color(0xFFD9C3A8),
        hero1 = Color(0xFF57201A), hero2 = Color(0xFF8D3F2F), hero3 = Color(0xFFB45C43),
        glow1 = Color(0xFFC18A4A), glow2 = Color(0xFF20505D)
    )

    val MossLight = BookQColors(
        isDark = false,
        ink = Color(0xFF1D2B22), ink2 = Color(0xFF46584B), muted = Color(0xFF708074),
        paper = Color(0xFFEDF0E4), card = Color(0xFFFBFCF4), line = Color(0xFFD7DDC9), line2 = Color(0xFFE6E9DC),
        primary = Color(0xFF2C4F3C), primary2 = Color(0xFF4F7F5F), accent = Color(0xFFA6753F), gold = Color(0xFFB08D55),
        green = Color(0xFF2E7A55), greenBg = Color(0xFFE3F0E7), red = Color(0xFFA84535), redBg = Color(0xFFFAE8E3),
        amber = Color(0xFF8A6A2F), amberLine = Color(0xFFD1BF97), amberBg = Color(0xFFF7F1E2),
        track = Color(0xFFE3E8DA), cream = Color(0xFFF4ECD9), softPaper = Color(0xFFFCFDF7),
        selectedBg = Color(0xFFECF2EA), answeredBg = Color(0xFFE8EFE5), chipBg = Color(0xFFF3F5EA),
        inkSoft = Color(0xFF5C6B3C), number = Color(0xFFCBD3BC),
        hero1 = Color(0xFF172D22), hero2 = Color(0xFF2C4F3C), hero3 = Color(0xFF4F7F5F),
        glow1 = Color(0xFFB08D55), glow2 = Color(0xFF4F7F5F)
    )

    val IndigoDark = BookQColors(
        isDark = true,
        ink = Color(0xFFF1F0E7), ink2 = Color(0xFFC5D0D2), muted = Color(0xFF8FA1A7),
        paper = Color(0xFF111B22), card = Color(0xFF192832), line = Color(0xFF2C3F49), line2 = Color(0xFF23343D),
        primary = Color(0xFF4C8FA4), primary2 = Color(0xFF70ADBB), accent = Color(0xFFD17A54), gold = Color(0xFFD2A65F),
        green = Color(0xFF6DB795), greenBg = Color(0xFF183830), red = Color(0xFFD9796B), redBg = Color(0xFF3B2424),
        amber = Color(0xFFD2A65F), amberLine = Color(0xFF5C4A31), amberBg = Color(0xFF2D2518),
        track = Color(0xFF24343C), cream = Color(0xFFF3EAD9), softPaper = Color(0xFF1C2B35),
        selectedBg = Color(0xFF203540), answeredBg = Color(0xFF1D3039), chipBg = Color(0xFF22323B),
        inkSoft = Color(0xFFC7A96E), number = Color(0xFF4A5C66),
        hero1 = Color(0xFF09141B), hero2 = Color(0xFF163A47), hero3 = Color(0xFF255566),
        glow1 = Color(0xFFD2A65F), glow2 = Color(0xFF70ADBB)
    )

    val RustDark = BookQColors(
        isDark = true,
        ink = Color(0xFFF4E8D8), ink2 = Color(0xFFD0BBAA), muted = Color(0xFF9F8776),
        paper = Color(0xFF1D1512), card = Color(0xFF2A1F19), line = Color(0xFF463329), line2 = Color(0xFF35261F),
        primary = Color(0xFFC76B4E), primary2 = Color(0xFFD98B6B), accent = Color(0xFF5FA0AC), gold = Color(0xFFD1A15B),
        green = Color(0xFF6FB38C), greenBg = Color(0xFF1D3329), red = Color(0xFFD97B67), redBg = Color(0xFF3B2422),
        amber = Color(0xFFD1A15B), amberLine = Color(0xFF6A5435), amberBg = Color(0xFF302616),
        track = Color(0xFF3D2B22), cream = Color(0xFFFBF1DF), softPaper = Color(0xFF32251D),
        selectedBg = Color(0xFF35241D), answeredBg = Color(0xFF30231B), chipBg = Color(0xFF33261E),
        inkSoft = Color(0xFFD2A06C), number = Color(0xFF5E4736),
        hero1 = Color(0xFF3A1712), hero2 = Color(0xFF7A3428), hero3 = Color(0xFFA8533D),
        glow1 = Color(0xFFC18A4A), glow2 = Color(0xFF5FA0AC)
    )

    val MossDark = BookQColors(
        isDark = true,
        ink = Color(0xFFEEF2E4), ink2 = Color(0xFFC4D1C5), muted = Color(0xFF8B9D8F),
        paper = Color(0xFF101A14), card = Color(0xFF18271E), line = Color(0xFF2D4234), line2 = Color(0xFF23362A),
        primary = Color(0xFF5F9C74), primary2 = Color(0xFF7DBB8E), accent = Color(0xFFC99655), gold = Color(0xFFC6A269),
        green = Color(0xFF6FB68E), greenBg = Color(0xFF18362A), red = Color(0xFFD67C6C), redBg = Color(0xFF3B2422),
        amber = Color(0xFFC6A269), amberLine = Color(0xFF5F4E34), amberBg = Color(0xFF2C2618),
        track = Color(0xFF2A3D30), cream = Color(0xFFF4ECD9), softPaper = Color(0xFF1D2D23),
        selectedBg = Color(0xFF233A2B), answeredBg = Color(0xFF213527), chipBg = Color(0xFF223328),
        inkSoft = Color(0xFFC8AA70), number = Color(0xFF455848),
        hero1 = Color(0xFF0C1A13), hero2 = Color(0xFF1E3B2A), hero3 = Color(0xFF315D43),
        glow1 = Color(0xFFC6A269), glow2 = Color(0xFF5F9C74)
    )

    fun of(theme: AppTheme, appearance: AppAppearance): BookQColors = when (theme) {
        AppTheme.INDIGO -> if (appearance == AppAppearance.DARK) IndigoDark else IndigoLight
        AppTheme.RUST -> if (appearance == AppAppearance.DARK) RustDark else RustLight
        AppTheme.MOSS -> if (appearance == AppAppearance.DARK) MossDark else MossLight
    }
}

object Palette {
    private var active by mutableStateOf(BookQPalettes.IndigoLight)

    val Current: BookQColors get() = active
    val IsDark: Boolean get() = active.isDark

    fun apply(theme: AppTheme, appearance: AppAppearance) {
        active = BookQPalettes.of(theme, appearance)
    }

    val Ink: Color get() = active.ink
    val Ink2: Color get() = active.ink2
    val Muted: Color get() = active.muted
    val Paper: Color get() = active.paper
    val Card: Color get() = active.card
    val Line: Color get() = active.line
    val Line2: Color get() = active.line2
    val Primary: Color get() = active.primary
    val Primary2: Color get() = active.primary2
    val Accent: Color get() = active.accent
    val Gold: Color get() = active.gold
    val Green: Color get() = active.green
    val GreenBg: Color get() = active.greenBg
    val Red: Color get() = active.red
    val RedBg: Color get() = active.redBg
    val Amber: Color get() = active.amber
    val AmberLine: Color get() = active.amberLine
    val AmberBg: Color get() = active.amberBg
    val Track: Color get() = active.track
    val Cream: Color get() = active.cream
    val SoftPaper: Color get() = active.softPaper
    val SelectedBg: Color get() = active.selectedBg
    val AnsweredBg: Color get() = active.answeredBg
    val ChipBg: Color get() = active.chipBg
    val InkSoft: Color get() = active.inkSoft
    val Number: Color get() = active.number
}

fun serif(size: Int, weight: FontWeight = FontWeight.Bold): TextStyle =
    TextStyle(fontFamily = FontFamily.Serif, fontSize = size.sp, fontWeight = weight, color = Palette.Ink)

fun sans(size: Int, weight: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size.sp, fontWeight = weight, color = Palette.Ink)

@Composable
fun BookQTheme(content: @Composable () -> Unit) {
    val colors = Palette.Current
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.card,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.card,
            outline = colors.line
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.card,
            outline = colors.line
        )
    }
    MaterialTheme(colorScheme = scheme) {
        content()
    }
}

@Composable
fun PaperBackground() {
    Box(Modifier.fillMaxSize().background(Palette.Paper)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Palette.Current.glow1.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 720.dp.toPx()
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Palette.Current.glow2.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = 620.dp.toPx()
                )
            )
            val step = 4.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(Palette.Ink.copy(alpha = if (Palette.IsDark) 0.014f else 0.022f), Offset(x, 0f), Offset(x, size.height), 0.5f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Palette.Ink.copy(alpha = if (Palette.IsDark) 0.014f else 0.022f), Offset(0f, y), Offset(size.width, y), 0.5f)
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
                    colors = listOf(Palette.Current.hero1, Palette.Current.hero2, Palette.Current.hero3),
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
                .background(if (Palette.IsDark) Palette.Current.card.copy(alpha = 0.96f) else Color(0xEF17242F), CircleShape)
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
