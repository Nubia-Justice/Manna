package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingPlanDao {
    @Query("SELECT * FROM reading_plans")
    fun getAllPlans(): Flow<List<ReadingPlan>>

    @Query("SELECT * FROM reading_plans WHERE id = :id")
    suspend fun getPlanById(id: Int): ReadingPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ReadingPlan): Long

    @Update
    suspend fun updatePlan(plan: ReadingPlan)

    @Transaction
    suspend fun activatePlan(planId: Int) {
        // Set all other plans inactive, set the target plan active
        deactivateAllPlans()
        setPlanActive(planId)
    }

    @Query("UPDATE reading_plans SET isActive = 0")
    suspend fun deactivateAllPlans()

    @Query("UPDATE reading_plans SET isActive = 1 WHERE id = :planId")
    suspend fun setPlanActive(planId: Int)
}

@Dao
interface ReadingPlanDayDao {
    @Query("SELECT * FROM reading_plan_days WHERE planId = :planId ORDER BY dayNumber ASC")
    fun getDaysForPlan(planId: Int): Flow<List<ReadingPlanDay>>

    @Query("SELECT * FROM reading_plan_days WHERE planId = :planId AND dayNumber = :dayNumber LIMIT 1")
    suspend fun getDay(planId: Int, dayNumber: Int): ReadingPlanDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: ReadingPlanDay)

    @Update
    suspend fun updateDay(day: ReadingPlanDay)

    @Query("SELECT COUNT(*) FROM reading_plan_days WHERE planId = :planId AND isCompleted = 1")
    fun getCompletedDaysCount(planId: Int): Flow<Int>

    @Query("SELECT * FROM reading_plan_days WHERE isCompleted = 1 ORDER BY completionDate DESC, dayNumber DESC")
    fun getAllCompletedDays(): Flow<List<ReadingPlanDay>>
}

@Dao
interface VerseInteractionDao {
    @Query("SELECT * FROM verse_interactions WHERE planId = :planId AND dayNumber = :dayNumber")
    fun getInteractionsForDay(planId: Int, dayNumber: Int): Flow<List<VerseInteraction>>

    @Query("SELECT * FROM verse_interactions WHERE id = :id LIMIT 1")
    suspend fun getInteractionById(id: String): VerseInteraction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInteraction(interaction: VerseInteraction)

    @Query("SELECT * FROM verse_interactions WHERE isHighlighted = 1 OR isMarked = 1 OR noteText != ''")
    fun getAllStarredInteractions(): Flow<List<VerseInteraction>>
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_records WHERE id = 1 LIMIT 1")
    fun getStreak(): Flow<StreakRecord?>

    @Query("SELECT * FROM streak_records WHERE id = 1 LIMIT 1")
    suspend fun getStreakDirect(): StreakRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakRecord)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM user_badges")
    fun getAllBadges(): Flow<List<UserBadge>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: UserBadge)

    @Query("UPDATE user_badges SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :badgeId")
    suspend fun unlockBadge(badgeId: String, timestamp: Long)
}
