# 📺 NexView: Smart YouTube Learning Tracker

**NexView** is a modern Android application that helps learners stay focused on educational YouTube content. With session-based tracking, Pomodoro integration, and keyword filtering, NexView ensures smarter, distraction-free learning.

---

## 🚀 Features

- **Smart Session Management**: Track watch history and liked videos per session, with auto-deletion options.
- **Filtering**: Title based as well as Custom keyword filters block distractions like TV shows or music.
- **Pomodoro Timer**: Built-in 25-min study / 5-min break cycles with reminders and tracking.
- **Watch Time Analytics**: Monitor total watch time and daily goals stored in Firestore.
- **Seamless YouTube Playback**: Like, dislike, share, and save videos with a clean UI.
- **Daily Goals**: Visual progress tracking and reminders to stay productive.

---

## 🛠 Technologies Used

- **Java (Android)**
- **Firebase Firestore**
- **YouTube Data API**
- **SharedPreferences**
- **Material UI Components**

---

## 📁 Project Structure

```
app/
├── manifests/
├── java/
│   └── com.example.nexview/
│       ├── auth/
│       │   ├── LoginActivity
│       │   ├── SigninActivity
│       │   └── SignupActivity
│       ├── pages/
│       │   ├── HistoryFragment
│       │   ├── HomeFragment
│       │   ├── LikedFragment
│       │   ├── MainActivity
│       │   ├── ProfileFragment
│       │   ├── SearchFragment
│       │   └── VideoPlayerActivity
│       ├── session/
│       │   ├── CreateSessionActivity
│       │   ├── Session
│       │   └── SessionAdapter
│       ├── youtube/
│       │   ├── VideoAdapter
│       │   ├── VideoModel
│       │   ├── YouTubeApiService
│       │   └── YouTubeResponse
│       ├── BreakDialogActivity
│       ├── GreenDecorator
│       ├── GreyDecorator
│       └── PomodoroTimerService
└── com.example.nexview (android/test)
```

---

## 🔁 Pomodoro Flow

1. **Start Timer** when video starts playing.
2. **25 mins work → 5 mins break** cycle begins.
3. **UI dialog** prompts break countdown and resumption.
4. **Pomodoro time** gets logged in Firestore under the active session.

---

## 🔐 Filtering Mechanism

- Keywords like "serial", "music", "tv show" are blocked from recommendations unless the selected topic matches.
- Uses **strict keyword match** (no approximate filtering).
- Checks both **title** and **description** of each video.

---

## 📌 Key Functionalities

- Video tracking with **timestamps** in Firestore.
- **Like, dislike, save, and share** capabilities.
- Switch between **portrait and landscape** video views.
- Shows **recommended videos** under currently watched content.
- Transition from video player to **bottom navigation fragments**.

---

## 📦 Installation & Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/yourusername/NexView.git
   ```
2. Open in **Android Studio** and let Gradle sync.
3. Set up Firebase:
   * Connect project to Firebase.
   * Add `google-services.json`.
4. Add required API keys in `strings.xml` or `gradle.properties`.
5. Run on emulator or device.

## ✨ Contributions

Contributions, issues, and feature requests are welcome! Feel free to fork the repo and submit a pull request.

## 📄 License

This project is licensed under the MIT License.

**NexView** – Learn Smarter. Watch Wiser. Stay Focused. 🎯
