package com.example

data class Quote(
    val id: Int,
    val text: String,
    val author: String,
    val category: String,
    val emoji: String
)

object QuoteRepository {
    val quotes = listOf(
        Quote(
            id = 1,
            text = "The only limit to our realization of tomorrow is our doubts of today.",
            author = "Franklin D. Roosevelt",
            category = "Belief",
            emoji = "✨"
        ),
        Quote(
            id = 2,
            text = "Do what you can, with what you have, where you are.",
            author = "Theodore Roosevelt",
            category = "Action",
            emoji = "📍"
        ),
        Quote(
            id = 3,
            text = "It always seems impossible until it is done.",
            author = "Nelson Mandela",
            category = "Perseverance",
            emoji = "🏆"
        ),
        Quote(
            id = 4,
            text = "Keep your eyes on the stars, and your feet on the ground.",
            author = "Theodore Roosevelt",
            category = "Focus",
            emoji = "🌌"
        ),
        Quote(
            id = 5,
            text = "The only way to do great work is to love what you do.",
            author = "Steve Jobs",
            category = "Passion",
            emoji = "💼"
        ),
        Quote(
            id = 6,
            text = "Believe you can and you're halfway there.",
            author = "Theodore Roosevelt",
            category = "Mindset",
            emoji = "🎯"
        ),
        Quote(
            id = 7,
            text = "The best way to predict the future is to create it.",
            author = "Peter Drucker",
            category = "Vision",
            emoji = "🚀"
        ),
        Quote(
            id = 8,
            text = "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            author = "Winston Churchill",
            category = "Resilience",
            emoji = "🛡️"
        ),
        Quote(
            id = 9,
            text = "You miss 100% of the shots you don't take.",
            author = "Wayne Gretzky",
            category = "Courage",
            emoji = "🏒"
        ),
        Quote(
            id = 10,
            text = "Act as if what you do makes a difference. It does.",
            author = "William James",
            category = "Purpose",
            emoji = "⚡"
        ),
        Quote(
            id = 11,
            text = "In the middle of every difficulty lies opportunity.",
            author = "Albert Einstein",
            category = "Opportunity",
            emoji = "💡"
        ),
        Quote(
            id = 12,
            text = "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
            author = "Ralph Waldo Emerson",
            category = "Wisdom",
            emoji = "💎"
        ),
        Quote(
            id = 13,
            text = "The dreamers are the saviors of the world.",
            author = "James Allen",
            category = "Dreams",
            emoji = "🌈"
        ),
        Quote(
            id = 14,
            text = "Happiness is not something ready-made. It comes from your own actions.",
            author = "Dalai Lama",
            category = "Joy",
            emoji = "☀️"
        ),
        Quote(
            id = 15,
            text = "Your time is limited, so don't waste it living someone else's life.",
            author = "Steve Jobs",
            category = "Time",
            emoji = "⏳"
        ),
        Quote(
            id = 16,
            text = "Go confidently in the direction of your dreams! Live the life you’ve imagined.",
            author = "Henry David Thoreau",
            category = "Direction",
            emoji = "🧭"
        ),
        Quote(
            id = 17,
            text = "The future belongs to those who believe in the beauty of their dreams.",
            author = "Eleanor Roosevelt",
            category = "Hope",
            emoji = "⭐"
        ),
        Quote(
            id = 18,
            text = "Do not go where the path may lead, go instead where there is no path and leave a trail.",
            author = "Ralph Waldo Emerson",
            category = "Leadership",
            emoji = "👣"
        ),
        Quote(
            id = 19,
            text = "Everything you’ve ever wanted is on the other side of fear.",
            author = "George Addair",
            category = "Fearless",
            emoji = "🦁"
        ),
        Quote(
            id = 20,
            text = "It is during our darkest moments that we must focus to see the light.",
            author = "Aristotle",
            category = "Hope",
            emoji = "🕯️"
        )
    )
}
