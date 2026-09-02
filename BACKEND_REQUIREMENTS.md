# 🌐 Production Backend API Requirements & Specification

**Project**: LearningApp (Android Mobile Application)  
**Target Engine**: RESTful JSON API  
**Base URL Pattern**: `https://api.learningapp.com/v1`

---

## 🔐 1. Authentication Service (`/v1/auth`)

### 1.1 Login (`POST /v1/auth/login`)
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "email": "student@learningapp.com",
  "password": "Password@123"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user": {
      "id": "usr_1001",
      "name": "Alex Johnson",
      "email": "student@learningapp.com"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "d98f7a6b5c..."
  }
}
```

---

## 📚 2. Dashboard, Study Materials & Unlock Service (`/v1/learning`)

### 2.1 Get Materials List (PDFs & Videos) (`GET /v1/learning/materials?search={query}&filter={filter}`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Response `200 OK`**:
```json
{
  "success": true,
  "data": [
    {
      "id": "pdf_101",
      "title": "Data Structures & Algorithms Cheat Sheet",
      "subject": "Computer Science",
      "pagesCount": 42,
      "fileSizeMb": "4.2 MB",
      "pdfUrl": "https://cdn.learningapp.com/pdfs/dsa_cheatsheet.pdf",
      "videoUrl": null,
      "videoDuration": null,
      "description": "Summary of Arrays, Trees, Graphs, and DP.",
      "category": "Core CS",
      "materialType": "PDF",
      "isLocked": false,
      "priceInInr": 0
    },
    {
      "id": "vid_101",
      "title": "Kotlin Coroutines & Flow Video Masterclass",
      "subject": "Mobile Development",
      "pagesCount": 0,
      "fileSizeMb": "0 MB",
      "pdfUrl": "",
      "videoUrl": "https://cdn.learningapp.com/videos/coroutines.mp4",
      "videoDuration": "45 Mins",
      "description": "Video lecture covering structured concurrency.",
      "category": "Video Lecture",
      "materialType": "VIDEO",
      "isLocked": true,
      "priceInInr": 199
    }
  ]
}
```

### 2.2 Get Available Exams & PYQs (`GET /v1/learning/exams?search={query}&filter={filter}`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Response `200 OK`**: Returns available 50-MCQ mock test & PYQ list with lock status and price.

### 2.3 Unlock Content / Process Payment Pass (`POST /v1/learning/unlock`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Body**:
```json
{
  "itemId": "vid_101",
  "itemType": "MATERIAL",
  "paymentTransactionId": "pay_tx_987123"
}
```
- **Response `200 OK`**: `{"success": true, "message": "Content unlocked successfully"}`

### 2.4 Get User Test Attempt History (`GET /v1/learning/history`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Response `200 OK`**:
```json
{
  "success": true,
  "data": [
    {
      "resultId": "res_887123",
      "examId": "exam_cs_01",
      "examTitle": "All India Full Length CS & General Aptitude Mock Test 1",
      "marksObtained": 152.0,
      "totalMarks": 200,
      "percentage": 76.0,
      "accuracy": 88.2,
      "rank": 28,
      "percentile": 99.37,
      "dateAttemptedFormatted": "02 Sep, 04:30 PM",
      "timeTakenSeconds": 2340
    }
  ]
}
```

---

## 📝 3. Mock Test & Question Engine Service (`/v1/exams`)

### 3.1 Get Questions for Exam (50 MCQs) (`GET /v1/exams/{examId}/questions`)
### 3.2 Submit Exam Answers & Calculate Results (`POST /v1/exams/{examId}/submit`)
### 3.3 Get Detailed Scorecard & Solutions Review (`GET /v1/exams/results/{resultId}`)
