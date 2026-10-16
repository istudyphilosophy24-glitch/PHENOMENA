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

val Paper = Color(0xFFF7F4ED)
val Ink = Color(0xFF192923)
val Moss = Color(0xFF284B43)
val Brass = Color(0xFFB78A4C)
val Muted = Color(0xFF68736B)
val Cream = Color(0xFFEEE9DD)
val Line = Color(0xFFDCD7CB)

@Composable
fun PhenomenaTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(primary = Moss, onPrimary = Color.White, background = Paper, onBackground = Ink,
        surface = Paper, onSurface = Ink, surfaceContainer = Cream, outline = Line, secondary = Brass)
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp, color = Brass, lineHeight = 15.sp)
}
@Composable
fun Display(text: String, modifier: Modifier = Modifier, size: Int = 30, color: Color = Ink) {
    Text(text, modifier, fontFamily = FontFamily.Serif, fontSize = size.sp, lineHeight = (size + 5).sp,
        fontWeight = FontWeight.Normal, color = color)
}
@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Text(text, modifier, fontSize = 14.sp, lineHeight = 23.sp, color = color)
}
@Composable
fun Rule() { HorizontalDivider(color = Line, thickness = 1.dp) }
@Composable
fun LabelTag(label: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(modifier = if (onClick == null) Modifier else Modifier.clickable { onClick() },
        color = if (selected) Moss else Cream, contentColor = if (selected) Color.White else Moss,
        shape = RoundedCornerShape(50.dp)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Moss),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) { Text(text) }
}
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = Color.White.copy(alpha = 0.76f), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable
fun RowHeadline(title: String, trailing: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontFamily = FontFamily.Serif, fontSize = 23.sp, color = Ink)
        if (trailing.isNotBlank()) Text(trailing, fontSize = 11.sp, color = Muted)
    }
}
@Composable
fun AccentTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
}
@Composable
fun EmptyNotice(text: String) {
    Panel(Modifier.fillMaxWidth()) { Icon(Icons.Default.Info, null, tint = Brass); Body(text, color = Muted) }
}
