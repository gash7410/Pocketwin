package com.pocketwin.launcher

import android.app.Application
import com.pocketwin.launcher.data.ContainerRepository
import com.pocketwin.launcher.engine.ComponentManager

class PocketWinApp : Application() {

    lateinit var containerRepository: ContainerRepository
        private set

    lateinit var componentManager: ComponentManager
        private set

    override fun onCreate() {
        super.onCreate()
        containerRepository = ContainerRepository(this)
        componentManager = ComponentManager(this)
    }
}
