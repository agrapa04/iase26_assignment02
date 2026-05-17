# Flaky Test Report

**Name:** Grapa, Adrienne

## Flaky Test 1 FileBettingServiceTest

**Test name:** `test file betting with threads`

**Root cause:**
The test tends to fail almost everytime because of a race conditions, when calling placeBet() or getBets() (not tested here but works the same), it would overwrite data that is being written simultaneously by one thread or the other leading to data loss. The outcome depends on thread scheduling, making it nondeterministic.

**Fix:**
in class FileBettingService(private val file: File)

     private val lock = Any() // For synchronizing access to the file

    fun placeBet(bet: Bet) = synchronized(lock) {
        val bets = readBets()
        bets[bet.matchId] = bet
        writeBets(bets.values)
    }//synchronized

    fun getBets(): List<Bet> = synchronized(lock) {
        readBets().values.toList()
    }//synchronized

A shared lock is declared to ensure an atomic read-modify-write operation. The two functions that could overwrite data are synchronized to avoid race conditions and therefore loss of data that where occuring before. This ensures the access/modification of the file one thread at the time.


## Flaky Test 2 FileBettingServiceTest

**Test name:** `fresh service has no bets`

**Root cause:**
The tests being executed in a random order, this specific test might get executed after the `save bets to the shared file` test, this would cause the file to contain bets when it shouldn't, because of the shared file `SHARED_BET_FILE` that is used in every test case. Resulting in a failing test. It's a problem of test isolation.

**Fix:**
in class FileBettingServiceTest

     @BeforeEach
     fun clearFile() {
        if (SHARED_BET_FILE.exists()) {
            SHARED_BET_FILE.writeText("") // Clear the file before each test to ensure isolation
        }
    }

With the import :

     import org.junit.jupiter.api.BeforeEach

Before each test (with `@BeforeEach`), it should be ensured that the file is clear to guaranty test isolation.

## Flaky Test 3 WorldCupTest

**Test name:** `evaluate returns zero when no bets are placed`

**Root cause:**


**Fix:**

## Flaky Test 4 WorldCupTest

**Test name:** `standings are stable when multiple teams tie on all criteria`

**Root cause:** this test calls calculate() which originally sorts the tied teams in a certain order based on points, goalDiff and goalsFor, which are non deterministic if all team tie on all criteria. This test is missing a deterministic tiebreaker criteria to ensure a stable ordering and a valide test.


**Fix:**
in object StandingsService
in fun calculate()

before :

     return accs.entries
            .map { (team, a) -> TableEntry(team, a.points, a.goalsFor, a.goalsAgainst) }
            .sortedWith(
                compareByDescending<TableEntry> { it.points }
                    .thenByDescending { it.goalDiff }
                    .thenByDescending { it.goalsFor }
            )
after :

     return accs.entries
            .map { (team, a) -> TableEntry(team, a.points, a.goalsFor, a.goalsAgainst) }
            .sortedWith(
                compareByDescending<TableEntry> { it.points }
                    .thenByDescending { it.goalDiff }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.team.name }// Tie-breaker for teams with identical points, goal      difference, and goals scored: alphabetical order of team name.
            )

we add `.thenBy{it.team.name}` to order the teams by alphabetical order if multiple teams tie on all criteria. This guarantees a deterministic ordering.

## Flaky Test 5 WorldCupTest

**Test name:** `load json from network`

**Root cause:**


**Fix:**
