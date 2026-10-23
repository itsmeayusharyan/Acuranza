package com.acuranza.app.tools

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.acuranza.app.AcuranzaApp
import com.acuranza.app.R
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.di.AppContainer
import com.acuranza.app.domain.MoneyIn
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.model.Money
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the "This month" widget shows. [barPercent] is spending against the overall budget, or
 * against income when no budget exists. [ratePercent] is the savings rate (share of income kept),
 * clamped to 0..100; null when there was no income to speak of.
 */
data class WidgetFigures(val income: Paise, val spent: Paise, val barPercent: Int) {
    val saved: Paise get() = income - spent

    val ratePercent: Int?
        get() = if (income > 0) ((saved * 100) / income).toInt().coerceIn(0, 100) else null

    companion object {
        fun of(period: Period, txns: List<TransactionEntity>, overallBudget: Paise?): WidgetFigures {
            val counted = txns.filter { SmsMapping.countsInStats(it) && Time.toDate(it.timestamp) in period }
            val spent = MoneyIn.netSpent(counted.filter { it.type == TxnType.EXPENSE }, counted.filter(MoneyIn::isGivenBack))
            val income = counted.filter(MoneyIn::isEarned).sumOf { it.amount }
            val base = overallBudget?.takeIf { it > 0 } ?: income.takeIf { it > 0 }
            val pct = if (base == null) 0 else ((spent * 100) / base).toInt().coerceIn(0, 100)
            return WidgetFigures(income, spent, pct)
        }
    }
}

/** Progress toward the user's configured savings target for the current period. */
data class GoalProgress(val saved: Paise, val target: Paise) {
    val percent: Int get() = if (target > 0) ((saved * 100) / target).toInt().coerceIn(0, 100) else 0
    val remaining: Paise get() = if (target > saved) target - saved else 0

    companion object {
        /** A null or zero target means the user has no goal configured. */
        fun of(saved: Paise, target: Paise?): GoalProgress? = target?.takeIf { it > 0 }?.let { GoalProgress(saved, it) }
    }
}

object WidgetUpdater {
    private const val HIDDEN = "₹ ••••"

    /** Amounts appear only without privacy mode, and with app lock on only if the user opted in. */
    fun showAmounts(hideAmounts: Boolean, lockEnabled: Boolean, amountsWhenLocked: Boolean): Boolean =
        !hideAmounts && (!lockEnabled || amountsWhenLocked)

    /** Refreshes every data widget whenever transactions, settings, widget prefs or the plan change. */
    @OptIn(FlowPreview::class)
    fun start(app: AcuranzaApp) {
        app.appScope.launch {
            combine(
                app.container.db.transactions().observeChanges(),
                app.container.settings.settings,
                ToolsSettings.get(app).prefs,
                app.container.featurePrefs.settings,
            ) { _, _, _, _ -> Unit }
                .debounce(1_500)
                .collect { runCatching { updateAll(app) } }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val c = (context.applicationContext as AcuranzaApp).container
        MonthWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateMonth(context, manager, ids, c)
        }
        SavingsGoalWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateSavingsGoal(context, manager, ids, c)
        }
    }

    /** The "This month" widget: income, spending, net savings, savings rate and budget progress. */
    suspend fun updateMonth(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val budget = c.db.budgets().all().firstOrNull { it.categoryKey == null }?.monthlyLimit
        val f = WidgetFigures.of(period, txns, budget)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)

        val views = RemoteViews(context.packageName, R.layout.widget_month)
        views.setTextViewText(R.id.widget_title, title(period, settings.monthStartDay))
        views.setTextViewText(R.id.widget_income, if (show) Money.format(f.income, showPaise = false) else HIDDEN)
        views.setTextViewText(R.id.widget_spent, if (show) Money.format(f.spent, showPaise = false) else HIDDEN)
        // Net savings is the headline figure. Negative savings keep their sign and turn red.
        views.setTextViewText(R.id.widget_saved, if (show) Money.format(f.saved, showPaise = false) else HIDDEN)
        views.setTextColor(
            R.id.widget_saved,
            context.getColor(
                when {
                    !show -> R.color.widget_ink
                    f.saved < 0 -> R.color.widget_overdue
                    else -> R.color.widget_ink
                },
            ),
        )
        val rate = f.ratePercent
        views.setTextViewText(
            R.id.widget_rate,
            when {
                !show -> ""
                rate == null -> "No income yet this month"
                else -> "$rate% saved"
            },
        )
        // Budget progress when a budget exists; the share of income spent otherwise.
        views.setProgressBar(R.id.widget_bar, 100, if (show) f.barPercent else 0, false)
        views.setTextViewText(
            R.id.widget_bar_caption,
            when {
                !show -> ""
                budget != null && budget > 0 -> "${f.barPercent}% of ${Money.compact(budget)} budget"
                f.income > 0 -> "${f.barPercent}% of income spent"
                else -> ""
            },
        )
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, null, 7_100))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** The savings-goal widget: this period's net savings against the configured target. */
    suspend fun updateSavingsGoal(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val f = WidgetFigures.of(period, txns, null)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val goal = GoalProgress.of(f.saved, c.featurePrefs.current().savingsTarget)

        val views = RemoteViews(context.packageName, R.layout.widget_goal)
        if (goal == null) {
            views.setViewVisibility(R.id.widget_goal_body, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_goal_body, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
            views.setTextViewText(
                R.id.widget_goal_amounts,
                if (show) "${Money.compact(goal.saved)} of ${Money.compact(goal.target)}" else HIDDEN,
            )
            views.setProgressBar(R.id.widget_goal_bar, 100, if (show) goal.percent else 0, false)
            views.setTextViewText(
                R.id.widget_goal_caption,
                when {
                    !show -> ""
                    goal.percent >= 100 -> "Goal reached"
                    else -> "${goal.percent}% · ${Money.compact(goal.remaining)} to go"
                },
            )
        }
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, null, 7_600))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun title(period: Period, monthStartDay: Int): String {
        if (monthStartDay == 1) return "This month"
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        return "${period.start.format(fmt)} – ${period.lastDay.format(fmt)}"
    }
}

/** "This month": income, spending, net savings, savings rate and budget progress. */
class MonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateMonth(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Savings goal": this period's savings against the configured target, with progress. */
class SavingsGoalWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateSavingsGoal(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * "Quick add": a 2×2 grid — each action opens the corresponding form or screen. The forms create
 * transactions, so repeated taps can never double-create anything.
 */
class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(R.id.widget_add_expense, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_EXPENSE, 7_201))
        views.setOnClickPendingIntent(R.id.widget_add_income, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_INCOME, 7_202))
        views.setOnClickPendingIntent(R.id.widget_add_transfer, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSFER, 7_203))
        views.setOnClickPendingIntent(R.id.widget_add_transactions, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSACTIONS, 7_204))
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}

/** "Shortcuts": compact navigation to the most-used screens. Static — no data, no refresh needed. */
class ShortcutsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)
        views.setOnClickPendingIntent(R.id.widget_shortcut_bills, QuickLaunch.pending(context, QuickLaunch.ROUTE_BILLS, 7_301))
        views.setOnClickPendingIntent(R.id.widget_shortcut_budgets, QuickLaunch.pending(context, QuickLaunch.ROUTE_PLANNING, 7_302))
        views.setOnClickPendingIntent(R.id.widget_shortcut_reports, QuickLaunch.pending(context, QuickLaunch.ROUTE_REPORTS, 7_303))
        views.setOnClickPendingIntent(R.id.widget_shortcut_accounts, QuickLaunch.pending(context, QuickLaunch.ROUTE_ACCOUNTS, 7_304))
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
