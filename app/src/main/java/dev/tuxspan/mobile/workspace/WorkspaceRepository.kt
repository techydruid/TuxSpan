package dev.tuxspan.mobile.workspace

import android.content.Context
import androidx.core.content.edit

enum class WorkspacePhase {
    NONE,
    INSTALL_DISPATCHED,
    NEEDS_REPAIR,
    READY,
}

data class SavedWorkspace(
    val recipeId: String,
    val phase: WorkspacePhase,
    val installStartedAtMillis: Long? = null,
)

class WorkspaceRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateSingleWorkspaceRecord()
    }

    fun loadAll(): List<SavedWorkspace> = workspaceIds().mapNotNull(::load)

    fun load(recipeId: String): SavedWorkspace? {
        if (recipeId !in workspaceIds()) return null
        val phase = runCatching {
            WorkspacePhase.valueOf(
                preferences.getString(phaseKey(recipeId), WorkspacePhase.NONE.name).orEmpty(),
            )
        }.getOrDefault(WorkspacePhase.NONE)
        val startedAt = preferences.getLong(startedKey(recipeId), 0L).takeIf { it > 0L }
        return SavedWorkspace(recipeId, phase, startedAt)
    }

    fun activeRecipeId(): String? = preferences.getString(KEY_ACTIVE_RECIPE, null)
        ?.takeIf { it in workspaceIds() }

    fun save(
        recipeId: String,
        phase: WorkspacePhase,
        installStartedAtMillis: Long? = null,
        makeActive: Boolean = true,
    ) {
        val ids = workspaceIds() + recipeId
        preferences.edit {
            putStringSet(KEY_WORKSPACE_IDS, ids)
            putString(phaseKey(recipeId), phase.name)
            if (phase == WorkspacePhase.INSTALL_DISPATCHED) {
                putLong(
                    startedKey(recipeId),
                    installStartedAtMillis
                        ?: preferences.getLong(startedKey(recipeId), 0L).takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                )
            } else {
                remove(startedKey(recipeId))
            }
            if (makeActive || activeRecipeId() == null) putString(KEY_ACTIVE_RECIPE, recipeId)
        }
    }

    fun setActive(recipeId: String): Boolean {
        val workspace = load(recipeId) ?: return false
        if (workspace.phase != WorkspacePhase.READY) return false
        preferences.edit { putString(KEY_ACTIVE_RECIPE, recipeId) }
        return true
    }

    fun remove(recipeId: String) {
        val remaining = workspaceIds() - recipeId
        preferences.edit {
            putStringSet(KEY_WORKSPACE_IDS, remaining)
            remove(phaseKey(recipeId))
            remove(startedKey(recipeId))
            if (preferences.getString(KEY_ACTIVE_RECIPE, null) == recipeId) {
                val replacement = remaining.sorted().firstOrNull()
                if (replacement == null) remove(KEY_ACTIVE_RECIPE)
                else putString(KEY_ACTIVE_RECIPE, replacement)
            }
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    private fun workspaceIds(): Set<String> =
        preferences.getStringSet(KEY_WORKSPACE_IDS, emptySet()).orEmpty().toSet()

    private fun migrateSingleWorkspaceRecord() {
        if (workspaceIds().isNotEmpty()) return
        val recipeId = preferences.getString(LEGACY_KEY_RECIPE, null) ?: return
        val phase = preferences.getString(LEGACY_KEY_PHASE, WorkspacePhase.NONE.name).orEmpty()
        val startedAt = preferences.getLong(LEGACY_KEY_INSTALL_STARTED_AT, 0L)
        preferences.edit {
            putStringSet(KEY_WORKSPACE_IDS, setOf(recipeId))
            putString(KEY_ACTIVE_RECIPE, recipeId)
            putString(phaseKey(recipeId), phase)
            if (startedAt > 0L) putLong(startedKey(recipeId), startedAt)
            remove(LEGACY_KEY_RECIPE)
            remove(LEGACY_KEY_PHASE)
            remove(LEGACY_KEY_INSTALL_STARTED_AT)
        }
    }

    private fun phaseKey(recipeId: String) = "workspace_${recipeId}_phase"
    private fun startedKey(recipeId: String) = "workspace_${recipeId}_started_at"

    private companion object {
        const val PREFS_NAME = "tuxspan_workspace"
        const val KEY_WORKSPACE_IDS = "workspace_ids"
        const val KEY_ACTIVE_RECIPE = "active_recipe_id"
        const val LEGACY_KEY_RECIPE = "recipe_id"
        const val LEGACY_KEY_PHASE = "phase"
        const val LEGACY_KEY_INSTALL_STARTED_AT = "install_started_at"
    }
}
