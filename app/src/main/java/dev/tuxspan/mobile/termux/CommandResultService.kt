package dev.tuxspan.mobile.termux

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CommandResultService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultBundle = intent?.getBundleExtra(TermuxContract.RESULT_BUNDLE)
        if (intent != null && resultBundle != null) {
            val result = CommandResult(
                requestId = intent.getIntExtra(EXTRA_REQUEST_ID, 0),
                tag = intent.getStringExtra(EXTRA_TAG).orEmpty(),
                stdout = resultBundle.getString(TermuxContract.RESULT_STDOUT).orEmpty(),
                stderr = resultBundle.getString(TermuxContract.RESULT_STDERR).orEmpty(),
                exitCode = resultBundle.getInt(TermuxContract.RESULT_EXIT_CODE, -1),
                internalError = resultBundle.getString(TermuxContract.RESULT_ERROR_MESSAGE).orEmpty(),
            )
            if (!result.succeeded) {
                Log.e(
                    LOG_TAG,
                    "${result.tag} failed (exit ${result.exitCode}): " +
                        (result.stderr.ifBlank { result.internalError }),
                )
            }
            CommandResultBus.publish(result)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        private const val LOG_TAG = "TuxSpanCommand"
        const val EXTRA_REQUEST_ID = "dev.tuxspan.mobile.request_id"
        const val EXTRA_TAG = "dev.tuxspan.mobile.request_tag"
    }
}
