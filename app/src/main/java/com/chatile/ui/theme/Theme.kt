package com.chatile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Palette(val primary: Color, val secondary: Color, val background: Color, val surface: Color, val raised: Color)

@Composable
fun PiecesTheme(theme: String = "violet", content: @Composable () -> Unit) {
    val palette = remember(theme) {
        when (theme) {
            "rose" -> Palette(Color(0xFFF28672), Color(0xFF5DCBC3), Color(0xFF171A18), Color(0xFF202724), Color(0xFF2C3530))
            "blue" -> Palette(Color(0xFF52D6C7), Color(0xFFFFC857), Color(0xFF0B171B), Color(0xFF142428), Color(0xFF20363A))
            "gold" -> Palette(Color(0xFFE8B85B), Color(0xFF58C9B7), Color(0xFF171916), Color(0xFF222823), Color(0xFF303830))
            "emerald" -> Palette(Color(0xFF62D6A5), Color(0xFFE8B85B), Color(0xFF0B1712), Color(0xFF14251D), Color(0xFF20382C))
            "amethyst" -> Palette(Color(0xFF65D6CF), Color(0xFFE8B85B), Color(0xFF141A19), Color(0xFF202927), Color(0xFF2D3734))
            "ruby" -> Palette(Color(0xFFFF6E78), Color(0xFFE8B85B), Color(0xFF1B0D10), Color(0xFF2C171B), Color(0xFF3D2227))
            "obsidian" -> Palette(Color(0xFFB8C0C6), Color(0xFF62D6A5), Color(0xFF101315), Color(0xFF1B2022), Color(0xFF292F32))
            "ivory" -> Palette(Color(0xFFE7E8D8), Color(0xFF68C5B9), Color(0xFF171A18), Color(0xFF252B27), Color(0xFF343C37))
            else -> Palette(VioletAccent, PinkAccent, NightBackground, NightSurface, NightSurfaceRaised)
        }
    }
    val scheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.background,
        primaryContainer = palette.raised,
        onPrimaryContainer = TextPrimary,
        secondary = palette.secondary,
        onSecondary = palette.background,
        secondaryContainer = palette.raised,
        onSecondaryContainer = TextPrimary,
        background = palette.background,
        onBackground = TextPrimary,
        surface = palette.surface,
        onSurface = TextPrimary,
        surfaceVariant = palette.raised,
        onSurfaceVariant = TextSecondary,
        outline = SubtleBorder,
        outlineVariant = SubtleBorder,
        error = ErrorAccent,
        onError = NightBackground,
        scrim = Color(0xCC050908),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = PiecesTypography,
        content = content,
    )
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
                contentDescription = "Badges Officiel",
                tint = Color(0xFFFFD700), // Gold
                modifier = modifier.size(iconSize)
            )
        }
        "admin", "moderator" -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color(0xFF1DA1F2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
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
                tint = Color(0xFFFFD700), // Gold
                modifier = modifier.size(iconSize)
            )
        }
        "premium" -> {
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(VioletAccent, RoundedCornerShape(2.dp))
            )
        }
        "creator" -> {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_edit),
                contentDescription = "Plume",
                tint = PinkAccent,
                modifier = modifier.size(iconSize)
            )
        }
        else -> {
            // Default badge
            Box(
                modifier = modifier
                    .size(iconSize)
                    .background(SubtleBorder, RoundedCornerShape(2.dp))
            )
        }
    }
}
