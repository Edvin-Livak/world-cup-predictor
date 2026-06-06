package com.sned.worldcuppredictor.data

import com.sned.worldcuppredictor.model.Match
import com.sned.worldcuppredictor.model.MatchStatus


//package com.sned.worldcuppredictor.data

val mockMatches = listOf(
    Match(
        id = 1,
        group = "Group A",
        homeTeam = "Mexico",
        awayTeam = "South Africa",
        homeFlag = "🇲🇽",
        awayFlag = "🇿🇦",
        kickoffTime = "2026-06-11 21:00",
        status = MatchStatus.SCHEDULED
    ),
    Match(
        id = 2,
        group = "Group A",
        homeTeam = "Germany",
        awayTeam = "Czechia",
        homeFlag = "🇩🇪",
        awayFlag = "🇨🇿",
        kickoffTime = "2026-06-11 18:00",
        status = MatchStatus.LIVE
    ),
    Match(
        id = 3,
        group = "Group B",
        homeTeam = "Brazil",
        awayTeam = "Morocco",
        homeFlag = "🇧🇷",
        awayFlag = "🇲🇦",
        kickoffTime = "2026-06-10 19:00",
        status = MatchStatus.FINISHED,
        actualHomeGoals = 3,
        actualAwayGoals = 1
    ),
    Match(
        id = 1,
        group = "Premier League",
        homeTeam = "Arsenal",
        awayTeam = "Wolves",
        homeFlag = "🏴",
        awayFlag = "🏴",
        kickoffTime = "2023",
        status = MatchStatus.SCHEDULED
    )

)

