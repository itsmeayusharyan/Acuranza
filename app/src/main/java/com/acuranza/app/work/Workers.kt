package com.acuranza.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.acuranza.app.AcuranzaApp
import com.acuranza.app.finance.Severity
import com.acuranza.app.model.Money
import com.acuranza.app.util.Time
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Daily auto-update: catches up on SMS, records net worth and posts alert-level insights, card-bill
 * and recurring-payment reminders.
 */
class DailyRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as AcuranzaApp).container
        val settings = c.settings.current()
        val today = Time.today()

        runCatching { c.recurring.recordDue(today) }
        runCatching { c.netWorth.snapshot(today) }
        runCatching {
            val auto = c.featurePrefs.current().autoBackup
            if (com.acuranza.app.security.AutoBackup.isDue(auto, System.currentTimeMillis())) {
                com.acuranza.app.security.AutoBackup.run(applicationContext, c, auto)
            }
        }

        if (settings.notifyInsights) {
            runCatching {
                // Card bills have their own reminders (and their own on/off switch), so bill insights aren't
                // posted here. Alerts are claimed one at a time so those beyond the daily limit stay unclaimed
                // and can be posted on a later run.
                val alerts = c.insights.generate().filter { it.severity == Severity.ALERT && !it.id.startsWith("bill_") }
                var posted = 0
                for (alert in alerts) {
                    if (posted >= MAX_INSIGHT_NOTIFICATIONS) break
                    if (alert.id !in c.settings.claimNewInsightNotifications(listOf(alert.id))) continue
                    val text = if (settings.hideAmounts) "Open Acuranza to see the details." else alert.message
                    c.notifications.insight(alert.id.hashCode() and 0xFFFF, alert.title, text)
                    posted++
                }
            }
            runCatching {
                // A subscription or repeat that just changed price.
                val changed = c.recurring.detected(today).filter { s ->
                    (s.priceChangePct ?: 0.0) >= PRICE_ALERT_PCT && !s.lastDate.isBefore(today.minusDays(7))
                }
                val fresh = c.featurePrefs.claimNotificationIds(changed.map { "p-${it.key}-${it.lastDate}" })
                changed.filter { "p-${it.key}-${it.lastDate}" in fresh }.take(MAX_INSIGHT_NOTIFICATIONS).forEach { s ->
                    val text = if (settings.hideAmounts) "Open Acuranza to see the details."
                    else "${Money.format(s.previousAmount ?: 0, showPaise = false)} → ${Money.format(s.lastAmount, showPaise = false)}. Still worth it?"
                    c.notifications.reminder(s.key.hashCode() and 0xFFFF, "${s.merchant} costs ${Math.round(s.priceChangePct ?: 0.0)}% more", text)
                }
            }
            runCatching {
                // Reminder-only schedules due today.
                for (rule in c.recurring.remindersDue(today)) {
                    val key = "r-${rule.id}-${rule.nextDay}"
                    if (key in c.featurePrefs.claimNotificationIds(listOf(key)) && rule.nextDay == today.toEpochDay()) {
                        val amount = if (settings.hideAmounts) "" else " · ${Money.format(rule.amount, showPaise = false)}"
                        c.notifications.reminder((rule.id.toInt() and 0xFFFF) + 1, "${rule.name} is due today", "Scheduled payment$amount")
                    }
                    c.recurring.advance(rule, today)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val MAX_INSIGHT_NOTIFICATIONS = 3
        const val PRICE_ALERT_PCT = 5.0
    }
}

/**
 * Full variant: the periodic inbox scanner. In the standard build this exists only as a
 * compile-time stub — it returns immediately, nothing schedules it, and R8 strips it.
 */
class SmsScanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!com.acuranza.app.BuildConfig.SMS_FULL) return Result.success()
        val c = (applicationContext as AcuranzaApp).container
        val runner = c.smsScanRunner ?: return Result.success()
        runCatching { runner.run(auto = true) }
            .onFailure { runCatching { c.settings.recordSmsScan("failed") } }
        return Result.success()
    }
}

/** Card bill reminders 3 days and 1 day before the due date, on the due date, and when overdue. */
class BillReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as AcuranzaApp).container
        val settings = c.settings.current()
        if (!settings.notifyBills) return Result.success()
        val due = c.bills.unpaid().filter { it.daysLeft != null && (it.daysLeft in REMIND_DAYS || it.daysLeft < 0) }
        val keys = due.associateBy { "bill-${it.bill.id}-${if (it.daysLeft!! < 0) "overdue" else it.daysLeft}" }
        val fresh = c.settings.claimNewBillNotifications(keys.keys)
        for ((key, item) in keys) {
            if (key !in fresh) continue
            val card = item.card?.name ?: "Card"
            val amount = if (settings.hideAmounts) "Your bill" else Money.format(item.bill.totalDue, showPaise = false)
            val title = when (val d = item.daysLeft!!) {
                0L -> "$card bill due today"
                1L -> "$card bill due tomorrow"
                in 2L..Long.MAX_VALUE -> "$card bill due in $d days"
                else -> "$card bill is overdue"
            }
            val min = item.bill.minimumDue?.takeIf { !settings.hideAmounts }?.let { " (minimum ${Money.format(it, showPaise = false)})" } ?: ""
            c.notifications.bill(item.bill.id.toInt(), title, "$amount$min. Pay on time to avoid interest and late fees.")
        }
        return Result.success()
    }

    companion object {
        val REMIND_DAYS = setOf(3L, 1L, 0L)
    }
}

object WorkScheduler {
    private const val DAILY = "acuranza-daily-refresh"
    private const val BILLS = "acuranza-bill-reminders"
    private const val SYNC_NOW = "acuranza-sync-now"
    private const val SMS_SCAN = "acuranza-sms-scan"

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyRefreshWorker>(Duration.ofHours(12))
                .setInitialDelay(Duration.ofMinutes(15))
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            BILLS,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BillReminderWorker>(Duration.ofDays(1))
                .setInitialDelay(delayUntil(LocalTime.of(9, 0)))
                .build(),
        )
    }

    /** Full variant: starts/stops the periodic inbox scan; a no-op in the standard build. */
    fun setSmsScanEnabled(context: Context, enabled: Boolean) {
        if (!com.acuranza.app.BuildConfig.SMS_FULL) return
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(SMS_SCAN)
            return
        }
        // WorkManager's floor for periodic work is 15 minutes — Android will not run it faster in
        // the background. The 10-minute experience the user chose comes from the in-app ticker
        // (SmsForegroundTicker) while the app is open; this schedule catches up in the background.
        wm.enqueueUniquePeriodicWork(
            SMS_SCAN,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SmsScanWorker>(Duration.ofMinutes(15)).build(),
        )
    }

    /** Runs the full refresh once, now. */
    fun syncNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DailyRefreshWorker>().build(),
        )
    }

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
