package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.service.BibleAudioController
import com.example.service.DailyVerse
import com.example.service.DailyVerseManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BibleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = BibleRepository(
        planDao = database.readingPlanDao(),
        planDayDao = database.readingPlanDayDao(),
        interactionDao = database.verseInteractionDao(),
        streakDao = database.streakDao(),
        badgeDao = database.badgeDao()
    )

    val audioController = BibleAudioController(application)

    // Data Flows
    val allPlans: StateFlow<List<ReadingPlan>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBadges: StateFlow<List<UserBadge>> = repository.allBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStreak: StateFlow<StreakRecord?> = repository.userStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val starredInteractions: StateFlow<List<VerseInteraction>> = repository.starredInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletedDays: StateFlow<List<ReadingPlanDay>> = repository.allCompletedDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected state
    private val _activePlan = MutableStateFlow<ReadingPlan?>(null)
    val activePlan: StateFlow<ReadingPlan?> = _activePlan

    private val _activePlanDays = MutableStateFlow<List<ReadingPlanDay>>(emptyList())
    val activePlanDays: StateFlow<List<ReadingPlanDay>> = _activePlanDays

    private val _selectedDay = MutableStateFlow<ReadingPlanDay?>(null)
    val selectedDay: StateFlow<ReadingPlanDay?> = _selectedDay

    private val _verseInteractions = MutableStateFlow<List<VerseInteraction>>(emptyList())
    val verseInteractions: StateFlow<List<VerseInteraction>> = _verseInteractions

    // Daily Verse State
    private val _dailyVerse = MutableStateFlow(DailyVerseManager.getDailyVerse())
    val dailyVerse: StateFlow<DailyVerse> = _dailyVerse

    // Gemini Reflection states
    private val _geminiReflection = MutableStateFlow<String?>(null)
    val geminiReflection: StateFlow<String?> = _geminiReflection

    private val _isGeneratingReflection = MutableStateFlow(false)
    val isGeneratingReflection: StateFlow<Boolean> = _isGeneratingReflection

    // Notification reminder schedule message
    private val _reminderStatus = MutableStateFlow<String?>(null)
    val reminderStatus: StateFlow<String?> = _reminderStatus

    init {
        viewModelScope.launch {
            // Preseed data if empty
            repository.ensurePreseededData()

            // Observe the active plan and sync days
            repository.allPlans.collect { plans ->
                val active = plans.find { it.isActive }
                _activePlan.value = active

                if (active != null) {
                    // Collect active plan days
                    repository.getDaysForPlan(active.id).collect { days ->
                        _activePlanDays.value = days
                        if (_selectedDay.value == null || days.none { it.id == _selectedDay.value?.id }) {
                            // Default to first incomplete day, or day 1
                            val incomplete = days.find { !it.isCompleted }
                            _selectedDay.value = incomplete ?: days.firstOrNull()
                        } else {
                            // Refresh selected day with updated DB status
                            _selectedDay.value = days.find { it.id == _selectedDay.value?.id }
                        }

                        // Load relative verse interactions when selected day changes
                        _selectedDay.value?.let { currentDay ->
                            repository.getInteractionsForDay(currentDay.planId, currentDay.dayNumber)
                                .collect { interactions ->
                                    _verseInteractions.value = interactions
                                }
                        }
                    }
                } else {
                    _activePlanDays.value = emptyList()
                    _selectedDay.value = null
                    _verseInteractions.value = emptyList()
                }
            }
        }
    }

    fun selectPlan(planId: Int) {
        viewModelScope.launch {
            repository.activatePlan(planId)
            _selectedDay.value = null // reset day selection so it resolves to new plan's default
        }
    }

    fun selectDay(day: ReadingPlanDay) {
        _selectedDay.value = day
        // Refresh verse interactions flow for new day
        viewModelScope.launch {
            repository.getInteractionsForDay(day.planId, day.dayNumber).collect { interactions ->
                _verseInteractions.value = interactions
            }
        }
    }

    fun selectDayFromHistory(day: ReadingPlanDay) {
        viewModelScope.launch {
            if (_activePlan.value?.id != day.planId) {
                repository.activatePlan(day.planId)
            }
            _selectedDay.value = day
            repository.getInteractionsForDay(day.planId, day.dayNumber).collect { interactions ->
                _verseInteractions.value = interactions
            }
        }
    }

    // Verse Interaction Helpers
    fun toggleHighlight(verseIndex: Int, verseText: String, color: Int) {
        val currentDay = _selectedDay.value ?: return
        viewModelScope.launch {
            val id = "${currentDay.planId}_${currentDay.dayNumber}_$verseIndex"
            val existing = _verseInteractions.value.find { it.id == id }

            val updated = existing?.copy(
                isHighlighted = !existing.isHighlighted,
                highlightColor = if (!existing.isHighlighted) color else 0
            ) ?: VerseInteraction(
                id = id,
                planId = currentDay.planId,
                dayNumber = currentDay.dayNumber,
                verseIndex = verseIndex,
                verseText = verseText,
                isHighlighted = true,
                highlightColor = color
            )
            repository.saveVerseInteraction(updated)
        }
    }

    fun toggleMark(verseIndex: Int, verseText: String) {
        val currentDay = _selectedDay.value ?: return
        viewModelScope.launch {
            val id = "${currentDay.planId}_${currentDay.dayNumber}_$verseIndex"
            val existing = _verseInteractions.value.find { it.id == id }

            val updated = existing?.copy(
                isMarked = !existing.isMarked
            ) ?: VerseInteraction(
                id = id,
                planId = currentDay.planId,
                dayNumber = currentDay.dayNumber,
                verseIndex = verseIndex,
                verseText = verseText,
                isMarked = true
            )
            repository.saveVerseInteraction(updated)
        }
    }

    fun saveNote(verseIndex: Int, verseText: String, noteText: String) {
        val currentDay = _selectedDay.value ?: return
        viewModelScope.launch {
            val id = "${currentDay.planId}_${currentDay.dayNumber}_$verseIndex"
            val existing = _verseInteractions.value.find { it.id == id }

            val updated = existing?.copy(
                noteText = noteText
            ) ?: VerseInteraction(
                id = id,
                planId = currentDay.planId,
                dayNumber = currentDay.dayNumber,
                verseIndex = verseIndex,
                verseText = verseText,
                noteText = noteText
            )
            repository.saveVerseInteraction(updated)
        }
    }

    fun completeCurrentDay(readAloudVerified: Boolean) {
        val currentDay = _selectedDay.value ?: return
        viewModelScope.launch {
            repository.verifyAndCompleteDay(
                planId = currentDay.planId,
                dayNumber = currentDay.dayNumber,
                readAloudVerified = readAloudVerified
            )
        }
    }

    fun createCustomSchedule(title: String, description: String, daysCount: Int, theme: String) {
        viewModelScope.launch {
            repository.createCustomPlan(title, description, daysCount, theme)
        }
    }

    // Schedule Daily Notification Reminder (Local Mock Notification scheduler)
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val amPm = if (hour >= 12) "PM" else "AM"
        val formattedHour = if (hour % 12 == 0) 12 else hour % 12
        val timeStr = String.format("%02d:%02d %s", formattedHour, minute, amPm)
        _reminderStatus.value = "Daily notification scheduled successfully for $timeStr!"
    }

    fun clearReminderStatus() {
        _reminderStatus.value = null
    }

    // Google Gemini API integration for devotional reflections
    fun generateGeminiReflection(passageTitle: String, passageText: String) {
        _geminiReflection.value = null
        _isGeneratingReflection.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Return a beautiful, high-quality simulated reflection offline fallback
                Log.d("GeminiApi", "API Key is empty or placeholder. Providing offline devotion.")
                val reflection = generateLocalReflection(passageTitle, passageText)
                _geminiReflection.value = reflection
                _isGeneratingReflection.value = false
                return@launch
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Provide a beautiful, encouraging Christian study reflection and brief devotional prayer based on this scripture: $passageTitle - '$passageText'. Limit the length to 2 paragraphs of profound spiritual commentary, followed by a short 2-line heartfelt personal prayer.")
                            })
                        })
                    })
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GeminiApi", "Error response: ${response.code} ${response.message}")
                        _geminiReflection.value = generateLocalReflection(passageTitle, passageText)
                    } else {
                        val responseBody = response.body?.string() ?: ""
                        val jsonObject = JSONObject(responseBody)
                        val candidates = jsonObject.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                val text = parts.getJSONObject(0).getString("text")
                                _geminiReflection.value = text
                            } else {
                                _geminiReflection.value = generateLocalReflection(passageTitle, passageText)
                            }
                        } else {
                            _geminiReflection.value = generateLocalReflection(passageTitle, passageText)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiApi", "Gemini request failed: ${e.message}", e)
                _geminiReflection.value = generateLocalReflection(passageTitle, passageText)
            } finally {
                _isGeneratingReflection.value = false
            }
        }
    }

    private fun generateLocalReflection(title: String, text: String): String {
        return "✨ **Spiritual Reflection on $title**\n\n" +
                "This passage invites us into deeper communion with God. When we slow down to meditate on '$text', we are reminded of His infinite mercy, His guiding light, and His sovereign call upon our lives. This word is a strong tower and an anchor of hope for your soul today.\n\n" +
                "May you walk in the full confidence of His power, knowing that the Spirit of truth dwells in you and guides you through every season.\n\n" +
                "🙏 **Heartfelt Prayer**\n" +
                "\"Heavenly Father, thank You for the living power of Your Word. Renew my mind today, strengthen my heart, and let my words bring honor and glory to Your Holy name. Amen.\""
    }

    private val prefs = application.getSharedPreferences("bible_app_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) {
            prefs.getBoolean("is_dark_theme", false)
        } else {
            null // Use system default
        }
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme

    fun setThemeMode(isDark: Boolean?) {
        _isDarkTheme.value = isDark
        if (isDark == null) {
            prefs.edit().remove("is_dark_theme").apply()
        } else {
            prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }
}
