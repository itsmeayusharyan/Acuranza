package com.acuranza.app.tools

import com.acuranza.app.data.CardBillEntity
import com.acuranza.app.data.RecurringRuleEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.AccountWithBalance
import com.acuranza.app.domain.BalanceSource
import com.acuranza.app.repo.BillItem
import com.acuranza.app.util.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The pure row logic behind the Balances, Recent and Upcoming widgets. */
class WidgetDataTest {

    // ---- balances ----

    private fun acct(
        id: Long,
        name: String,
        balance: Long,
        isCard: Boolean = false,
        outstanding: Long = 0,
        counted: Boolean = true,
    ) = AccountWithBalance(
        account = com.acuranza.app.data.AccountEntity(
            id = id, name = name, type = if (isCard) com.acuranza.app.model.AccountType.CREDIT_CARD else com.acuranza.app.model.AccountType.BANK,
            bank = "x", autoCreated = !counted,
        ),
        balance = balance,
        outstanding = outstanding,
        availableLimit = null,
        creditBalance = 0,
        source = BalanceSource.Entered(0, 0),
    )

    @Test fun balancesRowsShowCardDebtAndBankBalances() {
        val f = BalancesFigures.of(
            listOf(
                acct(1, "Slice", 701_96),
                acct(2, "Card", 0, isCard = true, outstanding = 12_000_00),
                acct(3, "Cash", 500_00),
            ),
        )
        assertEquals(3, f.rows.size)
        // Card owed (₹12,000) sorts ahead of the rest; its text is the owed amount.
        assertEquals("Card", f.rows[0].name)
        assertTrue(f.rows[0].isDebt)
        assertEquals("₹12,000", f.rows[0].text)
        assertEquals("₹702", f.rows[1].text)
    }

    @Test fun balancesSkipUnconfirmedAccountsAndCap() {
        val f = BalancesFigures.of(
            (1L..6L).map { acct(it, "A$it", it * 100_00) } + listOf(acct(99, "Pending", 999_00, counted = false)),
            max = 4,
        )
        assertEquals(4, f.rows.size)
        assertTrue(f.rows.none { it.name == "Pending" })
        assertEquals("A6", f.rows[0].name)
    }

    // ---- recent ----

    private fun txn(id: Long, type: TxnType, amount: Long, merchant: String?, category: String = "food") =
        TransactionEntity(
            id = id, accountId = 1, type = type, amount = amount, categoryKey = category,
            merchant = merchant, timestamp = LocalDateTime.of(2026, 10, 22, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
            source = TxnSource.MANUAL,
        )

    @Test fun recentRowsSignAndTitleFallbacks() {
        val rows = RecentRows.of(
            listOf(
                txn(1, TxnType.EXPENSE, 249_00, "Swiggy"),
                txn(2, TxnType.INCOME, 700_00, "AYUSH ARYAN", category = "other_income"),
                txn(3, TxnType.TRANSFER, 5_000_00, null, category = "unknown.key"),
                txn(4, TxnType.EXPENSE, 120_00, "  ", category = "transport"),
            ),
            mapOf("transport" to "Transport", "food" to "Food"),
        )
        assertEquals("- ₹249", rows[0].text)
        assertEquals("Swiggy", rows[0].title)
        assertEquals("+ ₹700", rows[1].text)
        assertEquals("AYUSH ARYAN", rows[1].title)
        // Transfer: no sign; unknown category falls back to the type word.
        assertTrue(rows[2].text.startsWith("₹"))
        assertEquals("Transfer", rows[2].title)
        // Blank merchant falls through to the category name.
        assertEquals("Transport", rows[3].title)
    }

    @Test fun recentRowsAreCapped() {
        val rows = RecentRows.of((1L..8L).map { txn(it, TxnType.EXPENSE, it * 100, null) }, emptyMap())
        assertEquals(4, rows.size)
    }

    // ---- upcoming ----

    private val today: LocalDate = LocalDate.of(2026, 10, 22)

    private fun bill(name: String, daysLeft: Long, due: Long = 500_00, paid: Boolean = false) = BillItem(
        bill = CardBillEntity(
            accountId = 1, totalDue = due, minimumDue = null,
            dueDate = today.toEpochDay() + daysLeft, source = TxnSource.MANUAL, fingerprint = "f-$name-$daysLeft", paid = paid,
        ),
        card = com.acuranza.app.data.AccountEntity(id = 1, name = name, type = com.acuranza.app.model.AccountType.CREDIT_CARD, bank = "x"),
        daysLeft = daysLeft,
    )

    private fun rule(name: String, inDays: Long, amount: Long = 299_00, active: Boolean = true) = RecurringRuleEntity(
        id = 1, name = name, type = TxnType.EXPENSE, amount = amount, categoryKey = "x", accountId = 1,
        cadence = "MONTHLY", anchorDay = 1, nextDay = today.plusDays(inDays).toEpochDay(), active = active,
    )

    @Test fun upcomingOrdersOverdueFirstThenBillsThenRecurring() {
        val rows = UpcomingItems.of(
            bills = listOf(bill("Later", 5), bill("Overdue", -2), bill("Soon", 1)),
            rules = listOf(rule("Netflix", 3)),
            today = today,
        )
        assertEquals(listOf("Overdue bill", "Soon bill", "Later bill", "Netflix"), rows.map { it.label })
        assertEquals("Overdue 2d", rows[0].dueText)
        assertTrue(rows[0].overdue)
        assertEquals("Tomorrow", rows[1].dueText)
        assertEquals("In 3d", rows[3].dueText)
    }

    @Test fun upcomingSkipsPaidAndDistantRecurring() {
        val rows = UpcomingItems.of(
            bills = listOf(bill("Paid", 2, paid = true), bill("Open", 2)),
            rules = listOf(rule("Far", 20), rule("Off", 3, active = false), rule("Near", 1)),
            today = today,
        )
        assertEquals(listOf("Open bill", "Near"), rows.map { it.label })
    }

    @Test fun upcomingEmptyAndCap() {
        assertTrue(UpcomingItems.of(emptyList(), emptyList(), today).isEmpty())
        assertEquals(
            4,
            UpcomingItems.of(
                (1L..5L).map { bill("B$it", it.toLong()) },
                (1L..3L).map { rule("R$it", it.toLong()) },
                today,
            ).size,
        )
    }

    @Test fun dueTextWording() {
        assertEquals("Due today", UpcomingItems.dueText(0))
        assertEquals("Tomorrow", UpcomingItems.dueText(1))
        assertEquals("In 6d", UpcomingItems.dueText(6))
        assertEquals("Overdue 3d", UpcomingItems.dueText(-3))
        assertEquals("No due date", UpcomingItems.dueText(null))
    }
}
