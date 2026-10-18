package com.acuranza.app

import android.app.Application
import com.acuranza.app.data.DefaultCategories
import com.acuranza.app.di.AppContainer
import com.acuranza.app.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AcuranzaApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Repositories and services; see [AppContainer]. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Channels need only the context: building the container here would open the database on the main thread.
        com.acuranza.app.notify.Notifications(this).createChannels()
        appScope.launch {
            com.acuranza.app.repo.PaydayLearner.start(appScope, container.db, container.settings)
            // Seeding with IGNORE adds categories introduced by an update without touching existing ones.
            container.db.categories().insertAll(DefaultCategories.all)
            runCatching {
                com.acuranza.app.repo.DataRepairs.run(container.db, container.settings, container.netWorth)
            }
            runCatching { com.acuranza.app.repo.AutoCategoriser.run(container.db) }
            runCatching { container.settings.clearLegacyGmail() }
        }
        // No SMS catch-up here: this also runs when an SMS wakes the app, and a silent catch-up could import that
        // message before the receiver, which would then skip its alert. Opening the app catches up (MainActivity).
        WorkScheduler.schedule(this)
        com.acuranza.app.tools.WidgetUpdater.start(this)
        appScope.launch { runCatching { com.acuranza.app.tools.DigestSchedule.sync(this@AcuranzaApp) } }
    }

    private val lastCatchUp = java.util.concurrent.atomic.AtomicLong(0)

    /** Catches up on bank SMS the live receiver missed (see [com.acuranza.app.sms.SmsCatchUp]), at most once a minute. */
    fun catchUpSms() {
        val now = System.currentTimeMillis()
        val last = lastCatchUp.get()
        if (now - last < CATCH_UP_EVERY_MILLIS || !lastCatchUp.compareAndSet(last, now)) return
        // The user is opening the app, so no "spent at" alerts: the new transactions are on screen.
        appScope.launch { com.acuranza.app.sms.SmsCatchUp.run(container, alert = false) }
    }

    private companion object {
        const val CATCH_UP_EVERY_MILLIS = 60_000L
    }
}
