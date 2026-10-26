package com.chatile.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chatile.ApiClient
import com.chatile.MainActivity

class ChatileWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ApiClient.init(context)
        val data = runCatching { ApiClient.getWidgetData() }.getOrNull()
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF19201D))).padding(14.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                Text("CHATILE", style = TextStyle(color = ColorProvider(Color(0xFF57D6BB))))
                Text("${data?.optInt("pieces", 0) ?: 0} Pièces")
                Text("${data?.optJSONObject("characterOfTheDay")?.optString("name") ?: "Personnage du jour"}")
                androidx.glance.Button(
                    text = if (data?.optBoolean("dailyCanClaim") == true) "Récupérer" else "Ouvrir CHATILE",
                    onClick = actionStartActivity(Intent(context, MainActivity::class.java)),
                )
            }
        }
    }
}

class ChatileWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChatileWidget()
}
