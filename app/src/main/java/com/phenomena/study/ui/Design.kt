package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// PHENOMENA 1.3: nocturne editorial design system.
// Raised surfaces deliberately remain dark; labels and affordances retain
// their contrast even when Android is configured for light mode.
val Paper = Color(0xFF0B1217)
val CardSurface = Color(0xFF152128)
val CardRaised = Color(0xFF1D2B31)
val Ink = Color(0xFFEFF4F1)
val Moss = Color(0xFF326F63)
val Brass = Color(0xFFE6BB81)
val Muted = Color(0xFFA0B0AF)
val Cream = Color(0xFF203037)
val Line = Color(0xFF2B4047)
val Mint = Color(0xFF9AD5B9)

@Composable
fun PhenomenaTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Mint, onPrimary = Paper,
        secondary = Brass, onSecondary = Paper,
        background = Paper, onBackground = Ink,
        surface = CardSurface, onSurface = Ink,
        surfaceVariant = Cream, onSurfaceVariant = Muted,
        surfaceContainer = CardSurface,
        outline = Line, outlineVariant = Line,
        primaryContainer = Moss, onPrimaryContainer = Ink
    )
    val type = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
        titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 23.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        labelLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold)
    )
    MaterialTheme(colorScheme = scheme, typography = type, shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(26.dp)
    ), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp, color = Brass, lineHeight = 15.sp)
}
@Composable
fun Display(text: String, modifier: Modifier = Modifier, size: Int = 30, color: Color = Ink) {
    Text(text, modifier, fontFamily = FontFamily.Serif, fontSize = size.sp, lineHeight = (size + 6).sp,
        fontWeight = FontWeight.Normal, color = color)
}
@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Text(text, modifier, fontSize = 14.sp, lineHeight = 24.sp, color = color)
}
@Composable
fun Rule() { HorizontalDivider(color = Line, thickness = 1.dp) }
@Composable
fun LabelTag(label: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(modifier = if (onClick == null) Modifier else Modifier.clickable { onClick() },
        color = if (selected) Moss else Cream, contentColor = Ink,
        border = BorderStroke(1.dp, if (selected) Mint.copy(alpha = .5f) else Line),
        shape = RoundedCornerShape(50.dp)) {
        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = if (selected) Color.White else Muted,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier, shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Paper),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)) { Text(text) }
}
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = CardSurface, shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
fun RowHeadline(title: String, trailing: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontFamily = FontFamily.Serif, fontSize = 24.sp, color = Ink)
        if (trailing.isNotBlank()) Text(trailing, fontSize = 10.sp, color = Muted, letterSpacing = 1.sp)
    }
}
@Composable
fun AccentTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink, lineHeight = 23.sp)
}
@Composable
fun EmptyNotice(text: String) {
    Panel(Modifier.fillMaxWidth()) { Icon(Icons.Default.Info, null, tint = Brass); Body(text, color = Muted) }
}
