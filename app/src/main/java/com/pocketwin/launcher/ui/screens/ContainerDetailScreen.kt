package com.pocketwin.launcher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.pocketwin.launcher.data.InstalledShortcut
import com.pocketwin.launcher.engine.ComponentCatalog
import com.pocketwin.launcher.engine.ComponentInstallState
import com.pocketwin.launcher.engine.ComponentKind
import com.pocketwin.launcher.engine.EngineComponent
import com.pocketwin.launcher.ui.PocketWinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(
    viewModel: PocketWinViewModel,
    containerId: String?,
    onBack: () -> Unit,
) {
    val containers by viewModel.containers.collectAsState()
    val container = containers.find { it.id == containerId }
    val componentStates by viewModel.componentStates.collectAsState()
    val runError by viewModel.runError.collectAsState()
    val context = LocalContext.current

    val rootfs = ComponentCatalog.bundled.find { it.kind == ComponentKind.ROOTFS }
    val wineBuild = ComponentCatalog.bundled.find { it.kind == ComponentKind.WINE_BUILD }

    val pickExeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && container != null) {
            val displayName = DocumentFile.fromSingleUri(context, uri)?.name ?: "app.exe"
            viewModel.importExecutable(container, uri, displayName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(container?.name ?: "Container") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (container == null) {
            Text("Container not found.", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Engine components", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (rootfs == null || wineBuild == null) {
                    Text(
                        "No engine components are configured (ComponentCatalog.bundled is empty). " +
                            "See README.md → \"Sourcing engine components\" to point this build at a rootfs and Wine build.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    EngineComponentRow(rootfs, componentStates[rootfs.id], onInstall = { viewModel.installComponent(rootfs) })
                    Spacer(Modifier.height(4.dp))
                    EngineComponentRow(wineBuild, componentStates[wineBuild.id], onInstall = { viewModel.installComponent(wineBuild) })
                }

                Spacer(Modifier.height(24.dp))
                Text("Apps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { pickExeLauncher.launch(arrayOf("*/*")) }) {
                    Text("Add executable…")
                }
                Spacer(Modifier.height(8.dp))

                if (runError != null) {
                    Text(
                        "Run failed: $runError",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Full output (if the process did start) is in this container's engine.log.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(container.shortcuts, key = { it.relativeExePath }) { shortcut ->
                        ShortcutRow(
                            shortcut = shortcut,
                            canRun = rootfs != null && wineBuild != null &&
                                viewModel.isComponentInstalled(rootfs) && viewModel.isComponentInstalled(wineBuild),
                            onRun = {
                                if (rootfs != null && wineBuild != null) {
                                    viewModel.run(container, rootfs, wineBuild, shortcut)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineComponentRow(component: EngineComponent, state: ComponentInstallState?, onInstall: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${component.displayName} (${component.version})", style = MaterialTheme.typography.bodyLarge)
            when (state) {
                is ComponentInstallState.Downloading -> {
                    val fraction = if (state.totalBytes > 0) state.bytesRead.toFloat() / state.totalBytes else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                }
                ComponentInstallState.Extracting -> Text("Extracting…", style = MaterialTheme.typography.bodyMedium)
                ComponentInstallState.Installed -> Text("Installed", style = MaterialTheme.typography.bodyMedium)
                is ComponentInstallState.Failed -> Text("Failed: ${state.message}", style = MaterialTheme.typography.bodyMedium)
                ComponentInstallState.NotInstalled, null -> Button(onClick = onInstall) { Text("Install") }
            }
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: InstalledShortcut, canRun: Boolean, onRun: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Text(shortcut.displayName, modifier = Modifier.padding(top = 10.dp))
            Button(onClick = onRun, enabled = canRun, modifier = Modifier.padding(start = 12.dp)) {
                Text("Run")
            }
        }
    }
}
