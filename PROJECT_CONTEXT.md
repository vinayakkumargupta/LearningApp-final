# 📱 Learning App — Android Project Context, Vision & Architecture Guide

> **Target Audience**: AI Agents & Android Developers.  
> This file provides complete awareness of the Android app's architecture, package structure, state management, screen flow, and developer guidelines.

---

## 🎯 1. Project Vision & Overview

LearningApp is a modern, scalable Android mobile application designed for students, competitive exam candidates, and content managers/teachers. The app provides:

- **Real Firebase Authentication**: Login, Signup, and Password Reset emails via `FirebaseAuth`, synced with Firestore `users/{uid}` documents.
- **Role-Based Access Control (RBAC)**:
  - `MEMBER` Role: Access to student dashboard, mock tests, PDFs, videos, test attempts, scorecards, and history.
  - `PENDING_ADMIN` Role: Student who requested admin access during signup, awaiting approval.
  - `ADMIN` Role: Content Admin (can create/edit exams, questions, study materials).
  - `SUPER_ADMIN` Role: Super Admin / App Owner (unrestricted control, user approvals, role promotion/demotion, bulk JSON import, content management).
- **Tabbed Dashboard**:
  - **Tab 0 (Questions & PYQs)**: Previous Year Papers & 50-MCQ Mock Series.
  - **Tab 1 (PDFs & Video Lectures)**: PDF Study Notes & Video Masterclasses.
  - **Tab 2 (Attempt History)**: Complete history log of past test scores, All India Ranks, percentiles, and scorecard links.
- **In-App Admin Content Console (`admin_console`)**:
  - **Tab 0 (User Roles)**: Approve pending admins, promote/demote user roles.
  - **Tab 1 (Exams & Qs)**: Create / Overwrite Exam Series with custom questions, options A–D, correct answer index, explanations.
  - **Tab 2 (PDFs & Videos)**: Create / Manage Study PDFs & Video Lectures with pricing.
  - **Tab 3 (📦 Bulk JSON Import)**: Bulk import multiple exam series and question banks from JSON directly into Firestore!
- **Real-Time All India Rank & Percentile Engine**:
  - Queries real student submissions in Firestore (`submissions` collection) for the exam.
  - Calculates exact student rank based on marks obtained vs other real students.
  - Computes exact percentile: `((Total Submissions - Rank + 1) / Total Submissions) * 100`.
- **Verified Scorecard & Solution Review**:
  - Scorecard displays real calculated marks, real rank, real percentile, and accuracy %.
  - Reconstructs exact user answers vs correct options with step-by-step explanations.

---

## 🛠️ 2. Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3 & Navigation Compose
- **Dependency Injection**: Dagger Hilt 2.60.1 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Firebase SDK**: Firebase BoM 33.7.0 (Authentication, Cloud Firestore, Analytics)
- **Architecture**: MVVM + Repository Pattern + Clean Architecture
- **Minimum SDK**: 24 (Android 7.0) | **Compile SDK**: 35 (Android 15)

---

## 📂 3. Package Structure & Directory Layout

```
com.univ.learningapp/
├── LearningApplication.kt                 # Application Entry Point (@HiltAndroidApp)
├── MainActivity.kt                        # Single Activity Host (@AndroidEntryPoint)
├── di/
│   └── RepositoryModule.kt                # Dagger Hilt @Binds bindings (FirebaseAuth/Firestore -> Domain Repos)
├── data/
│   ├── model/                             # Data Transfer Objects & Entities
│   │   ├── User.kt                        # User profile model (id, name, email, role: MEMBER | PENDING_ADMIN | ADMIN | SUPER_ADMIN)
│   │   ├── PdfMaterial.kt                 # PDF notes & Video model (type, title, subject, pages, duration, url, isLocked, price)
│   │   ├── Question.kt                    # Question & UserQuestionState models
│   │   ├── ExamInfo.kt                    # Exam metadata model (50 questions, 60 mins, 200 marks, category, isLocked, price)
│   │   ├── ExamResult.kt                  # Result, scorecard, rank, accuracy & review item models
│   │   └── TestHistoryItem.kt             # Test history log entity (date, score, rank, percentile, accuracy)
│   └── repository/                        # Real Firebase Repository Implementation
│       ├── FirebaseAuthRepositoryImpl.kt  # Real Firebase Auth & Firestore users/{uid} profile/role sync & approvals
│       ├── FirebaseLearningRepositoryImpl.kt # Real Firestore materials, test_series, admin CRUD, bulk JSON import
│       └── FirebaseTestRepositoryImpl.kt  # Dynamic questions fetch, real-time All India Rank & percentile
├── domain/
│   └── repository/                        # Abstraction Repository Interfaces
│       ├── AuthRepository.kt              # Auth & User Roles interface contract
│       ├── LearningRepository.kt          # PDFs/Videos, Exam list, unlock, test history, Admin CRUD, Bulk JSON contract
│       └── TestRepository.kt              # Mock test, submission & result review contract
└── ui/
    ├── theme/                             # Color.kt, Type.kt, Theme.kt (Custom M3 styling)
    ├── navigation/                        # Screen.kt, NavGraph.kt (Jetpack Compose Navigation)
    ├── admin/                             # Admin Console Feature
    │   ├── AdminViewModel.kt              # Admin content & user roles manager viewmodel
    │   └── AdminConsoleScreen.kt          # User roles, Exam & MCQ creator, PDF/Video creator, Bulk JSON Importer
    ├── auth/                              # Authentication Feature
    │   ├── AuthViewModel.kt               # StateFlow auth viewmodel
    │   ├── LoginScreen.kt                 # Login UI composable
    │   ├── SignupScreen.kt                # Signup UI composable
    │   └── ForgotPasswordScreen.kt        # Password reset UI composable
    ├── dashboard/                         # Dashboard Feature
    │   ├── DashboardViewModel.kt          # Tabs, search query, filter selection, unlock viewmodel
    │   └── DashboardScreen.kt             # TopBar history, search bar, 3 tabs, admin console button
    ├── history/                           # Test History Feature
    │   ├── TestHistoryViewModel.kt        # History loader viewmodel
    │   └── TestHistoryScreen.kt           # Past test attempts, rank, scores, scorecard link
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
| `login` | `LoginScreen` | Email/password sign-in against Firebase Auth |
| `signup` | `SignupScreen` | Account registration (`MEMBER` or `ADMIN`) |
| `forgot_password` | `ForgotPasswordScreen` | Password reset email request via Firebase |
| `dashboard` | `DashboardScreen` | User welcome, 3 Tabs (Mock Tests, Study Notes, History) |
| `admin_console` | `AdminConsoleScreen` | In-App Content Creator for `ADMIN`/`SUPER_ADMIN` users |
| `test_history` | `TestHistoryScreen` | History of all attempted test scores, ranks & review links |
| `mock_test/{examId}/{examTitle}` | `MockTestScreen` | 50-MCQ exam launcher with 60-min timer & 1..50 palette |
| `test_result/{resultId}` | `TestResultScreen` | Overall Scorecard, Real Rank #, Percentile %, Accuracy % |
| `detailed_review/{resultId}` | `DetailedReviewScreen` | Complete 50-question review with user answers vs correct options |
