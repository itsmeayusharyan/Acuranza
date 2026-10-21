package com.acuranza.app.notify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.acuranza.app.AcuranzaApp
import com.acuranza.app.data.sms.SmsNotificationFilter

/**
 * Automatic SMS capture, permissionless by design: when the user enables it (explicit consent +
 * the system's Notification Access grant), Acuranza reads the notifications the SMS app posts for
 * incoming messages and runs them through the exact same pipeline as a shared message. This is
 * the only Android mechanism that detects new bank SMS on arrival without the SMS permission —
 * which Play Protect hard-blocks on sideloaded installs.
 *
 * The service is deliberately inert unless all three hold: consent, the in-app toggle, and the
 * system grant. Anything the pipeline rejects is discarded immediately; accepted candidates are
 * parked for the user's review. Message text is never stored.
 */
class SmsNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val container = (applicationContext as? AcuranzaApp)?.container ?: return
        if (container.decoy || sbn == null) return
        if (!SmsNotificationFilter.isSmsPackage(sbn.packageName)) return

        // No consent, no toggle, no processing — checked on every notification, so withdrawing
        // consent stops capture instantly without waiting for the binding to change.
        val settings = kotlinx.coroutines.runBlocking { container.settings.current() }
        if (!settings.smsConsent || !settings.smsAutoCapture) {
            // Best effort: release the binding too, so the system stops delivering entirely.
            runCatching { requestUnbind() }
            return
        }

        val text = SmsNotificationFilter.bodyOf(
            sbn.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT),
            sbn.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT),
        ) ?: return

        // Same pipeline as share/paste: reject auth/promo/personal/ambiguous text, keep only
        // extracted transaction fields, park for review.
        kotlinx.coroutines.runBlocking {
            runCatching { container.smsImports.addPending(text, sbn.postTime) }
        }
    }
}
