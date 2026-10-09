package com.example.myapplication

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock Screen bypass karun screen forcefully on thevne
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContentView(R.layout.activity_main)

        val setAlarmButton = findViewById<Button>(R.id.setAlarmButton)

        setAlarmButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val alarmTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                }

                if (alarmTime.before(Calendar.getInstance())) {
                    alarmTime.add(Calendar.DATE, 1)
                }

                setExactAlarm(alarmTime.timeInMillis)
            }, hour, minute, false)

            timePickerDialog.show()
        }

        if (intent.getBooleanExtra("SHOW_TASK", false)) {
            showMathTaskDialog()
        }
    }

    private fun setExactAlarm(triggerTimeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Toast.makeText(this, "Roast Alarm Set!", Toast.LENGTH_SHORT).show()
    }

    private fun showMathTaskDialog() {
        val num1 = (5..15).random()
        val num2 = (5..15).random()
        val correctAnswer = num1 + num2

        val input = EditText(this)
        input.hint = "Uttar ithe liha"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("🚨 Alarm band karnyasathi task sodva!")
        builder.setMessage("Uttar dya: $num1 + $num2 = ?")
        builder.setView(input)
        builder.setCancelable(false)

        builder.setPositiveButton("SUBMIT") { dialog, _ ->
            val userAnswer = input.text.toString().toIntOrNull()

            if (userAnswer == correctAnswer) {
                // 1. Barobar uttar: Service dware correct_sound play hoil aani alarm band hoil
                val correctIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_PLAY_CORRECT_SOUND
                }
                startService(correctIntent)

                Toast.makeText(this, "Barobar uttar! Alarm band hot aahe...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                finish()
            } else {
                // 2. Chukiche uttar: Service dware wrong_sound vajel (alarm chaluch rahil)
                val wrongIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_PLAY_WRONG_SOUND
                }
                startService(wrongIntent)

                Toast.makeText(this, "Chukiche uttar! Alarm chaluch rahil 😂", Toast.LENGTH_SHORT).show()
                showMathTaskDialog() // Ganit punha vicharne
            }
        }

        builder.show()
    }
}