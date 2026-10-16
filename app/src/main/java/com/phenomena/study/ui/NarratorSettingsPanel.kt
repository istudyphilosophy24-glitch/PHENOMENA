package com.phenomena.study.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.player.StudyPlayer

/**
 * SOPHIA-inspired in-app Android speech engine and voice picker.
 * Android speech services appear here; ordinary reading apps do not.
 * No sharing, exporting, external intents, or cloud credentials are required.
 */
@Composable
fun NarratorSettingsPanel(player: StudyPlayer) {
    var showEngines by remember { mutableStateOf(false) }
    var showVoices by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("INSTALLED ANDROID SPEECH ENGINE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showEngines = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    player.engines.firstOrNull { it.id == player.engineId }?.label ?: "Android system default",
                    modifier = Modifier.weight(1f), maxLines = 2
                )
                Text(" ▾")
            }
            DropdownMenu(
                expanded = showEngines, onDismissRequest = { showEngines = false },
                modifier = Modifier.heightIn(max = 340.dp)
            ) {
                player.engines.forEach { engine ->
                    DropdownMenuItem(
                        text = { Text(engine.label, fontSize = 12.sp) },
                        onClick = { showEngines = false; player.chooseEngine(engine.id) }
                    )
                }
            }
        }

        Text("VOICE FROM SELECTED ENGINE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showVoices = true },
                enabled = player.ready && player.voices.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    player.voices.firstOrNull { it.id == player.voiceId }?.label ?: "Engine default voice",
                    modifier = Modifier.weight(1f), maxLines = 2
                )
                Text(" ▾")
            }
            DropdownMenu(
                expanded = showVoices, onDismissRequest = { showVoices = false },
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Use engine default", fontSize = 12.sp) },
                    onClick = { showVoices = false; player.chooseVoice("") }
                )
                player.voices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.label, fontSize = 12.sp) },
                        onClick = { showVoices = false; player.chooseVoice(voice.id) }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { player.previewVoice() }, enabled = player.ready) {
                Text("▶ Test voice", fontSize = 12.sp)
            }
            TextButton(onClick = { player.refreshEngines() }) {
                Text("Refresh engines", fontSize = 12.sp)
            }
        }

        Text(
            "Your engine and voice are saved and used for lessons and seminars. " +
            "ElevenReader itself does not currently register as an Android speech engine; " +
            "it cannot supply in-app voices through this selector.",
            fontSize = 11.sp, lineHeight = 17.sp, color = Color.Gray
        )
    }
}
