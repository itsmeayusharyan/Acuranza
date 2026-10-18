package com.acuranza.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.acuranza.app.data.prefs.Settings
import com.acuranza.app.data.prefs.ThemeMode
import com.acuranza.app.di.AppContainer
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.BrandPalette
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.TxnType
import com.acuranza.app.notify.Notifications
import com.acuranza.app.security.BiometricGate
import com.acuranza.app.security.UnlockResult
import com.acuranza.app.ui.accounts.AccountDetailScreen
import com.acuranza.app.ui.accounts.AccountEditScreen
import com.acuranza.app.ui.accounts.AccountsScreen
import com.acuranza.app.ui.bills.BillsScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.home.HomeScreen
import com.acuranza.app.ui.insights.InsightsScreen
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.TabRoutes
import com.acuranza.app.ui.nav.Tabs
import com.acuranza.app.ui.onboarding.OnboardingScreen
import com.acuranza.app.ui.planning.PlanningScreen
import com.acuranza.app.ui.settings.CategoriesScreen
import com.acuranza.app.ui.settings.MoreScreen
import com.acuranza.app.ui.transactions.AddTransactionScreen
import com.acuranza.app.ui.transactions.ReviewScreen
import com.acuranza.app.ui.transactions.TransactionDetailScreen
import com.acuranza.app.ui.transactions.TransactionsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.navigateToTab

/** Root of the UI: theme, splash, app lock, onboarding and navigation. */
@Composable
fun AcuranzaRoot(container: AppContainer, initialRoute: String?, onRouteConsumed: () -> Unit) {
    val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = settingsState
    val dark = when (settings?.themeMode ?: ThemeMode.DARK) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
            navigationBarStyle = if (dark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
        )
        onDispose {}
    }

    AcuranzaTheme(darkTheme = dark) {
        CompositionLocalProvider(
            LocalContainer provides container,
            LocalSettings provides (settings ?: Settings()),
            LocalHideAmounts provides (settings?.hideAmounts ?: false),
        ) {
            var splashDone by remember { mutableStateOf(AppLock.splashShown) }
            LaunchedEffect(Unit) {
                if (!AppLock.splashShown) {
                    delay(700)
                    AppLock.splashShown = true
                }
                splashDone = true
            }
            val lockEnabled = settings?.biometricLockEnabled == true
            val unlocked by AppLock.unlocked.collectAsStateWithLifecycle()

            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner, lockEnabled) {
                val obs = LifecycleEventObserver { _, e ->
                    when (e) {
                        Lifecycle.Event.ON_STOP -> AppLock.onBackground()
                        Lifecycle.Event.ON_START -> AppLock.onForeground(lockEnabled)
                        else -> Unit
                    }
                }
                owner.lifecycle.addObserver(obs)
                onDispose { owner.lifecycle.removeObserver(obs) }
            }

            // Acuranza requires notification permission before the main app: reminders are a core feature,
            // so a missing permission keeps the user on the gate instead of inside the app.
            var notifGranted by com.acuranza.app.ui.onboarding.rememberNotificationPermission()
            val stage = when {
                settings == null || !splashDone -> Stage.Splash
                !settings.onboardingDone -> Stage.Onboarding
                !notifGranted -> Stage.Gate
                else -> Stage.Main
            }
            val locked = lockEnabled && !unlocked && stage != Stage.Splash

            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                // The lock is drawn over the app instead of replacing it, so re-locking after a minute away keeps
                // the back stack and any half-filled form. While locked the app underneath is invisible to the eye
                // and to accessibility services.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (locked) Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { } else Modifier),
                ) {
                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "stage",
                    ) { s ->
                        when (s) {
                            Stage.Splash -> Splash()
                            Stage.Onboarding -> OnboardingScreen()
                            Stage.Gate -> com.acuranza.app.ui.onboarding.NotificationGateScreen()
                            Stage.Main -> MainShell(initialRoute, onRouteConsumed)
                        }
                    }
                }
                if (locked) LockScreen(onUnlocked = { AppLock.markUnlocked() })
            }
        }
    }
}

private enum class Stage { Splash, Onboarding, Gate, Main }

@Composable
private fun Splash() {
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }
    val alpha by animateFloatAsState(if (start) 1f else 0f, tween(300), label = "splash")
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        ) {
            AcuranzaLogo(size = 88.dp, shape = LogoShape.RoundedSquare)
            Spacer(Modifier.height(12.dp))
            Text("Acuranza", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = AcuranzaTheme.colors.ink)
        }
    }
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(BiometricGate.isAvailable(context)) }
    // App PIN / decoy PIN (see ui.tools.PinLockPad); biometrics always open the real profile.
    val toolsPrefs by remember { com.acuranza.app.tools.ToolsSettings.get(context).prefs }.collectAsStateWithLifecycle(initialValue = null)
    val realTheme = com.acuranza.app.ui.common.LocalSettings.current.themeMode
    val unlock: () -> Unit = unlock@{
        val activity = context as? FragmentActivity ?: return@unlock
        available = BiometricGate.isAvailable(context)
        if (!available) {
            // Fail closed: with no screen lock on the phone there is nothing to verify the user with.
            message = "Set a screen lock (PIN, pattern or fingerprint) on your phone to unlock Acuranza."
            return@unlock
        }
        scope.launch {
            when (val r = BiometricGate.authenticate(activity)) {
                UnlockResult.Success -> {
                    com.acuranza.app.security.DecoySession.deactivate()
                    onUnlocked()
                }
                is UnlockResult.Error -> message = r.message
            }
        }
    }
    // With an app PIN set the PIN pad shows first, so a decoy PIN can be entered without a prompt in the way.
    LaunchedEffect(toolsPrefs?.pinSet) { if (toolsPrefs?.pinSet == false) unlock() }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AcuranzaLogo(size = 72.dp, shape = LogoShape.RoundedSquare)
            Text("Acuranza is locked", style = MaterialTheme.typography.titleLarge, color = AcuranzaTheme.colors.ink)
            message?.let { Text(it, color = AcuranzaTheme.colors.subtleText, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
            val pinPrefs = toolsPrefs
            if (pinPrefs?.pinSet == true) {
                com.acuranza.app.ui.tools.PinLockPad(
                    prefs = pinPrefs,
                    onMatch = { match ->
                        scope.launch {
                            if (match == com.acuranza.app.security.PinMatch.DECOY) {
                                com.acuranza.app.security.DecoySession.activate(context, realTheme)
                            } else {
                                com.acuranza.app.security.DecoySession.deactivate()
                            }
                            onUnlocked()
                        }
                    },
                    onBiometric = if (available) unlock else null,
                )
            } else {
                PrimaryButton(text = "Unlock", onClick = unlock, icon = Icons.Outlined.Fingerprint, modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth())
            }
            if (!available && pinPrefs?.pinSet != true) {
                androidx.compose.material3.TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("Open security settings") }
            }
        }
    }
}

@Composable
private fun MainShell(initialRoute: String?, onRouteConsumed: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in TabRoutes

    LaunchedEffect(initialRoute) {
        val target = when (initialRoute) {
            Notifications.ROUTE_REVIEW -> Routes.REVIEW
            Notifications.ROUTE_BILLS -> Routes.BILLS
            Notifications.ROUTE_INSIGHTS -> Routes.INSIGHTS
            Notifications.ROUTE_WARRANTIES -> Routes.WARRANTIES
            Notifications.ROUTE_RECURRING -> Routes.RECURRING
            com.acuranza.app.tools.QuickLaunch.ROUTE_ADD_EXPENSE -> Routes.addTxn(TxnType.EXPENSE)
            com.acuranza.app.tools.QuickLaunch.ROUTE_ADD_INCOME -> Routes.addTxn(TxnType.INCOME)
            else -> null
        }
        if (target != null) {
            if (target in TabRoutes) nav.navigateToTab(target) else nav.navigate(target) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(showBar, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                BottomBar(nav, current)
            }
        },
    ) { padding ->
        Box(Modifier.padding(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp)) {
            AcuranzaNavHost(nav)
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: String?) {
    AcuranzaBottomBar(current, onTab = { route -> nav.navigateToTab(route) }, onAdd = { nav.navigate(Routes.addTxn()) })
}

/** Circular icon destinations flanking a raised ink add-circle, on a hairline-topped surface. */
@Composable
fun AcuranzaBottomBar(current: String?, onTab: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(AcuranzaTheme.colors.cardSurface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AcuranzaTheme.colors.hairline))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tabs.forEachIndexed { index, tab ->
                if (index == 2) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AcuranzaTheme.colors.ink)
                                .border(1.dp, AcuranzaTheme.colors.hairline, CircleShape)
                                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAdd)
                                .semantics { contentDescription = "Add transaction" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = AcuranzaTheme.colors.onInk, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                val selected = current == tab.route
                val tint = if (selected) AcuranzaTheme.colors.ink else AcuranzaTheme.colors.tertiaryText
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = androidx.compose.ui.semantics.Role.Tab,
                        ) {
                            if (!selected) onTab(tab.route)
                        }
                        .semantics {
                            this.selected = selected
                            contentDescription = tab.label
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) AcuranzaTheme.colors.accent.copy(alpha = 0.14f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AcuranzaNavHost(nav: NavHostController) {
    val idArg = listOf(navArgument("id") { type = NavType.LongType })
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = {
            if (targetState.destination.route in TabRoutes) fadeIn(tween(220))
            else slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            if (initialState.destination.route in TabRoutes) fadeOut(tween(180))
            else slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(260))
        },
    ) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.TRANSACTIONS) { TransactionsScreen(nav) }
        composable(Routes.PLANNING) { PlanningScreen(nav) }
        composable(Routes.SETTINGS) { MoreScreen(nav) }

        composable(Routes.REVIEW) { ReviewScreen(nav) }
        composable(
            Routes.ADD_TXN,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType; defaultValue = TxnType.EXPENSE.name },
                navArgument("account") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val type = runCatching { TxnType.valueOf(entry.arguments?.getString("type") ?: "") }.getOrDefault(TxnType.EXPENSE)
            val account = entry.arguments?.getLong("account")?.takeIf { it > 0 }
            AddTransactionScreen(nav, editId = null, initialType = type, initialAccountId = account)
        }
        composable(Routes.EDIT_TXN, arguments = idArg) { entry ->
            AddTransactionScreen(nav, editId = entry.arguments?.getLong("id"), initialType = TxnType.EXPENSE, initialAccountId = null)
        }
        composable(Routes.TXN_DETAIL, arguments = idArg) { entry -> TransactionDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(Routes.ACCOUNTS) { AccountsScreen(nav) }
        composable(Routes.ACCOUNT_DETAIL, arguments = idArg) { entry -> AccountDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(
            Routes.ACCOUNT_EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("card") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            AccountEditScreen(
                nav,
                editId = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                defaultCard = entry.arguments?.getBoolean("card") ?: false,
            )
        }
        composable(Routes.BILLS) { BillsScreen(nav) }
        composable(Routes.INSIGHTS) { InsightsScreen(nav) }
        composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
        composable(Routes.PENDING_ACCOUNTS) { com.acuranza.app.ui.accounts.PendingAccountsScreen(nav) }
        composable(Routes.UNCATEGORISED) { com.acuranza.app.ui.transactions.UncategorisedScreen(nav) }
        composable(Routes.RECURRING) { com.acuranza.app.ui.features.RecurringScreen(nav) }
        composable(
            Routes.RECURRING_EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("series") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            com.acuranza.app.ui.features.RecurringEditScreen(
                nav,
                entry.arguments!!.getLong("id").takeIf { it > 0 },
                entry.arguments!!.getString("series")?.takeIf { it.isNotBlank() },
            )
        }
        composable(Routes.LOANS) { com.acuranza.app.ui.features.LoansScreen(nav) }
        composable(Routes.LOAN_DETAIL, arguments = idArg) { entry -> com.acuranza.app.ui.features.LoanDetailScreen(nav, entry.arguments!!.getLong("id")) }
        composable(Routes.LOAN_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { entry ->
            com.acuranza.app.ui.features.LoanEditScreen(nav, entry.arguments!!.getLong("id").takeIf { it > 0 })
        }
        composable(Routes.DEBTS) { com.acuranza.app.ui.features.DebtsScreen(nav) }
        composable(Routes.AUTO_BACKUP) { com.acuranza.app.ui.features.AutoBackupScreen(nav) }
        composable(Routes.STATEMENT_IMPORT) { com.acuranza.app.ui.features.StatementImportScreen(nav) }
        composable(Routes.REPORTS) { com.acuranza.app.ui.features.ReportsScreen(nav) }
        composable(Routes.YEAR_REVIEW, arguments = listOf(navArgument("year") { type = NavType.IntType })) { entry ->
            com.acuranza.app.ui.features.YearReviewScreen(nav, entry.arguments!!.getInt("year"))
        }
        composable(Routes.CALENDAR) { com.acuranza.app.ui.features.CalendarScreen(nav) }
        composable(Routes.WARRANTIES) { com.acuranza.app.ui.features.WarrantiesScreen(nav) }
    }
}

