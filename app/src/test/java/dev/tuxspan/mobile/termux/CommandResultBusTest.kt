package dev.tuxspan.mobile.termux

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandResultBusTest {
    @Test
    fun simultaneousCallbacksAreDeliveredWithoutConflation() = runBlocking {
        val first = commandResult(requestId = 1, tag = "status")
        val second = commandResult(requestId = 2, tag = "scan")
        val received = async {
            withTimeout(2_000) { CommandResultBus.latest.take(2).toList() }
        }

        yield()
        CommandResultBus.publish(first)
        CommandResultBus.publish(second)

        assertEquals(listOf(first, second), received.await())
    }

    private fun commandResult(requestId: Int, tag: String) = CommandResult(
        requestId = requestId,
        tag = tag,
        stdout = "ok",
        stderr = "",
        exitCode = 0,
        internalError = "",
    )
}
