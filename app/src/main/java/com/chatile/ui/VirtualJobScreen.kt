package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun VirtualJobScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val shopData by vm.virtualShop.collectAsState()
    val summary by vm.virtualSummary.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadVirtualShop()
        vm.loadVirtualSummary()
    }

    val jobsArray = shopData?.optJSONArray("jobs") ?: JSONArray()
    val currentJob = summary?.optString("job") ?: "Sans emploi"

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("Carrière & Métiers", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💼 Emploi actuel", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(currentJob, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Text("Offres d’emploi disponibles", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(jobsArray.let { arr ->
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    list
                }, key = { it.getLong("id") }) { job ->
                    val title = job.getString("title")
                    val salary = job.optInt("salaryPieces", 10)
                    val reqLevel = job.optInt("requiredLevel", 1)
                    val desc = job.optString("description", "")
                    val isCurrent = title == currentJob

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) NightSurfaceRaised else NightSurface),
                        modifier = Modifier.fillMaxWidth().border(1.dp, if (isCurrent) VioletAccent else SubtleBorder, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text("Salaire : 💰 $salary Pièces / session · Niveau requis : $reqLevel", color = VioletAccent, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { vm.applyJob(title) { } },
                                enabled = !isCurrent,
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                            ) {
                                Text(if (isCurrent) "Actuel" else "Postuler")
                            }
                        }
                    }
                }
            }
        }
    }
}
