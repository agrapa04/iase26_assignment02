package de.seuhd.worldcup

import java.util.IdentityHashMap

/** A team's row in a group's standings table. */
data class TableEntry(
    val team: Team,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int
) {
    val goalDiff: Int get() = goalsFor - goalsAgainst
}

object StandingsService {

    /**
     * Build the standings table for a single [group]. Only matches with a final
     * score contribute; null scores are unplayed matches.
     *
     * Sort order: points, then goal difference, then goals scored, all descending.
     */
    fun calculate(group: Group): List<TableEntry> {
        data class Acc(var points: Int = 0, var goalsFor: Int = 0, var goalsAgainst: Int = 0)

        val accs = IdentityHashMap<Team, Acc>()
        for (team in group.teams) accs[team] = Acc()
        val teamById: Map<String, Team> = group.teams.associateBy { it.id }

        for (match in group.matches) {
            val home = match.homeScore ?: continue
            val away = match.awayScore ?: continue
            val homeTeam = teamById[match.homeTeam]
                ?: error("Match ${match.matchId} references unknown home team '${match.homeTeam}' in group '${group.name}'")
            val awayTeam = teamById[match.awayTeam]
                ?: error("Match ${match.matchId} references unknown away team '${match.awayTeam}' in group '${group.name}'")
            val homeAcc = accs.getValue(homeTeam)
            val awayAcc = accs.getValue(awayTeam)

            homeAcc.goalsFor += home
            homeAcc.goalsAgainst += away
            awayAcc.goalsFor += away
            awayAcc.goalsAgainst += home
            when {
                home > away -> homeAcc.points += 3
                home < away -> awayAcc.points += 3
                else -> {
                    homeAcc.points += 1
                    awayAcc.points += 1
                }
            }
        }

        return accs.entries
            .map { (team, a) -> TableEntry(team, a.points, a.goalsFor, a.goalsAgainst) }
            .sortedWith(
                compareByDescending<TableEntry> { it.points }
                    .thenByDescending { it.goalDiff }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.team.name }// Tie-breaker for teams with identical points, goal difference, and goals scored: alphabetical order of team name.
            )
    }
}
