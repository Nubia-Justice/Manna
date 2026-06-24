package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReadingPlan
import com.example.data.ReadingPlanDay
import com.example.data.UserBadge
import com.example.data.VerseInteraction
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: BibleViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("home") }

    // Collect States
    val activePlan by viewModel.activePlan.collectAsStateWithLifecycle()
    val activePlanDays by viewModel.activePlanDays.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val verseInteractions by viewModel.verseInteractions.collectAsStateWithLifecycle()
    val userStreak by viewModel.userStreak.collectAsStateWithLifecycle()
    val allBadges by viewModel.allBadges.collectAsStateWithLifecycle()
    val starredInteractions by viewModel.starredInteractions.collectAsStateWithLifecycle()
    val reminderStatus by viewModel.reminderStatus.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(if (currentTab == "home") Icons.Filled.Home else Icons.Outlined.Home, "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "read",
                    onClick = { currentTab = "read" },
                    icon = { Icon(if (currentTab == "read") Icons.Filled.MenuBook else Icons.Outlined.MenuBook, "Read") },
                    label = { Text("Read") },
                    modifier = Modifier.testTag("nav_read")
                )
                NavigationBarItem(
                    selected = currentTab == "schedules",
                    onClick = { currentTab = "schedules" },
                    icon = { Icon(if (currentTab == "schedules") Icons.Filled.Schedule else Icons.Outlined.Schedule, "Schedules") },
                    label = { Text("Schedules") },
                    modifier = Modifier.testTag("nav_schedules")
                )
                NavigationBarItem(
                    selected = currentTab == "notes",
                    onClick = { currentTab = "notes" },
                    icon = { Icon(if (currentTab == "notes") Icons.Filled.Bookmark else Icons.Outlined.Bookmark, "Journal") },
                    label = { Text("Journal") },
                    modifier = Modifier.testTag("nav_notes")
                )
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { currentTab = "history" },
                    icon = { Icon(if (currentTab == "history") Icons.Filled.History else Icons.Outlined.History, "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        // Snackbars & Reminders status box
        LaunchedEffect(reminderStatus) {
            if (reminderStatus != null) {
                delay(4000)
                viewModel.clearReminderStatus()
            }
        }

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    "home" -> HomeTabScreen(
                        viewModel = viewModel,
                        userStreak = userStreak,
                        activePlan = activePlan,
                        activePlanDays = activePlanDays,
                        allBadges = allBadges,
                        onNavigateToRead = { currentTab = "read" }
                    )
                    "read" -> ReadTabScreen(
                        viewModel = viewModel,
                        activePlan = activePlan,
                        selectedDay = selectedDay,
                        activeDays = activePlanDays,
                        verseInteractions = verseInteractions
                    )
                    "schedules" -> SchedulesTabScreen(
                        viewModel = viewModel,
                        onNavigateToRead = { currentTab = "read" }
                    )
                    "notes" -> JournalTabScreen(
                        viewModel = viewModel,
                        starredInteractions = starredInteractions,
                        onJumpToRead = { currentTab = "read" }
                    )
                    "history" -> HistoryTabScreen(
                        viewModel = viewModel,
                        onRevisitDay = { day ->
                            viewModel.selectDayFromHistory(day)
                            currentTab = "read"
                        }
                    )
                }
            }

            // Beautiful Floating Banner for Reminders
            reminderStatus?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.NotificationsActive, "Alarm Set", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// HOME TAB
// -----------------------------------------------------------------
@Composable
fun HomeTabScreen(
    viewModel: BibleViewModel,
    userStreak: com.example.data.StreakRecord?,
    activePlan: ReadingPlan?,
    activePlanDays: List<ReadingPlanDay>,
    allBadges: List<UserBadge>,
    onNavigateToRead: () -> Unit
) {
    val dailyVerse by viewModel.dailyVerse.collectAsStateWithLifecycle()
    val geminiReflection by viewModel.geminiReflection.collectAsStateWithLifecycle()
    val isGeneratingReflection by viewModel.isGeneratingReflection.collectAsStateWithLifecycle()

    var showReminderPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Header Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome Back,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Daily Bread",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Theme Mode Switcher Controls
                val isDarkPref by viewModel.isDarkTheme.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setThemeMode(null) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDarkPref == null) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("theme_auto")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessAuto,
                            contentDescription = "Auto Theme",
                            tint = if (isDarkPref == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setThemeMode(false) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDarkPref == false) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("theme_light")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "Light Theme",
                            tint = if (isDarkPref == false) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setThemeMode(true) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDarkPref == true) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("theme_dark")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Dark Theme",
                            tint = if (isDarkPref == true) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Streak & badging card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Streak Fire",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "${userStreak?.currentStreak ?: 0} Day Streak",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Personal best: ${userStreak?.maxStreak ?: 0} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToRead,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("streak_action_btn")
                    ) {
                        Text("Study Word")
                    }
                }
            }
        }

        // Daily Verse card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILY VERSE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Inspirational",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "“${dailyVerse.text}”",
                        style = MaterialTheme.typography.headlineSmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "— ${dailyVerse.reference}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = dailyVerse.reflection,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ask AI Study Helper Button (Gemini API Integration)
                    Button(
                        onClick = { viewModel.generateGeminiReflection(dailyVerse.reference, dailyVerse.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_ai_helper"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGeneratingReflection
                    ) {
                        if (isGeneratingReflection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Pondering Divine Truth...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, "AI", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reveal Deep Study Insight")
                        }
                    }

                    // Render Gemini Reflection response beautifully
                    geminiReflection?.let { reflection ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.TipsAndUpdates, "Insight", tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Study Assistant",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = reflection,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active plan progress view
        item {
            Text(
                text = "My Reading Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            if (activePlan != null) {
                val completedCount = activePlanDays.count { it.isCompleted }
                val progress = if (activePlanDays.isNotEmpty()) completedCount.toFloat() / activePlanDays.size else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePlan.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activePlan.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$completedCount/${activePlan.durationDays} Days",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToRead,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Start")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Daily Scripture Canvas")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Book, "No plan", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Study Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Go to Schedules tab to choose or construct your customizable Bible reading schedule!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // 30-Day Activity Progress Chart
        item {
            Text(
                text = "30-Day Devotional Consistency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val last30Days = remember(activePlanDays) {
                List(30) { index ->
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -index)
                    sdf.format(cal.time)
                }.reversed()
            }

            val completionDates = remember(activePlanDays) {
                activePlanDays.filter { it.isCompleted && !it.completionDate.isNullOrEmpty() }
                    .map { it.completionDate!! }
                    .toSet()
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Scripture Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Consistency over the past 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val activeDaysCount = last30Days.count { completionDates.contains(it) }
                            Text(
                                text = "$activeDaysCount/30 Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 10) {
                                    val index = row * 10 + col
                                    val dateStr = last30Days.getOrNull(index) ?: ""
                                    val isCompleted = completionDates.contains(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isCompleted) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Read",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "30 Days Ago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rest Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Read Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Badges achievements row
        item {
            Text(
                text = "Badges & Spiritual Growth",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allBadges) { badge ->
                    BadgeItemView(badge)
                }
            }
        }

        // Customizable study schedule reminders card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Alarm, "Notifications", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Daily Push Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable customizable notification alarms to prompt consistent daily devotional readings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showReminderPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("Customize Alert Schedule")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showReminderPicker) {
        var selectedHour by remember { mutableStateOf(8) }
        var selectedMin by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showReminderPicker = false },
            title = { Text("Customize Alert Schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select your preferred daily scripture reading notification trigger:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simulating interactive spinner controls for hour/minute
                        IconButton(onClick = { if (selectedHour > 1) selectedHour-- else selectedHour = 23 }) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Down")
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { if (selectedHour < 23) selectedHour++ else selectedHour = 0 }) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Up")
                        }

                        Text(":", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

                        IconButton(onClick = { if (selectedMin >= 5) selectedMin -= 5 else selectedMin = 55 }) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Down")
                        }
                        Text(
                            text = String.format("%02d", selectedMin),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { if (selectedMin <= 50) selectedMin += 5 else selectedMin = 0 }) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Up")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.scheduleDailyReminder(selectedHour, selectedMin)
                        showReminderPicker = false
                    },
                    modifier = Modifier.testTag("confirm_alarm_btn")
                ) {
                    Text("Set Reminder Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BadgeItemView(badge: UserBadge) {
    var showDetails by remember { mutableStateOf(false) }

    val iconVector = when (badge.id) {
        "first_reading" -> Icons.Filled.Star
        "streak_3" -> Icons.Filled.LocalFireDepartment
        "streak_7" -> Icons.Filled.EmojiEvents
        "voice_reader" -> Icons.Filled.Mic
        "note_taken" -> Icons.Filled.EditNote
        else -> Icons.Filled.WbSunny
    }

    val iconColor = if (badge.isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val cardColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = if (badge.isUnlocked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)) else BorderStroke(1.dp, Color.Transparent)

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { showDetails = true }
            .testTag("badge_${badge.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = badge.title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                if (!badge.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, "Locked", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = if (badge.isUnlocked) "Unlocked" else "Locked",
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconVector, badge.title, tint = iconColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(badge.title)
                }
            },
            text = {
                Column {
                    Text(badge.description)
                    if (badge.isUnlocked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Unlocked on: " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(badge.unlockedAt)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) { Text("Praise God") }
            }
        )
    }
}


// -----------------------------------------------------------------
// READ TAB
// -----------------------------------------------------------------
@Composable
fun ReadTabScreen(
    viewModel: BibleViewModel,
    activePlan: ReadingPlan?,
    selectedDay: ReadingPlanDay?,
    activeDays: List<ReadingPlanDay>,
    verseInteractions: List<VerseInteraction>
) {
    val scope = rememberCoroutineScope()

    val isSpeaking by viewModel.audioController.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.audioController.isListening.collectAsStateWithLifecycle()
    val vocalVolume by viewModel.audioController.vocalVolume.collectAsStateWithLifecycle()
    val recognizedText by viewModel.audioController.recognizedText.collectAsStateWithLifecycle()

    var showNotesEditorForVerse by remember { mutableStateOf<Int?>(null) }
    var notesDraftText by remember { mutableStateOf("") }

    var selectedVerseIndex by remember { mutableStateOf<Int?>(null) }

    var showVoiceVerifyDialog by remember { mutableStateOf(false) }

    // TTS Speed controls
    val currentRate by viewModel.audioController.ttsRate.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableStateOf(18) }

    if (activePlan == null || selectedDay == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Outlined.Book, "Select Plan", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select or Create a Reading Plan First", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Navigate to the Schedules tab to select an active track to get daily scriptures.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // Split passage into verses
    val verses = selectedDay.passageText.split("\n").filter { it.isNotBlank() }

    val filteredDays = remember(searchQuery, activeDays) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            activeDays.filter { day ->
                day.versesToRead.contains(searchQuery, ignoreCase = true) ||
                day.passageText.contains(searchQuery, ignoreCase = true) ||
                "Day ${day.dayNumber}".contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bible_search_bar"),
                placeholder = { Text("Search books, chapters, or verses...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }

        // Search Results suggestion list
        if (searchQuery.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Plan Matches (${filteredDays.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        
                        if (filteredDays.isEmpty()) {
                            Text(
                                text = "No books or chapters matched your search query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                filteredDays.take(5).forEach { matchedDay ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (matchedDay.id == selectedDay.id) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                viewModel.selectDay(matchedDay)
                                                searchQuery = "" // Clear query to close suggestions
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = matchedDay.versesToRead,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Day ${matchedDay.dayNumber} of ${activePlan.durationDays}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = "Jump",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (filteredDays.size > 5) {
                                    Text(
                                        text = "+ ${filteredDays.size - 5} more results",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Day Title Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = activePlan.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day ${selectedDay.dayNumber} of ${activePlan.durationDays}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDay.versesToRead,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal scrolling days list to switch day
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeDays) { dayItem ->
                            val isCurrent = dayItem.id == selectedDay.id
                            val statusColor = if (dayItem.isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        2.dp,
                                        if (dayItem.isCompleted) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectDay(dayItem) }
                                    .testTag("day_selector_${dayItem.dayNumber}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${dayItem.dayNumber}",
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    // small completed indicator dot
                                    if (dayItem.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TTS Reader controller floating toolbar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    viewModel.audioController.stopSpeaking()
                                } else {
                                    viewModel.audioController.speak(selectedDay.passageText)
                                }
                            },
                            modifier = Modifier.testTag("tts_play_btn")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                contentDescription = "Listen",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSpeaking) "Now Listening Aloud" else "Hands-free Audio Reader",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Speak and meditate on God's Word",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Speed controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rates = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                        rates.forEach { rate ->
                            Text(
                                text = "${rate}x",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentRate == rate) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentRate == rate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clickable { viewModel.audioController.setSpeechRate(rate) }
                            )
                        }
                    }
                }
            }
        }

        // Scripture Canvas / Verses rendering
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCRIPTURE READINGS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            letterSpacing = 1.2.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (fontSizeSp > 12) fontSizeSp -= 2 },
                                modifier = Modifier.size(28.dp).testTag("font_dec")
                            ) {
                                Icon(Icons.Filled.Remove, "Decrease Font", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${fontSizeSp}sp",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { if (fontSizeSp < 32) fontSizeSp += 2 },
                                modifier = Modifier.size(28.dp).testTag("font_inc")
                            ) {
                                Icon(Icons.Filled.Add, "Increase Font", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    verses.forEachIndexed { index, verse ->
                        val interaction = verseInteractions.find { it.verseIndex == index }
                        val isHighlighted = interaction?.isHighlighted == true
                        val isMarked = interaction?.isMarked == true
                        val noteText = interaction?.noteText ?: ""

                        val highlightColorValue = when (interaction?.highlightColor) {
                            1 -> HighYellow
                            2 -> HighBlue
                            3 -> HighGreen
                            4 -> HighPink
                            else -> Color.Transparent
                        }

                        val isSelected = selectedVerseIndex == index

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .drawBehind {
                                    if (isHighlighted) {
                                        drawRect(
                                            color = highlightColorValue,
                                            topLeft = Offset(0f, 0f),
                                            size = size
                                        )
                                    }
                                }
                                .clickable {
                                    selectedVerseIndex = if (isSelected) null else index
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("verse_line_$index")
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                if (isMarked) {
                                    Icon(
                                        Icons.Filled.Bookmark,
                                        contentDescription = "Bookmarked",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 4.dp, end = 4.dp)
                                    )
                                }
                                val isSearchHighlight = searchQuery.isNotBlank() && verse.contains(searchQuery, ignoreCase = true)
                                Text(
                                    text = verse,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp + 8).sp,
                                        fontWeight = if (isSearchHighlight) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSearchHighlight) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            // Show notes flag if exists
                            if (noteText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.EditNote, "My Notes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = noteText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Selected Verse Action Drawer
        selectedVerseIndex?.let { index ->
            val verseText = verses.getOrNull(index) ?: ""
            val interaction = verseInteractions.find { it.verseIndex == index }
            val isMarked = interaction?.isMarked == true

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Verse Action Panel: Verse ${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Highlight Palette
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Highlight Color:", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val colorsList = listOf(
                                    Pair(1, HighYellow),
                                    Pair(2, HighBlue),
                                    Pair(3, HighGreen),
                                    Pair(4, HighPink)
                                )
                                colorsList.forEach { (colorId, colorHex) ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorHex)
                                            .border(
                                                2.dp,
                                                if (interaction?.highlightColor == colorId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable {
                                                viewModel.toggleHighlight(index, verseText, colorId)
                                            }
                                    )
                                }
                                // Clear highlight
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                        .clickable {
                                            viewModel.toggleHighlight(index, verseText, 0)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Clear, "Clear Highlight", modifier = Modifier.size(14.dp), tint = Color.Red)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Bookmark & Notes buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { viewModel.toggleMark(index, verseText) },
                                modifier = Modifier.testTag("bookmark_verse_btn")
                            ) {
                                Icon(if (isMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, "Bookmark")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isMarked) "Remove Bookmark" else "Save Bookmark")
                            }

                            TextButton(
                                onClick = {
                                    notesDraftText = interaction?.noteText ?: ""
                                    showNotesEditorForVerse = index
                                },
                                modifier = Modifier.testTag("notes_verse_btn")
                            ) {
                                Icon(Icons.Filled.NoteAdd, "Add Notes")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Personal Reflection")
                            }
                        }
                    }
                }
            }
        }

        // Streak voice verify session completed card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedDay.isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (selectedDay.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedDay.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Daily Session Completed!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your streak is protected! Keep reading daily to build a faithful habit.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Mic,
                            "Unlocks Streak",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Protect Your Study Streak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Read scripture verses aloud to complete today's verification step and advance your streak!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showVoiceVerifyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mic_verify_trigger")
                        ) {
                            Icon(Icons.Filled.RecordVoiceOver, "Mic")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activate Voice Reading Aloud")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Personal Note writing editor pop-up
    showNotesEditorForVerse?.let { verseIdx ->
        AlertDialog(
            onDismissRequest = { showNotesEditorForVerse = null },
            title = { Text("Devotional Reflection (Verse ${verseIdx + 1})") },
            text = {
                OutlinedTextField(
                    value = notesDraftText,
                    onValueChange = { notesDraftText = it },
                    placeholder = { Text("Write your thoughts, insights or prayers about this verse...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("note_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val verseText = verses.getOrNull(verseIdx) ?: ""
                        viewModel.saveNote(verseIdx, verseText, notesDraftText)
                        showNotesEditorForVerse = null
                    },
                    modifier = Modifier.testTag("note_save_confirm")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesEditorForVerse = null }) { Text("Cancel") }
            }
        )
    }

    // Voice Verify Aloud interactive panel
    if (showVoiceVerifyDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.audioController.stopListening()
                showVoiceVerifyDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RecordVoiceOver, "Voice Verify", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Scripture Read-Aloud")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Read the active scripture line clearly into your device microphone:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    // Target Scripture snippet
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verses.firstOrNull() ?: "No scripture loaded",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Pulse waveform / Volume Bar anims
                    if (isListening) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(50.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live wave bars
                                val baseHeights = listOf(0.3f, 0.5f, 0.8f, 0.9f, 0.6f, 0.8f, 0.4f)
                                baseHeights.forEach { baseHeight ->
                                    val barHeight = (baseHeight * vocalVolume * 45).coerceIn(4.dp.value, 48.dp.value).dp
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(5.dp)
                                            .height(barHeight)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Listening... Speak the words clearly",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.audioController.startListening(verses.firstOrNull()) { result ->
                                    // Voice verified callback
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                .testTag("start_listening_voice")
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                "Microphone",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Tap microphone to begin reading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Display recognized texts
                    if (recognizedText.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Recognized Vocal Words:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "“$recognizedText”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.Check, "Match Successful", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scripture Voice Match: 100% verified!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeCurrentDay(readAloudVerified = true)
                        viewModel.audioController.stopListening()
                        showVoiceVerifyDialog = false
                    },
                    modifier = Modifier.testTag("complete_reading_button"),
                    enabled = recognizedText.isNotEmpty()
                ) {
                    Text("Complete Reading (Verify)")
                }
            },
            dismissButton = {
                // Allow manually skipping voice check if they are in public / no mic
                TextButton(
                    onClick = {
                        viewModel.completeCurrentDay(readAloudVerified = false)
                        viewModel.audioController.stopListening()
                        showVoiceVerifyDialog = false
                    },
                    modifier = Modifier.testTag("skip_voice_verify_btn")
                ) {
                    Text("Skip Aloud (Self-Verify)")
                }
            }
        )
    }
}


// -----------------------------------------------------------------
// SCHEDULES TAB
// -----------------------------------------------------------------
@Composable
fun SchedulesTabScreen(
    viewModel: BibleViewModel,
    onNavigateToRead: () -> Unit
) {
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()

    var showCreator by remember { mutableStateOf(false) }

    var planTitle by remember { mutableStateOf("") }
    var planDesc by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(5) }
    var selectedTheme by remember { mutableStateOf("faith") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bible Reading",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Study Tracks",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = { showCreator = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_plan_fab")
                ) {
                    Icon(Icons.Filled.Add, "New Plan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Construct Plan")
                }
            }
        }

        if (showCreator) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "🛠️ Construct Customizable Schedule",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = planTitle,
                            onValueChange = { planTitle = it },
                            label = { Text("Study Title (e.g., Gospel Reflection)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_title_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = planDesc,
                            onValueChange = { planDesc = it },
                            label = { Text("Plan Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_desc_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Duration Slider
                        Text("Plan Length (Days): $selectedDuration days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = selectedDuration.toFloat(),
                            onValueChange = { selectedDuration = it.toInt() },
                            valueRange = 3f..10f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Theme choice
                        Text("Theological Topic Focus:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("faith", "wisdom", "comfort", "strength")
                            themes.forEach { theme ->
                                val isSelected = selectedTheme == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedTheme = theme }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = theme.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCreator = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (planTitle.isNotBlank()) {
                                        viewModel.createCustomSchedule(
                                            title = planTitle,
                                            description = if (planDesc.isBlank()) "Custom generated study schedule" else planDesc,
                                            daysCount = selectedDuration,
                                            theme = selectedTheme
                                        )
                                        planTitle = ""
                                        planDesc = ""
                                        showCreator = false
                                        onNavigateToRead()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("submit_plan_btn")
                            ) {
                                Text("Activate & Create Schedule")
                            }
                        }
                    }
                }
            }
        }

        // List of current reading plans
        item {
            Text(
                text = "Available Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(allPlans) { plan ->
            val isActive = plan.isActive

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectPlan(plan.id) }
                    .testTag("plan_card_${plan.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plan.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DateRange, "Duration", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${plan.durationDays} Days Track",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (!isActive) {
                        Button(
                            onClick = {
                                viewModel.selectPlan(plan.id)
                                onNavigateToRead()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Activate")
                        }
                    } else {
                        IconButton(
                            onClick = onNavigateToRead
                        ) {
                            Icon(Icons.Filled.ChevronRight, "Read", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}


// -----------------------------------------------------------------
// JOURNAL & NOTES TAB
// -----------------------------------------------------------------
@Composable
fun JournalTabScreen(
    viewModel: BibleViewModel,
    starredInteractions: List<VerseInteraction>,
    onJumpToRead: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredInteractions = starredInteractions.filter {
        searchQuery.isEmpty() ||
                it.verseText.contains(searchQuery, ignoreCase = true) ||
                it.noteText.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Column {
                Text(
                    text = "Personal Study",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "Reflection Journal",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search my highlights, notes, or bookmarks...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search"),
                leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filteredInteractions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.EditNote, "No entries", modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching Study Records Found" else "Your Reflection Journal is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try editing your keywords." else "Highlight verses, mark bookmarks, and write down insights in the Read tab to compile your personal study journal!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredInteractions) { item ->
                val highlightColor = when (item.highlightColor) {
                    1 -> HighYellow
                    2 -> HighBlue
                    3 -> HighGreen
                    4 -> HighPink
                    else -> Color.Transparent
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MenuBook, "Scripture", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Day ${item.dayNumber} Reflection",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.isMarked) {
                                    Icon(Icons.Filled.Bookmark, "Bookmarked", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                }
                                if (item.isHighlighted) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(highlightColor)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Verse text bubble
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "“${item.verseText}”",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Reflection note if exists
                        if (item.noteText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Personal Notes:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.noteText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    // Normally we would jump back to specific day.
                                    // We can jump to Read tab
                                    onJumpToRead()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.OpenInNew, "Study", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Reading Canvas")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}


// -----------------------------------------------------------------
// READING HISTORY TAB
// -----------------------------------------------------------------
@Composable
fun HistoryTabScreen(
    viewModel: BibleViewModel,
    onRevisitDay: (ReadingPlanDay) -> Unit
) {
    val completedDays by viewModel.allCompletedDays.collectAsStateWithLifecycle()
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    val starredInteractions by viewModel.starredInteractions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val filteredCompletedDays = remember(completedDays, searchQuery, plans) {
        completedDays.filter { day ->
            val planTitle = plans.find { it.id == day.planId }?.title ?: ""
            searchQuery.isEmpty() ||
                    day.versesToRead.contains(searchQuery, ignoreCase = true) ||
                    day.completionDate?.contains(searchQuery, ignoreCase = true) == true ||
                    planTitle.contains(searchQuery, ignoreCase = true) ||
                    "Day ${day.dayNumber}".contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Column {
                Text(
                    text = "My Journey",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "Reading History",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // History Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search completed sessions, dates, plans...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search"),
                leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }

        if (filteredCompletedDays.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "No history",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching History Found" else "No Completed Sessions Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching for a different day, plan, or passage." else "When you complete a devotional reading day, it will show up here as a milestone on your faith journey!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredCompletedDays) { day ->
                val plan = plans.find { it.id == day.planId }
                val planTitle = plan?.title ?: "Reading Plan"

                val dayNotes = starredInteractions.filter {
                    it.planId == day.planId && it.dayNumber == day.dayNumber && it.noteText.isNotEmpty()
                }
                val dayHighlights = starredInteractions.filter {
                    it.planId == day.planId && it.dayNumber == day.dayNumber && it.isHighlighted
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_card_${day.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = planTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Day ${day.dayNumber}: ${day.versesToRead}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = "Date",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Read on ${day.completionDate ?: "Date Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        if (dayNotes.isNotEmpty() || dayHighlights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (dayHighlights.isNotEmpty()) {
                                Text(
                                    text = "✨ Highlights",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    dayHighlights.forEach { highlight ->
                                        Text(
                                            text = "• “${highlight.verseText}”",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            if (dayNotes.isNotEmpty()) {
                                if (dayHighlights.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = "📝 Personal Notes & Reflections",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    dayNotes.forEach { note ->
                                        Column {
                                            Text(
                                                text = "“${note.verseText}”",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = note.noteText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onRevisitDay(day) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.MenuBook, "Open Day", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Revisit Session")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
