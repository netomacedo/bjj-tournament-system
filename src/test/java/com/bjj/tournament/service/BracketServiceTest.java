package com.bjj.tournament.service;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import com.bjj.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BracketService, focused on round-robin match ordering
 */
@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private DivisionRepository divisionRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private BracketService bracketService;

    private Division division;
    private List<Athlete> athletes;

    @BeforeEach
    void setUp() {
        division = new Division();
        division.setId(1L);
        division.setBracketType(BracketType.ROUND_ROBIN);
        division.setAgeCategory(AgeCategory.ADULT);
        division.setMatchesGenerated(false);

        athletes = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            Athlete athlete = new Athlete();
            athlete.setId(i);
            athlete.setName("Athlete " + i);
            athletes.add(athlete);
        }
        division.setAthletes(athletes);
    }

    @Test
    void generateMatchesAutomatically_RoundRobin_NoAthleteFightsTwiceInARowMoreThanUnavoidable() {
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        // Every unique pair must play exactly once
        assertThat(matches).hasSize(6);

        // Count back-to-back repeats: an athlete appearing in both match i and match i+1
        int backToBackRepeats = 0;
        for (int i = 0; i < matches.size() - 1; i++) {
            Match current = matches.get(i);
            Match next = matches.get(i + 1);
            boolean sharesAthlete = next.getAthlete1().getId().equals(current.getAthlete1().getId())
                || next.getAthlete1().getId().equals(current.getAthlete2().getId())
                || next.getAthlete2().getId().equals(current.getAthlete1().getId())
                || next.getAthlete2().getId().equals(current.getAthlete2().getId());
            if (sharesAthlete) {
                backToBackRepeats++;
            }
        }

        // With 4 athletes and no bye, 2 back-to-back repeats (one per round transition)
        // is the mathematical minimum - the naive pairing order produces 3 in a row
        // for the first athlete alone, so this asserts the greedy scheduler achieves optimum.
        assertThat(backToBackRepeats).isEqualTo(2);
    }

    @Test
    void generateMatchesAutomatically_RoundRobin_EveryPairPlaysExactlyOnce() {
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        List<String> pairs = matches.stream()
            .map(m -> {
                long a = m.getAthlete1().getId();
                long b = m.getAthlete2().getId();
                return Math.min(a, b) + "-" + Math.max(a, b);
            })
            .toList();

        assertThat(pairs).containsExactlyInAnyOrder("1-2", "1-3", "1-4", "2-3", "2-4", "3-4");
        assertThat(pairs).doesNotHaveDuplicates();
    }

    @Test
    void generateMatchesAutomatically_WithThreeAthletesAndSingleEliminationSelected_SwitchesToRoundRobinPerIbjjfRules() {
        // IBJJF rule: exactly 3 athletes is run round-robin (everyone fights
        // twice), not single elimination with a bye straight to the final.
        // Even though the division was created with SINGLE_ELIMINATION
        // selected, generation should override it to ROUND_ROBIN so the
        // persisted bracket type matches how the matches are actually shaped.
        division.setBracketType(BracketType.SINGLE_ELIMINATION);
        List<Athlete> three = athletes.subList(0, 3);
        division.setAthletes(three);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        assertThat(division.getBracketType()).as("bracket type overridden to round robin")
            .isEqualTo(BracketType.ROUND_ROBIN);

        // Round robin for 3 athletes = 3 matches (every unique pair once), no byes
        assertThat(matches).hasSize(3);
        assertThat(matches).as("nobody gets a bye - every match has two real athletes")
            .allMatch(m -> m.getAthlete1() != null && m.getAthlete2() != null
                && m.getStatus() != MatchStatus.WALKOVER);

        List<String> pairs = matches.stream()
            .map(m -> {
                long a = m.getAthlete1().getId();
                long b = m.getAthlete2().getId();
                return Math.min(a, b) + "-" + Math.max(a, b);
            })
            .toList();
        assertThat(pairs).containsExactlyInAnyOrder("1-2", "1-3", "2-3");
    }

    @Test
    void generateMatchesAutomatically_WithThreeAthletesAndRoundRobinAlreadySelected_LeavesBracketTypeUnchanged() {
        // No-op guard: when round robin was already explicitly selected for a
        // 3-athlete division, generation shouldn't log/override anything - it
        // should just behave like any other round-robin division.
        List<Athlete> three = athletes.subList(0, 3);
        division.setAthletes(three);
        division.setBracketType(BracketType.ROUND_ROBIN);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        assertThat(division.getBracketType()).isEqualTo(BracketType.ROUND_ROBIN);
        assertThat(matches).hasSize(3);
    }

    @Test
    void generateMatchesAutomatically_WithOneAthlete_AwardsGoldAutomaticallyWithNoOpponent() {
        // IBJJF rule: a division with a single enrolled athlete gets gold
        // automatically - there's nobody left to fight.
        Athlete soleAthlete = athletes.get(0);
        division.setBracketType(BracketType.SINGLE_ELIMINATION);
        division.setAthletes(List.of(soleAthlete));

        List<Match> persisted = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(1);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Match> toSave = invocation.getArgument(0);
            for (Match m : toSave) {
                if (m.getId() == null) {
                    m.setId(idSequence.getAndIncrement());
                }
            }
            persisted.addAll(toSave);
            return toSave;
        });
        when(matchRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return persisted.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        });
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 2)).thenReturn(List.of());
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        assertThat(matches).hasSize(1);
        Match soloMatch = matches.get(0);
        assertThat(soloMatch.getAthlete1().getId()).isEqualTo(soleAthlete.getId());
        assertThat(soloMatch.getAthlete2()).isNull();
        assertThat(soloMatch.getStatus()).isEqualTo(MatchStatus.WALKOVER);
        assertThat(soloMatch.getWinner().getId()).isEqualTo(soleAthlete.getId());
        assertThat(division.getCompleted()).as("division completes immediately - no fight to wait for").isTrue();
        assertThat(division.getMatchesGenerated()).isTrue();
    }

    @Test
    void advanceWinnerToNextRound_WithWalkoverMatch_DoesNotThrow() {
        // A walkover-decided match must advance its winner just like a normally
        // completed one - this was previously rejected since the status check
        // only accepted MatchStatus.COMPLETED, silently breaking bracket
        // progression for every walkover (the exception was swallowed by
        // MatchService.recordWalkover's catch block). This exercises the
        // elimination-bracket "no next round exists" path specifically.
        division.setBracketType(BracketType.SINGLE_ELIMINATION);

        Athlete winner = athletes.get(0);
        Athlete loser = athletes.get(1);

        Match walkoverMatch = new Match();
        walkoverMatch.setId(10L);
        walkoverMatch.setDivision(division);
        walkoverMatch.setAthlete1(winner);
        walkoverMatch.setAthlete2(loser);
        walkoverMatch.setWinner(winner);
        walkoverMatch.setStatus(MatchStatus.WALKOVER);
        walkoverMatch.setRoundNumber(1);
        walkoverMatch.setMatchPosition(1);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(walkoverMatch));
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 2)).thenReturn(List.of());
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        bracketService.advanceWinnerToNextRound(10L, winner.getId());

        assertThat(division.getCompleted()).isTrue();
    }

    @Test
    void advanceWinnerToNextRound_RoundRobin_DoesNotMarkDivisionCompleteWhileMatchesRemain() {
        // Round robin has no "next round" - all matches are scheduled upfront in
        // round 1 ("all fight all"). Previously, finishing even the FIRST match
        // of a round robin incorrectly marked the whole division "completed"
        // (the elimination-style "no round 2 exists, so this was the final
        // match" fallback fired for every round-robin completion).
        Athlete a = athletes.get(0);
        Athlete b = athletes.get(1);
        Athlete c = athletes.get(2);
        Athlete d = athletes.get(3);

        Match decidedMatch = new Match();
        decidedMatch.setId(20L);
        decidedMatch.setDivision(division);
        decidedMatch.setAthlete1(a);
        decidedMatch.setAthlete2(b);
        decidedMatch.setWinner(a);
        decidedMatch.setStatus(MatchStatus.COMPLETED);
        decidedMatch.setRoundNumber(1);
        decidedMatch.setMatchPosition(1);

        Match pendingMatch = new Match();
        pendingMatch.setId(21L);
        pendingMatch.setDivision(division);
        pendingMatch.setAthlete1(c);
        pendingMatch.setAthlete2(d);
        pendingMatch.setStatus(MatchStatus.PENDING);
        pendingMatch.setRoundNumber(1);
        pendingMatch.setMatchPosition(2);

        when(matchRepository.findById(20L)).thenReturn(Optional.of(decidedMatch));
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 1))
            .thenReturn(List.of(decidedMatch, pendingMatch));

        bracketService.advanceWinnerToNextRound(20L, a.getId());

        assertThat(division.getCompleted()).isFalse();
    }

    @Test
    void advanceWinnerToNextRound_RoundRobin_MarksDivisionCompleteOnceEveryMatchIsDecided() {
        Athlete a = athletes.get(0);
        Athlete b = athletes.get(1);

        Match lastMatch = new Match();
        lastMatch.setId(22L);
        lastMatch.setDivision(division);
        lastMatch.setAthlete1(a);
        lastMatch.setAthlete2(b);
        lastMatch.setWinner(a);
        lastMatch.setStatus(MatchStatus.COMPLETED);
        lastMatch.setRoundNumber(1);
        lastMatch.setMatchPosition(1);

        Match alreadyDecidedMatch = new Match();
        alreadyDecidedMatch.setId(23L);
        alreadyDecidedMatch.setDivision(division);
        alreadyDecidedMatch.setAthlete1(athletes.get(2));
        alreadyDecidedMatch.setAthlete2(athletes.get(3));
        alreadyDecidedMatch.setWinner(athletes.get(2));
        alreadyDecidedMatch.setStatus(MatchStatus.COMPLETED);
        alreadyDecidedMatch.setRoundNumber(1);
        alreadyDecidedMatch.setMatchPosition(2);

        when(matchRepository.findById(22L)).thenReturn(Optional.of(lastMatch));
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 1))
            .thenReturn(List.of(lastMatch, alreadyDecidedMatch));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        bracketService.advanceWinnerToNextRound(22L, a.getId());

        assertThat(division.getCompleted()).isTrue();
    }

    @Test
    void advanceWinnerToNextRound_DoesNotCompleteTournament_WhileOtherDivisionsPending() {
        Tournament tournament = new Tournament();
        tournament.setId(100L);
        tournament.setCompleted(false);
        division.setTournament(tournament);

        Division otherDivision = new Division();
        otherDivision.setId(2L);
        otherDivision.setCompleted(false);

        Athlete a = athletes.get(0);
        Athlete b = athletes.get(1);

        Match lastMatch = new Match();
        lastMatch.setId(30L);
        lastMatch.setDivision(division);
        lastMatch.setAthlete1(a);
        lastMatch.setAthlete2(b);
        lastMatch.setWinner(a);
        lastMatch.setStatus(MatchStatus.COMPLETED);
        lastMatch.setRoundNumber(1);
        lastMatch.setMatchPosition(1);

        when(matchRepository.findById(30L)).thenReturn(Optional.of(lastMatch));
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 1)).thenReturn(List.of(lastMatch));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);
        when(divisionRepository.findByTournamentIdOrderByIdAsc(100L)).thenReturn(List.of(division, otherDivision));

        bracketService.advanceWinnerToNextRound(30L, a.getId());

        assertThat(division.getCompleted()).isTrue();
        assertThat(tournament.getCompleted()).isFalse();
    }

    @Test
    void advanceWinnerToNextRound_CompletesTournament_WhenEveryDivisionIsDone() {
        Tournament tournament = new Tournament();
        tournament.setId(101L);
        tournament.setCompleted(false);
        division.setTournament(tournament);

        Division otherDivision = new Division();
        otherDivision.setId(3L);
        otherDivision.setCompleted(true); // already finished

        Athlete a = athletes.get(0);
        Athlete b = athletes.get(1);

        Match lastMatch = new Match();
        lastMatch.setId(31L);
        lastMatch.setDivision(division);
        lastMatch.setAthlete1(a);
        lastMatch.setAthlete2(b);
        lastMatch.setWinner(a);
        lastMatch.setStatus(MatchStatus.COMPLETED);
        lastMatch.setRoundNumber(1);
        lastMatch.setMatchPosition(1);

        when(matchRepository.findById(31L)).thenReturn(Optional.of(lastMatch));
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 1)).thenReturn(List.of(lastMatch));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);
        when(divisionRepository.findByTournamentIdOrderByIdAsc(101L)).thenReturn(List.of(division, otherDivision));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

        bracketService.advanceWinnerToNextRound(31L, a.getId());

        assertThat(division.getCompleted()).isTrue();
        assertThat(tournament.getCompleted()).isTrue();
    }

    // ---- Single elimination: full bracket simulation for various athlete counts ----
    //
    // These don't just check round 1 - they actually play the entire bracket
    // out round by round (picking athlete1 as the winner of every match) and
    // confirm every match eventually gets decided, with exactly one champion
    // at the end. This is what actually proves byes propagate correctly
    // through every round, not just round 1 - the bug this was built to catch
    // only showed up a round or two in for 5 and 10 athletes, not round 1.

    // Note: 3 athletes is deliberately not covered here - generateMatchesAutomatically
    // now intercepts exactly 3 athletes and always uses round-robin (IBJJF rule),
    // so it never reaches generateSingleEliminationBracket's bye-to-final path.
    // See generateMatchesAutomatically_WithThreeAthletesAndSingleEliminationSelected_SwitchesToRoundRobinPerIbjjfRules.

    @Test
    void singleElimination_WithFiveAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(5);
    }

    @Test
    void singleElimination_WithSixAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(6);
    }

    @Test
    void singleElimination_WithSevenAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(7);
    }

    @Test
    void singleElimination_WithNineAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(9);
    }

    @Test
    void singleElimination_WithTenAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(10);
    }

    @Test
    void singleElimination_WithElevenAthletes_ResolvesToOneChampionWithNoStuckMatches() {
        assertBracketFullyResolves(11);
    }

    @Test
    void singleElimination_WithFiveAthletes_Round1HasTwoRealMatchesAndOneBye() {
        // The exact shape the user reported as broken: round 1 should pair up
        // as many real matches as possible (2, for 4 of the 5 athletes) and
        // give only the single leftover athlete a bye - not front-load all 3
        // mathematically-possible byes into round 1 as the old power-of-two
        // algorithm did.
        division.setBracketType(BracketType.SINGLE_ELIMINATION);
        List<Athlete> five = new ArrayList<>(athletes);
        Athlete fifthAthlete = new Athlete();
        fifthAthlete.setId(5L);
        fifthAthlete.setName("Athlete 5");
        five.add(fifthAthlete);
        division.setAthletes(five);

        // The round-1 bye is auto-advanced immediately (post-save loop in
        // generateMatchesAutomatically), which calls advanceWinnerToNextRound
        // and needs findById/matchPosition lookups to resolve - same pattern
        // as the other tests here that generate a real bye.
        List<Match> persisted = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(1);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Match> toSave = invocation.getArgument(0);
            for (Match m : toSave) {
                if (m.getId() == null) {
                    m.setId(idSequence.getAndIncrement());
                }
            }
            persisted.addAll(toSave);
            return toSave;
        });
        when(matchRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return persisted.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        });

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        List<Match> round1 = matches.stream().filter(m -> m.getRoundNumber() == 1).toList();
        long realMatches = round1.stream()
            .filter(m -> m.getAthlete1() != null && m.getAthlete2() != null)
            .count();
        long byeMatches = round1.stream()
            .filter(m -> m.getStatus() == MatchStatus.WALKOVER)
            .count();

        assertThat(realMatches).as("round 1 should have 2 real matches, not 1").isEqualTo(2);
        assertThat(byeMatches).as("round 1 should have exactly 1 bye, not 3").isEqualTo(1);
    }

    /**
     * Generates a single-elimination bracket for the given athlete count, then
     * simulates playing every match (athlete1 always wins, for determinism)
     * until no more matches can be completed. Asserts:
     * - total generated match rows = (athleteCount - 1) + (1 if athleteCount is
     *   odd, else 0) - every real match eliminates exactly one person
     *   (athleteCount - 1 of those are needed total), plus one extra row only
     *   when round 1 itself has a bye (only round 1 ever materializes an
     *   explicit bye row under the cascading scheme - see generateSingleEliminationBracket)
     * - every athlete appears in round 1 exactly once (nobody dropped)
     * - every single match ends up decided (WALKOVER or COMPLETED) - none left
     *   stuck in PENDING with a permanently-missing opponent, even when a
     *   winner must skip an entire round via advancesToRound/Position/Slot
     * - exactly one champion emerges in the final round
     */
    private void assertBracketFullyResolves(int athleteCount) {
        Division testDivision = new Division();
        testDivision.setId(1L);
        testDivision.setBracketType(BracketType.SINGLE_ELIMINATION);
        testDivision.setAgeCategory(AgeCategory.ADULT);
        testDivision.setMatchesGenerated(false);

        List<Athlete> testAthletes = new ArrayList<>();
        for (long i = 1; i <= athleteCount; i++) {
            Athlete athlete = new Athlete();
            athlete.setId(i);
            athlete.setName("Athlete " + i);
            testAthletes.add(athlete);
        }
        testDivision.setAthletes(testAthletes);

        List<Match> persisted = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(1);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(testDivision));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Match> toSave = invocation.getArgument(0);
            for (Match m : toSave) {
                if (m.getId() == null) {
                    m.setId(idSequence.getAndIncrement());
                }
            }
            persisted.addAll(toSave);
            return toSave;
        });
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return persisted.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        });
        when(matchRepository.findByDivisionIdAndRoundNumber(eq(1L), anyInt())).thenAnswer(invocation -> {
            int round = invocation.getArgument(1);
            return persisted.stream()
                .filter(m -> m.getRoundNumber() == round)
                .collect(Collectors.toList());
        });

        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        int expectedRows = (athleteCount - 1) + (athleteCount % 2);
        assertThat(matches).as("total match rows for %d athletes", athleteCount)
            .hasSize(expectedRows);

        List<Match> round1 = matches.stream().filter(m -> m.getRoundNumber() == 1).toList();
        List<Long> round1AthleteIds = round1.stream()
            .flatMap(m -> java.util.stream.Stream.of(m.getAthlete1(), m.getAthlete2()))
            .filter(Objects::nonNull)
            .map(Athlete::getId)
            .toList();
        assertThat(round1AthleteIds).as("every athlete appears in round 1 exactly once")
            .containsExactlyInAnyOrderElementsOf(testAthletes.stream().map(Athlete::getId).toList());

        // Play out every remaining match until nothing more can be completed
        boolean progressed = true;
        int safetyIterations = 0;
        while (progressed) {
            progressed = false;
            safetyIterations++;
            assertThat(safetyIterations).as("safety limit - bracket should resolve quickly").isLessThan(50);

            for (Match m : new ArrayList<>(persisted)) {
                if (m.getStatus() == MatchStatus.PENDING && m.getAthlete1() != null && m.getAthlete2() != null) {
                    Athlete winner = m.getAthlete1();
                    m.setStatus(MatchStatus.COMPLETED);
                    m.setWinner(winner);
                    bracketService.advanceWinnerToNextRound(m.getId(), winner.getId());
                    progressed = true;
                }
            }
        }

        assertThat(persisted).as("no match should be left stuck without a decided winner")
            .allMatch(m -> m.getStatus() == MatchStatus.WALKOVER || m.getStatus() == MatchStatus.COMPLETED);

        int finalRound = persisted.stream().mapToInt(Match::getRoundNumber).max().orElseThrow();
        List<Match> finalMatches = persisted.stream().filter(m -> m.getRoundNumber() == finalRound).toList();
        assertThat(finalMatches).as("exactly one final match").hasSize(1);
        assertThat(finalMatches.get(0).getWinner()).as("a champion was decided").isNotNull();
    }
}
