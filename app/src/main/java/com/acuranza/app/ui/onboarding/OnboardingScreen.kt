package com.acuranza.app.ui.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.BankBadge
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.Bank
import com.acuranza.app.ui.accounts.BankPicker
import com.acuranza.app.ui.common.AmountField
import com.acuranza.app.ui.common.FormColumn
import com.acuranza.app.ui.common.AcuranzaTextField
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.designsystem.theme.AcuranzaText
import kotlinx.coroutines.launch

private const val STEPS = 6

/** The one step that cannot be skipped: the notification permission is required before the app opens. */
private const val NOTIFICATIONS_STEP = 3

@Composable
fun OnboardingScreen() {
    val slideSpec = com.acuranza.app.designsystem.theme.motionTween<androidx.compose.ui.unit.IntOffset>(com.acuranza.app.designsystem.theme.Motion.EMPHASIZED)
    val fadeSpec = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.STANDARD)
    val fadeSpecOut = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.FAST)
    val c = LocalContainer.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val finish: () -> Unit = { scope.launch { c.settings.setOnboardingDone(true) } }
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDots(step, STEPS, Modifier.weight(1f))
            // The notification step is the gate: it is the only one without a Skip.
            if (step in 1 until STEPS - 1 && step != NOTIFICATIONS_STEP) TextButton(onClick = { step++ }) { Text("Skip") }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(slideSpec) { if (forward) it / 3 else -it / 3 } + fadeIn(fadeSpec)) togetherWith
                    (slideOutHorizontally(slideSpec) { if (forward) -it / 3 else it / 3 } + fadeOut(fadeSpecOut))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f),
        ) { s ->
            when (s) {
                0 -> Welcome { step = 1 }
                1 -> AccountsStep(card = false) { step = 2 }
                2 -> AccountsStep(card = true) { step = 3 }
                3 -> NotificationsStep { step = 4 }
                4 -> ImportStep { step = 5 }
                else -> DoneStep(finish)
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == step) 22.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (i <= step) MaterialTheme.colorScheme.primary else AcuranzaTheme.colors.divider,
                label = "dotColor",
            )
            Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun StepPage(
    title: String,
    subtitle: String,
    primary: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (icon != null) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AcuranzaTheme.colors.ink),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = AcuranzaTheme.colors.onInk) }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = AcuranzaText.screenTitle)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AcuranzaTheme.colors.subtleText)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(primary, onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.fillMaxWidth())
        if (secondary != null && onSecondary != null) {
            TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondary) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        AcuranzaLogo(size = 108.dp)
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Acuranza", style = AcuranzaText.screenTitle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Your expenses, income and cards in one private place.",
            style = MaterialTheme.typography.bodyLarge, color = AcuranzaTheme.colors.subtleText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        ValueProp(Icons.Outlined.Edit, "Fast entry", "Add an expense in seconds: calculator keypad, splits, tags and receipts.")
        ValueProp(Icons.Outlined.AccountBalanceWallet, "Plan with confidence", "Budgets, goals and safe-to-spend pace, computed on your phone.")
        ValueProp(Icons.Outlined.Lock, "Private by design", "Everything stays on your phone. No account, no cloud, no tracking.")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Get started", onClick = onNext, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ValueProp(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
        }
    }
}

@Composable
private fun AccountsStep(card: Boolean, onNext: () -> Unit) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val all by c.accounts.observeAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val added = all.filter { it.isCard == card }
    var bank by remember { mutableStateOf(Bank.HDFC) }
    var name by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf<Long?>(null) }
    var limit by remember { mutableStateOf<Long?>(null) }
    var statementDay by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    val defaultName = "${bank.shortName} ${if (card) "Credit Card" else "Savings"}"

    StepPage(
        title = if (card) "Your credit cards" else "Your bank accounts",
        subtitle = if (card) {
            "Add each card with its limit and dates, so Acuranza can track dues and remind you before bills are due."
        } else {
            "Add the accounts you use with today's balance. The last 4 digits let Acuranza match bank SMS to each account."
        },
        primary = if (added.isEmpty()) "Continue" else "Continue with ${added.size}",
        onPrimary = onNext,
    ) {
        added.forEach { a ->
            AcuranzaCard(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BankBadge(a.bank, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(a.account.label(), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    AmountText(if (card) a.outstanding else a.balance, style = AmountStyles.small)
                }
            }
        }
        FormColumn {
            BankPicker(bank) { bank = it }
            AcuranzaTextField(name, { name = it }, "Name", supporting = "Leave empty for “$defaultName”")
            AcuranzaTextField(last4, { last4 = it.filter(Char::isDigit).take(4) }, "Last 4 digits", keyboardType = KeyboardType.Number)
            AmountField(if (card) "Amount owed today" else "Balance today", amount, { amount = it })
            if (card) {
                AmountField("Credit limit", limit, { limit = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AcuranzaTextField(statementDay, { statementDay = it.filter(Char::isDigit).take(2) }, "Statement day", Modifier.weight(1f),
                        keyboardType = KeyboardType.Number)
                    AcuranzaTextField(dueDay, { dueDay = it.filter(Char::isDigit).take(2) }, "Due day", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
            }
            val addGuard = remember { com.acuranza.app.ui.common.ActionGuard() }
            OutlinedButton(
                onClick = onClick@{
                    if (!addGuard.tryStart()) return@onClick
                    haptic()
                    scope.launch {
                        try {
                            c.accounts.add(
                                AccountEntity(
                                    name = name.trim().ifEmpty { defaultName },
                                    type = if (card) AccountType.CREDIT_CARD else AccountType.BANK,
                                    bank = bank.name,
                                    last4 = last4.takeIf { it.length == 4 },
                                    openingBalance = amount ?: 0,
                                    creditLimit = if (card) limit else null,
                                    statementDay = statementDay.toIntOrNull()?.coerceIn(1, 31),
                                    dueDay = dueDay.toIntOrNull()?.coerceIn(1, 31),
                                    colorArgb = bank.brandColor,
                                ),
                            )
                            name = ""
                            last4 = ""
                            amount = null
                            limit = null
                            statementDay = ""
                            dueDay = ""
                        } finally {
                            addGuard.finish()
                        }
                    }
                },
                enabled = last4.isEmpty() || last4.length == 4,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (card) "Add card" else "Add account") }
        }
    }
}

/**
 * The one mandatory step: notifications. The primary button asks for the permission until it is
 * granted; a denial shows the settings path instead of letting the user into the app.
 */
@Composable
private fun NotificationsStep(onNext: () -> Unit) {
    val granted by rememberNotificationPermission()
    val request = rememberNotificationRequest()

    StepPage(
        title = "Turn on notifications",
        subtitle = "Acuranza is offline, so notifications are how it reaches you: bill due dates, unusual spending, " +
            "warranties ending and payments you scheduled — all composed on your phone.",
        primary = if (granted) "Continue" else "Allow notifications",
        onPrimary = { if (granted) onNext() else request() },
        icon = Icons.Outlined.NotificationsActive,
    ) {
        NotificationReasons()
        if (granted) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AcuranzaTheme.colors.good)
                Spacer(Modifier.width(8.dp))
                Text("Notifications are on", fontWeight = FontWeight.Medium)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            NotificationDeniedHelp(Modifier.fillMaxWidth())
        }
    }
}


@Composable
private fun ImportStep(onNext: () -> Unit) {
    StepPage(
        title = "Your history",
        subtitle = "Acuranza never reads your messages. Bring your past along by importing a bank statement CSV, or start fresh and add transactions as they happen.",
        primary = "Continue",
        onPrimary = onNext,
        icon = Icons.Outlined.AutoAwesome,
    ) {
        ValueProp(Icons.Outlined.Edit, "Quick manual entry", "A calculator keypad with tags, splits and receipts attached.")
        ValueProp(Icons.Outlined.Assessment, "Bank statement import", "CSV files from HDFC, ICICI, SBI, Axis, Kotak and more — from More › Import statement.")
        ValueProp(Icons.Outlined.Autorenew, "Recurring on autopilot", "Rent, SIPs and subscriptions you schedule add themselves on their dates.")
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    val c = LocalContainer.current
    val accounts by c.accounts.observeAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.7f))
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AcuranzaTheme.colors.good, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text("You're all set", style = AcuranzaText.screenTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            "${accounts.size} account${if (accounts.size == 1) "" else "s"} ready. Add transactions as you go, or import a bank statement CSV from More.",
            style = MaterialTheme.typography.bodyLarge,
            color = AcuranzaTheme.colors.subtleText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        accounts.take(4).forEach { a ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BankBadge(a.account.bankEnum(), size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Text(a.account.label())
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Open Acuranza", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
