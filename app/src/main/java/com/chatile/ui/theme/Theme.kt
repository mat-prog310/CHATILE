package com.chatile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChatileColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = ChatileSurface,
    onPrimaryContainer = ChatileTextPrimary,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = ChatileSurfaceVariant,
    onSecondaryContainer = ChatileTextPrimary,
    background = ChatileBackground,
    onBackground = ChatileTextPrimary,
    surface = ChatileSurface,
    onSurface = ChatileTextPrimary,
    surfaceVariant = ChatileSurfaceVariant,
    onSurfaceVariant = ChatileTextSecondary,
    outline = ChatileBorder,
    outlineVariant = ChatileBorder,
    error = ChatileError,
    onError = Color.White,
    scrim = Color(0xCC050908),
)

@Composable
fun PiecesTheme(theme: String = "noir_pur", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChatileColorScheme,
        typography = PiecesTypography,
        content = content,
    )
}

@Composable
fun ChatileScreenBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0F2E), ChatileBackground),
                    radius = 1200f
                )
            )
    ) {
        content()
    }
}

@Composable
fun BadgeIcon(
    badgeType: String,
    modifier: Modifier = Modifier
) {
    val iconSize = 14.dp
    
    when (badgeType) {
        "official" -> {
            Icon(
                painter = painterResource(id = android.R.drawable.star_on),
                contentDescription = "Badge Officiel",
                tint = ChatileAccent,
                modifier = modifier.size(iconSize)
            )
        }
        "admin", "moderator" -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        "premium_plus" -> {
            Icon(
                painter = painterResource(id = android.R.drawable.star_on),
                contentDescription = "Couronne",
                tint = Color.White,
                modifier = modifier.size(iconSize)
            )
        }
        "premium" -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
        "creator" -> {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_edit),
                contentDescription = "Plume",
                tint = Color.White,
                modifier = modifier.size(iconSize)
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(ChatileBorder, RoundedCornerShape(2.dp))
            )
        }
    }
}
