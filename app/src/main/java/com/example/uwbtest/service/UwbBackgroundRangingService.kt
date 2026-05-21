package com.example.uwbtest.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.usecase.StartRangingUseCase
import com.example.uwbtest.presentation.screen.oob.OobParamsHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background-only UWB ranging service (no foreground notification).
 *
 * Only usable when the device reports [UwbRangingCapabilities.isBackgroundRangingSupported] = true.
 * Subject to Android background process restrictions — the OS may kill this service under
 * memory pressure. Users are warned about this in the UI before selecting this mode.
 */
@AndroidEntryPoint
class UwbBackgroundRangingService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.uwbtest.ACTION_STOP_BG_RANGING"

        fun startIntent(context: Context) =
            Intent(context, UwbBackgroundRangingService::class.java)

        fun stopIntent(context: Context) =
            Intent(context, UwbBackgroundRangingService::class.java).also {
                it.action = ACTION_STOP
            }
    }

    @Inject lateinit var startRanging: StartRangingUseCase
    @Inject lateinit var bridge: RangingServiceBridge

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var rangingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRanging()
            return START_NOT_STICKY
        }

        rangingJob?.cancel()

        val params = OobParamsHolder.params ?: run {
            scope.launch { bridge.emit(RangingState.Failure("OobParams not found")) }
            stopSelf()
            return START_NOT_STICKY
        }

        bridge.setRunning(true)

        rangingJob = scope.launch {
            startRanging(params).collect { state ->
                bridge.emit(state)
                if (state is RangingState.Disconnected || state is RangingState.Failure) {
                    delay(3_000)
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        bridge.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun stopRanging() {
        rangingJob?.cancel()
        stopSelf()
    }
}
