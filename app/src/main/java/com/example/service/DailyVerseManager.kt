package com.example.service

import java.util.Calendar

data class DailyVerse(
    val reference: String,
    val text: String,
    val reflection: String
)

object DailyVerseManager {
    private val verses = listOf(
        DailyVerse(
            reference = "Romans 8:28",
            text = "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.",
            reflection = "Take comfort today knowing that nothing is wasted in God's hands. He is working behind the scenes of your life, molding every circumstance for your ultimate good and His divine glory."
        ),
        DailyVerse(
            reference = "Philippians 4:13",
            text = "I can do all things through Christ which strengtheneth me.",
            reflection = "Your energy and intelligence are finite, but Christ's strength is limitless. Whatever task or trial is before you today, approach it with confidence because His power resides in you."
        ),
        DailyVerse(
            reference = "Isaiah 40:31",
            text = "But they that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.",
            reflection = "In a world of constant hustle, waiting on God is where true strength is restored. Quiet your heart today and let Him lift your heavy burdens, renewing your spirit with fresh wind."
        ),
        DailyVerse(
            reference = "Joshua 1:9",
            text = "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the Lord thy God is with thee whithersoever thou goest.",
            reflection = "Fear is a natural response to the unknown, but courage is a decision rooted in God's presence. Remember today that you are never alone—the Creator of the stars walks beside you."
        ),
        DailyVerse(
            reference = "Psalm 23:1",
            text = "The Lord is my shepherd; I shall not want.",
            reflection = "A sheep wants for nothing because the shepherd provides everything. Trust that God knows your physical, emotional, and spiritual needs today, and He will lead you to quiet waters."
        ),
        DailyVerse(
            reference = "Proverbs 3:5-6",
            text = "Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.",
            reflection = "Yielding control can feel scary, but our own understanding is limited. Hand the steering wheel of your life to God today, and watch Him make straight paths through the wilderness."
        ),
        DailyVerse(
            reference = "Matthew 6:33",
            text = "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.",
            reflection = "Priorities shape our peace. When you focus your energy on seeking God's kingdom and pleasing Him first, the anxieties about tomorrow's needs naturally fade into His sovereign care."
        ),
        DailyVerse(
            reference = "Romans 12:2",
            text = "And be not conformed to this world: but be ye transformed by the renewing of your mind, that ye may prove what is that good, and acceptable, and perfect, will of God.",
            reflection = "Your mind is a gateway. Guard it against negative inputs and renew it daily through scripture. A renewed mind leads to a transformed life aligned with God's pleasing will."
        ),
        DailyVerse(
            reference = "Hebrews 11:1",
            text = "Now faith is the substance of things hoped for, the evidence of things not seen.",
            reflection = "Faith is not blind optimism; it is anchor-like assurance. It is trusting in the character of God even when you cannot see the outcome, resting in His unshakable promises."
        ),
        DailyVerse(
            reference = "Colossians 3:23",
            text = "And whatsoever ye do, do it heartily, as to the Lord, and not unto men.",
            reflection = "Whether washing dishes, writing code, or managing teams, every action is an act of worship. Shift your focus from seeking human approval to offering your work as a fragrant gift to God."
        ),
        DailyVerse(
            reference = "Galatians 5:22-23",
            text = "But the fruit of the Spirit is love, joy, peace, longsuffering, gentleness, goodness, faith, meekness, temperance: against such there is no law.",
            reflection = "You cannot force these fruits by willpower; they are cultivated as you abide in Christ. Invite the Holy Spirit to flow through your thoughts and words, spreading peace today."
        ),
        DailyVerse(
            reference = "James 1:5",
            text = "If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him.",
            reflection = "Stuck on a difficult decision? God doesn't hold back wisdom or criticize your confusion. Ask Him with an open heart, and expect Him to guide your steps generously."
        ),
        DailyVerse(
            reference = "1 Peter 5:7",
            text = "Casting all your care upon him; for he careth for you.",
            reflection = "You were never designed to carry the crushing weight of anxiety. Physically roll your worries onto Jesus today, because His shoulder is broad and He deeply cares for your well-being."
        ),
        DailyVerse(
            reference = "Zephaniah 3:17",
            text = "The Lord thy God in the midst of thee is mighty; he will save, he will rejoice over thee with joy; he will rest in his love, he will joy over thee with singing.",
            reflection = "Imagine the King of Heaven singing over you with joy! Rest in His perfect, unconditional love today, knowing that He delights in your existence and fights your battles."
        ),
        DailyVerse(
            reference = "Psalm 119:105",
            text = "Thy word is a lamp unto my feet, and a light unto my path.",
            reflection = "A lamp doesn't light up the whole horizon; it lights up the next step. Don't worry about the whole year—just trust God for the step in front of you today through His Word."
        )
    )

    fun getDailyVerse(): DailyVerse {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % verses.size
        return verses[index]
    }
}
