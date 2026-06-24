package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class BibleRepository(
    private val planDao: ReadingPlanDao,
    private val planDayDao: ReadingPlanDayDao,
    private val interactionDao: VerseInteractionDao,
    private val streakDao: StreakDao,
    private val badgeDao: BadgeDao
) {
    val allPlans: Flow<List<ReadingPlan>> = planDao.getAllPlans()
    val allBadges: Flow<List<UserBadge>> = badgeDao.getAllBadges()
    val userStreak: Flow<StreakRecord?> = streakDao.getStreak()
    val starredInteractions: Flow<List<VerseInteraction>> = interactionDao.getAllStarredInteractions()
    val allCompletedDays: Flow<List<ReadingPlanDay>> = planDayDao.getAllCompletedDays()

    fun getDaysForPlan(planId: Int): Flow<List<ReadingPlanDay>> = planDayDao.getDaysForPlan(planId)
    fun getCompletedCount(planId: Int): Flow<Int> = planDayDao.getCompletedDaysCount(planId)
    fun getInteractionsForDay(planId: Int, dayNumber: Int): Flow<List<VerseInteraction>> =
        interactionDao.getInteractionsForDay(planId, dayNumber)

    suspend fun getPlanById(id: Int): ReadingPlan? = planDao.getPlanById(id)
    suspend fun getDay(planId: Int, dayNumber: Int): ReadingPlanDay? = planDayDao.getDay(planId, dayNumber)

    suspend fun saveVerseInteraction(interaction: VerseInteraction) {
        interactionDao.insertOrUpdateInteraction(interaction)
        // Unlock scribe badge if user makes a highlight or writes a note
        if (interaction.isHighlighted || interaction.isMarked || interaction.noteText.isNotEmpty()) {
            unlockBadgeDirect("note_taken")
        }
    }

    suspend fun activatePlan(planId: Int) {
        planDao.activatePlan(planId)
    }

    suspend fun createCustomPlan(title: String, description: String, daysCount: Int, theme: String) {
        val newPlan = ReadingPlan(
            title = title,
            description = description,
            durationDays = daysCount,
            isActive = true
        )
        val planId = planDao.insertPlan(newPlan).toInt()

        // Generate scripture readings dynamically based on selected theme
        val scriptureTemplates = when (theme.lowercase()) {
            "wisdom" -> listOf(
                Pair("Proverbs 3:5-6", "5 Trust in the Lord with all thine heart; and lean not unto thine own understanding.\n6 In all thy ways acknowledge him, and he shall direct thy paths."),
                Pair("Psalm 111:10", "10 The fear of the Lord is the beginning of wisdom: a good understanding have all they that do his commandments: his praise endureth for ever."),
                Pair("James 1:5", "5 If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him."),
                Pair("Proverbs 4:7", "7 Wisdom is the principal thing; therefore get wisdom: and with all thy getting get understanding."),
                Pair("Colossians 3:16", "16 Let the word of Christ dwell in you richly in all wisdom; teaching and admonishing one another in psalms and hymns and spiritual songs, singing with grace in your hearts to the Lord.")
            )
            "comfort" -> listOf(
                Pair("Psalm 23:4", "4 Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me."),
                Pair("John 14:27", "27 Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid."),
                Pair("Matthew 11:28", "28 Come unto me, all ye that labour and are heavy laden, and I will give you rest."),
                Pair("Psalm 34:18", "18 The Lord is nigh unto them that are of a broken heart; and saveth such as be of a contrite spirit."),
                Pair("Revelation 21:4", "4 And God shall wipe away all tears from their eyes; and there shall be no more death, neither sorrow, nor crying, neither shall there be any more pain: for the former things are passed away.")
            )
            "strength" -> listOf(
                Pair("Isaiah 40:31", "31 But they that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."),
                Pair("Philippians 4:13", "13 I can do all things through Christ which strengtheneth me."),
                Pair("Joshua 1:9", "9 Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the Lord thy God is with thee whithersoever thou goest."),
                Pair("Ephesians 6:10", "10 Finally, my brethren, be strong in the Lord, and in the power of his might."),
                Pair("Psalm 28:7", "7 The Lord is my strength and my shield; my heart trusted in him, and I am helped: therefore my heart greatly rejoiceth; and with my song will I praise him.")
            )
            else -> listOf( // "faith" & default
                Pair("Hebrews 11:1", "1 Now faith is the substance of things hoped for, the evidence of things not seen."),
                Pair("Romans 10:17", "17 So then faith cometh by hearing, and hearing by the word of God."),
                Pair("Hebrews 11:6", "6 But without faith it is impossible to please him: for he that cometh to God must believe that he is, and that he is a rewarder of them that diligently seek him."),
                Pair("Ephesians 2:8", "8 For by grace are ye saved through faith; and that not of yourselves: it is the gift of God."),
                Pair("Romans 1:17", "17 For therein is the righteousness of God revealed from faith to faith: as it is written, The just shall live by faith.")
            )
        }

        for (day in 1..daysCount) {
            val template = scriptureTemplates[(day - 1) % scriptureTemplates.size]
            val passageText = template.second
            val versesTitle = "${template.first} (Day $day)"

            planDayDao.insertDay(
                ReadingPlanDay(
                    planId = planId,
                    dayNumber = day,
                    versesToRead = versesTitle,
                    passageText = passageText,
                    isCompleted = false
                )
            )
            // Wait, we need to INSERT the day rather than update!
            // Let's call insertDay but since we only have updateDay in DayDao,
            // let's make sure we insert properly or define insert.
            // Oh, we can define insert or use ReadingPlanDayDao's insert.
            // Let's create an insert method on our DB directly, or we can add it to the Dao.
            // Let's add standard insert to ReadingPlanDayDao. We'll do that shortly!
        }

        // Deactivate other plans and set this one active
        planDao.activatePlan(planId)
    }

    suspend fun insertPlanDayDirect(day: ReadingPlanDay) {
        // We'll write a helper in the DB / DAO or do it inside the DAO.
    }

    // Complete reading verification and update streaks!
    suspend fun verifyAndCompleteDay(planId: Int, dayNumber: Int, readAloudVerified: Boolean): Boolean {
        val day = planDayDao.getDay(planId, dayNumber) ?: return false
        if (day.isCompleted) return true // Already completed

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())

        // 1. Mark day as completed
        val updatedDay = day.copy(isCompleted = true, completionDate = todayStr)
        planDayDao.updateDay(updatedDay)

        // 2. Process streak record
        val currentStreakRecord = streakDao.getStreakDirect() ?: StreakRecord(id = 1)
        var nextStreak = currentStreakRecord.currentStreak
        val lastDateStr = currentStreakRecord.lastReadDate

        if (lastDateStr == null) {
            nextStreak = 1
        } else if (lastDateStr == todayStr) {
            // Already read today, streak remains same
        } else {
            // Compare dates to see if yesterday
            try {
                val lastDate = dateFormat.parse(lastDateStr)
                val todayDate = dateFormat.parse(todayStr)
                val diffMs = todayDate.time - lastDate.time
                val diffDays = diffMs / (1000 * 60 * 60 * 24)

                if (diffDays <= 1L) {
                    nextStreak += 1
                } else {
                    nextStreak = 1 // Streak broken, reset
                }
            } catch (e: Exception) {
                nextStreak = 1
            }
        }

        val nextMaxStreak = max(currentStreakRecord.maxStreak, nextStreak)
        val newStreakRecord = StreakRecord(
            id = 1,
            currentStreak = nextStreak,
            maxStreak = nextMaxStreak,
            lastReadDate = todayStr
        )
        streakDao.insertOrUpdateStreak(newStreakRecord)

        // 3. Unlock Badges dynamically
        unlockBadgeDirect("first_reading")

        if (nextStreak >= 3) {
            unlockBadgeDirect("streak_3")
        }
        if (nextStreak >= 7) {
            unlockBadgeDirect("streak_7")
        }
        if (readAloudVerified) {
            unlockBadgeDirect("voice_reader")
        }

        // Check Early Bird (if completion timestamp is before 8 AM)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour < 8) {
            unlockBadgeDirect("early_bird")
        }

        return true
    }

    private suspend fun unlockBadgeDirect(badgeId: String) {
        badgeDao.unlockBadge(badgeId, System.currentTimeMillis())
    }

    suspend fun ensurePreseededData() {
        Log.d("BibleRepository", "Starting seeding...")
        // 1. Check if reading plans exist
        val currentPlans = planDao.getAllPlans().first()
        if (currentPlans.isEmpty()) {
            Log.d("BibleRepository", "Database is empty. Seeding plans and badges.")

            // Insert initial plans
            val plan1Id = planDao.insertPlan(ReadingPlan(
                title = "Whole Bible Walkthrough",
                description = "An immersive journey starting in Genesis, visiting key Psalms, and resting in the Gospels.",
                durationDays = 5,
                isActive = true
            )).toInt()

            val plan2Id = planDao.insertPlan(ReadingPlan(
                title = "Wisdom Journey (Psalms & Proverbs)",
                description = "Daily inspirational words focused on godly instruction, reflection, and strength.",
                durationDays = 5,
                isActive = false
            )).toInt()

            // Insert Plan 1 Days
            val day1P1 = ReadingPlanDay(
                planId = plan1Id,
                dayNumber = 1,
                versesToRead = "Genesis 1:1-5",
                passageText = "1 In the beginning God created the heaven and the earth.\n" +
                        "2 And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.\n" +
                        "3 And God said, Let there be light: and there was light.\n" +
                        "4 And God saw the light, that it was good: and God divided the light from the darkness.\n" +
                        "5 And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day."
            )
            val day2P1 = ReadingPlanDay(
                planId = plan1Id,
                dayNumber = 2,
                versesToRead = "Psalm 23:1-6",
                passageText = "1 The Lord is my shepherd; I shall not want.\n" +
                        "2 He maketh me to lie down in green pastures: he leadeth me beside the still waters.\n" +
                        "3 He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake.\n" +
                        "4 Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me.\n" +
                        "5 Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil; my cup runneth over.\n" +
                        "6 Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the Lord for ever."
            )
            val day3P1 = ReadingPlanDay(
                planId = plan1Id,
                dayNumber = 3,
                versesToRead = "John 1:1-5",
                passageText = "1 In the beginning was the Word, and the Word was with God, and the Word was God.\n" +
                        "2 The same was in the beginning with God.\n" +
                        "3 All things were made by him; and without him was not any thing made that was made.\n" +
                        "4 In him was life; and the life was the light of men.\n" +
                        "5 And the light shineth in darkness; and the darkness comprehended it not."
            )
            val day4P1 = ReadingPlanDay(
                planId = plan1Id,
                dayNumber = 4,
                versesToRead = "Philippians 4:4-8",
                passageText = "4 Rejoice in the Lord alway: and again I say, Rejoice.\n" +
                        "5 Let your moderation be known unto all men. The Lord is at hand.\n" +
                        "6 Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.\n" +
                        "7 And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus.\n" +
                        "8 Finally, brethren, whatsoever things are true, whatsoever things are honest, whatsoever things are just, whatsoever things are pure, whatsoever things are lovely, whatsoever things are of good report; if there be any virtue, and if there be any praise, think on these things."
            )
            val day5P1 = ReadingPlanDay(
                planId = plan1Id,
                dayNumber = 5,
                versesToRead = "Romans 8:28-32",
                passageText = "28 And we know that all things work together for good to them that love God, to them who are the called according to his purpose.\n" +
                        "29 For whom he did foreknow, he also did predestinate to be conformed to the image of his Son, that he might be the firstborn among many brethren.\n" +
                        "30 Moreover whom he did predestinate, them he also called: and whom he called, them he also justified: and whom he justified, them he also glorified.\n" +
                        "31 What shall we then say to these things? If God be for us, who can be against us.\n" +
                        "32 He that spared not his own Son, but delivered him up for us all, how shall he not with him also freely give us all things."
            )

            // Insert Days for Plan 2 (Wisdom)
            val day1P2 = ReadingPlanDay(
                planId = plan2Id,
                dayNumber = 1,
                versesToRead = "Proverbs 3:5-6",
                passageText = "5 Trust in the Lord with all thine heart; and lean not unto thine own understanding.\n6 In all thy ways acknowledge him, and he shall direct thy paths."
            )
            val day2P2 = ReadingPlanDay(
                planId = plan2Id,
                dayNumber = 2,
                versesToRead = "Psalm 119:105",
                passageText = "105 Thy word is a lamp unto my feet, and a light unto my path."
            )
            val day3P2 = ReadingPlanDay(
                planId = plan2Id,
                dayNumber = 3,
                versesToRead = "Proverbs 4:18-19",
                passageText = "18 But the path of the just is as the shining light, that shineth more and more unto the perfect day.\n19 The way of the wicked is as darkness: they know not at what they stumble."
            )
            val day4P2 = ReadingPlanDay(
                planId = plan2Id,
                dayNumber = 4,
                versesToRead = "Psalm 19:1-4",
                passageText = "1 The heavens declare the glory of God; and the firmament sheweth his handywork.\n2 Day unto day uttereth speech, and night unto night sheweth knowledge.\n3 There is no speech nor language, where their voice is not heard.\n4 Their line is gone out through all the earth, and their words to the end of the world."
            )
            val day5P2 = ReadingPlanDay(
                planId = plan2Id,
                dayNumber = 5,
                versesToRead = "Proverbs 16:3-9",
                passageText = "3 Commit thy works unto the Lord, and thy thoughts shall be established.\n9 A man's heart deviseth his way: but the Lord directeth his steps."
            )

            // Insert day records
            planDayDao.insertDay(day1P1)
            planDayDao.insertDay(day2P1)
            planDayDao.insertDay(day3P1)
            planDayDao.insertDay(day4P1)
            planDayDao.insertDay(day5P1)

            planDayDao.insertDay(day1P2)
            planDayDao.insertDay(day2P2)
            planDayDao.insertDay(day3P2)
            planDayDao.insertDay(day4P2)
            planDayDao.insertDay(day5P2)

            // Save Streak Record
            streakDao.insertOrUpdateStreak(StreakRecord(id = 1, currentStreak = 0, maxStreak = 0, lastReadDate = null))

            // Seed initial badges
            badgeDao.insertBadge(UserBadge("first_reading", "First Step", "Completed your very first scripture study session!", "ic_star", false))
            badgeDao.insertBadge(UserBadge("streak_3", "Scripture Devotee", "Studied 3 days in a row!", "ic_fire", false))
            badgeDao.insertBadge(UserBadge("streak_7", "Faithful Disciple", "Studied 7 days in a row! Beautiful consistency.", "ic_crown", false))
            badgeDao.insertBadge(UserBadge("voice_reader", "Living Word", "Completed a verification by reading scripture aloud!", "ic_mic", false))
            badgeDao.insertBadge(UserBadge("note_taken", "Inspired Scribe", "Wrote a personal reflection or highlighted verses!", "ic_edit", false))
            badgeDao.insertBadge(UserBadge("early_bird", "Dawn Seeker", "Completed a study session before 8:00 AM!", "ic_wb_sunny", false))
        }
    }
}
