package com.univ.learningapp.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.univ.learningapp.data.model.ExamInfo
import com.univ.learningapp.data.model.MaterialType
import com.univ.learningapp.data.model.PdfMaterial
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.ui.history.HistoryCardItem
import com.univ.learningapp.ui.profile.ProfileTabContent
import com.univ.learningapp.ui.theme.SuccessGreen
import com.univ.learningapp.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartTest: (examId: String, examTitle: String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdminConsole: () -> Unit = {},
    onViewScorecard: (resultId: String) -> Unit = {},
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedPdfForView by remember { mutableStateOf<PdfMaterial?>(null) }
    var selectedItemForUnlock by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(id, isExam)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Learning Portal", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Admin Console Button (Visible if ADMIN or SUPER_ADMIN role)
                    val currentUser = (uiState as? DashboardUiState.Success)?.user
                    if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.SUPER_ADMIN) {
                        IconButton(onClick = onNavigateToAdminConsole) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Console",
                                tint = WarningOrange
                            )
                        }
                    }

                    // History Action Button
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "My Test History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Logout Action Button
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDashboardData() }) {
                            Text("Retry")
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // User Welcome Header Card
                        UserWelcomeHeader(
                            userName = state.user?.name ?: "Learner Student",
                            role = state.user?.role ?: UserRole.MEMBER,
                            onOpenAdminConsole = onNavigateToAdminConsole
                        )

                        // Live Search Bar (not applicable to the Profile tab)
                        if (state.activeTabIndex != 3) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        text = when (state.activeTabIndex) {
                                            0 -> "Search 50-MCQ mock tests, PYQs..."
                                            1 -> "Search PDFs, study notes, videos..."
                                            else -> "Search past test history..."
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // 3 Primary Tabs
                        TabRow(selectedTabIndex = state.activeTabIndex) {
                            Tab(
                                selected = state.activeTabIndex == 0,
                                onClick = { viewModel.selectTab(0) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mock Tests", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = state.activeTabIndex == 1,
                                onClick = { viewModel.selectTab(1) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDFs & Videos", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = state.activeTabIndex == 2,
                                onClick = { viewModel.selectTab(2) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("History", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = state.activeTabIndex == 3,
                                onClick = { viewModel.selectTab(3) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Profile", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }

                        // Filter Chips Bar (only relevant for Mock Tests / PDFs tabs)
                        if (state.activeTabIndex == 0 || state.activeTabIndex == 1) {
                            ScrollableTabRow(
                                selectedTabIndex = state.activeFilter.ordinal,
                                edgePadding = 16.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ContentFilter.values().forEach { filter ->
                                    val label = when (filter) {
                                        ContentFilter.ALL -> "All Items"
                                        ContentFilter.FREE -> "Free Access"
                                        ContentFilter.PREMIUM_LOCKED -> "Locked 🔒"
                                        ContentFilter.COMPLETED -> "Completed"
                                    }
                                    Tab(
                                        selected = state.activeFilter == filter,
                                        onClick = { viewModel.setFilter(filter) },
                                        text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }

                        // Tab Contents
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (state.activeTabIndex == 0) {
                                item {
                                    MockEngineBannerCard()
                                }

                                val filteredExams = state.exams.filter { exam ->
                                    val matchesQuery = exam.title.contains(state.searchQuery, ignoreCase = true) ||
                                            exam.subject.contains(state.searchQuery, ignoreCase = true)
                                    val matchesFilter = when (state.activeFilter) {
                                        ContentFilter.ALL -> true
                                        ContentFilter.FREE -> !exam.isLocked
                                        ContentFilter.PREMIUM_LOCKED -> exam.isLocked
                                        ContentFilter.COMPLETED -> exam.isCompleted
                                    }
                                    matchesQuery && matchesFilter
                                }

                                if (filteredExams.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No questions or mock tests found.", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                } else {
                                    items(filteredExams) { exam ->
                                        ExamCardItem(
                                            exam = exam,
                                            onStartTest = { onStartTest(exam.id, exam.title) },
                                            onUnlock = { selectedItemForUnlock = Pair(exam.id, true) }
                                        )
                                    }
                                }
                            } else if (state.activeTabIndex == 1) {
                                val filteredMaterials = state.pdfs.filter { mat ->
                                    val matchesQuery = mat.title.contains(state.searchQuery, ignoreCase = true) ||
                                            mat.subject.contains(state.searchQuery, ignoreCase = true) ||
                                            mat.category.contains(state.searchQuery, ignoreCase = true)
                                    val matchesFilter = when (state.activeFilter) {
                                        ContentFilter.ALL -> true
                                        ContentFilter.FREE -> !mat.isLocked
                                        ContentFilter.PREMIUM_LOCKED -> mat.isLocked
                                        ContentFilter.COMPLETED -> false
                                    }
                                    matchesQuery && matchesFilter
                                }

                                if (filteredMaterials.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No PDF notes or videos found.", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                } else {
                                    items(filteredMaterials) { mat ->
                                        StudyMaterialCardItem(
                                            material = mat,
                                            onView = { selectedPdfForView = mat },
                                            onUnlock = { selectedItemForUnlock = Pair(mat.id, false) }
                                        )
                                    }
                                }
                            } else if (state.activeTabIndex == 2) {
                                val filteredHistory = state.historyList.filter {
                                    it.examTitle.contains(state.searchQuery, ignoreCase = true)
                                }

                                if (filteredHistory.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillParentMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "No Attempted Tests Yet",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Take a 50-MCQ Mock Test from the 'Mock Tests' tab to view your scores, rank, and solution reviews here.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredHistory) { item ->
                                        HistoryCardItem(
                                            item = item,
                                            onViewScorecard = { onViewScorecard(item.resultId) }
                                        )
                                    }
                                }
                            } else {
                                item {
                                    ProfileTabContent()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF / Video Preview Dialog
    selectedPdfForView?.let { mat ->
        AlertDialog(
            onDismissRequest = { selectedPdfForView = null },
            icon = {
                Icon(
                    imageVector = if (mat.materialType == MaterialType.PDF) Icons.Default.PictureAsPdf else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(text = mat.title) },
            text = {
                Column {
                    Text(text = "Subject: ${mat.subject}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (mat.materialType == MaterialType.PDF) {
                        Text(text = "Pages: ${mat.pagesCount} | Size: ${mat.fileSizeMb}")
                    } else {
                        Text(text = "Video Duration: ${mat.videoDuration ?: "N/A"}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = mat.description, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val url = if (mat.materialType == MaterialType.PDF) mat.pdfUrl else mat.videoUrl
                            if (!url.isNullOrBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) { }
                        selectedPdfForView = null
                    }
                ) {
                    Text(if (mat.materialType == MaterialType.PDF) "Open PDF" else "Watch Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPdfForView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Payment / Unlock Dialog Simulation
    selectedItemForUnlock?.let { (id, isExam) ->
        AlertDialog(
            onDismissRequest = { selectedItemForUnlock = null },
            icon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = WarningOrange) },
            title = { Text("Unlock Full Pass", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Unlock lifetime access to this mock test series or study material.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Full 50-MCQ Exam Attempts", fontWeight = FontWeight.SemiBold)
                    Text("• All India Rank & Percentile Report")
                    Text("• Step-by-Step Detailed Solutions")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isExam) {
                            viewModel.unlockExam(id)
                        } else {
                            viewModel.unlockMaterial(id)
                        }
                        selectedItemForUnlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Pay & Unlock Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForUnlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserWelcomeHeader(
    userName: String,
    role: UserRole = UserRole.MEMBER,
    onOpenAdminConsole: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) WarningOrange else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hello, $userName 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = WarningOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (role == UserRole.SUPER_ADMIN) "SUPER ADMIN" else "ADMIN",
                                    color = WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Pick a 50-MCQ Test, PYQ Paper, or Study Note",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) {
                Button(
                    onClick = onOpenAdminConsole,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Admin ⚙️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MockEngineBannerCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "50-MCQ Full Length Speed Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Simulate real exam conditions: 60 minutes timer, 1..50 question palette grid, All India Rank, and step-by-step solution review.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun ExamCardItem(
    exam: ExamInfo,
    onStartTest: () -> Unit,
    onUnlock: () -> Unit
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
                AssistChip(
                    onClick = { },
                    label = { Text(exam.examCategory) }
                )

                if (exam.isLocked) {
                    Surface(
                        color = WarningOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Locked • ₹${exam.priceInInr}", fontSize = 11.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Unlocked ✓",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("• ${exam.totalQuestions} Questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("• ${exam.durationMinutes} Mins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("• ${exam.totalMarks} Marks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }

            if (exam.isCompleted && exam.previousScore != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Previous Score: ${exam.previousScore} / ${exam.totalMarks}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (exam.isLocked) {
                Button(
                    onClick = onUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Test Pass (₹${exam.priceInInr})", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStartTest,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (exam.isCompleted) "Re-take Mock Test" else "Start 50-MCQ Test", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudyMaterialCardItem(
    material: PdfMaterial,
    onView: () -> Unit,
    onUnlock: () -> Unit
) {
    val isVideo = material.materialType == MaterialType.VIDEO

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isVideo) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = if (isVideo) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isVideo) "${material.subject} • ${material.videoDuration}" else "${material.subject} • ${material.pagesCount} pages • ${material.fileSizeMb}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (material.isLocked) {
                IconButton(onClick = onUnlock) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Material",
                        tint = WarningOrange
                    )
                }
            } else {
                IconButton(onClick = onView) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.Default.Visibility,
                        contentDescription = "View Material",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
