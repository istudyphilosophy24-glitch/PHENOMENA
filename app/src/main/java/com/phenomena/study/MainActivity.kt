package com.phenomena.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phenomena.study.data.*
import com.phenomena.study.player.StudyPlayer
import com.phenomena.study.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhenomenaTheme {
                val context = LocalContext.current
                val catalog = remember { Catalog(context.applicationContext) }
                val dao = remember { StudyDatabase.get(context.applicationContext).dao() }
                val player = remember { StudyPlayer(context.applicationContext) }
                DisposableEffect(player) { onDispose { player.release() } }
                val notes by dao.notes().collectAsStateWithLifecycle(initialValue = emptyList())
                val progress by dao.progress().collectAsStateWithLifecycle(initialValue = emptyList())
                val imports by dao.imports().collectAsStateWithLifecycle(initialValue = emptyList())
                PhenomenaShell(catalog, dao, player, notes, progress, imports)
            }
        }
    }
}
