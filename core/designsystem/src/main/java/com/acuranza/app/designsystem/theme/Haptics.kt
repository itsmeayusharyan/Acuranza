package com.acuranza.app.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether app-generated haptic feedback is on. The app shell provides this from the Appearance
 * setting; design-system components can't read the app's settings store directly. Defaults to
 * true so previews and tests compose unchanged (they never perform a haptic anyway).
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * The calculator keypad's own haptic preference, provided by the app shell (master AND keypad
 * switches). Defaults to true so previews and tests compose unchanged and never vibrate anyway.
 */
val LocalKeypadHapticsEnabled = compositionLocalOf { true }
