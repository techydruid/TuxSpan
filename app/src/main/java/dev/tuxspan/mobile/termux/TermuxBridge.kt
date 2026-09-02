package dev.tuxspan.mobile.termux

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import dev.tuxspan.mobile.platform.CompanionInspector
import dev.tuxspan.mobile.workspace.ExperienceSettings
import java.util.concurrent.atomic.AtomicInteger

data class CommandRequest(
    val tag: String,
    val label: String,
    val description: String,
    val script: String,
    val background: Boolean,
    val collectResult: Boolean = background,
)

@SuppressLint("SdCardPath")
object TermuxContract {
    const val SERVICE = "com.termux.app.RunCommandService"
    const val ACTION = "com.termux.RUN_COMMAND"
    const val PATH = "com.termux.RUN_COMMAND_PATH"
    const val ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    const val LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
    const val DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"
    const val PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    const val RESULT_BUNDLE = "result"
    const val RESULT_STDOUT = "stdout"
    const val RESULT_STDERR = "stderr"
    const val RESULT_EXIT_CODE = "exitCode"
    const val RESULT_ERROR_MESSAGE = "errmsg"

    const val BASH = "/data/data/com.termux/files/usr/bin/bash"
    const val HOME = "/data/data/com.termux/files/home"
}

class TermuxBridge(private val context: Context) {
    fun dispatch(request: CommandRequest): Result<Int> = runCatching {
        check(isPackageInstalled(CompanionInspector.TERMUX_PACKAGE)) {
            "Termux is not installed."
        }
        check(
            context.checkSelfPermission(CompanionInspector.RUN_COMMAND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED,
        ) {
            "TuxSpan does not have Termux's Run command permission."
        }

        val requestId = nextRequestId.getAndIncrement()
        val intent = Intent()
            .setClassName(CompanionInspector.TERMUX_PACKAGE, TermuxContract.SERVICE)
            .setAction(TermuxContract.ACTION)
            .putExtra(TermuxContract.PATH, TermuxContract.BASH)
            .putExtra(TermuxContract.ARGUMENTS, arrayOf("-lc", request.script))
            .putExtra(TermuxContract.WORKDIR, TermuxContract.HOME)
            .putExtra(TermuxContract.BACKGROUND, request.background)
            .putExtra(TermuxContract.SESSION_ACTION, "0")
            .putExtra(TermuxContract.LABEL, request.label)
            .putExtra(TermuxContract.DESCRIPTION, request.description)

        if (request.collectResult) {
            val callbackIntent = Intent(context, CommandResultService::class.java)
                .putExtra(CommandResultService.EXTRA_REQUEST_ID, requestId)
                .putExtra(CommandResultService.EXTRA_TAG, request.tag)
            val callback = PendingIntent.getService(
                context,
                requestId,
                callbackIntent,
                PendingIntent.FLAG_ONE_SHOT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                },
            )
            intent.putExtra(TermuxContract.PENDING_INTENT, callback)
        }

        context.startService(intent)
        requestId
    }

    fun openTermux(): Boolean = openPackage(CompanionInspector.TERMUX_PACKAGE)

    fun openTermuxStorageSettings(): Boolean {
        val packageUri = "package:${CompanionInspector.TERMUX_PACKAGE}".toUri()
        val allFilesIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }
        if (allFilesIntent != null && allFilesIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(allFilesIntent)
            return true
        }
        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (detailsIntent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(detailsIntent)
        return true
    }

    fun openX11(): Boolean = openPackage(CompanionInspector.X11_PACKAGE)

    fun applyX11Preferences(settings: ExperienceSettings): Result<Unit> = runCatching {
        requireX11()
        val preferences = linkedMapOf(
            "fullscreen" to "true",
            "forceOrientation" to settings.screenOrientation.x11Value,
            "hideCutout" to "false",
            "displayResolutionMode" to "native",
            "displayScale" to "100",
            "touchMode" to settings.touchMode.x11Value,
            "showMouseHelper" to "false",
            "showAdditionalKbd" to "true",
            "showIMEWhileExternalConnected" to "false",
            "backButtonAction" to "toggle soft keyboard",
            "swipeUpAction" to "toggle additional key bar",
            "swipeDownAction" to "no action",
            "clipboardEnable" to "true",
            "screenIdleTimeout" to settings.performancePreset.idleTimeout,
        )
        val intent = Intent(X11_CHANGE_PREFERENCE_ACTION)
            .setPackage(CompanionInspector.X11_PACKAGE)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        preferences.forEach(intent::putExtra)
        context.sendBroadcast(intent)
    }

    fun prepareX11Controls(): Result<Unit> = runCatching {
        requireX11()
        context.sendBroadcast(
            Intent(X11_CHANGE_PREFERENCE_ACTION)
                .setPackage(CompanionInspector.X11_PACKAGE)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("showAdditionalKbd", "true"),
        )
    }

    fun setX11ControlsVisible(visible: Boolean): Result<Unit> = runCatching {
        requireX11()
        context.sendBroadcast(
            Intent(X11_CHANGE_PREFERENCE_ACTION)
                .setPackage(CompanionInspector.X11_PACKAGE)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("additionalKbdVisible", visible.toString()),
        )
    }

    fun refreshX11Controls(): Result<Unit> = runCatching {
        requireX11()
        context.sendBroadcast(
            Intent(X11_CHANGE_PREFERENCE_ACTION)
                .setPackage(CompanionInspector.X11_PACKAGE)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("showMouseHelper", "false"),
        )
    }

    fun toggleX11Controls(): Result<Unit> = runCatching {
        requireX11()
        context.sendBroadcast(
            Intent(X11_CUSTOM_ACTION)
                .setPackage(CompanionInspector.X11_PACKAGE)
                .putExtra("what", "swipeUp"),
        )
    }

    private fun openPackage(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    private fun requireX11() {
        check(isPackageInstalled(CompanionInspector.X11_PACKAGE)) {
            "Termux:X11 is not installed."
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val X11_CHANGE_PREFERENCE_ACTION = "com.termux.x11.CHANGE_PREFERENCE"
        const val X11_CUSTOM_ACTION = "com.termux.x11.ACTION_CUSTOM"
        val nextRequestId = AtomicInteger(10_000)
    }
}
