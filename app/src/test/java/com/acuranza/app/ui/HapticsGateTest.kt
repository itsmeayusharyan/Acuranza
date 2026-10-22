package com.acuranza.app.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.acuranza.app.data.prefs.Settings
import com.acuranza.app.ui.common.AppHaptics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The gate and strengths of app-generated haptics, verified against a recording fake. */
class HapticsGateTest {

    private class Fake : HapticFeedback {
        val calls = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            calls += hapticFeedbackType
        }
    }

    @Test fun disabledSuppressesEverything() {
        val fake = Fake()
        val haptics = AppHaptics(fake, enabled = false)
        haptics.tick()
        haptics.confirm()
        haptics() // legacy call-site form
        assertEquals(0, fake.calls.size)
    }

    @Test fun enabledUsesSubtleForTapsAndFirmForConfirmations() {
        val fake = Fake()
        val haptics = AppHaptics(fake, enabled = true)
        haptics.tick()
        haptics.confirm()
        assertEquals(listOf(HapticFeedbackType.TextHandleMove, HapticFeedbackType.LongPress), fake.calls)
    }

    @Test fun legacyInvokeIsTheConfirmation() {
        val fake = Fake()
        val haptics = AppHaptics(fake, enabled = true)
        haptics()
        assertEquals(listOf(HapticFeedbackType.LongPress), fake.calls)
    }

    @Test fun hapticsDefaultToOn() {
        assertTrue(Settings().hapticsEnabled)
    }
}
