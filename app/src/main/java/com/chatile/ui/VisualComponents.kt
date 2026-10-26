package com.chatile.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chatile.ui.theme.NightBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.PinkAccent
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import com.chatile.ui.theme.VioletDeep

@Composable
fun NightBackdrop(content: @Composable BoxScope.() -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), background, background.copy(alpha = 0.94f)),
                ),
            ),
        content = content,
    )
}

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    val motionEnabled = remember(context) { mutableStateOf(readMotionEnabled(context)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                motionEnabled.value = readMotionEnabled(context)
            }
        }
        val setting = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(setting, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return motionEnabled.value
}

private fun readMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) > 0f

@Composable
fun CoinOrb(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    description: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE6A6), Color(0xFFF0BC52), Color(0xFF9C6B20)),
                ),
            )
            .border(1.dp, Color(0xFFFFE7A8), CircleShape)
            .then(
                if (description == null) Modifier.clearAndSetSemantics { }
                else Modifier.semantics { contentDescription = description },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = size * 0.22f, top = size * 0.15f)
                .size(size * 0.23f)
                .background(Color(0xFFFFF6D8).copy(alpha = 0.88f), CircleShape),
        )
    }
}

@Composable
fun CharacterAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(VioletDeep, VioletAccent, PinkAccent)),
            )
            .border(1.dp, Color(0xFFC1EBDB).copy(alpha = 0.8f), CircleShape)
            .semantics { contentDescription = "Avatar de $name" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "✦",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
fun IconAction(
    symbol: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (emphasized) MaterialTheme.colorScheme.primary else NightSurfaceRaised)
            .border(1.dp, if (emphasized) MaterialTheme.colorScheme.primary else SubtleBorder, CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimary else TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    emphasized: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else NightSurface)
            .border(
                BorderStroke(1.dp, if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f) else SubtleBorder),
                RoundedCornerShape(18.dp),
            )
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = if (emphasized) MaterialTheme.colorScheme.primary else TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun SectionEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier,
    )
}

@Composable
fun QuietSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(NightSurface)
            .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp)),
        content = content,
    )
}
