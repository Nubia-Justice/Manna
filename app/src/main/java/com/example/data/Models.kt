package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_plans")
data class ReadingPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val durationDays: Int,
    val isActive: Boolean = false
)

@Entity(tableName = "reading_plan_days")
data class ReadingPlanDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val dayNumber: Int,
    val versesToRead: String,
    val passageText: String, // Holds the full Scripture passage for offline reading!
    val isCompleted: Boolean = false,
    val completionDate: String? = null // "yyyy-MM-dd"
)

@Entity(tableName = "verse_interactions")
data class VerseInteraction(
    @PrimaryKey val id: String, // format: "planId_dayNumber_verseIndex"
    val planId: Int,
    val dayNumber: Int,
    val verseIndex: Int,
    val verseText: String,
    val isHighlighted: Boolean = false,
    val highlightColor: Int = 0, // 0 for none, or color resource/hex values
    val isMarked: Boolean = false, // bookmark/favorite
    val noteText: String = ""
)

@Entity(tableName = "streak_records")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lastReadDate: String? = null // "yyyy-MM-dd"
)

@Entity(tableName = "user_badges")
data class UserBadge(
    @PrimaryKey val id: String, // e.g. "streak_3", "first_reading", "voice_reader"
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)
