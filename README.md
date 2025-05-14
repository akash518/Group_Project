<h1 align="center">📚 Task Tracker</h1>
<p align="center">
  A modern Android task management app that helps students organize their courses, track assignments, and boost productivity with integrated study tools.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/firebase-%23039BE5.svg?style=for-the-badge&logo=firebase" alt="Firebase">
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white" alt="Material Design">
</p>

## 🌟 Features

### 🔐 User Authentication
- Secure email/password authentication
- Auto-login capabilities
- Account management with logout functionality

### 📖 Course Management
- Create custom courses with unique color coding
- Visual progress tracking for each course
- Delete courses when completed
- Color palette system for easy identification

### ✅ Task Management
- Create tasks with custom titles and due dates
- Set specific times for deadlines
- Mark tasks as complete
- Delete tasks with long-press
- Week-by-week view with swipe navigation
- "All Tasks" view for comprehensive overview

### 📊 Progress Visualization
- Animated circular progress rings
- Course-specific progress tracking
- Weekly completion percentages
- Visual feedback with color-coded interfaces

### ⏰ Pomodoro Timer
- Built-in study timer with customizable duration
- Work/break session tracking
- Long breaks after 4 work sessions
- Reset and pause functionality

### 🔔 Smart Notifications
- Email reminders for upcoming tasks
- 24-hour advance notifications
- Automated reminder system
- Integration with Google Apps Script

### 📱 Modern UI/UX
- Material Design components
- Swipe gestures for navigation
- Smooth animations between screens
- Responsive layouts
- Dark mode support

### 💰 Monetization
- Strategic ad placement
- User-friendly ad controller
- Non-intrusive interstitial ads

## 🛠️ Tech Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Android SDK
- **Design**: Material Design Components
- **Architecture**: MVC Pattern

### Backend & Services
- **Authentication**: Firebase Auth
- **Database**: Firebase Firestore
- **Cloud Functions**: Google Apps Script (Email Service)
- **Ads**: Google AdMob

### Third-Party Libraries
- **OkHttp**: Network requests
- **Google Play Services**: Ads integration
- **Android Lifecycle Components**: Process lifecycle management

## 📂 Project Structure

```
com.example.groupproject/
├── 📱 Activities/
│   ├── HomeView.kt           # Main screen with MVC pattern
│   ├── PomodoroActivity.kt   # Pomodoro timer feature
│   ├── FullTaskViewActivity.kt # All tasks view
│   └── ManageCourses.kt      # Course management screen
│
├── 🎮 Controllers/
│   ├── HomeController.kt     # Main screen logic controller
│   └── AdController.kt       # Ad display management
│
├── 📊 Models/
│   └── HomeModel.kt          # Data management and business logic
│
├── 🔧 Utilities/
│   ├── EmailUtils.kt         # Email reminder functionality
│   ├── CourseColorManager.kt # Course color management
│   └── AdManager.kt          # Ad loading and display
│
├── 🎨 UI Components/
│   ├── ProgressView.kt       # Custom circular progress view
│   ├── TaskAdapter.kt        # RecyclerView adapter for tasks
│   └── CourseAdapter.kt      # RecyclerView adapter for courses
│
├── 📝 Dialogs/
│   ├── CreateAccount.kt      # Login/Signup dialog
│   ├── CourseCreation.kt     # Add course dialog
│   └── TaskCreation.kt       # Create task dialog
│
└── 📦 Data Classes/
    ├── Task.kt              # Task data model
    └── CourseProgress.kt    # Course progress model
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (Latest Version)
- Firebase Account
- Google AdMob Account
- Android SDK 24+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/task-tracker.git
cd task-tracker
```

2. **Firebase Setup**
   - Create a new Firebase project
   - Add Android app with package name `com.example.groupproject`
   - Download `google-services.json`
   - Place it in the `app/` directory

3. **Configure AdMob**
   - Set up AdMob account
   - Replace test ad unit IDs with production IDs
   - Update `AndroidManifest.xml` with your AdMob App ID

4. **Email Service Setup**
   - Deploy Google Apps Script for email functionality
   - Update webhook URL in `EmailUtils.kt`

5. **Build and Run**
```bash
./gradlew assembleDebug
```

## 📱 Screenshots

| Home Screen | Create Task | Pomodoro Timer |
|-------------|-------------|----------------|
| Task overview with progress rings | Add new assignments easily | Built-in study timer |

## 🎯 Key Functionalities

### Week Navigation
- Swipe left/right to navigate between weeks
- Visual date range indicator
- Automatic task filtering by week

### Task Status Management
- Three states: Pending, In Progress, Completed
- Visual indicators for overdue tasks
- One-tap completion marking

### Course Color System
- 9 predefined colors in palette
- Automatic color assignment
- Consistent color usage across app

### Ad Strategy
- Show ads on app launch (once daily)
- Display after every 3rd course/task addition
- Non-intrusive placement

## 🤝 Contributing

This project was developed by:
- **Akash Shah**
- **Nihaar Eppe**
- **Rahaf Alnifie**

### Development Guidelines
1. Follow Kotlin coding conventions
2. Maintain MVC architecture pattern
3. Write descriptive commit messages
4. Test on multiple device sizes
5. Document new features

### Pull Request Process
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Firebase for backend services
- Google Material Design for UI guidelines
- Android Developer Documentation
- AdMob for monetization platform

## 📞 Contact

For questions or support, please create an issue:
- GitHub Issues: [Create an issue](https://github.com/yourusername/task-tracker/issues)

---
