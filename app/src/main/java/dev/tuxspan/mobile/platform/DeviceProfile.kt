package dev.tuxspan.mobile.platform

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import java.util.Locale
import kotlin.math.roundToInt

enum class DeviceTier(val label: String, val detail: String) {
    DESKTOP_READY(
        label = "Desktop ready",
        detail = "A full XFCE workspace should be practical on this device.",
    ),
    BALANCED(
        label = "Balanced",
        detail = "Choose Canvas and keep the number of open apps modest.",
    ),
    TERMINAL_FIRST(
        label = "Terminal first",
        detail = "Spark is the safer starting point; desktop performance may be limited.",
    ),
    UNSUPPORTED(
        label = "Unsupported",
        detail = "This device does not meet TuxSpan's Android or ABI requirements.",
    ),
}

data class DeviceProfile(
    val maker: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val primaryAbi: String,
    val is64Bit: Boolean,
    val ramGb: Double,
    val freeStorageGb: Double,
    val hasVulkan: Boolean,
    val socLabel: String,
    val tier: DeviceTier,
    val densityDpi: Int,
    val smallestWidthDp: Int,
) {
    val deviceName: String = listOf(maker, model)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Android device" }
}

object DeviceInspector {
    fun inspect(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val ramGb = memoryInfo.totalMem.toDouble() / GIB

        val stat = StatFs(context.filesDir.absolutePath)
        val freeStorageGb = stat.availableBytes.toDouble() / GIB
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val hasVulkan = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL,
        )
        val supportedAbi = primaryAbi in setOf(
            "arm64-v8a",
            "armeabi-v7a",
            "x86_64",
            "x86",
        )

        val tier = when {
            !supportedAbi -> DeviceTier.UNSUPPORTED
            is64Bit && ramGb >= 5.5 && freeStorageGb >= 10 -> DeviceTier.DESKTOP_READY
            is64Bit && ramGb >= 3.5 && freeStorageGb >= 7 -> DeviceTier.BALANCED
            ramGb >= 1.8 && freeStorageGb >= 3 -> DeviceTier.TERMINAL_FIRST
            else -> DeviceTier.UNSUPPORTED
        }

        return DeviceProfile(
            maker = Build.MANUFACTURER.prettyName(),
            model = Build.MODEL.trim(),
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            primaryAbi = primaryAbi.ifBlank { "Unknown" },
            is64Bit = is64Bit,
            ramGb = ramGb.oneDecimal(),
            freeStorageGb = freeStorageGb.oneDecimal(),
            hasVulkan = hasVulkan,
            socLabel = socLabel(),
            tier = tier,
            densityDpi = context.resources.displayMetrics.densityDpi,
            smallestWidthDp = context.resources.configuration.smallestScreenWidthDp,
        )
    }

    private fun socLabel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return listOf(Build.SOC_MANUFACTURER, Build.SOC_MODEL)
                .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
                .joinToString(" ")
                .ifBlank { Build.HARDWARE }
        }
        return Build.HARDWARE
    }

    private fun String.prettyName(): String = trim()
        .lowercase(Locale.ROOT)
        .replaceFirstChar { it.titlecase(Locale.ROOT) }

    private fun Double.oneDecimal(): Double = (this * 10).roundToInt() / 10.0

    private const val GIB = 1024.0 * 1024.0 * 1024.0
}
