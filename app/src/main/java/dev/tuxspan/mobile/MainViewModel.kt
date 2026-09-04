package dev.tuxspan.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tuxspan.mobile.model.WorkspaceCatalog
import dev.tuxspan.mobile.model.WorkspaceKind
import dev.tuxspan.mobile.model.WorkspaceRecipe
import dev.tuxspan.mobile.platform.CompanionInspector
import dev.tuxspan.mobile.platform.CompanionStatus
import dev.tuxspan.mobile.platform.DeviceInspector
import dev.tuxspan.mobile.platform.DeviceProfile
import dev.tuxspan.mobile.termux.CommandRequest
import dev.tuxspan.mobile.termux.CommandResultBus
import dev.tuxspan.mobile.termux.TermuxBridge
import dev.tuxspan.mobile.workspace.SavedWorkspace
import dev.tuxspan.mobile.workspace.DisplayProfile
import dev.tuxspan.mobile.workspace.ExperienceRepository
import dev.tuxspan.mobile.workspace.ExperienceSettings
import dev.tuxspan.mobile.workspace.InstallProgress
import dev.tuxspan.mobile.workspace.InstallProgressStatus
import dev.tuxspan.mobile.workspace.PerformancePreset
import dev.tuxspan.mobile.workspace.ScreenOrientation
import dev.tuxspan.mobile.workspace.TouchMode
import dev.tuxspan.mobile.workspace.WorkspacePhase
import dev.tuxspan.mobile.workspace.WorkspaceRepository
import dev.tuxspan.mobile.workspace.WorkspaceScripts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val device: DeviceProfile,
    val companions: CompanionStatus,
    val selectedRecipe: WorkspaceRecipe,
    val installedWorkspaces: List<SavedWorkspace>,
    val activeWorkspaceId: String?,
    val experience: ExperienceSettings,
    val storageBridgeReady: Boolean = false,
    val storageBridgeChecking: Boolean = false,
    val sessionStatus: SessionStatus = SessionStatus.STOPPED,
    val sessionError: String? = null,
    val showBuildSheet: Boolean = false,
    val commandPreviewVisible: Boolean = false,
    val activeOperation: String? = null,
    val message: String? = null,
    val buildMessage: String? = null,
    val installProgress: InstallProgress? = null,
    val installingWorkspaceId: String? = null,
    val installingBundleId: String? = null,
) {
    fun workspace(recipeId: String): SavedWorkspace? =
        installedWorkspaces.firstOrNull { it.recipeId == recipeId }

    val activeWorkspace: SavedWorkspace?
        get() = activeWorkspaceId?.let(::workspace)

    val sessionNeedsRepair: Boolean
        get() = sessionError?.contains("desktop packages are incomplete", ignoreCase = true) == true

    val initialSetupRequired: Boolean
        get() = installedWorkspaces.isEmpty() && !experience.initialSetupCompleted

    val coreSetupReady: Boolean
        get() = companions.termuxInstalled &&
            companions.runCommandGranted &&
            experience.termuxConsentCompleted
}

enum class SessionStatus(val label: String) {
    STOPPED("Stopped"),
    STARTING("Starting"),
    RUNNING("Running"),
    STOPPING("Stopping"),
    ERROR("Launch failed"),
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkspaceRepository(application)
    private val experienceRepository = ExperienceRepository(application)
    private val bridge = TermuxBridge(application)
    private val initialWorkspaces = repository.loadAll()
    private val initialActiveRecipeId = repository.activeRecipeId()
        ?: initialWorkspaces.firstOrNull { it.phase == WorkspacePhase.READY }?.recipeId
    private val mutableState = MutableStateFlow(
        MainUiState(
            device = DeviceInspector.inspect(application),
            companions = CompanionInspector.inspect(application),
            selectedRecipe = WorkspaceCatalog.byId(initialActiveRecipeId),
            installedWorkspaces = initialWorkspaces,
            activeWorkspaceId = initialActiveRecipeId,
            installingWorkspaceId = initialWorkspaces
                .firstOrNull { it.phase == WorkspacePhase.INSTALL_DISPATCHED }
                ?.recipeId,
            experience = experienceRepository.load(),
        ),
    )
    val state: StateFlow<MainUiState> = mutableState.asStateFlow()
    private var installProgressJob: Job? = null
    private var consentCheckJob: Job? = null
    private var launchWatchdogJob: Job? = null
    private var displayPrepareWatchdogJob: Job? = null
    private var pendingDisplayRequestId: Int? = null
    private var pendingDisplayRecipe: WorkspaceRecipe? = null
    private var pendingDisplayExperience: ExperienceSettings? = null
    private var pendingDisplayDpi: Int = 144
    private var pendingLaunchRequestId: Int? = null
    private var pendingRestartStopRequestId: Int? = null
    private var pendingStatusRequestId: Int? = null
    private var pendingStatusWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            CommandResultBus.latest.collect { result ->
                when (result.tag) {
                    TAG_INSTALL -> {
                        val recipe = WorkspaceCatalog.byId(
                            mutableState.value.installingWorkspaceId
                                ?: mutableState.value.selectedRecipe.id,
                        )
                        if (result.succeeded && result.stdout.contains("TUXSPAN_INSTALL_READY")) {
                            installProgressJob?.cancel()
                            repository.save(recipe.id, WorkspacePhase.READY)
                            mutableState.update {
                                it.copy(
                                    installedWorkspaces = repository.loadAll(),
                                    activeWorkspaceId = repository.activeRecipeId(),
                                    selectedRecipe = recipe,
                                    activeOperation = null,
                                    installingWorkspaceId = null,
                                    showBuildSheet = false,
                                    message = "${recipe.name} finished installing and is ready to launch.",
                                    buildMessage = null,
                                    installProgress = null,
                                )
                            }
                        } else {
                            val failure = result.readableFailure(
                                "Setup stopped before it completed. Retry it; existing container files will be kept.",
                            )
                            mutableState.update {
                                installProgressJob?.cancel()
                                it.copy(
                                    activeOperation = null,
                                    installingWorkspaceId = null,
                                    message = failure,
                                    buildMessage = failure,
                                    installProgress = InstallProgress(
                                        status = InstallProgressStatus.FAILED,
                                        percent = null,
                                        step = 0,
                                        totalSteps = installStepCount(recipe),
                                        title = "Setup needs attention",
                                        detail = failure,
                                    ),
                                )
                            }
                        }
                    }

                    TAG_INSTALL_PROGRESS -> handleInstallProgress(result)

                    TAG_TERMUX_CONSENT -> handleTermuxConsentResult(result)

                    TAG_WORKSPACE_SCAN -> handleWorkspaceScan(result)

                    TAG_VERIFY -> {
                        val recipe = mutableState.value.selectedRecipe
                        if (result.succeeded && result.stdout.contains("TUXSPAN_READY")) {
                            installProgressJob?.cancel()
                            repository.save(recipe.id, WorkspacePhase.READY)
                            mutableState.update {
                                it.copy(
                                    installedWorkspaces = repository.loadAll(),
                                    activeWorkspaceId = repository.activeRecipeId(),
                                    activeOperation = null,
                                    showBuildSheet = false,
                                    message = "${recipe.name} is ready to launch.",
                                    buildMessage = null,
                                    installProgress = null,
                                )
                            }
                        } else {
                            val failure = result.readableFailure(
                                "Workspace files were not found yet. Run or resume setup in Termux, then verify again.",
                            )
                            mutableState.update {
                                it.copy(
                                    activeOperation = null,
                                    message = failure,
                                    buildMessage = failure,
                                )
                            }
                        }
                    }

                    TAG_BUNDLE -> mutableState.update {
                        it.copy(
                            activeOperation = null,
                            installingBundleId = null,
                            message = if (result.succeeded) {
                                "App bundle installed."
                            } else {
                                result.readableFailure("The app bundle could not be installed.")
                            },
                        )
                    }

                    TAG_STORAGE_STATUS -> mutableState.update {
                        it.copy(
                            storageBridgeReady = result.succeeded &&
                                result.stdout.contains("TUXSPAN_STORAGE_READY"),
                            storageBridgeChecking = false,
                        )
                    }

                    TAG_DISPLAY_PREPARE -> {
                        val expectedRequest = pendingDisplayRequestId
                        if (expectedRequest == null || result.requestId != expectedRequest) {
                            return@collect
                        }
                        displayPrepareWatchdogJob?.cancel()
                        displayPrepareWatchdogJob = null
                        val recipe = pendingDisplayRecipe
                        val experience = pendingDisplayExperience
                        val dpi = pendingDisplayDpi
                        pendingDisplayRequestId = null
                        pendingDisplayRecipe = null
                        pendingDisplayExperience = null
                        if (
                            result.succeeded &&
                            result.stdout.contains("TUXSPAN_DISPLAY_READY") &&
                            recipe != null &&
                            experience != null
                        ) {
                            continuePreparedDisplay(recipe, experience, dpi)
                        } else {
                            val failure = result.launchFailure()
                            mutableState.update {
                                it.copy(
                                    sessionStatus = SessionStatus.ERROR,
                                    sessionError = failure,
                                    message = failure,
                                )
                            }
                        }
                    }

                    TAG_LAUNCH -> {
                        val expectedRequest = pendingLaunchRequestId
                        if (expectedRequest != null && result.requestId != expectedRequest) {
                            return@collect
                        }
                        pendingLaunchRequestId = null
                        launchWatchdogJob?.cancel()
                        launchWatchdogJob = null
                        if (result.succeeded && result.stdout.contains("TUXSPAN_LAUNCH_STARTED")) {
                            mutableState.update {
                                it.copy(sessionStatus = SessionStatus.STARTING, sessionError = null)
                            }
                            openDesktopAfterLaunch()
                        } else {
                            val failure = result.launchFailure()
                            mutableState.update {
                                it.copy(
                                    sessionStatus = SessionStatus.ERROR,
                                    sessionError = failure,
                                    message = failure,
                                )
                            }
                        }
                    }

                    TAG_STATUS -> {
                        val expectedRequest = pendingStatusRequestId
                        val expectedWorkspace = pendingStatusWorkspaceId
                        if (
                            expectedRequest == null ||
                            result.requestId != expectedRequest ||
                            expectedWorkspace == null ||
                            mutableState.value.activeWorkspaceId != expectedWorkspace
                        ) {
                            return@collect
                        }
                        pendingStatusRequestId = null
                        pendingStatusWorkspaceId = null
                        mutableState.update {
                        if (it.sessionStatus == SessionStatus.STARTING) {
                            pendingLaunchRequestId = null
                            launchWatchdogJob?.cancel()
                            launchWatchdogJob = null
                        }
                        val status = when {
                            result.succeeded && result.stdout.contains("TUXSPAN_RUNNING") ->
                                SessionStatus.RUNNING
                            result.stdout.contains("TUXSPAN_ERROR") -> SessionStatus.ERROR
                            else -> SessionStatus.STOPPED
                        }
                        val statusError = result.stdout
                            .substringAfter("TUXSPAN_ERROR", "")
                            .trim()
                            .takeIf(String::isNotBlank)
                            ?.toLaunchFailureMessage()
                        it.copy(
                            sessionStatus = status,
                            sessionError = when (status) {
                                SessionStatus.ERROR -> statusError ?: it.sessionError
                                    ?: "The desktop process stopped unexpectedly. Tap Retry to start it again."
                                SessionStatus.RUNNING, SessionStatus.STOPPED -> null
                                else -> it.sessionError
                            },
                        )
                        }
                    }

                    TAG_STOP -> mutableState.update {
                        pendingLaunchRequestId = null
                        launchWatchdogJob?.cancel()
                        launchWatchdogJob = null
                        it.copy(
                            sessionStatus = if (result.succeeded) {
                                SessionStatus.STOPPED
                            } else {
                                SessionStatus.ERROR
                            },
                            message = if (result.succeeded) {
                                "Linux session stopped safely."
                            } else {
                                result.readableFailure("The session did not stop cleanly." )
                            },
                            sessionError = if (result.succeeded) null else it.sessionError,
                        )
                    }

                    TAG_RESTART_STOP -> {
                        val expectedRequest = pendingRestartStopRequestId
                        if (expectedRequest == null || result.requestId != expectedRequest) {
                            return@collect
                        }
                        pendingRestartStopRequestId = null
                        if (result.succeeded && result.stdout.contains("TUXSPAN_STOPPED")) {
                            mutableState.update {
                                it.copy(
                                    sessionStatus = SessionStatus.STOPPED,
                                    sessionError = null,
                                )
                            }
                            // The stop command has now reaped the old PRoot,
                            // accessibility bus, audio daemon, and X11 server.
                            // Starting before this callback caused intermittent
                            // relaunch failures on slower phones.
                            val restartRecipeId = mutableState.value.activeWorkspaceId
                            viewModelScope.launch {
                                // ACTION_STOP finishes the native server before
                                // Android removes the old X11 activity window.
                                // Let that completed activity teardown settle so
                                // the replacement launch intent is not discarded
                                // by OEM task-transition handling.
                                delay(750)
                                if (
                                    mutableState.value.activeWorkspaceId == restartRecipeId &&
                                    mutableState.value.sessionStatus == SessionStatus.STOPPED
                                ) {
                                    launchWorkspace()
                                }
                            }
                        } else {
                            val failure = result.readableFailure(
                                "The previous desktop did not stop cleanly. Retry the restart; workspace files are safe.",
                            )
                            mutableState.update {
                                it.copy(
                                    sessionStatus = SessionStatus.ERROR,
                                    sessionError = failure,
                                    message = failure,
                                )
                            }
                        }
                    }

                    TAG_X11_PREFERENCES -> mutableState.update {
                        it.copy(
                            message = when {
                                !result.succeeded -> result.readableFailure(
                                    "Termux:X11 did not accept the display settings.",
                                )
                                result.stdout.contains("TUXSPAN_X11_CONTROLS_SHOWN") ->
                                    "On-screen controls are visible."
                                result.stdout.contains("TUXSPAN_X11_CONTROLS_HIDDEN") ->
                                    "On-screen controls are hidden."
                                result.stdout.contains("TUXSPAN_X11_READY") ->
                                    "Linux display ready in fullscreen mode."
                                else -> "Termux:X11 display settings applied."
                            },
                        )
                    }
                }
                CommandResultBus.clear()
            }
        }
        repository.loadAll().firstOrNull { it.phase == WorkspacePhase.INSTALL_DISPATCHED }?.let {
            startInstallProgressPolling(WorkspaceCatalog.byId(it.recipeId))
        }
        scanWorkspaces()
        checkDownloadsBridge()
    }

    fun refresh() {
        val app = getApplication<Application>()
        val companions = CompanionInspector.inspect(app)
        val savedExperience = experienceRepository.load()
        val experience = if (!companions.termuxInstalled && savedExperience.termuxConsentCompleted) {
            savedExperience.copy(
                termuxConsentCompleted = false,
                initialSetupCompleted = false,
            ).also(experienceRepository::save)
        } else {
            savedExperience
        }
        mutableState.update {
            it.copy(
                device = DeviceInspector.inspect(app),
                companions = companions,
                installedWorkspaces = repository.loadAll(),
                activeWorkspaceId = repository.activeRecipeId(),
                experience = experience,
            )
        }
        val saved = repository.loadAll().firstOrNull {
            it.phase == WorkspacePhase.INSTALL_DISPATCHED
        }
        if (saved?.phase == WorkspacePhase.INSTALL_DISPATCHED) {
            startInstallProgressPolling(WorkspaceCatalog.byId(saved.recipeId))
        } else if (mutableState.value.activeOperation == null) {
            checkSessionStatus()
        }
        scanWorkspaces()
        checkDownloadsBridge()
    }

    private fun scanWorkspaces() {
        if (!mutableState.value.companions.runCommandGranted) return
        bridge.dispatch(
            CommandRequest(
                tag = TAG_WORKSPACE_SCAN,
                label = "Find TuxSpan workspaces",
                description = "Finds existing Canvas, Studio, and Forge installations.",
                script = WorkspaceScripts.discoverWorkspaces(),
                background = true,
                collectResult = true,
            ),
        )
    }

    private fun handleWorkspaceScan(result: dev.tuxspan.mobile.termux.CommandResult) {
        if (!result.succeeded) return
        val discovered = result.stdout.lineSequence().mapNotNull { line ->
            val fields = line.trim().split('|')
            if (fields.size != 3 || fields[0] != "TUXSPAN_WORKSPACE") return@mapNotNull null
            val recipe = WorkspaceCatalog.all.firstOrNull { it.id == fields[1] }
                ?: return@mapNotNull null
            val phase = runCatching { WorkspacePhase.valueOf(fields[2]) }.getOrNull()
                ?: return@mapNotNull null
            recipe.id to phase
        }.toList()
        if (discovered.isEmpty()) return

        discovered.forEach { (recipeId, phase) ->
            repository.save(recipeId, phase, makeActive = false)
        }
        val activeId = repository.activeRecipeId()
        mutableState.update {
            it.copy(
                installedWorkspaces = repository.loadAll(),
                activeWorkspaceId = activeId,
                selectedRecipe = if (it.activeWorkspaceId == null && activeId != null) {
                    WorkspaceCatalog.byId(activeId)
                } else {
                    it.selectedRecipe
                },
            )
        }
        if (mutableState.value.activeOperation == null) checkSessionStatus()
    }

    fun chooseRecipe(recipe: WorkspaceRecipe) {
        mutableState.update { it.copy(selectedRecipe = recipe, message = null) }
    }

    fun activateWorkspace(recipe: WorkspaceRecipe) {
        val workspace = repository.load(recipe.id)
        if (workspace?.phase != WorkspacePhase.READY) {
            mutableState.update { it.copy(message = "${recipe.name} is not ready yet.") }
            return
        }
        val previous = activeRecipeOrNull()
        if (
            previous != null && previous.id != recipe.id &&
            mutableState.value.sessionStatus in setOf(SessionStatus.RUNNING, SessionStatus.STARTING)
        ) {
            bridge.dispatch(
                CommandRequest(
                    tag = TAG_SWITCH_STOP,
                    label = "Switch TuxSpan workspace",
                    description = "Stops the current desktop before switching workspaces.",
                    script = WorkspaceScripts.stop(previous),
                    background = true,
                    collectResult = false,
                ),
            )
        }
        // A status callback from the workspace being left must never update
        // the newly selected workspace (especially a terminal workspace).
        pendingStatusRequestId = null
        pendingStatusWorkspaceId = null
        repository.setActive(recipe.id)
        mutableState.update {
            it.copy(
                selectedRecipe = recipe,
                installedWorkspaces = repository.loadAll(),
                activeWorkspaceId = repository.activeRecipeId(),
                sessionStatus = SessionStatus.STOPPED,
                sessionError = null,
                message = "${recipe.name} is now the active workspace.",
            )
        }
        checkSessionStatus()
    }

    fun openBuildSheet(recipe: WorkspaceRecipe = mutableState.value.selectedRecipe) {
        val installationInProgress =
            repository.load(recipe.id)?.phase == WorkspacePhase.INSTALL_DISPATCHED
        mutableState.update {
            it.copy(
                selectedRecipe = recipe,
                showBuildSheet = true,
                commandPreviewVisible = false,
                message = null,
                buildMessage = if (installationInProgress) it.buildMessage else null,
            )
        }
        if (installationInProgress) startInstallProgressPolling(recipe)
    }

    fun openActiveBuildSheet() {
        openBuildSheet(activeRecipeOrNull() ?: mutableState.value.selectedRecipe)
    }

    fun closeBuildSheet() {
        mutableState.update { it.copy(showBuildSheet = false, commandPreviewVisible = false) }
    }

    fun toggleCommandPreview() {
        mutableState.update { it.copy(commandPreviewVisible = !it.commandPreviewVisible) }
    }

    fun dispatchInstall() {
        val recipe = mutableState.value.selectedRecipe
        installProgressJob?.cancel()
        CommandResultBus.clear()
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_INSTALL,
                label = "Build TuxSpan ${recipe.name}",
                description = "Installs the reviewed ${recipe.distroLabel} recipe.",
                script = WorkspaceScripts.trackedInstall(recipe),
                background = false,
                // Package output can be very large. The durable progress/status
                // files are polled separately, avoiding a large Binder callback.
                collectResult = false,
            ),
        )
        if (result.isSuccess) {
            repository.save(
                recipe.id,
                WorkspacePhase.INSTALL_DISPATCHED,
                installStartedAtMillis = System.currentTimeMillis(),
                makeActive = false,
            )
            mutableState.update {
                it.copy(
                    installedWorkspaces = repository.loadAll(),
                    activeWorkspaceId = repository.activeRecipeId(),
                    activeOperation = TAG_INSTALL,
                    installingWorkspaceId = recipe.id,
                    message = "${recipe.name} installation opened in Termux. Return here anytime to check progress.",
                    buildMessage = null,
                    installProgress = InstallProgress.starting(installStepCount(recipe)),
                )
            }
            startInstallProgressPolling(recipe)
            viewModelScope.launch {
                // Android 10+ may prevent Termux's service from opening its own
                // activity. Give it time to create and select the terminal
                // session, then bring that already-running session forward.
                delay(900)
                if (!bridge.openTermux()) {
                    mutableState.update {
                        it.copy(message = "Installation started, but Termux could not be opened. Tap View live installation in Termux.")
                    }
                }
            }
        } else {
            mutableState.update {
                val failure = result.exceptionOrNull()?.message ?: "Could not start setup."
                it.copy(message = failure, buildMessage = failure)
            }
        }
    }

    fun verifyWorkspace() {
        val recipe = mutableState.value.selectedRecipe
        installProgressJob?.cancel()
        CommandResultBus.clear()
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_VERIFY,
                label = "Verify TuxSpan ${recipe.name}",
                description = "Checks only for TuxSpan's launcher and workspace record.",
                script = WorkspaceScripts.verify(recipe),
                background = true,
            ),
        )
        mutableState.update {
            if (result.isSuccess) {
                it.copy(
                    activeOperation = TAG_VERIFY,
                    message = "Checking the workspace…",
                    buildMessage = "Checking the Termux workspace…",
                    installProgress = null,
                )
            } else {
                val failure = result.exceptionOrNull()?.message ?: "Could not verify the workspace."
                it.copy(message = failure, buildMessage = failure)
            }
        }
        if (result.isSuccess) {
            viewModelScope.launch {
                delay(12_000)
                if (mutableState.value.activeOperation == TAG_VERIFY) {
                    mutableState.update {
                        it.copy(
                            activeOperation = null,
                            buildMessage = "Termux did not return a verification result. Open Termux and use the manual readiness check shown below.",
                        )
                    }
                }
            }
        }
    }

    fun launchWorkspace() {
        val recipe = activeRecipeOrNull() ?: return
        val experience = mutableState.value.experience
        val dpi = experience.displayProfile.adaptiveDpi(mutableState.value.device.densityDpi)
        if (recipe.kind == WorkspaceKind.DESKTOP) {
            clearPendingDisplay()
            val configured = bridge.applyX11Preferences(experience)
            if (configured.isFailure) {
                val failure = configured.exceptionOrNull()?.message
                    ?: "Termux:X11 could not be prepared for the desktop."
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = failure,
                        message = failure,
                    )
                }
                return
            }
            // OxygenOS can freeze Termux RunCommand jobs while Termux is fully
            // backgrounded. Keep Termux visible while the short native-display
            // preparer runs; the X11 activity is opened only after the native
            // socket has been confirmed.
            if (!bridge.openTermux()) {
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = "Termux could not be opened to prepare the Linux display.",
                        message = "Open Termux once, then tap Retry. Your workspace files are safe.",
                    )
                }
                return
            }
            val prepared = bridge.dispatch(
                CommandRequest(
                    tag = TAG_DISPLAY_PREPARE,
                    label = "Prepare TuxSpan display",
                    description = "Starts and verifies the native Termux:X11 server.",
                    script = WorkspaceScripts.prepareDisplay(dpi),
                    background = true,
                    collectResult = true,
                ),
            )
            if (prepared.isFailure) {
                val failure = prepared.exceptionOrNull()?.message
                    ?: "Termux:X11 could not be prepared for the desktop."
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = failure,
                        message = failure,
                    )
                }
                return
            }
            val prepareRequestId = prepared.getOrThrow()
            pendingDisplayRequestId = prepareRequestId
            pendingDisplayRecipe = recipe
            pendingDisplayExperience = experience
            pendingDisplayDpi = dpi
            mutableState.update {
                it.copy(sessionStatus = SessionStatus.STARTING, sessionError = null)
            }
            displayPrepareWatchdogJob = viewModelScope.launch {
                // Never start a second X11 launcher while the preparer is still
                // alive. Competing native servers race for the same socket and
                // are the root cause of intermittent launch failures. A cold
                // device normally finishes this step well inside this budget.
                delay(35_000)
                if (
                    pendingDisplayRequestId == prepareRequestId &&
                    mutableState.value.activeWorkspaceId == recipe.id &&
                    mutableState.value.sessionStatus == SessionStatus.STARTING
                ) {
                    pendingDisplayRequestId = null
                    pendingDisplayRecipe = null
                    pendingDisplayExperience = null
                    displayPrepareWatchdogJob = null
                    mutableState.update {
                        it.copy(
                            sessionStatus = SessionStatus.ERROR,
                            sessionError = "Termux did not finish preparing the display. Open Termux once, then tap Retry.",
                            message = "Display preparation timed out; no second launcher was started. Your workspace files are safe.",
                        )
                    }
                }
            }
            return
        }
        dispatchWorkspaceLaunch(recipe, experience, dpi)
    }

    private fun continuePreparedDisplay(
        recipe: WorkspaceRecipe,
        experience: ExperienceSettings,
        dpi: Int,
    ) {
        if (!bridge.openX11()) {
            mutableState.update {
                it.copy(
                    sessionStatus = SessionStatus.ERROR,
                    sessionError = "Termux:X11 could not open its Android display.",
                    message = "Termux:X11 could not open its Android display.",
                )
            }
            return
        }
        viewModelScope.launch {
            // The native server already owns a stable persistent socket. Let
            // Android attach its surface, then return Termux to the foreground
            // so OxygenOS cannot suspend the tracked Linux launcher.
            delay(500)
            if (!bridge.openTermux()) {
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = "Termux could not be opened to start the Linux workspace.",
                        message = "Open Termux once, then tap Retry. Your workspace files are safe.",
                    )
                }
                return@launch
            }
            delay(500)
            if (mutableState.value.activeWorkspaceId == recipe.id) {
                dispatchWorkspaceLaunch(recipe, experience, dpi)
            }
        }
    }

    private fun clearPendingDisplay() {
        displayPrepareWatchdogJob?.cancel()
        displayPrepareWatchdogJob = null
        pendingDisplayRequestId = null
        pendingDisplayRecipe = null
        pendingDisplayExperience = null
    }

    private fun dispatchWorkspaceLaunch(
        recipe: WorkspaceRecipe,
        experience: ExperienceSettings,
        dpi: Int,
    ) {
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_LAUNCH,
                label = "Launch TuxSpan ${recipe.name}",
                description = "Starts the saved ${recipe.distroLabel} workspace.",
                script = if (recipe.kind == WorkspaceKind.DESKTOP) {
                    WorkspaceScripts.trackedLaunch(recipe, experience, dpi)
                } else {
                    WorkspaceScripts.launch(recipe, experience, dpi)
                },
                background = recipe.kind == WorkspaceKind.DESKTOP,
                collectResult = recipe.kind == WorkspaceKind.DESKTOP,
            ),
        )
        if (result.isFailure) {
            mutableState.update {
                it.copy(
                    sessionStatus = SessionStatus.ERROR,
                    message = result.exceptionOrNull()?.message ?: "Could not launch the workspace.",
                )
            }
            return
        }
        if (recipe.kind == WorkspaceKind.DESKTOP) {
            val requestId = result.getOrThrow()
            pendingLaunchRequestId = requestId
            launchWatchdogJob?.cancel()
            mutableState.update {
                it.copy(sessionStatus = SessionStatus.STARTING, sessionError = null)
            }
            launchWatchdogJob = viewModelScope.launch {
                // A cold Termux restart may erase its volatile tmp directory
                // while the X11 server is coming up. The launcher now retries
                // that transition, so do not show a false failure mid-recovery.
                // The shell launcher can perform three complete native X11
                // recovery attempts before starting XFCE. Its callback timeout
                // must be longer than that recovery budget; otherwise the app
                // kills a healthy in-progress restart on slower devices.
                delay(100_000)
                if (
                    pendingLaunchRequestId == requestId &&
                    mutableState.value.sessionStatus == SessionStatus.STARTING
                ) {
                    // The callback may be delayed by Android, but the session
                    // state in Termux is authoritative. Check it before ever
                    // presenting a failure to the user.
                    checkSessionStatus()
                    delay(8_000)
                }
                if (
                    pendingLaunchRequestId == requestId &&
                    mutableState.value.sessionStatus == SessionStatus.STARTING
                ) {
                    pendingLaunchRequestId = null
                    mutableState.update {
                        it.copy(
                            sessionStatus = SessionStatus.ERROR,
                            sessionError = "TuxSpan could not confirm the desktop state. Tap Retry; your workspace files are safe.",
                            message = "The desktop state could not be confirmed. Tap Retry to check it again.",
                        )
                    }
                }
            }
        } else {
            mutableState.update { it.copy(sessionStatus = SessionStatus.RUNNING) }
        }
    }

    private fun openDesktopAfterLaunch() {
        val experience = mutableState.value.experience
        viewModelScope.launch {
            // Reopening is harmless when the prepared activity is already
            // visible and recovers if an OEM closed it during task rotation.
            if (!bridge.openX11()) {
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = "Termux:X11 could not open its Android display.",
                        message = "Termux:X11 could not open its Android display.",
                    )
                }
                return@launch
            }
            delay(250)
            val configured = bridge.applyX11Preferences(experience)
            if (configured.isFailure) {
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = configured.exceptionOrNull()?.message
                            ?: "Termux:X11 rejected the display and input settings.",
                        message = configured.exceptionOrNull()?.message
                            ?: "Termux:X11 did not accept the input and display settings.",
                    )
                }
                return@launch
            }
            delay(400)
            val hidden = bridge.setX11ControlsVisible(false)
            if (hidden.isFailure) {
                mutableState.update {
                    it.copy(
                        sessionStatus = SessionStatus.ERROR,
                        sessionError = hidden.exceptionOrNull()?.message
                            ?: "Termux:X11 could not hide its on-screen controls.",
                        message = hidden.exceptionOrNull()?.message
                            ?: "Termux:X11 did not save the hidden controls state.",
                    )
                }
                return@launch
            }
            delay(400)
            bridge.refreshX11Controls()
            mutableState.update {
                it.copy(message = "Linux display ready with phone-friendly input controls.")
            }
            delay(2_000)
            checkSessionStatus()
        }
    }

    fun stopWorkspace() {
        val recipe = activeRecipeOrNull() ?: return
        clearPendingDisplay()
        mutableState.update {
            it.copy(sessionStatus = SessionStatus.STOPPING, sessionError = null)
        }
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_STOP,
                label = "Stop TuxSpan ${recipe.name}",
                description = "Stops only the selected TuxSpan desktop session.",
                script = WorkspaceScripts.stop(recipe),
                background = true,
                collectResult = true,
            ),
        )
        if (result.isFailure) {
            mutableState.update {
                it.copy(
                    sessionStatus = SessionStatus.ERROR,
                    message = result.exceptionOrNull()?.message ?: "Could not stop the session.",
                )
            }
        }
    }

    fun restartWorkspace() {
        val recipe = activeRecipeOrNull() ?: return
        clearPendingDisplay()
        mutableState.update { it.copy(sessionStatus = SessionStatus.STOPPING) }
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_RESTART_STOP,
                label = "Restart TuxSpan ${recipe.name}",
                description = "Stops the current desktop before launching it with updated settings.",
                script = WorkspaceScripts.stop(recipe),
                background = true,
                collectResult = true,
            ),
        )
        if (result.isFailure) {
            mutableState.update {
                it.copy(
                    sessionStatus = SessionStatus.ERROR,
                    message = result.exceptionOrNull()?.message ?: "Could not restart the session.",
                )
            }
            return
        }
        pendingRestartStopRequestId = result.getOrThrow()
    }

    fun reconnectWorkspace() {
        val current = mutableState.value
        val recipe = activeRecipeOrNull() ?: return
        if (current.sessionStatus != SessionStatus.RUNNING) {
            launchWorkspace()
            return
        }
        if (recipe.kind == WorkspaceKind.TERMINAL) {
            if (!bridge.openTermux()) {
                mutableState.update { it.copy(message = "Termux could not be opened.") }
            } else {
                mutableState.update { it.copy(message = "Returned to the ${recipe.name} terminal.") }
            }
            return
        }
        if (!bridge.openX11()) {
            mutableState.update { it.copy(message = "Termux:X11 could not be opened.") }
            return
        }
        viewModelScope.launch {
            delay(500)
            val configured = bridge.applyX11Preferences(current.experience)
            if (configured.isFailure) {
                mutableState.update {
                    it.copy(
                        message = configured.exceptionOrNull()?.message
                            ?: "Termux:X11 did not accept the input and display settings.",
                    )
                }
                return@launch
            }
            delay(400)
            bridge.setX11ControlsVisible(false)
            delay(400)
            bridge.refreshX11Controls()
            mutableState.update {
                it.copy(message = "Display reconnected with the optional controls hidden.")
            }
        }
    }

    fun checkSessionStatus() {
        val current = mutableState.value
        val recipe = activeRecipeOrNull() ?: return
        if (
            recipe.kind != WorkspaceKind.DESKTOP ||
            current.activeWorkspace?.phase != WorkspacePhase.READY ||
            !current.companions.runCommandGranted
        ) return
        val dispatched = bridge.dispatch(
            CommandRequest(
                tag = TAG_STATUS,
                label = "Check TuxSpan session",
                description = "Checks whether the selected local desktop is running.",
                script = WorkspaceScripts.sessionStatus(recipe),
                background = true,
                collectResult = true,
            ),
        )
        if (dispatched.isSuccess) {
            pendingStatusRequestId = dispatched.getOrThrow()
            pendingStatusWorkspaceId = recipe.id
        }
    }

    fun setDisplayProfile(profile: DisplayProfile) = updateExperience(
        message = "${profile.label} profile selected. Restart Linux to apply its DPI and XFCE layout.",
    ) { it.copy(displayProfile = profile) }

    fun setScreenOrientation(orientation: ScreenOrientation) = updateExperience(
        message = "${orientation.label} selected. Restart Linux to apply the screen orientation.",
    ) { it.copy(screenOrientation = orientation) }

    fun setTouchMode(mode: TouchMode) = updateExperience(
        message = "${mode.label} selected. It will apply on launch or reconnect.",
    ) { it.copy(touchMode = mode) }

    fun setPerformancePreset(preset: PerformancePreset) = updateExperience(
        message = "${preset.label} preset selected. Restart Linux to apply it completely.",
    ) { it.copy(performancePreset = preset) }

    fun setDownloadsBridge(enabled: Boolean) {
        updateExperience(
            message = if (enabled) {
                "Opening Termux to request Android Downloads access. Approve Android's permission prompt, then return here."
            } else {
                "Android Downloads sharing is off. Existing files are not deleted."
            },
        ) { it.copy(downloadsBridgeEnabled = enabled) }
        mutableState.update {
            it.copy(storageBridgeReady = false, storageBridgeChecking = enabled)
        }
        if (enabled) {
            requestDownloadsAccess()
        }
    }

    fun requestDownloadsAccess() {
        val companions = mutableState.value.companions
        if (!companions.termuxInstalled) {
            mutableState.update {
                it.copy(storageBridgeChecking = false, message = "Install Termux before sharing Downloads.")
            }
            return
        }
        if (!companions.runCommandGranted) {
            mutableState.update {
                it.copy(
                    storageBridgeChecking = false,
                    message = "Grant TuxSpan's Run command permission before sharing Downloads.",
                )
            }
            return
        }
        mutableState.update { it.copy(storageBridgeReady = false, storageBridgeChecking = true) }
        if (!bridge.openTermux()) {
            mutableState.update {
                it.copy(
                    storageBridgeChecking = false,
                    message = "Termux could not be opened for the storage permission request.",
                )
            }
            return
        }
        viewModelScope.launch {
            // Samsung and other recent Android builds reject RUN_COMMAND while
            // the target Termux app is still backgrounded. Open it first, then
            // dispatch during Android's foreground transition grace period.
            delay(700)
            val dispatched = bridge.dispatch(
                CommandRequest(
                    tag = TAG_STORAGE,
                    label = "Enable Android Downloads",
                    description = "Requests Termux storage access and rebuilds its Downloads link.",
                    script = WorkspaceScripts.requestStorageAccess(),
                    background = false,
                    collectResult = false,
                ),
            )
            if (dispatched.isFailure) {
                mutableState.update {
                    it.copy(
                        storageBridgeChecking = false,
                        message = dispatched.exceptionOrNull()?.message
                            ?: "Termux could not start the storage permission request.",
                    )
                }
            }
        }
    }

    fun checkDownloadsBridge() {
        val current = mutableState.value
        if (!current.experience.downloadsBridgeEnabled) {
            mutableState.update {
                it.copy(storageBridgeReady = false, storageBridgeChecking = false)
            }
            return
        }
        if (!current.companions.runCommandGranted) {
            mutableState.update {
                it.copy(storageBridgeReady = false, storageBridgeChecking = false)
            }
            return
        }
        mutableState.update { it.copy(storageBridgeChecking = true) }
        val dispatched = bridge.dispatch(
            CommandRequest(
                tag = TAG_STORAGE_STATUS,
                label = "Check Android Downloads",
                description = "Verifies Termux can read Android Downloads.",
                script = WorkspaceScripts.storageAccessStatus(),
                background = true,
                collectResult = true,
            ),
        )
        if (dispatched.isFailure) {
            mutableState.update {
                it.copy(storageBridgeReady = false, storageBridgeChecking = false)
            }
        }
    }

    fun openTermuxStorageSettings() {
        if (!bridge.openTermuxStorageSettings()) {
            mutableState.update {
                it.copy(message = "Android could not open Termux's storage settings.")
            }
        }
    }

    fun completeTutorial() = updateExperience { it.copy(tutorialCompleted = true) }

    fun showTutorial() = updateExperience { it.copy(tutorialCompleted = false) }

    fun showX11Controls() {
        if (!bridge.openX11()) {
            mutableState.update { it.copy(message = "Termux:X11 could not be opened.") }
            return
        }
        viewModelScope.launch {
            delay(500)
            val prepared = bridge.prepareX11Controls()
            if (prepared.isFailure) {
                mutableState.update {
                    it.copy(message = prepared.exceptionOrNull()?.message ?: "Controls could not be shown.")
                }
                return@launch
            }
            delay(400)
            val visible = bridge.setX11ControlsVisible(true)
            delay(400)
            val refreshed = bridge.refreshX11Controls()
            mutableState.update {
                it.copy(
                    message = visible.exceptionOrNull()?.message
                        ?: refreshed.exceptionOrNull()?.message
                        ?: "On-screen controls are visible.",
                )
            }
        }
    }

    fun hideX11Controls() {
        if (!bridge.openX11()) {
            mutableState.update { it.copy(message = "Termux:X11 could not be opened.") }
            return
        }
        viewModelScope.launch {
            delay(500)
            val prepared = bridge.prepareX11Controls()
            if (prepared.isFailure) {
                mutableState.update {
                    it.copy(message = prepared.exceptionOrNull()?.message ?: "Controls could not be hidden.")
                }
                return@launch
            }
            delay(400)
            val hidden = bridge.setX11ControlsVisible(false)
            delay(400)
            val refreshed = bridge.refreshX11Controls()
            mutableState.update {
                it.copy(
                    message = hidden.exceptionOrNull()?.message
                        ?: refreshed.exceptionOrNull()?.message
                        ?: "On-screen controls are hidden.",
                )
            }
        }
    }

    private fun updateExperience(
        message: String? = null,
        transform: (ExperienceSettings) -> ExperienceSettings,
    ) {
        val updated = transform(mutableState.value.experience)
        experienceRepository.save(updated)
        mutableState.update { it.copy(experience = updated, message = message ?: it.message) }
    }

    fun installBundle(bundleId: String) {
        val recipe = activeRecipeOrNull() ?: return
        CommandResultBus.clear()
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_BUNDLE,
                label = "Add $bundleId tools",
                description = "Installs the reviewed $bundleId package group inside ${recipe.name}.",
                script = WorkspaceScripts.installBundle(recipe, bundleId),
                background = true,
                collectResult = true,
            ),
        )
        mutableState.update {
            if (result.isSuccess) {
                it.copy(
                    activeOperation = "bundle",
                    installingBundleId = bundleId,
                    message = "Installing the $bundleId pack in the background…",
                )
            } else {
                it.copy(message = result.exceptionOrNull()?.message ?: "Could not start package installation.")
            }
        }
    }

    fun removeWorkspace() {
        val recipe = activeRecipeOrNull() ?: return
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_REMOVE,
                label = "Remove TuxSpan ${recipe.name}",
                description = "Permanently removes this Linux workspace after your confirmation in TuxSpan.",
                script = WorkspaceScripts.remove(recipe),
                background = false,
                collectResult = false,
            ),
        )
        if (result.isSuccess) {
            repository.remove(recipe.id)
            val activeId = repository.activeRecipeId()
            mutableState.update {
                it.copy(
                    installedWorkspaces = repository.loadAll(),
                    activeWorkspaceId = activeId,
                    selectedRecipe = WorkspaceCatalog.byId(activeId),
                    sessionStatus = SessionStatus.STOPPED,
                    message = "${recipe.name} removal opened in Termux.",
                )
            }
        } else {
            mutableState.update {
                it.copy(message = result.exceptionOrNull()?.message ?: "Could not start removal.")
            }
        }
    }

    fun openTermux() {
        if (!bridge.openTermux()) {
            mutableState.update { it.copy(message = "Termux is not installed.") }
        }
    }

    fun verifyTermuxConsent() {
        val companions = mutableState.value.companions
        if (!companions.termuxInstalled) {
            mutableState.update { it.copy(message = "Install and open Termux first.") }
            return
        }
        if (!companions.runCommandGranted) {
            mutableState.update { it.copy(message = "Grant the Run command permission first.") }
            return
        }
        consentCheckJob?.cancel()
        CommandResultBus.clear()
        val result = bridge.dispatch(
            CommandRequest(
                tag = TAG_TERMUX_CONSENT,
                label = "Verify TuxSpan setup",
                description = "Checks that Termux accepts commands from TuxSpan.",
                script = "printf 'TUXSPAN_CONSENT_READY\\n'",
                background = true,
                collectResult = true,
            ),
        )
        if (result.isFailure) {
            mutableState.update {
                it.copy(
                    activeOperation = null,
                    message = result.exceptionOrNull()?.message
                        ?: "Termux could not verify the one-time consent.",
                )
            }
            return
        }
        mutableState.update {
            it.copy(
                activeOperation = TAG_TERMUX_CONSENT,
                message = "Checking the Termux connection…",
            )
        }
        consentCheckJob = viewModelScope.launch {
            delay(10_000)
            if (mutableState.value.activeOperation == TAG_TERMUX_CONSENT) {
                mutableState.update {
                    it.copy(
                        activeOperation = null,
                        message = "Termux did not confirm the connection. Run the copied consent command in Termux, then try again.",
                    )
                }
            }
        }
    }

    private fun handleTermuxConsentResult(result: dev.tuxspan.mobile.termux.CommandResult) {
        consentCheckJob?.cancel()
        val verified = result.succeeded && result.stdout.contains("TUXSPAN_CONSENT_READY")
        val rawFailure = listOf(result.internalError, result.stderr, result.stdout)
            .joinToString("\n")
        val failureMessage = if (rawFailure.contains("allow-external-apps", ignoreCase = true)) {
            "Termux consent is not enabled yet. Tap Copy, run the command in Termux, then check again."
        } else {
            "Termux could not confirm the connection. Make sure the copied command finished, then check again."
        }
        val updated = if (verified) {
            mutableState.value.experience.copy(termuxConsentCompleted = true)
                .also(experienceRepository::save)
        } else {
            mutableState.value.experience
        }
        mutableState.update {
            it.copy(
                experience = updated,
                activeOperation = null,
                message = if (verified) {
                    "Termux is connected. You can now choose a Linux workspace."
                } else {
                    failureMessage
                },
            )
        }
    }

    fun completeInitialSetup() {
        if (!mutableState.value.coreSetupReady) {
            mutableState.update { it.copy(message = "Complete the required Termux steps first.") }
            return
        }
        updateExperience { it.copy(initialSetupCompleted = true) }
    }

    fun dismissMessage() {
        mutableState.update { it.copy(message = null) }
    }

    fun currentInstallScript(): String = WorkspaceScripts.install(mutableState.value.selectedRecipe)

    fun currentManualInstallCommand(): String =
        WorkspaceScripts.manualInstallCommand(mutableState.value.selectedRecipe)

    private fun startInstallProgressPolling(recipe: WorkspaceRecipe) {
        if (
            installProgressJob?.isActive == true &&
            mutableState.value.installingWorkspaceId == recipe.id
        ) {
            return
        }
        installProgressJob?.cancel()
        mutableState.update {
            it.copy(
                activeOperation = TAG_INSTALL,
                installingWorkspaceId = recipe.id,
                installProgress = it.installProgress
                    ?: InstallProgress.starting(installStepCount(recipe)),
            )
        }
        installProgressJob = viewModelScope.launch {
            while (isActive) {
                val saved = repository.load(recipe.id)
                if (saved?.phase != WorkspacePhase.INSTALL_DISPATCHED) {
                    break
                }
                bridge.dispatch(
                    CommandRequest(
                        tag = TAG_INSTALL_PROGRESS,
                        label = "Check TuxSpan setup progress",
                        description = "Reads the current background installation stage.",
                        script = WorkspaceScripts.installProgress(recipe),
                        background = true,
                        collectResult = true,
                    ),
                )
                delay(3_000)
            }
        }
    }

    private fun handleInstallProgress(result: dev.tuxspan.mobile.termux.CommandResult) {
        if (!result.succeeded) return
        val progress = InstallProgress.parse(result.stdout) ?: return
        val recipe = WorkspaceCatalog.byId(
            mutableState.value.installingWorkspaceId
                ?: repository.loadAll().firstOrNull {
                    it.phase == WorkspacePhase.INSTALL_DISPATCHED
                }?.recipeId,
        )
        when (progress.status) {
            InstallProgressStatus.READY -> {
                installProgressJob?.cancel()
                repository.save(recipe.id, WorkspacePhase.READY)
                mutableState.update {
                    it.copy(
                        installedWorkspaces = repository.loadAll(),
                        activeWorkspaceId = repository.activeRecipeId(),
                        selectedRecipe = recipe,
                        activeOperation = null,
                        installingWorkspaceId = null,
                        showBuildSheet = false,
                        installProgress = null,
                        buildMessage = null,
                        message = "${recipe.name} finished installing and is ready to launch.",
                    )
                }
            }

            InstallProgressStatus.FAILED -> {
                installProgressJob?.cancel()
                mutableState.update {
                    it.copy(
                        activeOperation = null,
                        installingWorkspaceId = null,
                        installProgress = progress,
                        buildMessage = progress.detail,
                        message = progress.detail,
                    )
                }
            }

            InstallProgressStatus.RUNNING, InstallProgressStatus.UNKNOWN -> mutableState.update {
                it.copy(
                    activeOperation = TAG_INSTALL,
                    installProgress = progress,
                    buildMessage = null,
                )
            }
        }
    }

    private fun installStepCount(recipe: WorkspaceRecipe): Int =
        if (recipe.kind == WorkspaceKind.DESKTOP) 7 else 6

    private fun activeRecipeOrNull(): WorkspaceRecipe? =
        mutableState.value.activeWorkspaceId?.let(WorkspaceCatalog::byId)

    private fun dev.tuxspan.mobile.termux.CommandResult.readableFailure(fallback: String): String =
        internalError.ifBlank { stderr.ifBlank { fallback } }.take(300)

    private fun dev.tuxspan.mobile.termux.CommandResult.launchFailure(): String =
        internalError.ifBlank { stderr.ifBlank { stdout } }
            .toLaunchFailureMessage()

    private fun String.toLaunchFailureMessage(): String {
        val details = lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot {
                it.startsWith("Current launch state:") ||
                    it == "Desktop launch failed. Last log lines:" ||
                    it.startsWith("Android Downloads permission") ||
                    it.startsWith("xauth:")
            }
            .toList()
        val cause = details.lastOrNull()
            ?: "The desktop process stopped before it could open."
        return when {
            cause.contains("display was not accepting clients", ignoreCase = true) ||
                cause.contains("display socket was not available", ignoreCase = true) ->
                "Termux:X11 did not become ready in time. TuxSpan will restart the display when you retry."
            cause.contains("server did not start", ignoreCase = true) ->
                "The Termux:X11 server did not start. Open Termux:X11 once, return here, and tap Retry."
            cause.contains("can't chmod", ignoreCase = true) &&
                cause.contains("proot-", ignoreCase = true) ->
                "Termux restarted while Linux was opening. TuxSpan will rebuild the temporary runtime safely when you retry."
            cause.contains("startxfce4: not found", ignoreCase = true) ||
                cause.contains("xfce4-session: not found", ignoreCase = true) ->
                "Studio's desktop packages are incomplete. Tap Repair workspace; downloaded files will be kept."
            else -> cause.take(420)
        }
    }

    private companion object {
        const val TAG_INSTALL = "install"
        const val TAG_INSTALL_PROGRESS = "install_progress"
        const val TAG_TERMUX_CONSENT = "termux_consent"
        const val TAG_WORKSPACE_SCAN = "workspace_scan"
        const val TAG_SWITCH_STOP = "workspace_switch_stop"
        const val TAG_VERIFY = "verify"
        const val TAG_DISPLAY_PREPARE = "display_prepare"
        const val TAG_LAUNCH = "launch"
        const val TAG_BUNDLE = "bundle"
        const val TAG_REMOVE = "remove"
        const val TAG_X11_PREFERENCES = "x11_preferences"
        const val TAG_STATUS = "session_status"
        const val TAG_STOP = "session_stop"
        const val TAG_RESTART_STOP = "session_restart_stop"
        const val TAG_STORAGE = "storage_access"
        const val TAG_STORAGE_STATUS = "storage_access_status"
    }
}
