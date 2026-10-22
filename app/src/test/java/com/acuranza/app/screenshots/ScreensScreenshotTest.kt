package com.acuranza.app.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.acuranza.app.data.BudgetEntity
import com.acuranza.app.data.GoalEntity
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.finance.Budgets
import com.acuranza.app.finance.Goals
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.BudgetStatus
import com.acuranza.app.repo.NotificationItem
import com.acuranza.app.repo.NotificationItem.Kind
import com.acuranza.app.repo.GoalStatus
import com.acuranza.app.ui.AcuranzaBottomBar
import com.acuranza.app.ui.notifications.NotificationsContent
import com.acuranza.app.ui.bills.BillsContent
import com.acuranza.app.ui.bills.BillsState
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.home.EditHomeContent
import com.acuranza.app.ui.home.HomeContent
import com.acuranza.app.ui.home.HomeLayout
import com.acuranza.app.ui.home.HomeState
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.planning.PlanningContent
import com.acuranza.app.ui.planning.PlanningState
import com.acuranza.app.ui.about.AboutContent
import com.acuranza.app.ui.settings.RowTone
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import com.acuranza.app.ui.settings.SettingsToggle
import com.acuranza.app.ui.transactions.AddTransactionContent
import com.acuranza.app.ui.transactions.DayGroup
import com.acuranza.app.ui.transactions.EditorRefs
import com.acuranza.app.ui.transactions.TransactionsContent
import com.acuranza.app.ui.transactions.TxnListState
import com.acuranza.app.util.Period
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TheaterComedy

class ScreensScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val today = LocalDate.of(2026, 10, 14)

    /** A phone frame: screen content above the real bottom bar, like the running app. */
    @Composable
    private fun Frame(dark: Boolean = false, tab: String? = null, content: @Composable () -> Unit) {
        AcuranzaTheme(darkTheme = dark) {
          androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.weight(1f)) {
                    content()
                }
                if (tab != null) AcuranzaBottomBar(tab, onTab = {})
            }
          }
        }
    }

    private fun homeState() = HomeState(
        loading = false,
        netWorth = FakeData.netWorth,
        trend = FakeData.trend,
        summary = FakeData.summary,
        unreviewed = 3,
        bills = FakeData.bills,
        billsDue = FakeData.bills.size,
        insights = FakeData.insights,
        recent = FakeData.recent,
        accounts = FakeData.accounts,
    )

    @Test
    fun home_light() = paparazzi.snapshot { Frame(tab = Routes.HOME) { HomeContent(homeState(), hidden = false) } }

    @Test
    fun home_left_to_spend() = paparazzi.snapshot {
        val safe = com.acuranza.app.repo.SafeToSpend.plan(
            today = java.time.LocalDate.of(2026, 10, 15), cycle = com.acuranza.app.finance.PayCycle(25), liquid = 40_000_000L, cardDues = 0,
            upcoming = listOf(com.acuranza.app.finance.BudgetMath.Due(java.time.LocalDate.of(2026, 10, 18), 200_000L, "SIP")),
            overallBudget = null, incomeThisPeriod = 10_000_000L, usualMonthlySpend = null, spentThisPeriod = 3_000_000L,
            salary = 10_000_000L, savingsTarget = 2_000_000L,
        )
        Frame(tab = Routes.HOME) { HomeContent(homeState().copy(safeToSpend = safe), hidden = false) }
    }

    @Test
    fun home_dark() = paparazzi.snapshot { Frame(dark = true, tab = Routes.HOME) { HomeContent(homeState(), hidden = false) } }

    @Test
    fun home_privacy() = paparazzi.snapshot { Frame(tab = Routes.HOME) { HomeContent(homeState(), hidden = true) } }

    @Test
    fun activity() = paparazzi.snapshot {
        val groups = FakeData.recent.groupBy { it.date }.map { (d, items) -> DayGroup(d, items, items.sumOf { it.signedAmount }) }
        Frame(tab = Routes.TRANSACTIONS) {
            TransactionsContent(
                TxnListState(
                    period = Period(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1)),
                    groups = groups,
                    income = FakeData.summary.income,
                    expense = FakeData.summary.expense,
                    loaded = true,
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun add_transaction() = paparazzi.snapshot {
        Frame {
            AddTransactionContent(
                refs = EditorRefs(FakeData.accounts.map { it.account }, FakeData.categories),
                editId = null,
                initialType = TxnType.EXPENSE,
                initialAccountId = FakeData.hdfc.id,
                initialAmount = "642",
                initialCategoryKey = "food",
                initialMerchant = "Swiggy",
            )
        }
    }

    private fun budget(key: String?, limit: Long, spent: Long) = BudgetStatus(
        BudgetEntity(id = (key ?: "all").hashCode().toLong(), categoryKey = key, monthlyLimit = limit * 100),
        key?.let(FakeData::cat),
        spent * 100,
        Budgets.project(limit * 100, spent * 100, today),
    )

    private fun planState() = PlanningState(
        budgets = listOf(
            budget(null, 70_000, 61_240),
            budget("food", 10_000, 9_820),
            budget("shopping", 12_000, 8_450),
            budget("transport", 6_000, 4_120),
        ),
        goals = listOf(
            GoalStatus(
                GoalEntity(id = 1, name = "Emergency fund", target = 6_00_000_00, saved = 3_80_000_00, deadline = today.plusMonths(10).toEpochDay()),
                Goals.plan(6_00_000_00, 3_80_000_00, today.plusMonths(10), today, 7.0),
            ),
            GoalStatus(
                GoalEntity(id = 2, name = "Japan trip", target = 2_50_000_00, saved = 60_000_00, deadline = today.plusMonths(14).toEpochDay(), icon = "flight"),
                Goals.plan(2_50_000_00, 60_000_00, today.plusMonths(14), today, 6.0),
            ),
        ),
        categories = FakeData.categories,
        loaded = true,
    )

    @Test
    fun plan_budgets() = paparazzi.snapshot { Frame { PlanningContent(planState(), tab = 0) } }

    @Test
    fun plan_goals() = paparazzi.snapshot { Frame { PlanningContent(planState(), tab = 1) } }

    @Test
    fun card_bills() = paparazzi.snapshot {
        Frame {
            BillsContent(
                BillsState(bills = FakeData.bills, cards = listOf(FakeData.kotakCard, FakeData.dbsCard), banks = listOf(FakeData.hdfc), loaded = true),
                hidden = false,
            )
        }
    }

    /** Settings is assembled from the same row widgets the More screen uses. */
    @Test
    fun settings() = paparazzi.snapshot { Frame(tab = Routes.SETTINGS) { settingsBody() } }

    /** Dark is the default theme; the settings screen must read well in it. */
    @Test
    fun settings_dark() = paparazzi.snapshot { Frame(dark = true, tab = Routes.SETTINGS) { settingsBody() } }

    @Composable
    private fun settingsBody() {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AcuranzaLogo(size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Settings", style = AcuranzaText.screenTitle)
                    Text("Your money, on your phone", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                }
            }
            AcuranzaCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                SettingsRow("Appearance", "Theme, accent colour, hide amounts, edit home", Icons.Outlined.Palette) {}
                SettingsRow("Data management", "Backups, imports, sample data, erase", Icons.Outlined.Backup) {}
                SettingsRow("Financial tools", "Recurring, loans, lending, reports, budgets & goals", Icons.Outlined.Category) {}
                SettingsRow("Management", "Accounts, imports and categories", Icons.Outlined.AccountBalance) {}
                SettingsRow("Notifications", "Reminders, and where your budget month starts", Icons.Outlined.Alarm) {}
                SettingsRow("Privacy & security", "App lock, PINs, encryption and recovery", Icons.Outlined.Shield) {}
                SettingsRow("Tools", "Export data, daily digest and widgets", Icons.Outlined.Tune) {}
                SettingsRow("About", "Version, privacy policy and licence", Icons.Outlined.Info, divider = false) {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @Test
    fun bottom_bar_analytics_active() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) { Box(Modifier.fillMaxSize()) }
    }

    @Test
    fun about() = paparazzi.snapshot {
        AcuranzaTheme(darkTheme = true) {
            AboutContent(
                onBack = {},
                onPrivacy = {},
                onTerms = {},
                onLicence = {},
            )
        }
    }

    @Test
    fun categories() = paparazzi.snapshot {
        AcuranzaTheme(darkTheme = true) {
            com.acuranza.app.ui.settings.CategoriesContent(
                cats = listOf(
                    com.acuranza.app.data.CategoryEntity(key = "food", name = "Food & Dining", type = TxnType.EXPENSE, icon = "restaurant", colorArgb = 0xFFFF7A59),
                    com.acuranza.app.data.CategoryEntity(key = "custom_goa", name = "Goa trip", type = TxnType.EXPENSE, icon = "flight", colorArgb = 0xFF3BC9DB, isDefault = false),
                    com.acuranza.app.data.CategoryEntity(key = "salary", name = "Salary", type = TxnType.INCOME, icon = "payments", colorArgb = 0xFF2F9E44),
                ),
            )
        }
    }

    @Test
    fun edit_home() = paparazzi.snapshot {
        AcuranzaTheme(darkTheme = true) {
            EditHomeContent(
                layout = HomeLayout.decode("quick,!heat,stats"),
                saved = true,
            )
        }
    }

    @Test
    fun notifications() = paparazzi.snapshot {
        AcuranzaTheme(darkTheme = true) {
            NotificationsContent(
                items = listOf(
                    NotificationItem(
                        id = "bill_1",
                        kind = Kind.BILL,
                        title = "Axis Card bill overdue",
                        message = "₹12,345 was due 12 Oct.",
                        whenLabel = "7d overdue",
                        route = "bills",
                        epochDay = 0,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "bill_2",
                        kind = Kind.BILL,
                        title = "HDFC Card bill due in 2 days",
                        message = "₹4,500 is due on 21 Oct.",
                        whenLabel = "In 2 days",
                        route = "bills",
                        epochDay = 0,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "insight_a",
                        kind = Kind.INSIGHT,
                        title = "Dining is up 40% this month",
                        message = "You've spent more on dining than usual.",
                        whenLabel = "Today",
                        route = "insights",
                        epochDay = 0,
                        unread = false,
                    ),
                ),
                hidden = false,
            )
        }
    }

    // ── v1.13.21 coverage additions: consent, insights, dark variants ────────────────────────
    @Test
    fun sms_consent() = paparazzi.snapshot {
        Frame { com.acuranza.app.ui.settings.SmsConsentContent(Modifier, onAgree = {}) }
    }

    @Test
    fun insights_empty() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) {
            com.acuranza.app.ui.insights.InsightsContent(insights = emptyList(), hidden = false)
        }
    }

    private fun insight(id: String, severity: com.acuranza.app.finance.Severity, title: String, message: String) =
        com.acuranza.app.finance.Insight(id = id, severity = severity, title = title, message = message)

    @Test
    fun insights_populated() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) {
            com.acuranza.app.ui.insights.InsightsContent(
                insights = listOf(
                    insight("i1", com.acuranza.app.finance.Severity.ALERT, "Overspending on Food & Dining",
                        "You've spent 82% of this month's food budget with 9 days to go."),
                    insight("i2", com.acuranza.app.finance.Severity.WARNING, "Rising transport costs",
                        "Transport spending is 34% above your 3-month average."),
                    insight("i3", com.acuranza.app.finance.Severity.TIP, "Idle cash in HDFC ••4521",
                        "₹18,400 has been idle for 45 days — a recurring SIP could put it to work."),
                    insight("i4", com.acuranza.app.finance.Severity.GOOD, "Savings rate up",
                        "You saved 22% of income this month, up from 14% last month."),
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun activity_dark() = paparazzi.snapshot {
        val groups = FakeData.recent.groupBy { it.date }.map { (d, items) -> DayGroup(d, items, items.sumOf { it.signedAmount }) }
        Frame(dark = true, tab = Routes.TRANSACTIONS) {
            TransactionsContent(
                TxnListState(
                    period = Period(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1)),
                    groups = groups,
                    income = FakeData.summary.income,
                    expense = FakeData.summary.expense,
                    loaded = true,
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun add_transaction_dark() = paparazzi.snapshot {
        Frame(dark = true) {
            AddTransactionContent(
                refs = EditorRefs(FakeData.accounts.map { it.account }, FakeData.categories),
                editId = null,
                initialType = TxnType.EXPENSE,
                initialAccountId = FakeData.hdfc.id,
                initialAmount = "642",
                initialCategoryKey = "food",
                initialMerchant = "Swiggy",
            )
        }
    }

    @Test
    fun about_dark() = paparazzi.snapshot {
        Frame(dark = true) {
            AboutContent(
                onBack = {},
                onPrivacy = {},
                onTerms = {},
                onLicence = {},
            )
        }
    }

    @Test
    fun splash_and_logo() = paparazzi.snapshot {
        AcuranzaTheme {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                AcuranzaLogo(size = 88.dp, shape = LogoShape.RoundedSquare)
                Spacer(Modifier.height(12.dp))
                Text("Acuranza", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge, color = AcuranzaTheme.colors.ink)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    AcuranzaLogo(size = 96.dp, shape = LogoShape.RoundedSquare)
                    AcuranzaLogo(size = 64.dp, shape = LogoShape.Circle)
                    AcuranzaLogo(size = 40.dp, shape = LogoShape.RoundedSquare)
                }
                Spacer(Modifier.height(48.dp))
                Box(Modifier.fillMaxWidth())
            }
        }
    }

}
