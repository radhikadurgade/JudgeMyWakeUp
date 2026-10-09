# ⏰ JudgeMyWakeUp - The Mocking Alarm App

**JudgeMyWakeUp** is a humorous, high-accountability alarm clock application for Android designed to force users out of bed through interactive challenges and customized Marathi audio roasts.

---

## ✨ Features

- 🔒 **Screen Lock Challenge:** Locks the screen when the alarm goes off, forcing the user to solve a math puzzle (e.g., `15 + 7 = ?`).
- 🗣️ **Marathi Audio Roasts:** Uses Text-To-Speech (TTS) to play funny and sarcastic Marathi audio roasts if the user inputs an incorrect answer.
- ⚙️ **Foreground Service:** Built with a persistent Android Foreground Service and WakeLock to ensure the alarm triggers reliably even in the background.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **IDE:** Android Studio
- **Min SDK:** Android 8.0 (API Level 26)
- **Key Android Components:** 
  - `Foreground Service` (with `mediaPlayback` type)
  - `WakeLock` & `NotificationManager`
  - `TextToSpeech` API (`mr_IN` Marathi locale support)
  - `BroadcastReceiver`

---

## 🚀 How It Works

1. The alarm triggers and launches the `AlarmService`.
2. `MainActivity` comes to the foreground with full screen intent displaying a math problem.
3. If the user answers **incorrectly**, the main alarm stops briefly to play a randomized Marathi roast dialogue before looping back.
4. The alarm only turns off once the correct answer is submitted!
