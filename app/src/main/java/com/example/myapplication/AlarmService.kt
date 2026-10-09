package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {

    private var alarmPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_PLAY_WRONG_SOUND = "ACTION_PLAY_WRONG_SOUND"
        const val ACTION_PLAY_CORRECT_SOUND = "ACTION_PLAY_CORRECT_SOUND"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. WakeLock - CPU aani Screen chalu thevnyasathi
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "RoastAlarm::ServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)

        // 2. TextToSpeech initialize karne
        tts = TextToSpeech(this, this)

        // 3. Foreground Service Start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(101, createNotification())
        }

        // 4. Activity open karne
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("SHOW_TASK", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(activityIntent)

        // 5. Main alarm sound chalu karne
        startAlarmAudio()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Marathi language set karne
            val marathiLocale = Locale("mr", "IN")
            val result = tts?.setLanguage(marathiLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Jar Marathi supported nasel tar Hindi/English fallback
                tts?.language = Locale("hi", "IN")
            }

            isTtsReady = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "ROAST_SPEECH") {
                        // Marathi roast bolun jhalyavar main alarm punha chalu karne
                        startAlarmAudio()
                    }
                }

                override fun onError(utteranceId: String?) {
                    startAlarmAudio()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_WRONG_SOUND -> {
                // 1. Chukiche uttar dilyavar main alarm thambva
                stopAlarmAudio()
                // 2. Crazy Marathi Roast bolee
                speakCrazyMarathiRoast()
            }
            ACTION_PLAY_CORRECT_SOUND -> {
                // Barobar uttar dilyavar sarv band kara
                stopAlarmAudio()
                tts?.stop()
                stopSelf()
            }
            ACTION_STOP_ALARM -> {
                stopAlarmAudio()
                tts?.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAlarmAudio() {
        try {
            if (alarmPlayer == null) {
                val resId = resources.getIdentifier("alarm_loop", "raw", packageName)
                if (resId != 0) {
                    alarmPlayer = MediaPlayer.create(this, resId)
                } else {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    alarmPlayer = MediaPlayer().apply { setDataSource(applicationContext, alarmUri); prepare() }
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                alarmPlayer?.setAudioAttributes(audioAttributes)
                alarmPlayer?.isLooping = true
            }

            if (alarmPlayer?.isPlaying == false) {
                alarmPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmAudio() {
        try {
            if (alarmPlayer?.isPlaying == true) {
                alarmPlayer?.stop()
            }
            alarmPlayer?.release()
            alarmPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakCrazyMarathiRoast() {
        // Crazy Marathi Roast Dialogues
        val marathiDialogues = listOf(
            "अरे भावा! चुकीचं उत्तर दिलं! उठ आता, अजून किती झोपशील?",
            "काय यार! साधं गणित पण येत नाही का? मेंदू अजून झोपलाय वाटतं!",
            "चुकीचं उत्तर! अरे तुझा अलार्म असा कधीच बंद होणार नाही, उठ लवकर!",
            "अरे झोपड्या! चुकीचं उत्तर कसं देतोयस? डोळे उघड आणि नीट उत्तर दे!"
        )
        val randomRoast = marathiDialogues.random()

        if (isTtsReady) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ROAST_SPEECH")
            tts?.speak(randomRoast, TextToSpeech.QUEUE_FLUSH, params, "ROAST_SPEECH")
        } else {
            startAlarmAudio()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "roast_alarm_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Roast Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SHOW_TASK", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 अलार्म चालू आहे!")
            .setContentText("टास्क सोडवून अलार्म बंद करा!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarmAudio()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}