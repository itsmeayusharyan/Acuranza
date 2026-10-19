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
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.finance.Budgets
import com.acuranza.app.finance.Goals
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.BudgetStatus
import com.acuranza.app.repo.GoalStatus
import com.acuranza.app.ui.AcuranzaBottomBar
import com.acuranza.app.ui.bills.BillsContent
import com.acuranza.app.ui.bills.BillsState
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.home.HomeContent
import com.acuranza.app.ui.management.ManagementContent
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.RealEstateAgent
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
            SettingsSection("Financial tools") {
                SettingsRow("Recurring payments", "Upcoming payments, AutoPay mandates, price changes", Icons.Outlined.Autorenew) {}
                SettingsRow("Loans & EMIs", "What's left, prepayment savings, tax split", Icons.Outlined.RealEstateAgent) {}
                SettingsRow("Reports", "Monthly and yearly PDF, year in review, calendar", Icons.Outlined.Assessment, divider = false) {}
            }
            SettingsSection("Appearance & privacy") {
                SettingsRow(
                    "Theme",
                    "Follow the system, or pick light or dark",
                    Icons.Outlined.Palette,
                    footer = { SegmentedTabs(listOf("System", "Light", "Dark"), 2, onSelect = {}) },
                )
                SettingsToggle("Hide amounts", "Show ₹ •••• until you tap the eye", false, {}, Icons.Outlined.VisibilityOff)
                SettingsToggle("App lock", "Fingerprint, face or screen lock when opening Acuranza", true, {}, Icons.Outlined.Fingerprint)
                SettingsToggle("Block screenshots", "Hide Acuranza in the recents preview and block screenshots", true, {}, Icons.Outlined.ScreenLockPortrait)
                SettingsRow(
                    "Database encryption",
                    "AES-256 (SQLCipher), key kept in the phone's secure hardware keystore",
                    Icons.Outlined.Lock,
                    enabled = false,
                    divider = false,
                    trailing = { Pill("On", AcuranzaTheme.colors.good) },
                )
            }
            SettingsSection("Notifications") {
                SettingsToggle("Bill reminders", "3 days and 1 day before the due date", true, {}, Icons.Outlined.Alarm)
                SettingsToggle("Important insights", "Overspending and unusual spends", false, {}, Icons.Outlined.Bolt, divider = false)
            }
            SettingsSection("Security") {
                SettingsRow("App PIN", "On", Icons.Outlined.Password) {}
                SettingsRow("Decoy PIN", "Off · a second PIN that opens a sample profile", Icons.Outlined.TheaterComedy) {}
                SettingsRow("Recovery options", "What still works if a PIN is forgotten or the phone is lost", Icons.Outlined.Shield, divider = false) {}
            }
            SettingsSection("Data management") {
                SettingsRow("Back up", "Encrypted file protected by a password", Icons.Outlined.Backup) {}
                SettingsRow("Automatic backups", "Daily or weekly, to a folder you choose", Icons.Outlined.Sync) {}
                SettingsRow("Erase all data", "Deletes accounts, transactions and bills", Icons.Outlined.DeleteForever, tone = RowTone.DESTRUCTIVE, divider = false) {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    /** The Management tab is assembled from the same widgets its screen uses. */
    @Test
    fun management() = paparazzi.snapshot {
        Frame(tab = Routes.MANAGEMENT) { ManagementContent({}, {}, {}, {}) }
    }

    @Test
    fun bottom_bar_analytics_active() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) { Box(Modifier.fillMaxSize()) }
    }

    @Test
    fun bottom_bar_management_active() = paparazzi.snapshot {
        Frame(tab = Routes.MANAGEMENT) { Box(Modifier.fillMaxSize()) }
    }

    @Test
    fun about() = paparazzi.snapshot {
        AcuranzaTheme(darkTheme = true) {
            AboutContent(
                versionLabel = "Version 1.3.0 (4)",
                onBack = {},
                onPrivacy = {},
                onLicence = {},
                onVisitSource = {},
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
