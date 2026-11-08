package com.chatile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.VioletAccent

@Composable
fun VirtualItemThumbnail(
    item: VirtualItem,
    modifier: Modifier = Modifier
) {
    val isRareOrEpic = item.rarity in listOf("rare", "epique", "legendaire")
    val borderColor = when (item.rarity) {
        "legendaire" -> Color(0xFFFFD700)
        "epique" -> Color(0xFF9C27B0)
        "rare" -> Color(0xFF2196F3)
        else -> Color(0xFF262626)
    }

    Box(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NightSurface)
            .border(width = if (isRareOrEpic) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2A1B4E), Color(0xFF140B24)),
                            start = Offset(0f, 0f),
                            end = Offset(90f, 90f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    when (item.category) {
                        "house" -> {
                            val path = Path().apply {
                                moveTo(size.width / 2f, 5f)
                                lineTo(size.width - 5f, size.height / 2f)
                                lineTo(size.width - 15f, size.height / 2f)
                                lineTo(size.width - 15f, size.height - 5f)
                                lineTo(15f, size.height - 5f)
                                lineTo(15f, size.height / 2f)
                                lineTo(5f, size.height / 2f)
                                close()
                            }
                            drawPath(path, color = Color(0xFFB47CFF))
                            drawRect(
                                color = Color(0xFFFFD1A9),
                                topLeft = Offset(size.width / 2f - 10f, size.height / 2f + 5f),
                                size = androidx.compose.ui.geometry.Size(20f, 25f)
                            )
                        }
                        "clothing" -> {
                            val path = Path().apply {
                                moveTo(size.width * 0.3f, 10f)
                                quadraticBezierTo(size.width * 0.5f, 20f, size.width * 0.7f, 10f)
                                lineTo(size.width * 0.9f, 25f)
                                lineTo(size.width * 0.75f, 35f)
                                lineTo(size.width * 0.7f, size.height - 5f)
                                lineTo(size.width * 0.3f, size.height - 5f)
                                lineTo(size.width * 0.25f, 35f)
                                lineTo(size.width * 0.1f, 25f)
                                close()
                            }
                            drawPath(path, color = Color(0xFF00E5FF))
                        }
                        "vehicle" -> {
                            drawRoundRect(
                                color = Color(0xFFFF5252),
                                topLeft = Offset(5f, size.height * 0.4f),
                                size = androidx.compose.ui.geometry.Size(size.width - 10f, size.height * 0.4f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                            drawCircle(color = Color.DarkGray, radius = 10f, center = Offset(size.width * 0.3f, size.height * 0.8f))
                            drawCircle(color = Color.DarkGray, radius = 10f, center = Offset(size.width * 0.7f, size.height * 0.8f))
                        }
                        "pet" -> {
                            drawCircle(color = Color(0xFFFFB74D), radius = 22f, center = Offset(size.width / 2f, size.height / 2f + 5f))
                            drawCircle(color = Color(0xFFF57C00), radius = 10f, center = Offset(size.width / 2f - 18f, size.height / 2f - 12f))
                            drawCircle(color = Color(0xFFF57C00), radius = 10f, center = Offset(size.width / 2f + 18f, size.height / 2f - 12f))
                        }
                        else -> {
                            drawCircle(color = VioletAccent, radius = 20f)
                        }
                    }
                }
            }

            Text(
                text = item.name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🪙", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${item.price}",
                    color = VioletAccent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
