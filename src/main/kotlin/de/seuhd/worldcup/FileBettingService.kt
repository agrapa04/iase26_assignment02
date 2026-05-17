package de.seuhd.worldcup

import java.io.File

/**
 * File-backed bet store. Each bet is persisted as one `matchId,predictionCode`
 * line; placing a bet for a match id that already exists overwrites the previous
 * line.
 */
class FileBettingService(private val file: File) {

    private val lock = Any() // For synchronizing access to the file

    fun placeBet(bet: Bet) = synchronized(lock) {
        val bets = readBets()
        bets[bet.matchId] = bet
        writeBets(bets.values)
    }//synchronized

    fun getBets(): List<Bet> = synchronized(lock) {
        readBets().values.toList()
    }//synchronized

    private fun readBets(): MutableMap<Int, Bet> {
        if (!file.exists()) return mutableMapOf()
        return file.useLines { lines ->
            lines.filter { it.isNotBlank() }
                .map(::parseBet)
                .associateByTo(LinkedHashMap()) { it.matchId }
        }
    }

    private fun writeBets(bets: Collection<Bet>) {
        file.writeText(bets.joinToString(separator = "\n", postfix = "\n") { formatBet(it) })
    }

    private fun formatBet(bet: Bet): String = "${bet.matchId},${bet.prediction.code}"

    private fun parseBet(line: String): Bet {
        val (id, code) = line.split(",", limit = 2)
        return Bet(matchId = id.toInt(), prediction = Prediction.fromCode(code.toInt()))
    }
}
