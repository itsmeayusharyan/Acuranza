package com.acuranza.app.ui.management

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.navigateToTab
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection

/**
 * The Management tab: the accounts-and-data hub that used to be Settings' first section.
 * Insights is a tab destination, so it opens through the tab switch rather than a push.
 */
@Composable
fun ManagementScreen(nav: NavController) {
    ManagementContent(
        onAccounts = { nav.navigate(Routes.ACCOUNTS) },
        onReview = { nav.navigate(Routes.REVIEW) },
        onInsights = { nav.navigateToTab(Routes.INSIGHTS) },
        onCategories = { nav.navigate(Routes.CATEGORIES) },
    )
}

/** Stateless Management tab content, rendered from callbacks; used by the app and by screenshot tests. */
@Composable
fun ManagementContent(
    onAccounts: () -> Unit,
    onReview: () -> Unit,
    onInsights: () -> Unit,
    onCategories: () -> Unit,
) {
    AcuranzaScreen(title = "Management", subtitle = "Accounts, imports, insights and categories") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            item(key = "account") {
                SettingsSection("Account & management") {
                    SettingsRow("Accounts & cards", "Balances, limits and reconciliation", Icons.Outlined.AccountBalance) { onAccounts() }
                    SettingsRow("Review imports", "Confirm imported transactions", Icons.Outlined.Inbox) { onReview() }
                    SettingsRow("Insights", "Spending analysis and unusual activity", Icons.Outlined.QueryStats) { onInsights() }
                    SettingsRow("Categories", "Your spending and income categories", Icons.Outlined.Category, divider = false) { onCategories() }
                }
            }
            item(key = "footer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
