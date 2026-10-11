package com.pocketwin.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val vm: VoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { VoiceScreen(vm) } } }
    }
}

@Composable
private fun VoiceScreen(vm: VoiceViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening()
    }
    fun onMic() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.startListening() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    var typed by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text("PocketVoice", style = MaterialTheme.typography.headlineSmall)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (vm.messages.isEmpty()) {
                item { Text("Tap the mic and say \"help\".", Modifier.padding(top = 16.dp)) }
            }
            items(vm.messages) { m ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.fromUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ) { Text(m.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                }
            }
        }
        if (vm.partial.isNotEmpty()) Text(vm.partial, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                typed, { typed = it }, Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Or type a command") },
            )
            TextButton(onClick = { if (typed.isNotBlank()) { vm.submit(typed); typed = "" } }) { Text("Send") }
            FilledIconButton(onClick = ::onMic, Modifier.size(56.dp)) {
                Icon(if (vm.listening) Icons.Filled.Mic else Icons.Filled.MicNone, "Speak")
            }
        }
    }
}
