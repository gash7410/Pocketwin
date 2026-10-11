package com.pocketwin.voice

import android.content.Context
import android.os.BatteryManager
import java.time.LocalDateTime

class AndroidEnvironment(private val context: Context) : Environment {
    override fun now(): LocalDateTime = LocalDateTime.now()

    override fun batteryPercent(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
    }
}
