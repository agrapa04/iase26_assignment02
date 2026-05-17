package de.seuhd.worldcup

import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder

/** Tests for [FileBettingService]. */
@TestMethodOrder(MethodOrderer.Random::class)
class FileBettingServiceTest {

    @BeforeEach
    fun clearFile() {
        if (SHARED_BET_FILE.exists()) {
            SHARED_BET_FILE.writeText("") // Clear the file before each test to ensure isolation
        }
    }

    @Test
    fun `test file betting with threads`() {
        val file = createTempFile("bets", ".txt").toFile()
        val service = FileBettingService(file)

        val thread1 = Thread {
            repeat(50) { i -> service.placeBet(Bet(i, Prediction.HOME_WIN)) }
        }
        val thread2 = Thread {
            repeat(50) { i -> service.placeBet(Bet(i + 50, Prediction.AWAY_WIN)) }
        }

        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        // Each thread placed 50 unique bets → 100 total expected.
        assertEquals(100, service.getBets().size)

        file.delete()
    }

    companion object {
        // PID makes the filename unique per JVM launch.
        val SHARED_BET_FILE = File(
            System.getProperty("java.io.tmpdir"),
            "worldcup-shared-bets-${ProcessHandle.current().pid()}.txt"
        )
    }

    @Test
    fun `save bets to the shared file`() {
        val service = FileBettingService(SHARED_BET_FILE)
        service.placeBet(Bet(1, Prediction.HOME_WIN))
        service.placeBet(Bet(2, Prediction.DRAW))
        service.placeBet(Bet(3, Prediction.AWAY_WIN))
        val bets = service.getBets()
        assertEquals(3, bets.size)
    }

    @Test
    fun `fresh service has no bets`() {
        val service = FileBettingService(SHARED_BET_FILE)
        assertEquals(0, service.getBets().size)
    }
}
