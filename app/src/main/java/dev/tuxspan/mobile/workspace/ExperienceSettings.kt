package dev.tuxspan.mobile.workspace

import android.content.Context
import androidx.core.content.edit
import kotlin.math.roundToInt

enum class DisplayProfile(val label: String, val description: String) {
    MOBILE("Mobile", "Larger text and controls with a clean bottom dock"),
    DESKTOP("Desktop", "More workspace with a compact bottom dock"),
    ;

    fun adaptiveDpi(androidDensityDpi: Int): Int = when (this) {
        MOBILE -> (androidDensityDpi * 0.42f).roundToInt().coerceIn(144, 192)
        DESKTOP -> (androidDensityDpi * 0.30f).roundToInt().coerceIn(108, 144)
    }

    val cursorSize: Int
        get() = when (this) {
            MOBILE -> 48
            DESKTOP -> 24
        }

}

enum class ScreenOrientation(val label: String, val x11Value: String) {
    LANDSCAPE("Landscape", "landscape"),
    PORTRAIT("Portrait", "portrait"),
    AUTO("Auto-rotate", "auto"),
}

enum class TouchMode(val label: String, val x11Value: String) {
    DIRECT("Direct touch", "3"),
    TOUCHPAD("Touchpad", "1"),
}

enum class PerformancePreset(
    val label: String,
    val description: String,
    val idleTimeout: String,
) {
    BATTERY("Battery", "Lets the display sleep after five minutes", "5"),
    BALANCED("Balanced", "Responsive defaults for everyday use", "system"),
    PERFORMANCE("Performance", "Keeps the display awake during long sessions", "never"),
}

data class ExperienceSettings(
    val displayProfile: DisplayProfile = DisplayProfile.MOBILE,
    val screenOrientation: ScreenOrientation = ScreenOrientation.LANDSCAPE,
    val touchMode: TouchMode = TouchMode.TOUCHPAD,
    val performancePreset: PerformancePreset = PerformancePreset.BALANCED,
    val downloadsBridgeEnabled: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val termuxConsentCompleted: Boolean = false,
    val initialSetupCompleted: Boolean = false,
)

class ExperienceRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ExperienceSettings {
        val saved = ExperienceSettings(
            displayProfile = enumValue(KEY_DISPLAY_PROFILE, DisplayProfile.MOBILE),
            screenOrientation = enumValue(KEY_ORIENTATION, ScreenOrientation.LANDSCAPE),
            touchMode = enumValue(KEY_TOUCH_MODE, TouchMode.TOUCHPAD),
            performancePreset = enumValue(KEY_PERFORMANCE, PerformancePreset.BALANCED),
            downloadsBridgeEnabled = preferences.getBoolean(KEY_DOWNLOADS, false),
            tutorialCompleted = preferences.getBoolean(KEY_TUTORIAL, false),
            termuxConsentCompleted = preferences.getBoolean(KEY_TERMUX_CONSENT, false),
            initialSetupCompleted = preferences.getBoolean(KEY_INITIAL_SETUP, false),
        )
        if (preferences.getInt(KEY_SETTINGS_VERSION, 0) >= CURRENT_SETTINGS_VERSION) return saved

        val migrated = saved.copy(
            displayProfile = DisplayProfile.MOBILE,
        )
        save(migrated)
        return migrated
    }

    fun save(settings: ExperienceSettings) {
        preferences.edit {
            putString(KEY_DISPLAY_PROFILE, settings.displayProfile.name)
            putString(KEY_ORIENTATION, settings.screenOrientation.name)
            putString(KEY_TOUCH_MODE, settings.touchMode.name)
            putString(KEY_PERFORMANCE, settings.performancePreset.name)
            putBoolean(KEY_DOWNLOADS, settings.downloadsBridgeEnabled)
            putBoolean(KEY_TUTORIAL, settings.tutorialCompleted)
            putBoolean(KEY_TERMUX_CONSENT, settings.termuxConsentCompleted)
            putBoolean(KEY_INITIAL_SETUP, settings.initialSetupCompleted)
            putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(preferences.getString(key, fallback.name).orEmpty()) }
            .getOrDefault(fallback)

    private companion object {
        const val PREFS_NAME = "tuxspan_experience"
        const val KEY_DISPLAY_PROFILE = "display_profile"
        const val KEY_ORIENTATION = "screen_orientation"
        const val KEY_TOUCH_MODE = "touch_mode"
        const val KEY_PERFORMANCE = "performance_preset"
        const val KEY_DOWNLOADS = "downloads_bridge"
        const val KEY_TUTORIAL = "tutorial_completed"
        const val KEY_TERMUX_CONSENT = "termux_consent_completed"
        const val KEY_INITIAL_SETUP = "initial_setup_completed"
        const val KEY_SETTINGS_VERSION = "settings_version"
        const val CURRENT_SETTINGS_VERSION = 4
    }
}
