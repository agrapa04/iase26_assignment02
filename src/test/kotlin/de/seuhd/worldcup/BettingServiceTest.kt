package de.seuhd.worldcup

import kotlin.test.BeforeTest
import kotlin.test.Test

class BettingServiceTest {

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

    @BeforeTest
    fun resetBets() {
        BettingService.clear()
    }

    // ── evaluateBonus ──────────────────────────────────────────────────────────

    @Test
    fun `evaluateBonus awards 3 points for an exact score prediction`() {
        // Given a match with a final score of 2-1
        val match = match(id = 1, home = "Team A", away = "Team B", hs = 2, aws = 1)
        // And a bet predicting exactly 2-1
        BettingService.placeBet(Bet(matchId = 1, prediction = Prediction.HOME_WIN, predictedHomeScore = 2, predictedAwayScore = 1))
        // When evaluating the bonus
        val bonus = BettingService.evaluateBonus(listOf(match))
        // Then the bonus should be 3 points
        assert(bonus == 3) { "Expected bonus of 3 points for exact score prediction, but got $bonus" }
    }

    @Test
    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
        // Given a match with a final score of 1-1
        val match = match(id = 2, home = "Team C", away = "Team D", hs = 1, aws = 1)
        // And a bet predicting a draw without exact scores
        BettingService.placeBet(Bet(matchId = 2, prediction = Prediction.DRAW, predictedHomeScore = null, predictedAwayScore = null))
        // When evaluating the bonus
        val bonus = BettingService.evaluateBonus(listOf(match))
        // Then the bonus should be 1 point
        assert(bonus == 1) { "Expected bonus of 1 point for correct outcome without exact score, but got $bonus" }
    }

    @Test
    fun `evaluateBonus awards 0 points for a wrong prediction`() {
        // Given a match with a final score of 0-2
        val match = match(id = 3, home = "Team E", away = "Team F", hs = 0, aws = 2)
        // And a bet predicting a home win
        BettingService.placeBet(Bet(matchId = 3, prediction = Prediction.HOME_WIN, predictedHomeScore = null, predictedAwayScore = null))
        // When evaluating the bonus
        val bonus = BettingService.evaluateBonus(listOf(match))
        // Then the bonus should be 0 points
        assert(bonus == 0) { "Expected bonus of 0 points for wrong prediction, but got $bonus" }
    }

    @Test
    fun `evaluateBonus ignores unplayed matches`() {
        // Given a match that has not been played yet (no scores)
        val match = match(id = 4, home = "Team G", away = "Team H", hs = null, aws = null)
        // And a bet predicting an outcome
        BettingService.placeBet(Bet(matchId = 4, prediction = Prediction.AWAY_WIN, predictedHomeScore = null, predictedAwayScore = null))
        // When evaluating the bonus
        val bonus = BettingService.evaluateBonus(listOf(match))
        // Then the bonus should be 0 points since the match has not been played
        assert(bonus == 0) { "Expected bonus of 0 points for unplayed match, but got $bonus" }
    }

    // ── removeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `removeBet removes an existing bet so it no longer affects evaluation`() {
        // Given a match with a final score of 3-0
        val match = match(id = 5, home = "Team I", away = "Team J", hs = 3, aws = 0)
        // And a bet predicting a home win
        BettingService.placeBet(Bet(matchId = 5, prediction = Prediction.HOME_WIN, predictedHomeScore = null, predictedAwayScore = null))
        // When removing the bet
        BettingService.removeBet(5)
        // And evaluating the result
        val result = BettingService.evaluate(listOf(match))
        // Then the bet should no longer be counted as evaluated
        assert(result.evaluated == 0) { "Expected 0 evaluated bets after removal, but got ${result.evaluated}" }
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        // Given a match with a final score of 1-2
        val match = match(id = 6, home = "Team K", away = "Team L", hs = 1, aws = 2)
        // And no bet placed for that match
        // When removing a bet for that matchId
        BettingService.removeBet(6)
        // And evaluating the result
        val result = BettingService.evaluate(listOf(match))
        // Then the evaluation should still work without errors and show 0 evaluated bets
        assert(result.evaluated == 0) { "Expected 0 evaluated bets when no bet exists, but got ${result.evaluated}" }
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        // Given a match with a final score of 2-2
        val match = match(id = 7, home = "Team M", away = "Team N", hs = 2, aws = 2)
        // And an existing bet predicting a home win
        BettingService.placeBet(Bet(matchId = 7, prediction = Prediction.HOME_WIN, predictedHomeScore = null, predictedAwayScore = null))
        // When changing the bet to predict a draw
        BettingService.changeBet(Bet(matchId = 7, prediction = Prediction.DRAW, predictedHomeScore = null, predictedAwayScore = null))
        // And evaluating the result
        val result = BettingService.evaluate(listOf(match))
        // Then the updated bet should be counted as correct
        assert(result.correct == 1) { "Expected 1 correct bet after change, but got ${result.correct}" }
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        // Given a match with a final score of 0-0
        val match = match(id = 8, home = "Team O", away = "Team P", hs = 0, aws = 0)
        // And no existing bet for that match
        // When changing a bet for that matchId
        var exceptionThrown = false
        try {
            BettingService.changeBet(Bet(matchId = 8, prediction = Prediction.DRAW, predictedHomeScore = null, predictedAwayScore = null))
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        // Then an IllegalArgumentException should be thrown
        assert(exceptionThrown) { "Expected IllegalArgumentException when changing non-existent bet, but no exception was thrown" }
    }
}