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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the "This month" widget shows. [barPercent] is spending against the budget, or against income without one. */
data class WidgetFigures(val income: Paise, val spent: Paise, val barPercent: Int) {
    val saved: Paise get() = income - spent

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

object WidgetUpdater {
    private const val HIDDEN = "₹ ••••"

    /** Amounts appear only without privacy mode, and with app lock on only if the user opted in. */
    fun showAmounts(hideAmounts: Boolean, lockEnabled: Boolean, amountsWhenLocked: Boolean): Boolean =
        !hideAmounts && (!lockEnabled || amountsWhenLocked)

    /** Refreshes every data widget whenever transactions, bills, rules, settings or widget prefs change. */
    @OptIn(FlowPreview::class)
    fun start(app: AcuranzaApp) {
        app.appScope.launch {
            combine(
                app.container.db.transactions().observeChanges(),
                app.container.settings.settings,
                ToolsSettings.get(app).prefs,
                app.container.bills.observeUnpaid(),
                app.container.recurring.observeRules(),
            ) { _, _, _, _, _ -> Unit }
                .debounce(1_500)
                .collect { runCatching { updateAll(app) } }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val c = (context.applicationContext as AcuranzaApp).container
        MonthWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateMonth(context, manager, ids)
        }
        BalancesWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateBalances(context, manager, ids, c)
        }
        RecentWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateRecent(context, manager, ids, c)
        }
        UpcomingWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateUpcoming(context, manager, ids, c)
        }
    }

    suspend fun updateMonth(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val c = (context.applicationContext as AcuranzaApp).container
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val budget = c.db.budgets().all().firstOrNull { it.categoryKey == null }?.monthlyLimit
        val f = WidgetFigures.of(period, txns, budget)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)

        val views = RemoteViews(context.packageName, R.layout.widget_month)
        views.setTextViewText(R.id.widget_title, title(period, settings.monthStartDay))
        views.setTextViewText(R.id.widget_spent, if (show) Money.format(f.spent, showPaise = false) else HIDDEN)
        views.setTextViewText(R.id.widget_income, "Income\n${if (show) Money.compact(f.income) else HIDDEN}")
        views.setTextViewText(R.id.widget_saved, "Saved\n${if (show) Money.compact(f.saved) else HIDDEN}")
        views.setProgressBar(R.id.widget_bar, 100, if (show) f.barPercent else 0, false)
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, null, 7_100))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /**
     * Balances: the net-worth line comes from the SAME flow the Home screen reads, so the two can
     * never disagree; rows mirror the Accounts screen (cards show what's owed).
     */
    suspend fun updateBalances(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val accounts = c.accounts.observeAccounts().first()
        val net = c.netWorth.observeNetWorth().first().total
        val rows = BalancesFigures.of(accounts).rows

        val views = RemoteViews(context.packageName, R.layout.widget_balances)
        views.setTextViewText(R.id.widget_net, if (show) Money.format(net, showPaise = false) else HIDDEN)
        val ink = context.getColor(R.color.widget_ink)
        val subtle = context.getColor(R.color.widget_subtle)
        views.setViewVisibility(R.id.widget_empty, if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        rows.forEachIndexed { i, row ->
            views.setViewVisibility(ROW_IDS[i], android.view.View.VISIBLE)
            views.setTextViewText(NAME_IDS[i], row.name + if (row.isDebt) " · owed" else "")
            views.setTextViewText(AMOUNT_IDS[i], if (show) row.text else HIDDEN)
            views.setTextColor(NAME_IDS[i], if (row.isDebt) subtle else ink)
        }
        (rows.size..3).forEach { views.setViewVisibility(ROW_IDS[it], android.view.View.GONE) }
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, QuickLaunch.ROUTE_ACCOUNTS, 7_310))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** Latest transactions; income rows carry the app's income colour; a row opens its detail. */
    suspend fun updateRecent(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val txns = c.db.transactions().observeRecent(4).first()
        val categoryNames = c.db.categories().all().associate { it.key to it.name }
        val rows = RecentRows.of(txns, categoryNames)

        val views = RemoteViews(context.packageName, R.layout.widget_recent)
        val ink = context.getColor(R.color.widget_ink)
        val income = context.getColor(R.color.widget_income)
        views.setViewVisibility(R.id.widget_empty, if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        rows.forEachIndexed { i, row ->
            views.setViewVisibility(RECENT_ROW_IDS[i], android.view.View.VISIBLE)
            views.setTextViewText(RECENT_NAME_IDS[i], row.title)
            views.setTextViewText(RECENT_AMOUNT_IDS[i], if (show) row.text else HIDDEN)
            views.setTextColor(RECENT_AMOUNT_IDS[i], if (row.isIn) income else ink)
            views.setOnClickPendingIntent(
                RECENT_ROW_IDS[i],
                QuickLaunch.pending(context, QuickLaunch.TXN_VIEW_PREFIX + row.id, 7_400 + i),
            )
        }
        (rows.size..3).forEach { views.setViewVisibility(RECENT_ROW_IDS[it], android.view.View.GONE) }
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSACTIONS, 7_409))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** Unpaid bills (overdue first) plus recurring payments inside a 7-day horizon. */
    suspend fun updateUpcoming(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val bills = c.bills.observeUnpaid().first()
        val rules = c.recurring.observeRules().first()
        val rows = UpcomingItems.of(bills, rules, Time.today())

        val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
        val overdue = context.getColor(R.color.widget_overdue)
        val subtle = context.getColor(R.color.widget_subtle)
        views.setViewVisibility(R.id.widget_empty, if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        rows.forEachIndexed { i, row ->
            views.setViewVisibility(UPCOMING_ROW_IDS[i], android.view.View.VISIBLE)
            views.setTextViewText(UPCOMING_LABEL_IDS[i], row.label)
            views.setTextViewText(UPCOMING_DUE_IDS[i], row.dueText)
            views.setTextColor(UPCOMING_DUE_IDS[i], if (row.overdue) overdue else subtle)
            views.setTextViewText(UPCOMING_AMOUNT_IDS[i], if (show) row.amountText else HIDDEN)
        }
        (rows.size..3).forEach { views.setViewVisibility(UPCOMING_ROW_IDS[it], android.view.View.GONE) }
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, QuickLaunch.ROUTE_BILLS, 7_510))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun title(period: Period, monthStartDay: Int): String {
        if (monthStartDay == 1) return "This month"
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        return "${period.start.format(fmt)} – ${period.lastDay.format(fmt)}"
    }

    // Fixed row slots in the widget layouts — RemoteViews can't inflate lists without a service.
    private val ROW_IDS = listOf(R.id.balances_row_0, R.id.balances_row_1, R.id.balances_row_2, R.id.balances_row_3)
    private val NAME_IDS = listOf(
        R.id.widget_balance_0_name, R.id.widget_balance_1_name, R.id.widget_balance_2_name, R.id.widget_balance_3_name,
    )
    private val AMOUNT_IDS = listOf(
        R.id.widget_balance_0_amount, R.id.widget_balance_1_amount, R.id.widget_balance_2_amount, R.id.widget_balance_3_amount,
    )
    private val RECENT_ROW_IDS = listOf(R.id.recent_row_0, R.id.recent_row_1, R.id.recent_row_2, R.id.recent_row_3)
    private val RECENT_NAME_IDS = listOf(
        R.id.widget_recent_0_name, R.id.widget_recent_1_name, R.id.widget_recent_2_name, R.id.widget_recent_3_name,
    )
    private val RECENT_AMOUNT_IDS = listOf(
        R.id.widget_recent_0_amount, R.id.widget_recent_1_amount, R.id.widget_recent_2_amount, R.id.widget_recent_3_amount,
    )
    private val UPCOMING_ROW_IDS = listOf(R.id.upcoming_row_0, R.id.upcoming_row_1, R.id.upcoming_row_2, R.id.upcoming_row_3)
    private val UPCOMING_LABEL_IDS = listOf(
        R.id.widget_upcoming_0_label, R.id.widget_upcoming_1_label, R.id.widget_upcoming_2_label, R.id.widget_upcoming_3_label,
    )
    private val UPCOMING_DUE_IDS = listOf(
        R.id.widget_upcoming_0_due, R.id.widget_upcoming_1_due, R.id.widget_upcoming_2_due, R.id.widget_upcoming_3_due,
    )
    private val UPCOMING_AMOUNT_IDS = listOf(
        R.id.widget_upcoming_0_amount, R.id.widget_upcoming_1_amount, R.id.widget_upcoming_2_amount, R.id.widget_upcoming_3_amount,
    )
}

/** Balances: net worth plus the strongest confirmed accounts; cards show what's owed. */
class BalancesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateBalances(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Recent transactions: latest four, a row opens its detail. */
class RecentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateRecent(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Upcoming payments: unpaid card bills and recurring entries due within a week. */
class UpcomingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateUpcoming(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "This month": spent, income and saved for the current budgeting period. */
class MonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateMonth(context, manager, ids) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Quick add": one tap to the add-expense, add-income or transfer screen, or the transaction list. */
class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(R.id.widget_add_expense, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_EXPENSE, 7_201))
        views.setOnClickPendingIntent(R.id.widget_add_income, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_INCOME, 7_202))
        views.setOnClickPendingIntent(R.id.widget_transfer, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSFER, 7_203))
        views.setOnClickPendingIntent(R.id.widget_transactions, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSACTIONS, 7_204))
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
