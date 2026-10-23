package com.acuranza.app.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * The standard build's path to the full build: the user picks an Acuranza APK from storage,
 * the app verifies it is genuinely this app (package name + signing certificate + version),
 * and stages it through Android's PackageInstaller session — the same installer-session flow
 * third-party installers use, where Android's own install confirmation runs and the user
 * stays in control of every step, including any security-check prompt the system raises.
 *
 * Nothing here disables or evades any check: the picked APK's permissions are declared in it,
 * the user confirms the install through Android's dialog, and the security scan runs as usual.
 */
class PackageUpgradeInstaller(private val context: Context) {

    /** What the picked file turned out to be, before anything is staged. */
    sealed interface Verified {
        data class Ok(val versionCode: Long, val versionName: String?) : Verified
        data class Rejected(val reason: PackageUpgradeValidator.Decision) : Verified
    }

    /**
     * The signing certificates of a PackageInfo across API levels: SigningInfo exists only from
     * 28, so 26–27 fall back to the deprecated-but-functional signatures field, with the matching
     * request flag. Without this the in-app installer would see zero certificates on Android 8.x
     * and reject its own legitimate APK.
     */
    @Suppress("DEPRECATION")
    private fun signingCertsOf(info: android.content.pm.PackageInfo): Array<out android.content.pm.Signature> =
        if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty()
        else info.signatures.orEmpty()

    private val signingFlags: Int =
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    /** Reads the picked APK's manifest and checks it against this app. */
    fun verify(apkFile: File): Verified {
        val info = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, signingFlags)
            ?: return Verified.Rejected(PackageUpgradeValidator.Decision.NotAnApk)
        val own = runCatching {
            context.packageManager.getPackageInfo(context.packageName, signingFlags)
        }.getOrNull() ?: return Verified.Rejected(PackageUpgradeValidator.Decision.NotAnApk)

        val pickedCerts = signingCertsOf(info)
        val ownCerts = signingCertsOf(own)
        val signaturesMatch = pickedCerts.isNotEmpty() && pickedCerts.size == ownCerts.size &&
            pickedCerts.zip(ownCerts).all { (a, b) -> a.toByteArray().contentEquals(b.toByteArray()) }

        val decision = PackageUpgradeValidator.decide(
            pickedPackage = info.packageName,
            pickedVersionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            currentPackage = context.packageName,
            currentVersionCode = own.versionCode,
            signaturesMatch = signaturesMatch,
        )
        return if (decision == PackageUpgradeValidator.Decision.Ok) {
            Verified.Ok(
                if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                info.versionName,
            )
        } else {
            Verified.Rejected(decision)
        }
    }

    /**
     * Stages the verified APK as an in-place update through a PackageInstaller session and
     * commits it. Android answers on [InstallResultReceiver]; a pending user action launches
     * the system's install confirmation, where the user — and any security check — has the
     * final word.
     */
    fun install(apkFile: File): Boolean {
        var sessionId = -1
        val ok = runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("acuranza-update", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { it.copyTo(out) }
            }
            val confirm = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).putExtra(EXTRA_SESSION, sessionId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(confirm.intentSender)
            true
        }.getOrDefault(false)
        // The session owns its bytes after commit; the picked copy is dead weight either way.
        runCatching { apkFile.delete() }
        if (!ok && sessionId != -1) runCatching { context.packageManager.packageInstaller.abandonSession(sessionId) }
        return ok
    }

    companion object {
        const val EXTRA_SESSION = "acuranza.session"
        /** Re-set before each install attempt; survives while the process lives. */
        var onResult: ((Int, String?) -> Unit)? = null
    }
}

/** Receives the session results, launches the user-action intent when the system asks. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    runCatching {
                        context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS ->
                PackageUpgradeInstaller.onResult?.invoke(PackageInstaller.STATUS_SUCCESS, null)
            else ->
                PackageUpgradeInstaller.onResult?.invoke(status, message)
        }
    }
}
