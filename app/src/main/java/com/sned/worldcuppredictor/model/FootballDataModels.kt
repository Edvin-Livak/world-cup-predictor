package com.sned.worldcuppredictor.model

class FootballDataModels {
    data class FootballDataResponse(
        val matches: List<MatchDto>
    )

    data class MatchDto(
        val id: Int,
        val utcDate: String,
        val status: String,
        val homeTeam: TeamDto,
        val awayTeam: TeamDto
    )

    data class TeamDto(
        val name: String?
    )
}