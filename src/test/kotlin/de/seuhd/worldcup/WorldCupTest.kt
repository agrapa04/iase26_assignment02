package de.seuhd.worldcup

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the pure logic in [StandingsService] and [BettingService]. Derived
 * directly from the assignment rules:
 *
 *   - Standings: Win = 3, Draw = 1, Loss = 0; ties broken by goal difference.
 *   - Matches with null scores are unplayed and must not contribute to standings.
 *   - Betting score: +1 per correct outcome (Win/Loss/Draw); only matches that have
 *     both a stored bet and a final score count toward "evaluated".
 */
class WorldCupTest {

    private fun match(id: Int, home: String, away: String, hs: Int?, aws: Int?) =
        Match(
            matchId = id,
            round = "Matchday 1",
            date = "2026-06-01",
            homeTeam = home,
            awayTeam = away,
            homeScore = hs,
            awayScore = aws,
            ground = "Test Stadium"
        )

    private val teams = listOf(
        Team("AAA", "Alpha"),
        Team("BBB", "Beta"),
        Team("CCC", "Gamma")
    )

    @BeforeEach
    fun resetBets() {
        BettingService.clear()
    }

    @Test
    fun `points follow 3-1-0 for win-draw-loss`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "BBB", "CCC", 1, 1)
        )
        val table = StandingsService.calculate(Group("G", teams, matches)).associateBy { it.team.id }

        assertEquals(3, table.getValue("AAA").points)
        assertEquals(1, table.getValue("BBB").points)
        assertEquals(1, table.getValue("CCC").points)
    }

    @Test
    fun `goal difference breaks ties on equal points`() {
        // Both Alpha and Gamma end on 4 points; Gamma has the better goal difference.
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "AAA", "CCC", 1, 1),
            match(3, "BBB", "CCC", 0, 3)
        )
        val table = StandingsService.calculate(Group("G", teams, matches))

        assertEquals(listOf("Gamma", "Alpha", "Beta"), table.map { it.team.name })
        assertEquals(4, table[0].points)
        assertEquals(4, table[1].points)
        assertEquals(3, table[0].goalDiff)
        assertEquals(2, table[1].goalDiff)
    }

    @Test
    fun `unplayed matches do not affect standings`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "BBB", "CCC", null, null)
        )
        val table = StandingsService.calculate(Group("G", teams, matches)).associateBy { it.team.id }

        assertEquals(3, table.getValue("AAA").points)
        assertEquals(0, table.getValue("BBB").points)
        assertEquals(0, table.getValue("CCC").points)
        assertEquals(0, table.getValue("CCC").goalsFor)
    }

    @Test
    fun `betting score awards one point per correct outcome`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "AAA", "CCC", 1, 1),
            match(3, "BBB", "CCC", 0, 3)
        )

        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))
        BettingService.placeBet(Bet(2, Prediction.DRAW))
        BettingService.placeBet(Bet(3, Prediction.HOME_WIN))

        val result = BettingService.evaluate(matches)

        assertEquals(2, result.correct)
        assertEquals(3, result.evaluated)
        assertEquals(1, result.incorrect)
    }

    @Test
    fun `betting score ignores matches without a stored bet`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "AAA", "CCC", 1, 1)
        )
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))

        val result = BettingService.evaluate(matches)

        assertEquals(1, result.correct)
        assertEquals(1, result.evaluated)
    }

    @Test
    fun `betting score ignores unplayed matches even when a bet exists`() {
        val matches = listOf(match(1, "AAA", "BBB", null, null))
        BettingService.placeBet(Bet(1, Prediction.HOME_WIN))

        val result = BettingService.evaluate(matches)

        assertEquals(0, result.correct)
        assertEquals(0, result.evaluated)
    }

    @Test
    fun `evaluate returns zero when no bets are placed`() {
        val matches = listOf(
            match(1, "AAA", "BBB", 2, 0),
            match(2, "BBB", "CCC", 1, 1)
        )
        val result = BettingService.evaluate(matches)

        assertEquals(0, result.evaluated)
        assertEquals(0, result.correct)
    }//flaky

    @Test
    fun `standings are stable when multiple teams tie on all criteria`() {
        // AAA and BBB end on identical points, GD and GF.
        val matches = listOf(
            match(1, "AAA", "BBB", 1, 1),
            match(2, "AAA", "CCC", 1, 0),
            match(3, "BBB", "CCC", 1, 0),
        )
        val table = StandingsService.calculate(Group("G", teams, matches))

        assertEquals(listOf("Alpha", "Beta", "Gamma"), table.map { it.team.name })
    }

    @Test
    fun `prediction codes round-trip through the assignment-defined mapping`() {
        assertEquals(Prediction.HOME_WIN, Prediction.fromCode(1))
        assertEquals(Prediction.AWAY_WIN, Prediction.fromCode(2))
        assertEquals(Prediction.DRAW, Prediction.fromCode(0))
    }

    @Test
    fun `outcome derivation matches the assignment scoring rules`() {
        assertEquals(Prediction.HOME_WIN, Prediction.outcomeOf(2, 1))
        assertEquals(Prediction.AWAY_WIN, Prediction.outcomeOf(0, 3))
        assertEquals(Prediction.DRAW, Prediction.outcomeOf(1, 1))
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    fun `load json from network`(){

        val jsonNetwork = JsonLoader.loadJsonFromNetwork()
        val jsonLocal = JsonLoader.loadJson()

        assertEquals(jsonLocal, jsonNetwork)
    }
}
