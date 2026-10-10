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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
