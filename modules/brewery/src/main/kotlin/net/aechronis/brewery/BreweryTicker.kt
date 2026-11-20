package net.aechronis.brewery

import net.aechronis.server.modules.ModuleScheduler
import net.minestom.server.timer.TaskSchedule
import java.util.concurrent.CopyOnWriteArrayList

/** One 20-tick loop for the whole module. Callbacks get wall-clock millis and must skip unloaded chunks. */
object BreweryTicker {
    private val callbacks = CopyOnWriteArrayList<(Long) -> Unit>()

    fun register(callback: (now: Long) -> Unit) {
        callbacks += callback
    }

    fun start() {
        ModuleScheduler
            .buildTask {
                val now = System.currentTimeMillis()
                callbacks.forEach { callback -> runCatching { callback(now) }.onFailure(Throwable::printStackTrace) }
            }.repeat(TaskSchedule.tick(20))
            .schedule()
    }
}
