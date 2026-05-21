package com.example.uwbtest.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.uwbtest.R
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.usecase.StartRangingUseCase
import com.example.uwbtest.presentation.MainActivity
import com.example.uwbtest.presentation.screen.oob.OobParamsHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UwbRangingService : LifecycleService() {

    companion object {
        const val ACTION_STOP = "com.example.uwbtest.ACTION_STOP_RANGING"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "uwb_ranging_channel"
        private const val WAKELOCK_TAG = "UwbTester:RangingWakeLock"

        fun startIntent(context: Context) = Intent(context, UwbRangingService::class.java)

        fun stopIntent(context: Context) =
            Intent(context, UwbRangingService::class.java).also { it.action = ACTION_STOP }
    }

    @Inject lateinit var startRanging: StartRangingUseCase
    @Inject lateinit var bridge: RangingServiceBridge

    private var rangingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Always satisfy the ANR timer immediately — Android requires startForeground() within
        // 5 seconds of startForegroundService(), regardless of what the intent asks us to do.
        startForeground(NOTIFICATION_ID, buildNotification(null))

        if (intent?.action == ACTION_STOP) {
            stopRanging()
            return START_NOT_STICKY
        }

        // Idempotent: cancel any existing session before starting a new one.
        rangingJob?.cancel()
        releaseWakeLock()

        bridge.setRunning(true)
        acquireWakeLock()

        val params = OobParamsHolder.params ?: run {
            lifecycleScope.launch { bridge.emit(RangingState.Failure("OobParams not found")) }
            stopSelf()
            return START_NOT_STICKY
        }

        rangingJob = lifecycleScope.launch {
            startRanging(params).collect { state ->
                bridge.emit(state)
                updateNotification(state)
                if (state is RangingState.Disconnected || state is RangingState.Failure) {
                    delay(3_000)
                    stopSelf()
                }
            }
        }

        // START_NOT_STICKY: if the process is killed, do not restart — OobParamsHolder
        // would be empty and ranging cannot resume without the original session parameters.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        bridge.setRunning(false)
        rangingJob?.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun stopRanging() {
        bridge.setRunning(false)
        rangingJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).also {
            it.acquire(30 * 60 * 1000L) // 30-minute safety timeout
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: RangingState?): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.ROUTE_RANGING)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentText = when (state) {
            is RangingState.Active -> buildList {
                state.distanceMeters?.let { add(getString(R.string.notif_distance, it)) }
                state.azimuthDegrees?.let { add(getString(R.string.notif_azimuth, it)) }
            }.joinToString(" • ").ifEmpty { getString(R.string.notif_initializing) }
            is RangingState.Initializing -> getString(R.string.notif_initializing)
            is RangingState.Disconnected -> getString(R.string.notif_disconnected)
            is RangingState.Failure -> getString(R.string.notif_failure)
            else -> getString(R.string.notif_initializing)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(state: RangingState) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(state))
    }
}
