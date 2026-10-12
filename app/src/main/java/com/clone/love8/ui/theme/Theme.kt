package com.clone.love8.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LovePink = Color(0xFFFF5C8A)
val LovePinkDark = Color(0xFFE0436F)
val LovePinkLight = Color(0xFFFFD6E2)
val LoveCream = Color(0xFFFFF6F8)
val LovePurple = Color(0xFF9C6ADE)
val LoveText = Color(0xFF3D1A26)
val LoveSubText = Color(0xFF8A6470)

val LoveGradient = Brush.linearGradient(listOf(Color(0xFFFF9EBB), Color(0xFFFF5C8A)))
val SoftGradient = Brush.verticalGradient(listOf(Color(0xFFFFE3EC), LoveCream))
val SkyGradient = Brush.verticalGradient(listOf(Color(0xFFDDEBFF), Color(0xFFFFE9F1)))
val PremiumGradient = Brush.linearGradient(listOf(Color(0xFFFFC371), Color(0xFFFF5F6D)))

private val LoveColors = lightColorScheme(
    primary = LovePink,
    onPrimary = Color.White,
    primaryContainer = LovePinkLight,
    onPrimaryContainer = Color(0xFF5A0F25),
    secondary = LovePurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBDDFF),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFFFF9F43),
    background = LoveCream,
    onBackground = LoveText,
    surface = Color.White,
    onSurface = LoveText,
    surfaceVariant = Color(0xFFFCEFF3),
    onSurfaceVariant = LoveSubText,
    outline = Color(0xFFE8C5D0),
)

@Composable
fun Love8Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LoveColors, typography = Typography(), content = content)
}
