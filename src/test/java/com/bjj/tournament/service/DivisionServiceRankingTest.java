package com.bjj.tournament.service;

import com.bjj.tournament.dto.AthleteRankingDTO;
import com.bjj.tournament.entity.*;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import com.bjj.tournament.repository.TournamentRepository;
import com.bjj.tournament.security.OrganizationAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DivisionService ranking calculation
 */
@ExtendWith(MockitoExtension.class)
class DivisionServiceRankingTest {

    @Mock
    private DivisionRepository divisionRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @InjectMocks
    private DivisionService divisionService;

    private Division division;
    private Athlete athlete1;
    private Athlete athlete2;
    private Athlete athlete3;
    private Athlete athlete4;

    @BeforeEach
    void setUp() {
        Tournament tournament = new Tournament();
        tournament.setId(100L);
        tournament.setOrganizationId(1L);

        division = new Division();
        division.setId(1L);
        division.setTournament(tournament);
        division.setMatchesGenerated(true);

        athlete1 = new Athlete();
        athlete1.setId(1L);
        athlete1.setName("Levy");
        athlete1.setTeam("Team A");

        athlete2 = new Athlete();
        athlete2.setId(2L);
        athlete2.setName("Neto");
        athlete2.setTeam("Team B");

        athlete3 = new Athlete();
        athlete3.setId(3L);
        athlete3.setName("Athlete 3");
        athlete3.setTeam("Team C");

        athlete4 = new Athlete();
        athlete4.setId(4L);
        athlete4.setName("Athlete 4");
        athlete4.setTeam("Team D");
    }

    @Test
    void getDivisionRankings_WithCompletedBracket_ReturnsCorrectMedalPositions() {
        // Given - 4 athlete bracket with finals and semifinals completed
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = createCompletedBracket();
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertNotNull(rankings);
        assertEquals(4, rankings.size());

        // Verify Gold medal (Finals winner - Levy)
        AthleteRankingDTO gold = rankings.stream()
            .filter(r -> r.getPosition() == 1)
            .findFirst()
            .orElseThrow();
        assertEquals("Levy", gold.getAthleteName());
        assertEquals("GOLD", gold.getMedal());
        assertEquals(2, gold.getWins()); // Won semifinal and final

        // Verify Silver medal (Finals loser - Neto)
        AthleteRankingDTO silver = rankings.stream()
            .filter(r -> r.getPosition() == 2)
            .findFirst()
            .orElseThrow();
        assertEquals("Neto", silver.getAthleteName());
        assertEquals("SILVER", silver.getMedal());
        assertEquals(1, silver.getWins()); // Won semifinal, lost final

        // Verify Bronze medals (Semifinal losers)
        List<AthleteRankingDTO> bronzes = rankings.stream()
            .filter(r -> r.getPosition() == 3)
            .toList();
        assertEquals(2, bronzes.size());
        assertTrue(bronzes.stream().allMatch(b -> "BRONZE".equals(b.getMedal())));
    }

    @Test
    void getDivisionRankings_WithThreeAthleteBracketIncludingABye_DoesNotThrowAndAssignsCorrectMedals() {
        // Given - 3-athlete single elimination: round 1 has one real match plus
        // a bye (athlete3 advances automatically with no opponent, athlete2 ==
        // null), and the final is the round-1 winner against the bye recipient.
        // This previously threw an NPE because the bye match was incorrectly
        // treated as a "semifinal" with a loser to award bronze to.
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = new ArrayList<>();

        // Round 1: Levy vs Neto (Levy wins)
        matches.add(createMatch(1L, 1, athlete1, athlete2, athlete1, 6, 2));

        // Round 1: bye - Athlete 3 advances automatically, no opponent
        Match byeMatch = new Match();
        byeMatch.setId(2L);
        byeMatch.setRoundNumber(1);
        byeMatch.setMatchPosition(2);
        byeMatch.setDivision(division);
        byeMatch.setAthlete1(athlete3);
        byeMatch.setAthlete2(null);
        byeMatch.setWinner(athlete3);
        byeMatch.setStatus(MatchStatus.WALKOVER);
        matches.add(byeMatch);

        // Round 2 (final): Levy vs Athlete 3 (Athlete 3 wins)
        matches.add(createMatch(3L, 2, athlete1, athlete3, athlete3, 4, 6));

        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertNotNull(rankings);
        assertEquals(3, rankings.size());

        AthleteRankingDTO gold = rankings.stream().filter(r -> r.getPosition() == 1).findFirst().orElseThrow();
        assertEquals("Athlete 3", gold.getAthleteName());
        assertEquals("GOLD", gold.getMedal());

        AthleteRankingDTO silver = rankings.stream().filter(r -> r.getPosition() == 2).findFirst().orElseThrow();
        assertEquals("Levy", silver.getAthleteName());
        assertEquals("SILVER", silver.getMedal());

        // Neto lost the only real round-1 match, so gets bronze; the bye
        // recipient never played a real semifinal, so isn't bronze-eligible
        List<AthleteRankingDTO> bronzes = rankings.stream().filter(r -> r.getPosition() == 3).toList();
        assertEquals(1, bronzes.size());
        assertEquals("Neto", bronzes.get(0).getAthleteName());
    }

    @Test
    void getDivisionRankings_WithFiveAthleteCascadingBracket_StillAwardsTwoBronzeMedals() {
        // Given - 5-athlete cascading-bye bracket shape: round 1 has 2 real
        // matches + 1 bye (not the old front-loaded "1 real + 3 byes"), so
        // one eventual finalist (Athlete3) skips round 2 entirely and
        // advances straight from round 1 to the final. The old "finalsRound-1
        // losers get bronze" logic would only find ONE real match in round 2
        // under this shape (Athlete5 vs Levy) and silently drop to 1 bronze
        // instead of 2 - this is the regression the generalized "each
        // finalist's own most recent real match" logic exists to prevent.
        Athlete athlete5 = new Athlete();
        athlete5.setId(5L);
        athlete5.setName("Athlete 5");
        athlete5.setTeam("Team E");

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = new ArrayList<>();

        // Round 1: Levy beats Neto; Athlete3 beats Athlete4; Athlete5 byes
        matches.add(createMatch(1L, 1, athlete1, athlete2, athlete1, 6, 2));
        matches.add(createMatch(2L, 1, athlete3, athlete4, athlete3, 4, 0));

        Match byeMatch = new Match();
        byeMatch.setId(3L);
        byeMatch.setRoundNumber(1);
        byeMatch.setMatchPosition(3);
        byeMatch.setDivision(division);
        byeMatch.setAthlete1(athlete5);
        byeMatch.setAthlete2(null);
        byeMatch.setWinner(athlete5);
        byeMatch.setStatus(MatchStatus.WALKOVER);
        matches.add(byeMatch);

        // Round 2: Athlete3's win skips this round entirely (advances
        // straight to the final) - only Athlete5 vs Levy is a real round-2
        // match here, Levy wins
        matches.add(createMatch(4L, 2, athlete5, athlete1, athlete1, 2, 6));

        // Round 3 (final): Levy vs Athlete3 - Levy wins (GOLD), Athlete3 (SILVER)
        matches.add(createMatch(5L, 3, athlete1, athlete3, athlete1, 8, 4));

        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        AthleteRankingDTO gold = rankings.stream().filter(r -> r.getPosition() == 1).findFirst().orElseThrow();
        assertEquals("Levy", gold.getAthleteName());
        assertEquals("GOLD", gold.getMedal());

        AthleteRankingDTO silver = rankings.stream().filter(r -> r.getPosition() == 2).findFirst().orElseThrow();
        assertEquals("Athlete 3", silver.getAthleteName());
        assertEquals("SILVER", silver.getMedal());

        // Bronze: Athlete5 lost to Levy in round 2 (Levy's most recent real
        // match before the final); Athlete4 lost to Athlete3 in round 1
        // (Athlete3's only, and therefore most recent, real match before
        // the final) - two bronzes despite the two finalists having played
        // a different number of real matches to get there.
        List<AthleteRankingDTO> bronzes = rankings.stream().filter(r -> r.getPosition() == 3).toList();
        assertEquals(2, bronzes.size(), "two bronzes should still be awarded under the cascading bracket shape");
        assertTrue(bronzes.stream().anyMatch(b -> "Athlete 5".equals(b.getAthleteName())));
        assertTrue(bronzes.stream().anyMatch(b -> "Athlete 4".equals(b.getAthleteName())));
    }

    @Test
    void getDivisionRankings_WithNoMatchesGenerated_ReturnsEmptyList() {
        // Given
        division.setMatchesGenerated(false);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertNotNull(rankings);
        assertTrue(rankings.isEmpty());
    }

    @Test
    void getDivisionRankings_WithIncompleteBracket_ReturnsRankingsWithoutGoldSilver() {
        // Given - Only semifinals completed, finals pending
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = createIncompleteBracket();
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertNotNull(rankings);
        // No gold or silver medals should be assigned without completed finals
        assertTrue(rankings.stream().noneMatch(r -> "GOLD".equals(r.getMedal())));
        assertTrue(rankings.stream().noneMatch(r -> "SILVER".equals(r.getMedal())));
    }

    @Test
    void getDivisionRankings_DivisionNotFound_ThrowsException() {
        // Given
        when(divisionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> divisionService.getDivisionRankings(999L));
    }

    @Test
    void getDivisionRankings_CalculatesWinsAndPoints_Correctly() {
        // Given
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        List<Match> matches = createCompletedBracket();
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        AthleteRankingDTO levy = rankings.stream()
            .filter(r -> "Levy".equals(r.getAthleteName()))
            .findFirst()
            .orElseThrow();

        assertEquals(2, levy.getWins());
        assertEquals(0, levy.getLosses());
        assertTrue(levy.getTotalPoints() > 0);
    }

    // ---- Round robin ranking tests ----

    @Test
    void getDivisionRankings_RoundRobin_AssignsGoldSilverAndTwoBronzes() {
        // Given - 4 athletes, every pair plays once, clean win counts: 3/2/1/0
        division.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = List.of(
            createMatch(1L, 1, athlete1, athlete2, athlete1, 6, 2), // Levy beats Neto
            createMatch(2L, 1, athlete1, athlete3, athlete1, 6, 0), // Levy beats Athlete3
            createMatch(3L, 1, athlete1, athlete4, athlete1, 6, 0), // Levy beats Athlete4
            createMatch(4L, 1, athlete2, athlete3, athlete2, 4, 2), // Neto beats Athlete3
            createMatch(5L, 1, athlete2, athlete4, athlete2, 4, 0), // Neto beats Athlete4
            createMatch(6L, 1, athlete3, athlete4, athlete3, 2, 0)  // Athlete3 beats Athlete4
        );
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then - top 2 by win count get gold/silver; round robin always awards
        // bronze to BOTH the 3rd and 4th-ranked athletes, even though
        // Athlete4 here has strictly fewer wins than Athlete3 (0 vs 1) - this
        // is the intentional "two bronzes" rule, not a tie-break.
        assertEquals(4, rankings.size());

        assertEquals("Levy", rankings.stream().filter(r -> r.getPosition() == 1).findFirst().orElseThrow().getAthleteName());
        assertEquals("GOLD", rankings.stream().filter(r -> r.getPosition() == 1).findFirst().orElseThrow().getMedal());

        assertEquals("Neto", rankings.stream().filter(r -> r.getPosition() == 2).findFirst().orElseThrow().getAthleteName());
        assertEquals("SILVER", rankings.stream().filter(r -> r.getPosition() == 2).findFirst().orElseThrow().getMedal());

        List<AthleteRankingDTO> bronzes = rankings.stream().filter(r -> r.getPosition() == 3).toList();
        assertEquals(2, bronzes.size());
        assertTrue(bronzes.stream().allMatch(b -> "BRONZE".equals(b.getMedal())));
        assertTrue(bronzes.stream().anyMatch(b -> "Athlete 3".equals(b.getAthleteName())));
        assertTrue(bronzes.stream().anyMatch(b -> "Athlete 4".equals(b.getAthleteName())));
    }

    @Test
    void getDivisionRankings_RoundRobin_TiebreaksBySubmissionWinsWhenWinsAreEqual() {
        // Given - Levy and Neto both have 1 win each, but Levy's was by
        // submission and Neto's was on points - Levy should rank higher.
        division.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = List.of(
            createMatchWithSubmission(1L, athlete1, athlete3, athlete1, 0, 0, true),  // Levy submits Athlete3
            createMatch(2L, 1, athlete2, athlete4, athlete2, 4, 2)                     // Neto beats Athlete4 on points
        );
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        AthleteRankingDTO levy = rankings.stream().filter(r -> "Levy".equals(r.getAthleteName())).findFirst().orElseThrow();
        AthleteRankingDTO neto = rankings.stream().filter(r -> "Neto".equals(r.getAthleteName())).findFirst().orElseThrow();

        assertEquals(1, levy.getWins());
        assertEquals(1, neto.getWins());
        assertEquals(1, levy.getSubmissionWins());
        assertEquals(0, neto.getSubmissionWins());
        assertTrue(levy.getPosition() < neto.getPosition(),
            "Levy (submission win) should rank above Neto (points win) despite equal win counts");
    }

    @Test
    void getDivisionRankings_RoundRobin_TiebreaksByPointsWhenWinsAndSubmissionsAreEqual() {
        // Given - Levy and Neto both have 1 win each, no submissions either
        // way, but Levy scored more points in their win - Levy ranks higher.
        division.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = List.of(
            createMatch(1L, 1, athlete1, athlete3, athlete1, 10, 0), // Levy wins big on points
            createMatch(2L, 1, athlete2, athlete4, athlete2, 2, 0)   // Neto wins narrowly on points
        );
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        AthleteRankingDTO levy = rankings.stream().filter(r -> "Levy".equals(r.getAthleteName())).findFirst().orElseThrow();
        AthleteRankingDTO neto = rankings.stream().filter(r -> "Neto".equals(r.getAthleteName())).findFirst().orElseThrow();

        assertEquals(levy.getWins(), neto.getWins());
        assertEquals(levy.getSubmissionWins(), neto.getSubmissionWins());
        assertTrue(levy.getTotalPoints() > neto.getTotalPoints());
        assertTrue(levy.getPosition() < neto.getPosition(),
            "Levy (more points) should rank above Neto despite equal wins and submissions");
    }

    @Test
    void getDivisionRankings_RoundRobin_FifthPlaceIsPositionFourNotFive() {
        // Given - 5 athletes with a clean win-count ladder: 4/3/2/1/0.
        // Positions 3 and 4 (by sort order) both get bronze per the
        // "two bronzes" rule, and the 5th-ranked athlete must land on
        // position 4, not be skipped to position 5.
        Athlete athlete5 = new Athlete();
        athlete5.setId(5L);
        athlete5.setName("Athlete 5");
        athlete5.setTeam("Team E");

        division.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = List.of(
            createMatch(1L, 1, athlete1, athlete2, athlete1, 2, 0),
            createMatch(2L, 1, athlete1, athlete3, athlete1, 2, 0),
            createMatch(3L, 1, athlete1, athlete4, athlete1, 2, 0),
            createMatch(4L, 1, athlete1, athlete5, athlete1, 2, 0),
            createMatch(5L, 1, athlete2, athlete3, athlete2, 2, 0),
            createMatch(6L, 1, athlete2, athlete4, athlete2, 2, 0),
            createMatch(7L, 1, athlete2, athlete5, athlete2, 2, 0),
            createMatch(8L, 1, athlete3, athlete4, athlete3, 2, 0),
            createMatch(9L, 1, athlete3, athlete5, athlete3, 2, 0),
            createMatch(10L, 1, athlete4, athlete5, athlete4, 2, 0)
        );
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertEquals(5, rankings.size());
        assertEquals(List.of(1, 2, 3, 3, 4), rankings.stream()
            .sorted(Comparator.comparing(AthleteRankingDTO::getWins).reversed())
            .map(AthleteRankingDTO::getPosition)
            .toList());

        AthleteRankingDTO lastPlace = rankings.stream()
            .filter(r -> "Athlete 5".equals(r.getAthleteName()))
            .findFirst()
            .orElseThrow();
        assertEquals(0, lastPlace.getWins());
        assertEquals(4, lastPlace.getPosition(), "5th-ranked athlete should be position 4, not skipped to 5");
    }

    @Test
    void getDivisionRankings_RoundRobin_WithThreeAthletes_AssignsSingleBronzeNotTwo() {
        // Given - IBJJF 3-athlete division run as round-robin: with only 3
        // competitors there's no 4th place to pair with 3rd, so the "two
        // bronzes" rule should naturally yield just one bronze medal.
        division.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        List<Match> matches = List.of(
            createMatch(1L, 1, athlete1, athlete2, athlete1, 6, 2), // Levy beats Neto
            createMatch(2L, 1, athlete1, athlete3, athlete1, 6, 0), // Levy beats Athlete3
            createMatch(3L, 1, athlete2, athlete3, athlete2, 4, 2)  // Neto beats Athlete3
        );
        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(matches);

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertEquals(3, rankings.size());
        assertEquals("GOLD", rankings.stream().filter(r -> r.getPosition() == 1).findFirst().orElseThrow().getMedal());
        assertEquals("SILVER", rankings.stream().filter(r -> r.getPosition() == 2).findFirst().orElseThrow().getMedal());

        List<AthleteRankingDTO> bronzes = rankings.stream().filter(r -> r.getPosition() == 3).toList();
        assertEquals(1, bronzes.size(), "only one athlete left to take 3rd when there are just 3 competitors");
        assertEquals("Athlete 3", bronzes.get(0).getAthleteName());
    }

    @Test
    void getDivisionRankings_WithSoleAthlete_AssignsGoldOnlyWithNoSilverOrBronze() {
        // Given - IBJJF rule: a division with a single enrolled athlete is
        // awarded gold automatically via a walkover with no opponent.
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        Match soloMatch = new Match();
        soloMatch.setId(1L);
        soloMatch.setRoundNumber(1);
        soloMatch.setMatchPosition(1);
        soloMatch.setDivision(division);
        soloMatch.setAthlete1(athlete1);
        soloMatch.setAthlete2(null);
        soloMatch.setWinner(athlete1);
        soloMatch.setStatus(MatchStatus.WALKOVER);

        when(matchRepository.findByDivisionIdOrderByRoundNumberAsc(1L)).thenReturn(List.of(soloMatch));

        // When
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(1L);

        // Then
        assertEquals(1, rankings.size());
        AthleteRankingDTO gold = rankings.get(0);
        assertEquals("Levy", gold.getAthleteName());
        assertEquals("GOLD", gold.getMedal());
        assertEquals(1, gold.getPosition());
        assertEquals(0, gold.getWins(), "a walkover isn't a real win to tally");
    }

    // Helper method to create a completed bracket
    private List<Match> createCompletedBracket() {
        List<Match> matches = new ArrayList<>();

        // Semifinal 1: Levy vs Athlete 3 (Levy wins)
        Match semi1 = createMatch(1L, 1, athlete1, athlete3, athlete1, 6, 2);
        matches.add(semi1);

        // Semifinal 2: Neto vs Athlete 4 (Neto wins)
        Match semi2 = createMatch(2L, 1, athlete2, athlete4, athlete2, 4, 0);
        matches.add(semi2);

        // Finals: Levy vs Neto (Levy wins)
        Match finals = createMatch(3L, 2, athlete1, athlete2, athlete1, 8, 4);
        matches.add(finals);

        return matches;
    }

    // Helper method to create incomplete bracket (no finals)
    private List<Match> createIncompleteBracket() {
        List<Match> matches = new ArrayList<>();

        // Semifinal 1: Levy vs Athlete 3 (Levy wins)
        Match semi1 = createMatch(1L, 1, athlete1, athlete3, athlete1, 6, 2);
        matches.add(semi1);

        // Semifinal 2: Neto vs Athlete 4 (Neto wins)
        Match semi2 = createMatch(2L, 1, athlete2, athlete4, athlete2, 4, 0);
        matches.add(semi2);

        // Finals: Pending (not completed)
        Match finals = new Match();
        finals.setId(3L);
        finals.setRoundNumber(2);
        finals.setMatchPosition(1);
        finals.setDivision(division);
        finals.setAthlete1(athlete1);
        finals.setAthlete2(athlete2);
        finals.setStatus(MatchStatus.PENDING);
        matches.add(finals);

        return matches;
    }

    // Helper to create a completed match
    private Match createMatch(Long id, int roundNumber, Athlete a1, Athlete a2,
                            Athlete winner, int a1Points, int a2Points) {
        Match match = new Match();
        match.setId(id);
        match.setRoundNumber(roundNumber);
        match.setMatchPosition(1);
        match.setDivision(division);
        match.setAthlete1(a1);
        match.setAthlete2(a2);
        match.setWinner(winner);
        match.setAthlete1Points(a1Points);
        match.setAthlete2Points(a2Points);
        match.setStatus(MatchStatus.COMPLETED);
        return match;
    }

    // Helper to create a completed match decided by submission
    private Match createMatchWithSubmission(Long id, Athlete a1, Athlete a2,
                            Athlete winner, int a1Points, int a2Points, boolean finishedBySubmission) {
        Match match = createMatch(id, 1, a1, a2, winner, a1Points, a2Points);
        match.setFinishedBySubmission(finishedBySubmission);
        return match;
    }
}
