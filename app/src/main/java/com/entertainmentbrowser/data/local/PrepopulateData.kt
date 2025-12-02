package com.entertainmentbrowser.data.local

import com.entertainmentbrowser.data.local.entity.WebsiteEntity

object PrepopulateData {
    
    fun getWebsites(): List<WebsiteEntity> = listOf(
        // Streaming Services (15)
        WebsiteEntity(
            id = 1,
            name = "Netflix",
            url = "https://a.asd.homes/",
            category = "STREAMING",
            logoUrl = "data:image/png;base64,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",
            description = "Watch TV shows and movies anytime, anywhere",
            backgroundColor = "#E50914",
            isFavorite = false,
            order = 1
        ),
        WebsiteEntity(
            id = 2,
            name = "Disney+",
            url = "https://3rabxn.com/  ",
            category = "STREAMING",
            logoUrl = " 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",
            description = "Stream Disney, Pixar, Marvel, Star Wars, and more",
            backgroundColor = "#113CCF",
            isFavorite = false,
            order = 2
        ),
        WebsiteEntity(
            id = 3,
            name = "Amazon Prime Video",
            url = "https://www.xxbrits.com/ ",
            category = "STREAMING",
            logoUrl = "data:image/jpeg;base64,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",
            description = "Watch movies, TV, and live events",
            backgroundColor = "#00A8E1",
            isFavorite = false,
            order = 3
        ),
        WebsiteEntity(
            id = 4,
            name = "HBO Max",
            url = "https://www.alohatube.com/arab.html",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/hbo-max-1.svg",
            description = "Stream HBO originals, movies, and more",
            backgroundColor = "#0014FF",
            isFavorite = false,
            order = 4
        ),
        WebsiteEntity(
            id = 5,
            name = "Hulu",
            url = "https://www.hulu.com",
            category = "STREAMING",
            logoUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQXkKnstBBfDVhYw3wtpH40VI_5Dw_JKOFaqw&s",
            description = "Watch current TV shows and movies",
            backgroundColor = "#1CE783",
            isFavorite = false,
            order = 5
        ),
        WebsiteEntity(
            id = 6,
            name = "Apple TV+",
            url = "https://adblock-tester.com/",
            category = "STREAMING",
            logoUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTLzuzB1tQYR4zLPpa_d_ryAANbucRKXOQlew&s",
            description = "Original shows and movies from Apple",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 6
        ),
        WebsiteEntity(
            id = 7,
            name = "google",
            url = "https://www.google.com/",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/paramount-plus.svg",
            description = "Stream Paramount originals and classics",
            backgroundColor = "#0064FF",
            isFavorite = false,
            order = 7
        ),
        WebsiteEntity(
            id = 8,
            name = "borntobefuck",
            url = "https://uk.borntobefuck.com/",
            category = "STREAMING",
            logoUrl = "data:image/png;base64,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",
            description = "NBCUniversal's streaming service",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 8
        ),
        WebsiteEntity(
            id = 9,
            name = "gotanynudes",
            url = "https://gotanynudes.com/",
            category = "STREAMING",
            logoUrl = "https://gotanynudes.com/wp-content/uploads/2025/11/logo-original-gotanynudes.png",
            description = "Watch anime and read manga",
            backgroundColor = "#F47521",
            isFavorite = false,
            order = 9
        ),
        WebsiteEntity(
            id = 10,
            name = "porn4fans",
            url = "https://www.porn4fans.com/",
            category = "STREAMING",
            logoUrl = "https://www.porn4fans.com/static/images/header_elements/thanksgiving/logo_1.svg",
            description = "Stream anime in English dub and sub",
            backgroundColor = "#410099",
            isFavorite = false,
            order = 10
        ),
        WebsiteEntity(
            id = 11,
            name = "internetchicks",
            url = "https://internetchicks.com/",
            category = "STREAMING",
            logoUrl = "https://internetchicks.com/wp-content/uploads/2021/02/cropped-Untitled-design-56.png?x39449",
            description = "Original series, movies, and sports",
            backgroundColor = "#D4001A",
            isFavorite = false,
            order = 11
        ),
        WebsiteEntity(
            id = 12,
            name = "pimpbunny",
            url = "https://pimpbunny.com/",
            category = "STREAMING",
            logoUrl = "data:image/jpeg;base64,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",
            description = "Premium original series and movies",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 12
        ),
        WebsiteEntity(
            id = 13,
            name = "Discovery+",
            url = "https://www.discoveryplus.com",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/discovery-plus.svg",
            description = "Stream reality TV and documentaries",
            backgroundColor = "#0075FF",
            isFavorite = false,
            order = 13
        ),
        WebsiteEntity(
            id = 14,
            name = "ESPN+",
            url = "https://plus.espn.com",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/espn.svg",
            description = "Live sports and original shows",
            backgroundColor = "#D3212D",
            isFavorite = false,
            order = 14
        ),
        WebsiteEntity(
            id = 15,
            name = "Tubi",
            url = "https://tubitv.com",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tubi.svg",
            description = "Free movies and TV shows",
            backgroundColor = "#FA541C",
            isFavorite = false,
            order = 15
        ),
        
        // TV Shows (10)
        WebsiteEntity(
            id = 16,
            name = "IMDb",
            url = "https://www.imdb.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/imdb-1.svg",
            description = "Movies, TV shows, and celebrity info",
            backgroundColor = "#F5C518",
            isFavorite = false,
            order = 16
        ),
        WebsiteEntity(
            id = 17,
            name = "TV Guide",
            url = "https://www.tvguide.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tv-guide.svg",
            description = "TV listings and entertainment news",
            backgroundColor = "#E50914",
            isFavorite = false,
            order = 17
        ),
        WebsiteEntity(
            id = 18,
            name = "Rotten Tomatoes",
            url = "https://www.rottentomatoes.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/rotten-tomatoes.svg",
            description = "Movie and TV reviews and ratings",
            backgroundColor = "#FA320A",
            isFavorite = false,
            order = 18
        ),
        WebsiteEntity(
            id = 19,
            name = "Metacritic",
            url = "https://www.metacritic.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/metacritic.svg",
            description = "Aggregate reviews for entertainment",
            backgroundColor = "#FFCC33",
            isFavorite = false,
            order = 19
        ),
        WebsiteEntity(
            id = 20,
            name = "Trakt",
            url = "https://trakt.tv",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/trakt.svg",
            description = "Track TV shows and movies you watch",
            backgroundColor = "#ED1C24",
            isFavorite = false,
            order = 20
        ),
        WebsiteEntity(
            id = 21,
            name = "TVMaze",
            url = "https://www.tvmaze.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tvmaze.svg",
            description = "TV show database and schedule",
            backgroundColor = "#1E1E1E",
            isFavorite = false,
            order = 21
        ),
        WebsiteEntity(
            id = 22,
            name = "TheTVDB",
            url = "https://thetvdb.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/thetvdb.svg",
            description = "Community-driven TV database",
            backgroundColor = "#1E90FF",
            isFavorite = false,
            order = 22
        ),
        WebsiteEntity(
            id = 23,
            name = "JustWatch",
            url = "https://www.justwatch.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/justwatch.svg",
            description = "Find where to stream movies and TV",
            backgroundColor = "#FFD500",
            isFavorite = false,
            order = 23
        ),
        WebsiteEntity(
            id = 24,
            name = "Reelgood",
            url = "https://reelgood.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/reelgood.svg",
            description = "Universal guide to streaming content",
            backgroundColor = "#FF6B6B",
            isFavorite = false,
            order = 24
        ),
        WebsiteEntity(
            id = 25,
            name = "TV Time",
            url = "https://www.tvtime.com",
            category = "TV_SHOWS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tv-time.svg",
            description = "Track and discover TV shows",
            backgroundColor = "#FF5722",
            isFavorite = false,
            order = 25
        ),
        
        // Books (10)
        WebsiteEntity(
            id = 26,
            name = "Goodreads",
            url = "https://www.goodreads.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/goodreads.svg",
            description = "Book reviews and recommendations",
            backgroundColor = "#553B08",
            isFavorite = false,
            order = 26
        ),
        WebsiteEntity(
            id = 27,
            name = "Audible",
            url = "https://www.audible.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/audible.svg",
            description = "Audiobooks and podcasts",
            backgroundColor = "#FF9900",
            isFavorite = false,
            order = 27
        ),
        WebsiteEntity(
            id = 28,
            name = "Kindle",
            url = "https://read.amazon.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/kindle.svg",
            description = "Read eBooks on any device",
            backgroundColor = "#232F3E",
            isFavorite = false,
            order = 28
        ),
        WebsiteEntity(
            id = 29,
            name = "Scribd",
            url = "https://www.scribd.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/scribd.svg",
            description = "Unlimited books and audiobooks",
            backgroundColor = "#1A7BBA",
            isFavorite = false,
            order = 29
        ),
        WebsiteEntity(
            id = 30,
            name = "Kobo",
            url = "https://www.kobo.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/kobo.svg",
            description = "eBooks and eReaders",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 30
        ),
        WebsiteEntity(
            id = 31,
            name = "Project Gutenberg",
            url = "https://www.gutenberg.org",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/project-gutenberg.svg",
            description = "Free eBooks in the public domain",
            backgroundColor = "#6A1B9A",
            isFavorite = false,
            order = 31
        ),
        WebsiteEntity(
            id = 32,
            name = "Wattpad",
            url = "https://www.wattpad.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/wattpad.svg",
            description = "Read and write original stories",
            backgroundColor = "#FF5722",
            isFavorite = false,
            order = 32
        ),
        WebsiteEntity(
            id = 33,
            name = "BookBub",
            url = "https://www.bookbub.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/bookbub.svg",
            description = "Discover new books and deals",
            backgroundColor = "#D32F2F",
            isFavorite = false,
            order = 33
        ),
        WebsiteEntity(
            id = 34,
            name = "LibraryThing",
            url = "https://www.librarything.com",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/librarything.svg",
            description = "Catalog your books online",
            backgroundColor = "#8B4513",
            isFavorite = false,
            order = 34
        ),
        WebsiteEntity(
            id = 35,
            name = "Open Library",
            url = "https://openlibrary.org",
            category = "BOOKS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/open-library.svg",
            description = "Free digital library",
            backgroundColor = "#D4A574",
            isFavorite = false,
            order = 35
        ),
        
        // Video Platforms (10)
        WebsiteEntity(
            id = 36,
            name = "YouTube",
            url = "https://www.youtube.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/youtube-icon.svg",
            description = "Watch and share videos",
            backgroundColor = "#FF0000",
            isFavorite = false,
            order = 36
        ),
        WebsiteEntity(
            id = 37,
            name = "Vimeo",
            url = "https://vimeo.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/vimeo-icon.svg",
            description = "High-quality video sharing",
            backgroundColor = "#1AB7EA",
            isFavorite = false,
            order = 37
        ),
        WebsiteEntity(
            id = 38,
            name = "Dailymotion",
            url = "https://www.dailymotion.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/dailymotion.svg",
            description = "Watch, publish, and share videos",
            backgroundColor = "#0066DC",
            isFavorite = false,
            order = 38
        ),
        WebsiteEntity(
            id = 39,
            name = "Twitch",
            url = "https://www.twitch.tv",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/twitch.svg",
            description = "Live streaming for gamers",
            backgroundColor = "#9146FF",
            isFavorite = false,
            order = 39
        ),
        WebsiteEntity(
            id = 40,
            name = "TikTok",
            url = "https://www.tiktok.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tiktok-icon.svg",
            description = "Short-form video content",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 40
        ),
        WebsiteEntity(
            id = 41,
            name = "Rumble",
            url = "https://rumble.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/rumble.svg",
            description = "Video sharing platform",
            backgroundColor = "#85C742",
            isFavorite = false,
            order = 41
        ),
        WebsiteEntity(
            id = 42,
            name = "Odysee",
            url = "https://odysee.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/odysee.svg",
            description = "Decentralized video platform",
            backgroundColor = "#EF1970",
            isFavorite = false,
            order = 42
        ),
        WebsiteEntity(
            id = 43,
            name = "Bitchute",
            url = "https://www.bitchute.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/bitchute.svg",
            description = "Peer-to-peer video sharing",
            backgroundColor = "#F24E1E",
            isFavorite = false,
            order = 43
        ),
        WebsiteEntity(
            id = 44,
            name = "Veoh",
            url = "https://www.veoh.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/veoh.svg",
            description = "Watch and upload videos",
            backgroundColor = "#FF6600",
            isFavorite = false,
            order = 44
        ),
        WebsiteEntity(
            id = 45,
            name = "Metacafe",
            url = "https://www.metacafe.com",
            category = "VIDEO_PLATFORMS",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/metacafe.svg",
            description = "Short-form video entertainment",
            backgroundColor = "#FF9900",
            isFavorite = false,
            order = 45
        ),
        
        // Social Media (IDs 46-50)
        WebsiteEntity(
            id = 46,
            name = "TikTok",
            url = "https://www.tiktok.com",
            category = "SOCIAL_MEDIA",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/tiktok-icon.svg",
            description = "Short-form video content",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 46
        ),
        WebsiteEntity(
            id = 47,
            name = "Instagram",
            url = "https://www.instagram.com",
            category = "SOCIAL_MEDIA",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/instagram-icon.svg",
            description = "Photos and videos sharing",
            backgroundColor = "#E1306C",
            isFavorite = false,
            order = 47
        ),
        WebsiteEntity(
            id = 48,
            name = "Facebook",
            url = "https://www.facebook.com",
            category = "SOCIAL_MEDIA",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/facebook-icon.svg",
            description = "Connect with friends and family",
            backgroundColor = "#1877F2",
            isFavorite = false,
            order = 48
        ),
        WebsiteEntity(
            id = 49,
            name = "Twitter/X",
            url = "https://twitter.com",
            category = "SOCIAL_MEDIA",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/x-2.svg",
            description = "What's happening in the world",
            backgroundColor = "#000000",
            isFavorite = false,
            order = 49
        ),
        WebsiteEntity(
            id = 50,
            name = "Snapchat",
            url = "https://www.snapchat.com",
            category = "SOCIAL_MEDIA",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/snapchat.svg",
            description = "Share moments with friends",
            backgroundColor = "#FFFC00",
            isFavorite = false,
            order = 50
        ),
        
        // Games (IDs 51-55)
        WebsiteEntity(
            id = 51,
            name = "Steam",
            url = "https://store.steampowered.com",
            category = "GAMES",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/steam-icon-logo.svg",
            description = "PC gaming platform",
            backgroundColor = "#1B2838",
            isFavorite = false,
            order = 51
        ),
        WebsiteEntity(
            id = 52,
            name = "Epic Games",
            url = "https://www.epicgames.com",
            category = "GAMES",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/epic-games-2.svg",
            description = "Free games and store",
            backgroundColor = "#2F2D2E",
            isFavorite = false,
            order = 52
        ),
        WebsiteEntity(
            id = 53,
            name = "Roblox",
            url = "https://www.roblox.com",
            category = "GAMES",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/roblox.svg",
            description = "Play and create games",
            backgroundColor = "#E2231A",
            isFavorite = false,
            order = 53
        ),
        WebsiteEntity(
            id = 54,
            name = "Miniclip",
            url = "https://www.miniclip.com",
            category = "GAMES",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/miniclip.svg",
            description = "Free online games",
            backgroundColor = "#FF6600",
            isFavorite = false,
            order = 54
        ),
        WebsiteEntity(
            id = 55,
            name = "Poki",
            url = "https://poki.com",
            category = "GAMES",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/poki.svg",
            description = "Free online games",
            backgroundColor = "#009CFF",
            isFavorite = false,
            order = 55
        ),
        
        // Video Call (IDs 56-59)
        WebsiteEntity(
            id = 56,
            name = "Zoom",
            url = "https://zoom.us",
            category = "VIDEO_CALL",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/zoom-icon.svg",
            description = "Video conferencing",
            backgroundColor = "#2D8CFF",
            isFavorite = false,
            order = 56
        ),
        WebsiteEntity(
            id = 57,
            name = "Google Meet",
            url = "https://meet.google.com",
            category = "VIDEO_CALL",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/google-meet-icon.svg",
            description = "Video meetings by Google",
            backgroundColor = "#00897B",
            isFavorite = false,
            order = 57
        ),
        WebsiteEntity(
            id = 58,
            name = "Microsoft Teams",
            url = "https://teams.microsoft.com",
            category = "VIDEO_CALL",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/microsoft-teams-1.svg",
            description = "Team collaboration and meetings",
            backgroundColor = "#6264A7",
            isFavorite = false,
            order = 58
        ),
        WebsiteEntity(
            id = 59,
            name = "Discord",
            url = "https://discord.com",
            category = "VIDEO_CALL",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/discord-icon.svg",
            description = "Voice, video, and text chat",
            backgroundColor = "#5865F2",
            isFavorite = false,
            order = 59
        ),
        
        // Arabic (IDs 60-64)
        WebsiteEntity(
            id = 60,
            name = "شاهد",
            url = "https://shahid.mbc.net",
            category = "ARABIC",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/shahid.svg",
            description = "مسلسلات وأفلام عربية",
            backgroundColor = "#00B140",
            isFavorite = false,
            order = 60
        ),
        WebsiteEntity(
            id = 61,
            name = "وياك",
            url = "https://weyyak.com",
            category = "ARABIC",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/weyyak.svg",
            description = "أفلام ومسلسلات مجانية",
            backgroundColor = "#FF0000",
            isFavorite = false,
            order = 61
        ),
        WebsiteEntity(
            id = 62,
            name = "إيجي بست",
            url = "https://egybest.org",
            category = "ARABIC",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/egybest.svg",
            description = "أفلام ومسلسلات",
            backgroundColor = "#1A237E",
            isFavorite = false,
            order = 62
        ),
        WebsiteEntity(
            id = 63,
            name = "سيما كلوب",
            url = "https://cimaclub.cam",
            category = "ARABIC",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/cimaclub.svg",
            description = "سينما عربية وأجنبية",
            backgroundColor = "#E91E63",
            isFavorite = false,
            order = 63
        ),
        WebsiteEntity(
            id = 64,
            name = "نيك",
            url = "https://faselhd.express",
            category = "ARABIC",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/faselhd.svg",
            description = "مشاهدة الأفلام والمسلسلات",
            backgroundColor = "#673AB7",
            isFavorite = false,
            order = 64
        )
    )
}
