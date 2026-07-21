package com.pocketwin.launcher.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pocketwin.launcher.PocketWinApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs [ComponentManager.install] in the foreground so a multi-hundred-MB rootfs/Wine
 * download survives the app being backgrounded, with progress visible in the notification
 * shade rather than only inside the activity.
 */
class ComponentDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var componentManager: ComponentManager

    override fun onCreate() {
        super.onCreate()
        componentManager = (application as PocketWinApp).componentManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val componentId = intent?.getStringExtra(EXTRA_COMPONENT_ID)
        val component = intent?.let(::componentFromIntent)
        startForeground(NOTIFICATION_ID, buildNotification("Preparing $componentId…", indeterminate = true))

        if (component == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            runCatching { componentManager.install(component) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun buildNotification(text: String, indeterminate: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketWin engine setup")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, indeterminate)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Engine downloads", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "engine_downloads"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_COMPONENT_ID = "component_id"

        // Intent extras are flattened EngineComponent fields rather than a Parcelable/Serializable
        // payload so this stays trivial to construct from Compose call sites.
        private const val EXTRA_KIND = "component_kind"
        private const val EXTRA_DISPLAY_NAME = "component_display_name"
        private const val EXTRA_VERSION = "component_version"
        private const val EXTRA_TARGET_ABI = "component_target_abi"
        private const val EXTRA_DOWNLOAD_URL = "component_download_url"
        private const val EXTRA_SHA256 = "component_sha256"
        private const val EXTRA_SIZE_BYTES = "component_size_bytes"

        fun buildIntent(context: android.content.Context, component: EngineComponent) =
            Intent(context, ComponentDownloadService::class.java).apply {
                putExtra(EXTRA_COMPONENT_ID, component.id)
                putExtra(EXTRA_KIND, component.kind.name)
                putExtra(EXTRA_DISPLAY_NAME, component.displayName)
                putExtra(EXTRA_VERSION, component.version)
                putExtra(EXTRA_TARGET_ABI, component.targetAbi)
                putExtra(EXTRA_DOWNLOAD_URL, component.downloadUrl)
                putExtra(EXTRA_SHA256, component.sha256)
                putExtra(EXTRA_SIZE_BYTES, component.sizeBytes)
            }

        private fun componentFromIntent(intent: Intent): EngineComponent? {
            val id = intent.getStringExtra(EXTRA_COMPONENT_ID) ?: return null
            val kind = intent.getStringExtra(EXTRA_KIND)?.let { ComponentKind.valueOf(it) } ?: return null
            return EngineComponent(
                id = id,
                kind = kind,
                displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: id,
                version = intent.getStringExtra(EXTRA_VERSION) ?: "unknown",
                targetAbi = intent.getStringExtra(EXTRA_TARGET_ABI) ?: "arm64-v8a",
                downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return null,
                sha256 = intent.getStringExtra(EXTRA_SHA256) ?: return null,
                sizeBytes = intent.getLongExtra(EXTRA_SIZE_BYTES, 0L),
            )
        }
    }
}
