# 📱 Learning App — Android Project Context, Vision & Architecture Guide

> **Target Audience**: AI Agents & Android Developers.  
> This file provides complete awareness of the Android app's architecture, package structure, state management, screen flow, and developer guidelines.

---

## 🎯 1. Project Vision & Overview

LearningApp is a modern, scalable Android mobile application designed for students and competitive exam candidates. The app provides:
- **Authentication**: Login, Signup, Forgot Password with input validation.
- **Tabbed Dashboard**:
  - **Tab 1 (Questions & PYQs)**: Previous Year Question papers (PYQ) & 50-MCQ Mock Series.
  - **Tab 2 (PDFs & Video Lectures)**: PDF Study Notes & Video Masterclasses.
- **Live Search & Bottom Filter Bar**: Real-time searching across titles/subjects + category filtering (All, Free Access, Locked / Premium, Completed).
- **Lock & Payment Unlock System**: Content items display "Locked 🔒" badges and price tags, with a simulated payment dialog flow that unlocks content for the student upon payment.
- **Test History Log & Performance Analytics**: Top Bar History button on the Dashboard to view all past test attempts, scores obtained, All India Ranks, percentiles, accuracy %, and a button to re-review solutions for any past test.
- **Full-Length 50-MCQ Mock Test Engine**: Features a 60-minute countdown timer, interactive 1–50 question palette, answer bookmarking, automated scoring (+4/-1 scheme), All India Rank calculation, percentile analytics, and detailed step-by-step solution reviews.

The app is built using **Clean Architecture (MVVM + Repository Pattern)** and **Dagger Hilt** dependency injection.

---

## 🛠️ 2. Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3 & Navigation Compose
- **Dependency Injection**: Dagger Hilt 2.60.1 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Annotation Processing**: KSP (Kotlin Symbol Processing)
- **Asynchronous State**: Kotlin Coroutines & StateFlow
- **Architecture**: MVVM + Repository Pattern + Clean Architecture
- **Minimum SDK**: 24 (Android 7.0) | **Compile SDK**: 35 (Android 15)

---

## 📂 3. Package Structure & Directory Layout

```
com.univ.learningapp/
├── LearningApplication.kt                 # Application Entry Point (@HiltAndroidApp)
├── MainActivity.kt                        # Single Activity Host (@AndroidEntryPoint)
├── di/
│   └── RepositoryModule.kt                # Dagger Hilt @Binds bindings (Interfaces -> Impl)
├── data/
│   ├── model/                             # Data Transfer Objects & Entities
│   │   ├── User.kt                        # User profile model (id, name, email, token)
│   │   ├── PdfMaterial.kt                 # PDF notes & Video model (type, title, subject, pages, duration, url, isLocked, price)
│   │   ├── Question.kt                    # Question & UserQuestionState models
│   │   ├── ExamInfo.kt                    # Exam metadata model (50 questions, 60 mins, 200 marks, category, isLocked, price)
│   │   ├── ExamResult.kt                  # Result, scorecard, rank, accuracy & review item models
│   │   └── TestHistoryItem.kt             # Test history log entity (date, score, rank, percentile, accuracy)
│   └── repository/                        # Local Mock Repository Implementations
│       ├── AuthRepositoryImpl.kt          # Mock login, signup, password reset flow
│       ├── LearningRepositoryImpl.kt      # Mock PDF/Video list, exam list, unlock flow, test history
│       └── TestRepositoryImpl.kt          # 50-MCQ question generator, timer & rank calculator
├── domain/
│   └── repository/                        # Abstraction Repository Interfaces
│       ├── AuthRepository.kt              # Auth interface contract
│       ├── LearningRepository.kt          # PDFs/Videos, Exam list, unlock, test history contract
│       └── TestRepository.kt              # Mock test, submission & result review contract
└── ui/
    ├── theme/                             # Color.kt, Type.kt, Theme.kt (Custom M3 styling)
    ├── navigation/                        # Screen.kt, NavGraph.kt (Jetpack Compose Navigation)
    ├── auth/                              # Authentication Feature
    │   ├── AuthViewModel.kt               # StateFlow auth viewmodel
    │   ├── LoginScreen.kt                 # Login UI composable
    │   ├── SignupScreen.kt                # Signup UI composable
    │   └── ForgotPasswordScreen.kt        # Password reset UI composable
    ├── dashboard/                         # Dashboard Feature
    │   ├── DashboardViewModel.kt          # Tabs, search query, filter selection, unlock viewmodel
    │   └── DashboardScreen.kt             # TopBar history, search bar, 2 tabs, filter chips, lock/unlock payment dialog
    ├── history/                           # Test History Feature
    │   ├── TestHistoryViewModel.kt        # History loader viewmodel
    │   └── TestHistoryScreen.kt           # Past test attempts, rank, scores, solution review link
    └── test/                              # Mock Test & Solution Review Feature
        ├── TestViewModel.kt               # Timer, 50-MCQ state, palette state & submit viewmodel
        ├── MockTestScreen.kt              # Timer header, 1..50 question palette, submit dialog
        ├── TestResultScreen.kt            # Scorecard, Rank #, Percentile %, Accuracy %
        └── DetailedReviewScreen.kt        # Tab filters (All, Correct, Incorrect, Unattempted), Solutions
```

---

## 🗺️ 4. Navigation Routes & Screen Flow

| Route | Composable Screen | Purpose |
| :--- | :--- | :--- |
| `login` | `LoginScreen` | Email/password sign-in, signup/forgot pass links |
| `signup` | `SignupScreen` | Account registration |
| `forgot_password` | `ForgotPasswordScreen` | Password reset link request |
| `dashboard` | `DashboardScreen` | User welcome, 2 Tabs (Questions/Videos), Search & Filters |
| `test_history` | `TestHistoryScreen` | History of all attempted test scores, ranks & review links |
| `mock_test/{examId}/{examTitle}` | `MockTestScreen` | 50-MCQ exam launcher with 60-min timer & 1..50 palette |
| `test_result/{resultId}` | `TestResultScreen` | Overall Scorecard, Rank #, Percentile %, Accuracy % |
| `detailed_review/{resultId}` | `DetailedReviewScreen` | Complete 50-question review with solutions & filters |

---

## 🔌 5. How to Connect Live REST APIs

To connect a live REST API backend:
1. Add Retrofit/Ktor dependencies in `gradle/libs.versions.toml`.
2. Create API service interfaces in `data/remote/` (e.g., `AuthApiService`, `LearningApiService`).
3. Create remote repository implementations (e.g., `RemoteAuthRepositoryImpl`, `RemoteLearningRepositoryImpl`).
4. Update Dagger Hilt bindings in [`RepositoryModule.kt`](file:///C:/Users/vinu8/AndroidStudioProjects/LearningApp/app/src/main/java/com/univ/learningapp/di/RepositoryModule.kt):
   Change `@Binds` implementations from local mock classes to remote classes.
5. All ViewModels and Composables consume Domain Repository Interfaces, so no UI code changes are needed!
