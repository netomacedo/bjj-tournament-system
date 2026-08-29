package com.bjj.tournament.service;

import com.bjj.tournament.dto.AthleteRankingDTO;
import com.bjj.tournament.entity.*;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import com.bjj.tournament.repository.TournamentRepository;
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

    @InjectMocks
    private DivisionService divisionService;

    private Division division;
    private Athlete athlete1;
    private Athlete athlete2;
    private Athlete athlete3;
    private Athlete athlete4;

    @BeforeEach
    void setUp() {
        division = new Division();
        division.setId(1L);
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
}
