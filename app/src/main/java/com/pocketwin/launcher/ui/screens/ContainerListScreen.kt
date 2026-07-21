package com.pocketwin.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketwin.launcher.data.Container
import com.pocketwin.launcher.data.ContainerArchitecture
import com.pocketwin.launcher.ui.PocketWinViewModel

@Composable
fun ContainerListScreen(
    viewModel: PocketWinViewModel,
    onOpenContainer: (Container) -> Unit,
) {
    val containers by viewModel.containers.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PocketWin") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New container")
            }
        },
    ) { padding ->
        if (containers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            ) {
                Text("No containers yet. Tap + to create your first Windows environment.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(containers, key = { it.id }) { container ->
                    ContainerRow(container = container, onClick = { onOpenContainer(container) })
                }
            }
        }
    }

    if (showCreateDialog) {
        NewContainerDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, arch ->
                viewModel.createContainer(name, arch)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun ContainerRow(container: Container, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(container.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${container.architecture.name} · ${container.shortcuts.size} app(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Filled.Computer, contentDescription = null)
        }
    }
}

@Composable
private fun NewContainerDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ContainerArchitecture) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var architecture by remember { mutableStateOf(ContainerArchitecture.WIN64) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New container") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Row {
                    RadioButton(
                        selected = architecture == ContainerArchitecture.WIN64,
                        onClick = { architecture = ContainerArchitecture.WIN64 },
                    )
                    Text("64-bit (Box64)", modifier = Modifier.padding(top = 12.dp))
                }
                Row {
                    RadioButton(
                        selected = architecture == ContainerArchitecture.WIN32,
                        onClick = { architecture = ContainerArchitecture.WIN32 },
                    )
                    Text("32-bit (Box86)", modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, architecture) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
