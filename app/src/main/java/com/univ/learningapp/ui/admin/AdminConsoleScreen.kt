package com.univ.learningapp.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.MaterialType
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.data.model.User
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.ui.theme.SuccessGreen
import com.univ.learningapp.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val existingExams by viewModel.existingExams.collectAsState()
    val existingMaterials by viewModel.existingMaterials.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser = viewModel.currentUser

    var activeTab by remember { mutableStateOf(0) } // 0: Users, 1: Exams, 2: Materials, 3: Bulk JSON

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin Console ⚙️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status/Error Banner
            when (val state = uiState) {
                is AdminUiState.Success -> {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.message, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is AdminUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                else -> {}
            }

            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("User Roles", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Exams & Qs", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("PDFs & Videos", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("📦 Bulk JSON", fontWeight = FontWeight.Bold) }
                )
            }

            when (activeTab) {
                0 -> UserRolesAdminManager(
                    allUsers = allUsers,
                    currentUserRole = currentUser?.role,
                    onUpdateRole = { uid, newRole -> viewModel.updateUserRole(uid, newRole) }
                )
                1 -> ExamAdminManager(
                    existingExams = existingExams,
                    isLoading = uiState is AdminUiState.Loading,
                    onSaveExam = { exam, questions -> viewModel.saveExam(exam, questions) },
                    onDeleteExam = { id -> viewModel.deleteExam(id) }
                )
                2 -> MaterialAdminManager(
                    existingMaterials = existingMaterials,
                    isLoading = uiState is AdminUiState.Loading,
                    onSaveMaterial = { material -> viewModel.saveMaterial(material) },
                    onDeleteMaterial = { id -> viewModel.deleteMaterial(id) }
                )
                else -> BulkAdminManager(
                    isLoading = uiState is AdminUiState.Loading,
                    onUploadBulkJson = { jsonString -> viewModel.bulkUploadExamsJson(jsonString) }
                )
            }
        }
    }
}

@Composable
fun BulkAdminManager(
    isLoading: Boolean,
    onUploadBulkJson: (String) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var showSchema by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidated by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bulk Import Exams & Questions from JSON",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste a single exam JSON object, or an array of exam objects. Uploading with an existing Exam ID overwrites its question list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        isValidated = false
                        validationMessage = null
                    },
                    label = { Text("Paste Exam Series JSON Here") },
                    placeholder = { Text("[\n  {\n    \"id\": \"exam_mock_51_cs\",\n    \"title\": \"All India Mock Test 5\", ...\n  }\n]") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSchema = !showSchema },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (showSchema) Icons.Default.ExpandLess else Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showSchema) "Hide Schema" else "Show Schema", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val raw = jsonInput.trim()
                            if (raw.isBlank()) {
                                validationMessage = "⚠ Please paste JSON content first."
                                isValidated = false
                                return@OutlinedButton
                            }
                            try {
                                val array = if (raw.startsWith("[")) org.json.JSONArray(raw) else org.json.JSONArray().put(org.json.JSONObject(raw))
                                var totalQs = 0
                                for (i in 0 until array.length()) {
                                    val qArr = array.getJSONObject(i).optJSONArray("questions")
                                    totalQs += qArr?.length() ?: 0
                                }
                                validationMessage = "✓ Valid JSON format! Detected ${array.length()} exam(s) and $totalQs total questions."
                                isValidated = true
                            } catch (e: Exception) {
                                validationMessage = "✗ JSON Error: ${e.localizedMessage}"
                                isValidated = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Validate JSON", fontSize = 12.sp)
                    }
                }

                validationMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontWeight = FontWeight.Bold,
                        color = if (isValidated) SuccessGreen else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = { onUploadBulkJson(jsonInput) },
                    enabled = !isLoading && jsonInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload All to Firestore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSchema) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Expected JSON Schema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """[
  {
    "id": "exam_mock_51_cs",
    "title": "All India Mock Test 5",
    "subject": "Computer Science",
    "examCategory": "MCQ Mock Test",
    "durationMinutes": 60,
    "totalMarks": 200,
    "priceInInr": 0,
    "questions": [
      {
        "questionText": "Which keyword in Kotlin declares a read-only variable?",
        "options": ["var", "val", "const var", "final var"],
        "correctOptionIndex": 1,
        "explanation": "'val' creates a read-only variable in Kotlin.",
        "subject": "Kotlin Programming"
      }
    ]
  }
]""",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserRolesAdminManager(
    allUsers: List<User>,
    currentUserRole: UserRole?,
    onUpdateRole: (String, UserRole) -> Unit
) {
    val isSuperAdmin = currentUserRole == UserRole.SUPER_ADMIN
    val isAdmin = currentUserRole == UserRole.ADMIN

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "User Approvals & Role Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSuperAdmin) "You are logged in as SUPER_ADMIN. You can approve pending admins, promote members, or demote roles." else "View registered platform members.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        items(allUsers) { user ->
            UserRoleCard(
                user = user,
                isSuperAdmin = isSuperAdmin,
                isAdmin = isAdmin,
                onUpdateRole = { newRole -> onUpdateRole(user.id, newRole) }
            )
        }
    }
}

@Composable
fun UserRoleCard(
    user: User,
    isSuperAdmin: Boolean,
    isAdmin: Boolean,
    onUpdateRole: (UserRole) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (user.role) {
                                    UserRole.SUPER_ADMIN -> WarningOrange
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                    UserRole.PENDING_ADMIN -> WarningOrange.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = user.name, fontWeight = FontWeight.Bold)
                        // Hide email if viewer is not Super Admin
                        if (isSuperAdmin) {
                            Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                Surface(
                    color = when (user.role) {
                        UserRole.SUPER_ADMIN -> WarningOrange.copy(alpha = 0.2f)
                        UserRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                        UserRole.PENDING_ADMIN -> WarningOrange.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = user.role.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (user.role) {
                            UserRole.SUPER_ADMIN -> WarningOrange
                            UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                            UserRole.PENDING_ADMIN -> WarningOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Only Super Admins and Admins get to see action buttons
            if ((isSuperAdmin || isAdmin) && user.role != UserRole.SUPER_ADMIN) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (user.role == UserRole.PENDING_ADMIN || user.role == UserRole.MEMBER) {
                        Button(
                            onClick = { onUpdateRole(UserRole.ADMIN) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Promote / Approve ADMIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (user.role == UserRole.ADMIN) {
                        // Only SUPER_ADMIN can demote another ADMIN
                        if (isSuperAdmin) {
                            OutlinedButton(
                                onClick = { onUpdateRole(UserRole.MEMBER) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Demote to MEMBER", fontSize = 12.sp)
                            }
                        } else {
                            Text("Role managed by Super Admin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamAdminManager(
    existingExams: List<ExamInfo>,
    isLoading: Boolean,
    onSaveExam: (ExamInfo, List<Question>) -> Unit,
    onDeleteExam: (String) -> Unit
) {
    var examId by remember { mutableStateOf("exam_mock_${(10..99).random()}") }
    var examTitle by remember { mutableStateOf("") }
    var examSubject by remember { mutableStateOf("Computer Science") }
    var examCategory by remember { mutableStateOf("MCQ Mock Test") }
    var durationMinutes by remember { mutableStateOf("60") }
    var totalMarks by remember { mutableStateOf("200") }
    var priceInInr by remember { mutableStateOf("0") }

    var questionsList by remember {
        mutableStateOf(
            mutableListOf(
                QuestionFormState(
                    id = 1,
                    questionText = "",
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctOptionIndex = 0,
                    explanation = ""
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create / Overwrite Exam Series", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = examId,
                    onValueChange = { examId = it },
                    label = { Text("Exam ID (Firestore Document ID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examTitle,
                    onValueChange = { examTitle = it },
                    label = { Text("Exam Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examSubject,
                        onValueChange = { examSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = examCategory,
                        onValueChange = { examCategory = it },
                        label = { Text("Category (MCQ / PYQ)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Duration (Mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceInInr,
                        onValueChange = { priceInInr = it },
                        label = { Text("Price (0 = Free)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Text("Questions (${questionsList.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                questionsList.forEachIndexed { qIdx, qForm ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Question #${qIdx + 1}", fontWeight = FontWeight.Bold)
                                if (questionsList.size > 1) {
                                    IconButton(onClick = {
                                        val newList = questionsList.toMutableList()
                                        newList.removeAt(qIdx)
                                        questionsList = newList
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = qForm.questionText,
                                onValueChange = {
                                    val newList = questionsList.toMutableList()
                                    newList[qIdx] = qForm.copy(questionText = it)
                                    questionsList = newList
                                },
                                label = { Text("Question Text") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = qForm.optionA,
                                    onValueChange = {
                                        val newList = questionsList.toMutableList()
                                        newList[qIdx] = qForm.copy(optionA = it)
                                        questionsList = newList
                                    },
                                    label = { Text("Option A") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = qForm.optionB,
                                    onValueChange = {
                                        val newList = questionsList.toMutableList()
                                        newList[qIdx] = qForm.copy(optionB = it)
                                        questionsList = newList
                                    },
                                    label = { Text("Option B") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = qForm.optionC,
                                    onValueChange = {
                                        val newList = questionsList.toMutableList()
                                        newList[qIdx] = qForm.copy(optionC = it)
                                        questionsList = newList
                                    },
                                    label = { Text("Option C") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = qForm.optionD,
                                    onValueChange = {
                                        val newList = questionsList.toMutableList()
                                        newList[qIdx] = qForm.copy(optionD = it)
                                        questionsList = newList
                                    },
                                    label = { Text("Option D") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Correct Answer: ", fontWeight = FontWeight.Bold)
                                listOf("A", "B", "C", "D").forEachIndexed { optIdx, optLabel ->
                                    FilterChip(
                                        selected = qForm.correctOptionIndex == optIdx,
                                        onClick = {
                                            val newList = questionsList.toMutableList()
                                            newList[qIdx] = qForm.copy(correctOptionIndex = optIdx)
                                            questionsList = newList
                                        },
                                        label = { Text(optLabel) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = qForm.explanation,
                                onValueChange = {
                                    val newList = questionsList.toMutableList()
                                    newList[qIdx] = qForm.copy(explanation = it)
                                    questionsList = newList
                                },
                                label = { Text("Solution / Explanation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val newList = questionsList.toMutableList()
                        newList.add(
                            QuestionFormState(
                                id = newList.size + 1,
                                questionText = "",
                                optionA = "",
                                optionB = "",
                                optionC = "",
                                optionD = "",
                                correctOptionIndex = 0,
                                explanation = ""
                            )
                        )
                        questionsList = newList
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Question")
                }

                Button(
                    onClick = {
                        val parsedQuestions = questionsList.mapIndexed { idx, qf ->
                            Question(
                                id = idx + 1,
                                questionText = qf.questionText,
                                options = listOf(qf.optionA, qf.optionB, qf.optionC, qf.optionD),
                                correctOptionIndex = qf.correctOptionIndex,
                                explanation = qf.explanation,
                                subject = examSubject
                            )
                        }
                        val exam = ExamInfo(
                            id = examId,
                            title = examTitle,
                            subject = examSubject,
                            totalQuestions = parsedQuestions.size,
                            durationMinutes = durationMinutes.toIntOrNull() ?: 60,
                            totalMarks = totalMarks.toIntOrNull() ?: (parsedQuestions.size * 4),
                            priceInInr = priceInInr.toIntOrNull() ?: 0,
                            isLocked = (priceInInr.toIntOrNull() ?: 0) > 0,
                            examCategory = examCategory
                        )
                        onSaveExam(exam, parsedQuestions)
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save Exam & Questions to Firestore", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Existing Exams List
        Text("Existing Exams in Firestore (${existingExams.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        existingExams.forEach { ex ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ex.title, fontWeight = FontWeight.Bold)
                        Text("${ex.id} • ${ex.totalQuestions} Qs • ${ex.durationMinutes} Mins • Price: ₹${ex.priceInInr}", fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDeleteExam(ex.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialAdminManager(
    existingMaterials: List<PdfMaterial>,
    isLoading: Boolean,
    onSaveMaterial: (PdfMaterial) -> Unit,
    onDeleteMaterial: (String) -> Unit
) {
    var matId by remember { mutableStateOf("pdf_${(10..99).random()}") }
    var matTitle by remember { mutableStateOf("") }
    var matSubject by remember { mutableStateOf("Computer Science") }
    var matCategory by remember { mutableStateOf("Notes") }
    var matType by remember { mutableStateOf(MaterialType.PDF) }
    var pdfUrl by remember { mutableStateOf("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf") }
    var videoUrl by remember { mutableStateOf("") }
    var videoDuration by remember { mutableStateOf("45 Mins") }
    var pagesCount by remember { mutableStateOf("25") }
    var priceInInr by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Study Notes / Videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = matId,
                    onValueChange = { matId = it },
                    label = { Text("Material ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = matTitle,
                    onValueChange = { matTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = matSubject,
                        onValueChange = { matSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = matCategory,
                        onValueChange = { matCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type: ", fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = matType == MaterialType.PDF,
                        onClick = { matType = MaterialType.PDF },
                        label = { Text("PDF Notes") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = matType == MaterialType.VIDEO,
                        onClick = { matType = MaterialType.VIDEO },
                        label = { Text("Video Lecture") }
                    )
                }

                if (matType == MaterialType.PDF) {
                    OutlinedTextField(
                        value = pdfUrl,
                        onValueChange = { pdfUrl = it },
                        label = { Text("PDF Download / Public URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pagesCount,
                        onValueChange = { pagesCount = it },
                        label = { Text("Pages Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("Video Stream / MP4 URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = videoDuration,
                        onValueChange = { videoDuration = it },
                        label = { Text("Video Duration (e.g. 45 Mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = priceInInr,
                    onValueChange = { priceInInr = it },
                    label = { Text("Price in INR (0 = Free)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val mat = PdfMaterial(
                            id = matId,
                            title = matTitle,
                            subject = matSubject,
                            pagesCount = pagesCount.toIntOrNull() ?: 0,
                            pdfUrl = pdfUrl,
                            videoUrl = if (matType == MaterialType.VIDEO) videoUrl else null,
                            videoDuration = if (matType == MaterialType.VIDEO) videoDuration else null,
                            description = description,
                            category = matCategory,
                            materialType = matType,
                            isLocked = (priceInInr.toIntOrNull() ?: 0) > 0,
                            priceInInr = priceInInr.toIntOrNull() ?: 0
                        )
                        onSaveMaterial(mat)
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save Material to Firestore", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Existing Materials List
        Text("Existing Materials in Firestore (${existingMaterials.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        existingMaterials.forEach { mat ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mat.title, fontWeight = FontWeight.Bold)
                        Text("${mat.id} • ${mat.materialType.name} • Price: ₹${mat.priceInInr}", fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDeleteMaterial(mat.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

data class QuestionFormState(
    val id: Int,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int,
    val explanation: String
)
