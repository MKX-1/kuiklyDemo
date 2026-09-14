package com.example.aistock.adapter

import com.tencent.kuikly.core.render.android.adapter.IKRThreadAdapter
import java.util.concurrent.Executors

/**
 * 线程适配器：把 Kuikly 的子线程任务交给宿主线程池执行。
 */
class KRThreadAdapter : IKRThreadAdapter {

    override fun executeOnSubThread(task: () -> Unit) {
        subThreadPoolExecutor.execute(task)
    }

    /**
     * Compose 场景建议返回 8MB，避免深层布局递归导致 StackOverflow。
     */
    override fun stackSize(): Long = 8L * 1024 * 1024
}

private val subThreadPoolExecutor by lazy {
    Executors.newFixedThreadPool(2)
}
