package com.acuranza.app.tools

import com.acuranza.app.data.RecurringRuleEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.Money
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.AccountWithBalance
import com.acuranza.app.repo.BillItem
import java.time.LocalDate

/**
 * Pure row-selection logic for the Balances, Recent and Upcoming widgets. No Android classes —
 * the AppWidgetProvider code only renders what these return, so the numbers the widgets show are
 * unit-tested against the same definitions the app screens use.
 */

/** One account line on the Balances widget. Cards show the amount still OWED, not available credit. */
data class BalancesRow(val name: String, val text: String, val isDebt: Boolean, val isCard: Boolean)

data class BalancesFigures(val rows: List<BalancesRow>) {
    companion object {
        /**
         * Rows mirror the Accounts screen: only confirmed accounts (`counted`), strongest positions
         * first, capped at [max]. The widget's net line is rendered from the SAME `NetWorth` flow the
         * Home screen reads — it is never recomputed here, so the two can never disagree.
         */
        fun of(accounts: List<AccountWithBalance>, max: Int = 4): BalancesFigures =
            BalancesFigures(
                accounts.filter { it.counted }
                    .sortedByDescending { if (it.isCard) it.outstanding else it.balance }
                    .take(max)
                    .map { a ->
                        if (a.isCard) BalancesRow(a.account.name, Money.format(a.outstanding, showPaise = false), isDebt = true, isCard = true)
                        else BalancesRow(a.account.name, Money.format(a.balance, showPaise = false), isDebt = false, isCard = false)
                    },
            )
    }
}

/** One line on the Recent-transactions widget. [text] carries the direction sign. */
data class RecentRow(val id: Long, val title: String, val text: String, val isIn: Boolean)

object RecentRows {
    /** Latest-first rows: merchant, else the category's display name, else a neutral fallback. */
    fun of(txns: List<TransactionEntity>, categoryNames: Map<String, String>, max: Int = 4): List<RecentRow> =
        txns.take(max).map { t ->
            val title = t.merchant?.takeIf { it.isNotBlank() }
                ?: categoryNames[t.categoryKey]
                ?: when (t.type) {
                    TxnType.EXPENSE -> "Expense"
                    TxnType.INCOME -> "Income"
                    TxnType.TRANSFER -> "Transfer"
                }
            val prefix = when (t.type) {
                TxnType.INCOME -> "+ "
                TxnType.EXPENSE -> "- "
                TxnType.TRANSFER -> ""
            }
            RecentRow(t.id, title, prefix + Money.format(t.amount, showPaise = false), t.type == TxnType.INCOME)
        }
}

/** One line on the Upcoming widget: an unpaid card bill or a recurring payment inside the horizon. */
data class UpcomingRow(val label: String, val amountText: String, val dueText: String, val overdue: Boolean, val isBill: Boolean)

object UpcomingItems {
    /**
         * Unpaid bills sorted by days left (overdue first, most-overdue at the top), then recurring
         * payments whose next occurrence falls within [horizonDays] of [today]. Capped at [max].
         */
    fun of(
        bills: List<BillItem>,
        rules: List<RecurringRuleEntity>,
        today: LocalDate,
        horizonDays: Long = 7,
        max: Int = 4,
    ): List<UpcomingRow> {
        val billRows = bills.asSequence()
            .filter { !it.bill.paid && it.dueDate != null }
            .sortedBy { it.daysLeft ?: Long.MAX_VALUE }
            .take(max)
            .map { b ->
                val days = b.daysLeft
                UpcomingRow(
                    label = b.card?.name?.let { "$it bill" } ?: "Card bill",
                    amountText = Money.format(b.bill.totalDue, showPaise = false),
                    dueText = dueText(days),
                    overdue = b.isOverdue,
                    isBill = true,
                )
            }
        val horizonEnd = today.plusDays(horizonDays)
        val recurringRows = rules.asSequence()
            .filter { it.active && it.nextDay in today.toEpochDay()..horizonEnd.toEpochDay() }
            .sortedBy { it.nextDay }
            .map { r ->
                val days = r.nextDay - today.toEpochDay()
                UpcomingRow(
                    label = r.name.ifBlank { "Recurring ${r.type.name.lowercase()}" },
                    amountText = Money.format(r.amount, showPaise = false),
                    dueText = dueText(days),
                    overdue = false,
                    isBill = false,
                )
            }
        return (billRows + recurringRows).take(max).toList()
    }

    /** Overdue carries negative [days]; the wording matches the Bills screen. */
    fun dueText(days: Long?): String = when {
        days == null -> "No due date"
        days < 0 -> "Overdue ${-days}d"
        days == 0L -> "Due today"
        days == 1L -> "Tomorrow"
        else -> "In ${days}d"
    }
}
