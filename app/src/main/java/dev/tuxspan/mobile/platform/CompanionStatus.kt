package dev.tuxspan.mobile.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

data class CompanionStatus(
    val termuxInstalled: Boolean,
    val x11Installed: Boolean,
    val runCommandGranted: Boolean,
) {
    val ready: Boolean = termuxInstalled && runCommandGranted
}

object CompanionInspector {
    const val TERMUX_PACKAGE = "com.termux"
    const val X11_PACKAGE = "com.termux.x11"
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val TERMUX_URL = "https://f-droid.org/packages/com.termux/"
    const val X11_URL = "https://github.com/termux/termux-x11/releases/tag/nightly"

    fun inspect(context: Context): CompanionStatus = CompanionStatus(
        termuxInstalled = context.isInstalled(TERMUX_PACKAGE),
        x11Installed = context.isInstalled(X11_PACKAGE),
        runCommandGranted = context.checkSelfPermission(RUN_COMMAND_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED,
    )

    fun openUrl(context: Context, url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun Context.isInstalled(packageName: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
