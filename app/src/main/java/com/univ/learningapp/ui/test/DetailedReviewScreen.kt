package com.univ.learningapp.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.univ.learningapp.data.model.QuestionReviewItem
import com.univ.learningapp.ui.theme.ErrorRed
import com.univ.learningapp.ui.theme.SuccessGreen
import com.univ.learningapp.ui.theme.UnattemptedGray

enum class ReviewFilter(val title: String) {
    ALL("All Questions"),
    CORRECT("Correct"),
    INCORRECT("Incorrect"),
    UNATTEMPTED("Unattempted")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedReviewScreen(
    resultId: String,
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit
) {
    val resultState by viewModel.resultState.collectAsState()
    var selectedFilter by remember { mutableStateOf(ReviewFilter.ALL) }

    LaunchedEffect(resultId) {
        viewModel.loadResult(resultId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questions & Detailed Solutions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = resultState) {
                is ResultUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ResultUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ResultUiState.Success -> {
                    val reviewItems = state.result.reviewItems

                    val filteredItems = remember(selectedFilter, reviewItems) {
                        when (selectedFilter) {
                            ReviewFilter.ALL -> reviewItems
                            ReviewFilter.CORRECT -> reviewItems.filter { it.isCorrect }
                            ReviewFilter.INCORRECT -> reviewItems.filter { !it.isCorrect && !it.isUnattempted }
                            ReviewFilter.UNATTEMPTED -> reviewItems.filter { it.isUnattempted }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Tab Row
                        ScrollableTabRow(
                            selectedTabIndex = selectedFilter.ordinal,
                            edgePadding = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReviewFilter.values().forEach { filter ->
                                val count = when (filter) {
                                    ReviewFilter.ALL -> reviewItems.size
                                    ReviewFilter.CORRECT -> reviewItems.count { it.isCorrect }
                                    ReviewFilter.INCORRECT -> reviewItems.count { !it.isCorrect && !it.isUnattempted }
                                    ReviewFilter.UNATTEMPTED -> reviewItems.count { it.isUnattempted }
                                }
                                Tab(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    text = { Text("${filter.title} ($count)", fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        // Question Review List
                        if (filteredItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No questions in this category.", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(filteredItems) { index, reviewItem ->
                                    QuestionReviewCard(
                                        displayIndex = reviewItem.question.id,
                                        reviewItem = reviewItem
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionReviewCard(
    displayIndex: Int,
    reviewItem: QuestionReviewItem
) {
    val question = reviewItem.question

    val statusColor = when {
        reviewItem.isCorrect -> SuccessGreen
        reviewItem.isUnattempted -> UnattemptedGray
        else -> ErrorRed
    }

    val statusLabel = when {
        reviewItem.isCorrect -> "Correct"
        reviewItem.isUnattempted -> "Unattempted"
        else -> "Incorrect"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Q Number + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q$displayIndex. ${question.subject}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Text
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options List with Solution Highlights
            question.options.forEachIndexed { optIndex, optionText ->
                val isSelectedByUser = reviewItem.selectedOptionIndex == optIndex
                val isCorrectAnswer = question.correctOptionIndex == optIndex

                val optionBg = when {
                    isCorrectAnswer -> SuccessGreen.copy(alpha = 0.15f)
                    isSelectedByUser && !reviewItem.isCorrect -> ErrorRed.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }

                val optionBorderColor = when {
                    isCorrectAnswer -> SuccessGreen
                    isSelectedByUser && !reviewItem.isCorrect -> ErrorRed
                    else -> Color.Transparent
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = optionBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, optionBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val labels = listOf("A", "B", "C", "D")
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCorrectAnswer -> SuccessGreen
                                        isSelectedByUser && !reviewItem.isCorrect -> ErrorRed
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels.getOrElse(optIndex) { "${optIndex + 1}" },
                                color = if (isCorrectAnswer || isSelectedByUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCorrectAnswer || isSelectedByUser) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        if (isCorrectAnswer) {
                            Text(
                                text = "Correct",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        } else if (isSelectedByUser) {
                            Text(
                                text = "Your Choice",
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detailed Explanation & Solution:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
