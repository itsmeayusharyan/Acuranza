package com.acuranza.app.ui.management

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.acuranza.app.ui.accounts.ordinal
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.settings.PaydaySettings
import com.acuranza.app.ui.settings.SettingsPickerRow
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import com.acuranza.app.ui.settings.SettingsToggle
import kotlinx.coroutines.launch

/** The Management tab's live settings values; the screen reads them from [com.acuranza.app.ui.common.LocalSettings]. */
data class ManagementState(
    val notifyBills: Boolean = false,
    val notifyInsights: Boolean = false,
    val monthStartDay: Int = 1,
)

/**
 * The Management tab: accounts and cards, the financial tools, notification and budget-month
 * preferences — the groups that used to live at the top of Settings. Insights is a tab of its
 * own, so it has no row here.
 */
@Composable
fun ManagementScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ensurePermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    ManagementContent(
        state = ManagementState(
            notifyBills = s.notifyBills,
            notifyInsights = s.notifyInsights,
            monthStartDay = s.monthStartDay,
        ),
        onAccounts = { nav.navigate(Routes.ACCOUNTS) },
        onReview = { nav.navigate(Routes.REVIEW) },
        onCategories = { nav.navigate(Routes.CATEGORIES) },
        onRecurring = { nav.navigate(Routes.RECURRING) },
        onLoans = { nav.navigate(Routes.LOANS) },
        onLending = { nav.navigate(Routes.DEBTS) },
        onReports = { nav.navigate(Routes.REPORTS) },
        onWarranties = { nav.navigate(Routes.WARRANTIES) },
        onPlanning = { nav.navigate(Routes.PLANNING) },
        onNotifyBills = { v ->
            if (v) ensurePermission()
            scope.launch { c.settings.setNotifyBills(v) }
        },
        onNotifyInsights = { v ->
            if (v) ensurePermission()
            scope.launch { c.settings.setNotifyInsights(v) }
        },
        onMonthStartDay = { d -> scope.launch { c.settings.setMonthStartDay(d) } },
        payday = { PaydaySettings(s) },
    )
}

/**
 * Stateless Management tab content, rendered from callbacks; used by the app and by screenshot
 * tests. [payday] is the payday rule block (it reads the app container, so the tests pass an
 * empty slot).
 */
@Composable
fun ManagementContent(
    state: ManagementState,
    onAccounts: () -> Unit = {},
    onReview: () -> Unit = {},
    onCategories: () -> Unit = {},
    onRecurring: () -> Unit = {},
    onLoans: () -> Unit = {},
    onLending: () -> Unit = {},
    onReports: () -> Unit = {},
    onWarranties: () -> Unit = {},
    onPlanning: () -> Unit = {},
    onNotifyBills: (Boolean) -> Unit = {},
    onNotifyInsights: (Boolean) -> Unit = {},
    onMonthStartDay: (Int) -> Unit = {},
    payday: @Composable () -> Unit = {},
) {
    AcuranzaScreen(title = "Management", subtitle = "Accounts, financial tools and preferences") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            item(key = "account") {
                SettingsSection("Account & management") {
                    SettingsRow("Accounts & cards", "Balances, limits and reconciliation", Icons.Outlined.AccountBalance) { onAccounts() }
                    SettingsRow("Review imports", "Confirm imported transactions", Icons.Outlined.Inbox) { onReview() }
                    SettingsRow("Categories", "Your spending and income categories", Icons.Outlined.Category, divider = false) { onCategories() }
                }
            }
            item(key = "tools") {
                SettingsSection("Financial tools") {
                    SettingsRow("Recurring payments", "Upcoming payments, AutoPay mandates, price changes", Icons.Outlined.Autorenew) { onRecurring() }
                    SettingsRow("Loans & EMIs", "What's left, prepayment savings, tax split", Icons.Outlined.RealEstateAgent) { onLoans() }
                    SettingsRow("Lending", "Money between you and friends or family", Icons.Outlined.Handshake) { onLending() }
                    SettingsRow("Reports", "Monthly and yearly PDF, year in review, calendar", Icons.Outlined.Assessment) { onReports() }
                    SettingsRow("Warranties", "Receipts with a warranty end date", Icons.Outlined.VerifiedUser) { onWarranties() }
                    SettingsRow("Budgets & goals", "Budgets, savings goals and your salary plan", Icons.Outlined.TrackChanges, divider = false) { onPlanning() }
                }
            }
            item(key = "notif") {
                SettingsSection("Notifications") {
                    SettingsToggle("Bill reminders", "3 days and 1 day before the due date", state.notifyBills, onNotifyBills, Icons.Outlined.Alarm)
                    SettingsToggle("Important insights", "Overspending and unusual spends", state.notifyInsights, onNotifyInsights, Icons.Outlined.Bolt, divider = false)
                }
            }
            item(key = "period") {
                SettingsSection("Budget month") {
                    SettingsPickerRow(
                        "Month starts on",
                        (1..28).toList(),
                        state.monthStartDay,
                        { if (it == 1) "1st (calendar month)" else ordinal(it) + " (e.g. salary day)" },
                        Icons.Outlined.CalendarMonth,
                        divider = state.monthStartDay > 1,
                    ) { onMonthStartDay(it) }
                    payday()
                }
            }
            item(key = "footer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
