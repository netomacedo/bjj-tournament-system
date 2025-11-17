package com.bjj.tournament.service;

import com.bjj.tournament.dto.MatchResponseDTO;
import com.bjj.tournament.dto.MatchUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchService
 * Tests match CRUD operations, scoring, and winner determination
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private BracketService bracketService;

    @InjectMocks
    private MatchService matchService;

    private Match testMatch;
    private Athlete athlete1;
    private Athlete athlete2;
    private Division division;

    @BeforeEach
    void setUp() {
        // Create test division
        division = new Division();
        division.setId(1L);
        division.setName("Adult Male Blue Belt");

        // Create test athletes
        athlete1 = new Athlete();
        athlete1.setId(1L);
        athlete1.setName("John Doe");
        athlete1.setTeam("Team A");

        athlete2 = new Athlete();
        athlete2.setId(2L);
        athlete2.setName("Jane Smith");
        athlete2.setTeam("Team B");

        // Create test match
        testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setDivision(division);
        testMatch.setAthlete1(athlete1);
        testMatch.setAthlete2(athlete2);
        testMatch.setRoundNumber(1);
        testMatch.setMatchPosition(1);
        testMatch.setStatus(MatchStatus.PENDING);
        testMatch.setAthlete1Points(0);
        testMatch.setAthlete2Points(0);
        testMatch.setAthlete1Advantages(0);
        testMatch.setAthlete2Advantages(0);
        testMatch.setAthlete1Penalties(0);
        testMatch.setAthlete2Penalties(0);
        testMatch.setFinishedBySubmission(false);
    }

    @Test
    void getMatchById_WhenMatchExists_ReturnsMatchDTO() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        // When
        MatchResponseDTO result = matchService.getMatchById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAthlete1Name()).isEqualTo("John Doe");
        assertThat(result.getAthlete2Name()).isEqualTo("Jane Smith");
        assertThat(result.getDivisionName()).isEqualTo("Adult Male Blue Belt");
        verify(matchRepository).findById(1L);
    }

    @Test
    void getMatchById_WhenMatchNotFound_ThrowsException() {
        // Given
        when(matchRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> matchService.getMatchById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Match not found");
    }

    @Test
    void getMatchesByDivision_ReturnsListOfMatchDTOs() {
        // Given
        Match match2 = new Match();
        match2.setId(2L);
        match2.setDivision(division);
        match2.setRoundNumber(1);
        match2.setMatchPosition(2);
        match2.setStatus(MatchStatus.PENDING);

        when(matchRepository.findByDivisionId(1L)).thenReturn(Arrays.asList(testMatch, match2));

        // When
        List<MatchResponseDTO> results = matchService.getMatchesByDivision(1L);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(1).getId()).isEqualTo(2L);
        verify(matchRepository).findByDivisionId(1L);
    }

    @Test
    void updateMatch_UpdatesScoresCorrectly() {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setAthlete1Points(4);
        updateDTO.setAthlete2Points(2);
        updateDTO.setAthlete1Advantages(1);
        updateDTO.setStatus(MatchStatus.IN_PROGRESS);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        MatchResponseDTO result = matchService.updateMatch(1L, updateDTO);

        // Then
        assertThat(result.getAthlete1Points()).isEqualTo(4);
        assertThat(result.getAthlete2Points()).isEqualTo(2);
        assertThat(result.getAthlete1Advantages()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void updateMatch_WhenCompletedWithWinner_DeterminesWinnerByPoints() {
        // Given
        testMatch.setAthlete1Points(6);
        testMatch.setAthlete2Points(2);

        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setStatus(MatchStatus.COMPLETED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match savedMatch = invocation.getArgument(0);
            savedMatch.determineWinner();
            return savedMatch;
        });

        // When
        MatchResponseDTO result = matchService.updateMatch(1L, updateDTO);

        // Then
        assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(result.getWinnerId()).isEqualTo(athlete1.getId());
        verify(bracketService).advanceWinnerToNextRound(1L, athlete1.getId());
    }

    @Test
    void updateMatch_WhenCompletedWithManualWinner_SetsWinnerCorrectly() {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setStatus(MatchStatus.COMPLETED);
        updateDTO.setWinnerId(2L);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete2));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        MatchResponseDTO result = matchService.updateMatch(1L, updateDTO);

        // Then
        assertThat(result.getWinnerId()).isEqualTo(2L);
        verify(athleteRepository).findById(2L);
        verify(bracketService).advanceWinnerToNextRound(1L, 2L);
    }

    @Test
    void updateMatch_WithSubmission_RecordsSubmissionCorrectly() {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setFinishedBySubmission(true);
        updateDTO.setSubmissionType("Triangle Choke");
        updateDTO.setWinnerId(1L);
        updateDTO.setStatus(MatchStatus.COMPLETED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete1));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        MatchResponseDTO result = matchService.updateMatch(1L, updateDTO);

        // Then
        assertThat(result.getFinishedBySubmission()).isTrue();
        assertThat(result.getSubmissionType()).isEqualTo("Triangle Choke");
        assertThat(result.getWinnerId()).isEqualTo(1L);
    }

    @Test
    void startMatch_ChangesStatusToInProgress() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        Match result = matchService.startMatch(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        verify(matchRepository).save(testMatch);
    }

    @Test
    void startMatch_WhenNotPending_ThrowsException() {
        // Given
        testMatch.setStatus(MatchStatus.COMPLETED);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        // When/Then
        assertThatThrownBy(() -> matchService.startMatch(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only pending matches can be started");
    }

    @Test
    void startMatch_WhenMissingAthlete_ThrowsException() {
        // Given
        testMatch.setAthlete2(null);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));

        // When/Then
        assertThatThrownBy(() -> matchService.startMatch(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Both athletes must be assigned");
    }

    @Test
    void recordSubmission_RecordsSubmissionAndAdvancesWinner() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete1));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        Match result = matchService.recordSubmission(1L, 1L, "Armbar");

        // Then
        assertThat(result.getFinishedBySubmission()).isTrue();
        assertThat(result.getSubmissionType()).isEqualTo("Armbar");
        assertThat(result.getWinner()).isEqualTo(athlete1);
        assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        verify(bracketService).advanceWinnerToNextRound(1L, 1L);
    }

    @Test
    void recordSubmission_WhenWinnerNotInMatch_ThrowsException() {
        // Given
        Athlete outsideAthlete = new Athlete();
        outsideAthlete.setId(999L);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(athleteRepository.findById(999L)).thenReturn(Optional.of(outsideAthlete));

        // When/Then
        assertThatThrownBy(() -> matchService.recordSubmission(1L, 999L, "Armbar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Winner must be one of the match participants");
    }

    @Test
    void recordWalkover_RecordsWalkoverAndAdvancesWinner() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete2));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        Match result = matchService.recordWalkover(1L, 2L);

        // Then
        assertThat(result.getWinner()).isEqualTo(athlete2);
        assertThat(result.getStatus()).isEqualTo(MatchStatus.WALKOVER);
        assertThat(result.getNotes()).contains("Walkover");
        verify(bracketService).advanceWinnerToNextRound(1L, 2L);
    }

    @Test
    void assignToMat_AssignsMatNumberCorrectly() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // When
        Match result = matchService.assignToMat(1L, 3);

        // Then
        assertThat(result.getMatNumber()).isEqualTo(3);
        verify(matchRepository).save(testMatch);
    }

    @Test
    void generateMatches_CallsBracketServiceAndReturnsDTO() {
        // Given
        List<Match> matches = Arrays.asList(testMatch);
        when(bracketService.generateMatchesAutomatically(1L)).thenReturn(matches);

        // When
        List<MatchResponseDTO> results = matchService.generateMatches(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        verify(bracketService).generateMatchesAutomatically(1L);
    }

    @Test
    void getMatchesByDivisionAndRound_ReturnsFilteredMatches() {
        // Given
        when(matchRepository.findByDivisionIdAndRoundNumber(1L, 1)).thenReturn(Arrays.asList(testMatch));

        // When
        List<Match> results = matchService.getMatchesByDivisionAndRound(1L, 1);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoundNumber()).isEqualTo(1);
        verify(matchRepository).findByDivisionIdAndRoundNumber(1L, 1);
    }

    @Test
    void getPendingMatches_ReturnsOnlyPendingMatches() {
        // Given
        when(matchRepository.findPendingMatchesByDivision(1L)).thenReturn(Arrays.asList(testMatch));

        // When
        List<Match> results = matchService.getPendingMatches(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(MatchStatus.PENDING);
        verify(matchRepository).findPendingMatchesByDivision(1L);
    }
}