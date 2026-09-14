package com.example.aistock

import android.app.Application

/**
 * 宿主 Application：持有全局 Context，供图片加载等适配器使用。
 */
class KRApplication : Application() {

    init {
        application = this
    }

    companion object {
        lateinit var application: Application
            private set
    }
}
