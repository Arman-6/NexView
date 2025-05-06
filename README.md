<div align="center">
  <img src="https://iili.io/3NiEwkg.png" alt="NexView Logo" width="400"/>
  
  # 📺 NexView: Content Curation Platform
  

  <p align="center">
    <a href="#features">Features</a> •
    <a href="#demo">Demo</a> •
    <a href="#installation">Installation</a> •
    <a href="#technologies">Technologies</a> •
    <a href="#architecture">Architecture</a> •
    <a href="#contribute">Contribute</a>
  </p>
  
  <br>
  
  <p><b>Smart learning platform that keeps you focused on educational content</b></p>
  <p>📱 Session tracking • ⏱️ Pomodoro integration • 🔍 Smart filtering</p>
</div>

<hr>

<a name="features"></a>
## ✨ Features

<table>
  <tr>
    <td width="50%">
      <h3 align="center">📊 Smart Session Management</h3>
      <p>Track watch history and liked videos per session, with auto-deletion options for better focus and organization.</p>
    </td>
    <td width="50%">
      <h3 align="center">🔎 Content Filtering</h3>
      <p>Title-based and custom keyword filters block distractions like TV shows or music, keeping your learning focused.</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3 align="center">⏱️ Pomodoro Timer</h3>
      <p>Built-in 25-min study / 5-min break cycles with reminders and tracking to maximize productivity.</p>
    </td>
    <td width="50%">
      <h3 align="center">📈 Watch Time Analytics</h3>
      <p>Monitor total watch time and daily goals stored in Firestore with beautiful visual reports.</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3 align="center">🎬 Seamless YouTube Playback</h3>
      <p>Like, dislike, share, and save videos with a clean, intuitive UI designed for learning.</p>
    </td>
    <td width="50%">
      <h3 align="center">🎯 Daily Goals</h3>
      <p>Visual progress tracking and reminders to help you stay productive and meet your learning objectives.</p>
    </td>
  </tr>
</table>


<hr>

<a name="demo"></a>
## 🎥 App Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="https://iili.io/3NiZma2.jpg" alt="Login Screen" width="200"/>
        <p>Login Screen</p>
      </td>
      <td align="center">
        <img src="https://iili.io/3NiZb3l.jpg" alt="DND" width="200"/>
        <p>DND</p>
      </td>
      <td align="center">
        <img src="https://iili.io/3NiZt44.jpg" alt="Home" width="200"/>
        <p>Home</p>
      </td>
       <td align="center">
        <img src="https://iili.io/3NiZZGf.jpg" alt="Search" width="200"/>
        <p>Search</p>
      </td>
       <td align="center">
        <img src="https://iili.io/3NiZQCG.jpg" alt="Video Player" width="200"/>
        <p>Video Player</p>
      </td>
       <td align="center">
        <img src="https://iili.io/3NiZyy7.jpg" alt="Profile and Daily Goal" width="200"/>
        <p>Profile and Daily Goal</p>
      </td>
       <td align="center">
        <img src="https://iili.io/3NiZpvS.jpg" alt="Streak and Pomodoro" width="200"/>
        <p>Streak and Pomodoro</p>
      </td>
    </tr>
  </table>
</div>

<hr>

<a name="technologies"></a>
## 🛠️ Technologies Used

<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase"/>
  <img src="https://img.shields.io/badge/YouTube_API-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="YouTube API"/>
  <img src="https://img.shields.io/badge/Material_UI-0081CB?style=for-the-badge&logo=material-ui&logoColor=white" alt="Material UI"/>
</div>

<hr>

<a name="architecture"></a>
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

<hr>

## 🔄 Pomodoro Flow

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Step 1</b></td>
      <td align="center"><b>Step 2</b></td>
      <td align="center"><b>Step 3</b></td>
      <td align="center"><b>Step 4</b></td>
    </tr>
    <tr>
      <td align="center">Start Timer when<br>video begins</td>
      <td align="center">25 mins work →<br>5 mins break cycle</td>
      <td align="center">UI dialog prompts<br>break countdown</td>
      <td align="center">Pomodoro data<br>logged in Firestore</td>
    </tr>
    <tr>
      <td align="center">⏱️</td>
      <td align="center">🔄</td>
      <td align="center">💬</td>
      <td align="center">💾</td>
    </tr>
  </table>
</div>

<hr>

## 🔐 Filtering Mechanism

<div align="center">
  <table>
    <tr>
      <td width="70%">
        <ul>
          <li>Keywords like "serial", "music", "tv show" are blocked from recommendations</li>
          <li>Uses <b>strict keyword match</b> for precise filtering</li>
          <li>Scans both <b>title</b> and <b>description</b> of each video</li>
          <li>Topic-based exceptions available for relevant educational content</li>
        </ul>
      </td>
      <td width="30%" align="center">
        <h3>🚫 → 🎬</h3>
        <p>Distractions filtered,<br>learning enhanced</p>
      </td>
    </tr>
  </table>
</div>

<hr>

## 📌 Key Functionalities

<div align="center">
  <table>
    <tr>
      <td align="center" width="20%"><b>📝</b><br>Video tracking with<br>timestamps</td>
      <td align="center" width="20%"><b>👍👎</b><br>Like, dislike, save,<br>and share</td>
      <td align="center" width="20%"><b>🔄</b><br>Portrait and<br>landscape views</td>
      <td align="center" width="20%"><b>🎯</b><br>Smart video<br>recommendations</td>
      <td align="center" width="20%"><b>🧭</b><br>Intuitive<br>navigation</td>
    </tr>
  </table>
</div>

<hr>

<a name="installation"></a>
## 📦 Installation & Setup

<div align="center">
  <table>
    <tr>
      <td><b>Step 1:</b></td>
      <td>Clone the repository</td>
      <td>
        <code>git clone https://github.com/yourusername/NexView.git</code>
      </td>
    </tr>
    <tr>
      <td><b>Step 2:</b></td>
      <td>Open in Android Studio</td>
      <td>Let Gradle sync all dependencies</td>
    </tr>
    <tr>
      <td><b>Step 3:</b></td>
      <td>Set up Firebase</td>
      <td>
        Connect to Firebase<br>
        Add <code>google-services.json</code>
      </td>
    </tr>
    <tr>
      <td><b>Step 4:</b></td>
      <td>Add API keys</td>
      <td>
        Add keys in <code>strings.xml</code> or <code>gradle.properties</code>
      </td>
    </tr>
    <tr>
      <td><b>Step 5:</b></td>
      <td>Run the app</td>
      <td>On emulator or physical device</td>
    </tr>
  </table>
</div>

<hr>

<a name="contribute"></a>
## ✨ Contributions

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <h3>🍴 Fork</h3>
        <p>Fork this repository</p>
      </td>
      <td align="center" width="33%">
        <h3>👩‍💻 Code</h3>
        <p>Make your changes</p>
      </td>
      <td align="center" width="33%">
        <h3>🔀 PR</h3>
        <p>Submit a pull request</p>
      </td>
    </tr>
  </table>
  <p>Contributions, issues, and feature requests are welcome!</p>
</div>

<hr>

<div align="center">
  
  <h2><strong>NexView</strong> – Learn Smarter. Watch Wiser. Stay Focused. 🎯</h2>
</div>
