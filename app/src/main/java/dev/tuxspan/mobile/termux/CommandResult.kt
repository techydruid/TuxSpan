package dev.tuxspan.mobile.termux

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class CommandResult(
    val requestId: Int,
    val tag: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val internalError: String,
) {
    val succeeded: Boolean = exitCode == 0 && internalError.isBlank()
}

object CommandResultBus {
    // Command callbacks can arrive almost simultaneously when the app resumes
    // (workspace scan, session status, and a pending launch result). A StateFlow
    // conflates those values and can silently discard the session result. Keep
    // every callback in order until the ViewModel consumes it.
    private val results = Channel<CommandResult>(Channel.UNLIMITED)
    val latest: Flow<CommandResult> = results.receiveAsFlow()

    fun publish(result: CommandResult) {
        results.trySend(result)
    }

    /** Retained for callers that previously cleared the conflated StateFlow. */
    fun clear() = Unit
}
