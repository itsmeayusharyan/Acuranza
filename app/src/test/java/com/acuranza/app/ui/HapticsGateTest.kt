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
        haptics.keypadTick()
        haptics() // legacy call-site form
        assertEquals(0, fake.calls.size)
    }

    @Test fun masterOffKeepsCategoriesSilentButChoicesAreOnlyAGateAway() {
        // Individual preferences are stored independently; the master switch suppresses only.
        val fake = Fake()
        val haptics = AppHaptics(fake, enabled = false, keypad = true, actions = true, tabsAndButtons = true)
        haptics.tick(); haptics.confirm(); haptics.keypadTick()
        assertEquals(0, fake.calls.size)
        val restored = AppHaptics(fake, enabled = true, keypad = true, actions = true, tabsAndButtons = true)
        restored.tick(); restored.confirm(); restored.keypadTick()
        assertEquals(3, fake.calls.size)
    }

    @Test fun categorySwitchesSuppressOnlyTheirOwnFeedback() {
        val fake = Fake()
        val haptics = AppHaptics(fake, enabled = true, keypad = false, actions = false, tabsAndButtons = true)
        haptics.tick(); haptics.confirm(); haptics.keypadTick()
        assertEquals(listOf(HapticFeedbackType.TextHandleMove), fake.calls)
    }

    @Test fun strengthMapsToTheTwoAndroidTypes() {
        val light = Fake()
        val hapticsLight = AppHaptics(light, enabled = true, intensity = com.acuranza.app.ui.common.HapticIntensity.LIGHT)
        hapticsLight.tick(); hapticsLight.confirm()
        assertEquals(listOf(HapticFeedbackType.TextHandleMove, HapticFeedbackType.TextHandleMove), light.calls)

        val strong = Fake()
        val hapticsStrong = AppHaptics(strong, enabled = true, intensity = com.acuranza.app.ui.common.HapticIntensity.STRONG)
        hapticsStrong.tick(); hapticsStrong.confirm()
        assertEquals(listOf(HapticFeedbackType.LongPress, HapticFeedbackType.LongPress), strong.calls)
    }

    @Test fun unknownIntensityFallsBackToStandard() {
        assertEquals(com.acuranza.app.ui.common.HapticIntensity.STANDARD, com.acuranza.app.ui.common.HapticIntensity.of("NOPE"))
        assertEquals(com.acuranza.app.ui.common.HapticIntensity.LIGHT, com.acuranza.app.ui.common.HapticIntensity.of("LIGHT"))
        assertEquals(com.acuranza.app.ui.common.HapticIntensity.STANDARD, com.acuranza.app.ui.common.HapticIntensity.of(null))
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
