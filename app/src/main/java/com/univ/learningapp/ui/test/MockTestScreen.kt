package com.univ.learningapp.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.univ.learningapp.data.model.Question
import com.univ.learningapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    examId: String,
    examTitle: String,
    viewModel: TestViewModel,
    onTestSubmitted: (resultId: String) -> Unit,
    onCancel: () -> Unit
) {
    val testState by viewModel.testState.collectAsState()
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showPaletteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(examId) {
        viewModel.loadMockTest(examId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = testState) {
            is TestUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Preparing 50 Questions Mock Test...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            is TestUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadMockTest(examId) }) {
                        Text("Retry")
                    }
                }
            }
            is TestUiState.ActiveTest -> {
                val currentQuestion = state.questions[state.currentQuestionIndex]
                val selectedOption = state.userAnswers[currentQuestion.id]
                val isFlagged = state.flaggedQuestions.contains(currentQuestion.id)

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = examTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Q ${state.currentQuestionIndex + 1} of ${state.questions.size}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            actions = {
                                // Timer Chip
                                TimerChip(seconds = state.remainingTimeSeconds)

                                Spacer(modifier = Modifier.width(8.dp))

                                // Grid Palette Icon
                                IconButton(onClick = { showPaletteSheet = !showPaletteSheet }) {
                                    Icon(Icons.Default.GridView, contentDescription = "Question Palette")
                                }

                                // Submit Button
                                Button(
                                    onClick = { showSubmitDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Submit", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        TestBottomNavigation(
                            currentIndex = state.currentQuestionIndex,
                            totalQuestions = state.questions.size,
                            isFlagged = isFlagged,
                            hasSelectedOption = selectedOption != null,
                            onPrevious = { viewModel.previousQuestion() },
                            onNext = { viewModel.nextQuestion() },
                            onClearResponse = { viewModel.clearOption(currentQuestion.id) },
                            onToggleFlag = { viewModel.toggleFlag(currentQuestion.id) }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Question Header & Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { },
                                label = { Text(currentQuestion.subject) },
                                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )

                            if (isFlagged) {
                                Surface(
                                    color = WarningOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Marked for Review", fontSize = 12.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Question Text Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Q${state.currentQuestionIndex + 1}. ${currentQuestion.questionText}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 26.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Select your option:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Options A, B, C, D List
                        currentQuestion.options.forEachIndexed { index, optionText ->
                            val isSelected = selectedOption == index
                            OptionCard(
                                optionIndex = index,
                                optionText = optionText,
                                isSelected = isSelected,
                                onClick = { viewModel.selectOption(currentQuestion.id, index) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Question Palette Grid Modal / Sheet
                if (showPaletteSheet) {
                    QuestionPaletteDialog(
                        questions = state.questions,
                        userAnswers = state.userAnswers,
                        flaggedQuestions = state.flaggedQuestions,
                        currentIndex = state.currentQuestionIndex,
                        onSelectQuestion = { index ->
                            viewModel.goToQuestion(index)
                            showPaletteSheet = false
                        },
                        onDismiss = { showPaletteSheet = false }
                    )
                }

                // Submit Confirmation Dialog
                if (showSubmitDialog) {
                    val answeredCount = state.userAnswers.size
                    val unattemptedCount = state.questions.size - answeredCount
                    val flaggedCount = state.flaggedQuestions.size

                    AlertDialog(
                        onDismissRequest = { showSubmitDialog = false },
                        icon = { Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text("Submit Mock Test?") },
                        text = {
                            Column {
                                Text("Are you sure you want to finish and submit your exam?")
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("• Total Questions: ${state.questions.size}", fontWeight = FontWeight.SemiBold)
                                Text("• Answered: $answeredCount", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text("• Unattempted: $unattemptedCount", color = UnattemptedGray, fontWeight = FontWeight.Bold)
                                Text("• Marked for Review: $flaggedCount", color = WarningOrange, fontWeight = FontWeight.Bold)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showSubmitDialog = false
                                    viewModel.submitTest(examId, examTitle, onTestSubmitted)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("Yes, Submit Test")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSubmitDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TimerChip(seconds: Long) {
    val minutes = seconds / 60
    val secs = seconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    val isLowTime = seconds < 300 // < 5 mins

    Surface(
        color = if (isLowTime) ErrorRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (isLowTime) ErrorRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formattedTime,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isLowTime) ErrorRed else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun OptionCard(
    optionIndex: Int,
    optionText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val optionLabels = listOf("A", "B", "C", "D")
    val label = optionLabels.getOrElse(optionIndex) { "${optionIndex + 1}" }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TestBottomNavigation(
    currentIndex: Int,
    totalQuestions: Int,
    isFlagged: Boolean,
    hasSelectedOption: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClearResponse: () -> Unit,
    onToggleFlag: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = onPrevious,
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous")
            }

            // Flag for Review Button
            IconButton(onClick = onToggleFlag) {
                Icon(
                    imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                    contentDescription = "Flag for Review",
                    tint = if (isFlagged) WarningOrange else MaterialTheme.colorScheme.onSurface
                )
            }

            // Clear response option button
            if (hasSelectedOption) {
                TextButton(onClick = onClearResponse) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }

            // Next / Save & Next Button
            Button(
                onClick = onNext,
                enabled = currentIndex < totalQuestions - 1,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save & Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun QuestionPaletteDialog(
    questions: List<Question>,
    userAnswers: Map<Int, Int?>,
    flaggedQuestions: Set<Int>,
    currentIndex: Int,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Question Palette (1 - ${questions.size})", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = SuccessGreen, label = "Answered")
                    LegendItem(color = UnattemptedGray, label = "Unattempted")
                    LegendItem(color = WarningOrange, label = "Flagged")
                    LegendItem(color = CurrentBlue, label = "Current")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid 1..50
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(questions) { index, question ->
                        val isCurrent = index == currentIndex
                        val isAnswered = userAnswers.containsKey(question.id) && userAnswers[question.id] != null
                        val isFlagged = flaggedQuestions.contains(question.id)

                        val bg = when {
                            isCurrent -> CurrentBlue
                            isFlagged -> WarningOrange
                            isAnswered -> SuccessGreen
                            else -> UnattemptedGray.copy(alpha = 0.3f)
                        }

                        val textColor = if (isCurrent || isFlagged || isAnswered) Color.White else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { onSelectQuestion(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}
