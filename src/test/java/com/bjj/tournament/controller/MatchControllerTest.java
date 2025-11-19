package com.bjj.tournament.controller;

import com.bjj.tournament.dto.MatchResponseDTO;
import com.bjj.tournament.dto.MatchUpdateDTO;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MatchController
 * Tests REST API endpoints for match operations
 */
@WebMvcTest(
    controllers = MatchController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.bjj.tournament.security.JwtAuthenticationFilter.class,
            com.bjj.tournament.security.JwtTokenProvider.class,
            com.bjj.tournament.security.CustomUserDetailsService.class,
            com.bjj.tournament.security.SecurityConfig.class
        }
    ),
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    }
)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchService matchService;

    private MatchResponseDTO testMatchDTO;

    @BeforeEach
    void setUp() {
        testMatchDTO = new MatchResponseDTO();
        testMatchDTO.setId(1L);
        testMatchDTO.setDivisionId(1L);
        testMatchDTO.setDivisionName("Adult Male Blue Belt");
        testMatchDTO.setAthlete1Id(1L);
        testMatchDTO.setAthlete1Name("John Doe");
        testMatchDTO.setAthlete1Team("Team A");
        testMatchDTO.setAthlete2Id(2L);
        testMatchDTO.setAthlete2Name("Jane Smith");
        testMatchDTO.setAthlete2Team("Team B");
        testMatchDTO.setStatus(MatchStatus.PENDING);
        testMatchDTO.setRoundNumber(1);
        testMatchDTO.setMatchPosition(1);
        testMatchDTO.setAthlete1Points(0);
        testMatchDTO.setAthlete2Points(0);
        testMatchDTO.setAthlete1Advantages(0);
        testMatchDTO.setAthlete2Advantages(0);
        testMatchDTO.setAthlete1Penalties(0);
        testMatchDTO.setAthlete2Penalties(0);
        testMatchDTO.setAthlete1TotalScore(0);
        testMatchDTO.setAthlete2TotalScore(0);
        testMatchDTO.setFinishedBySubmission(false);
    }

    @Test
    void getMatchesByDivision_ReturnsListOfMatches() throws Exception {
        // Given
        List<MatchResponseDTO> matches = Arrays.asList(testMatchDTO);
        when(matchService.getMatchesByDivision(1L)).thenReturn(matches);

        // When/Then
        mockMvc.perform(get("/api/divisions/1/matches"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].athlete1Name", is("John Doe")))
            .andExpect(jsonPath("$[0].athlete2Name", is("Jane Smith")))
            .andExpect(jsonPath("$[0].divisionName", is("Adult Male Blue Belt")));

        verify(matchService).getMatchesByDivision(1L);
    }

    @Test
    void getMatchesByDivisionLegacy_ReturnsListOfMatches() throws Exception {
        // Given
        List<MatchResponseDTO> matches = Arrays.asList(testMatchDTO);
        when(matchService.getMatchesByDivision(1L)).thenReturn(matches);

        // When/Then
        mockMvc.perform(get("/api/matches/division/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)));

        verify(matchService).getMatchesByDivision(1L);
    }

    @Test
    void getMatchById_WhenMatchExists_ReturnsMatch() throws Exception {
        // Given
        when(matchService.getMatchById(1L)).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(get("/api/matches/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.athlete1Name", is("John Doe")))
            .andExpect(jsonPath("$.status", is("PENDING")));

        verify(matchService).getMatchById(1L);
    }

    @Test
    void updateMatch_WithValidData_UpdatesMatch() throws Exception {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setAthlete1Points(4);
        updateDTO.setAthlete2Points(2);
        updateDTO.setAthlete1Advantages(1);
        updateDTO.setStatus(MatchStatus.IN_PROGRESS);

        testMatchDTO.setAthlete1Points(4);
        testMatchDTO.setAthlete2Points(2);
        testMatchDTO.setAthlete1Advantages(1);
        testMatchDTO.setStatus(MatchStatus.IN_PROGRESS);

        when(matchService.updateMatch(eq(1L), any(MatchUpdateDTO.class))).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(put("/api/matches/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.athlete1Points", is(4)))
            .andExpect(jsonPath("$.athlete2Points", is(2)))
            .andExpect(jsonPath("$.athlete1Advantages", is(1)))
            .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        verify(matchService).updateMatch(eq(1L), any(MatchUpdateDTO.class));
    }

    @Test
    void updateMatch_CompletingMatch_SetsWinner() throws Exception {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setStatus(MatchStatus.COMPLETED);
        updateDTO.setWinnerId(1L);

        testMatchDTO.setStatus(MatchStatus.COMPLETED);
        testMatchDTO.setWinnerId(1L);
        testMatchDTO.setWinnerName("John Doe");

        when(matchService.updateMatch(eq(1L), any(MatchUpdateDTO.class))).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(put("/api/matches/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("COMPLETED")))
            .andExpect(jsonPath("$.winnerId", is(1)))
            .andExpect(jsonPath("$.winnerName", is("John Doe")));
    }

    @Test
    void generateMatches_CreatesMatchesForDivision() throws Exception {
        // Given
        List<MatchResponseDTO> generatedMatches = Arrays.asList(testMatchDTO);
        when(matchService.generateMatches(1L)).thenReturn(generatedMatches);

        // When/Then
        mockMvc.perform(post("/api/divisions/1/generate-matches"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)));

        verify(matchService).generateMatches(1L);
    }

    @Test
    void updateMatch_WithSubmission_RecordsSubmission() throws Exception {
        // Given
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setFinishedBySubmission(true);
        updateDTO.setSubmissionType("Triangle Choke");
        updateDTO.setWinnerId(1L);
        updateDTO.setStatus(MatchStatus.COMPLETED);

        testMatchDTO.setFinishedBySubmission(true);
        testMatchDTO.setSubmissionType("Triangle Choke");
        testMatchDTO.setWinnerId(1L);
        testMatchDTO.setStatus(MatchStatus.COMPLETED);

        when(matchService.updateMatch(eq(1L), any(MatchUpdateDTO.class))).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(put("/api/matches/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finishedBySubmission", is(true)))
            .andExpect(jsonPath("$.submissionType", is("Triangle Choke")))
            .andExpect(jsonPath("$.winnerId", is(1)));
    }

    @Test
    void getMatchById_WhenMatchNotFound_Returns404() throws Exception {
        // Given
        when(matchService.getMatchById(anyLong()))
            .thenThrow(new IllegalArgumentException("Match not found"));

        // When/Then
        mockMvc.perform(get("/api/matches/999"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void startMatch_ChangesStatusToInProgress() throws Exception {
        // Given
        testMatchDTO.setStatus(MatchStatus.IN_PROGRESS);
        when(matchService.startMatchAndReturn(1L)).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(post("/api/matches/1/start"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        verify(matchService).startMatchAndReturn(1L);
    }

    @Test
    void startMatch_WhenMatchNotPending_ThrowsException() throws Exception {
        // Given
        when(matchService.startMatchAndReturn(1L))
            .thenThrow(new IllegalStateException("Only pending matches can be started"));

        // When/Then
        mockMvc.perform(post("/api/matches/1/start"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void completeMatch_DeterminesWinnerAndChangesStatus() throws Exception {
        // Given
        testMatchDTO.setStatus(MatchStatus.COMPLETED);
        testMatchDTO.setAthlete1Points(9);
        testMatchDTO.setAthlete2Points(2);
        testMatchDTO.setWinnerId(1L);
        testMatchDTO.setWinnerName("John Doe");
        when(matchService.completeMatchAndReturn(1L)).thenReturn(testMatchDTO);

        // When/Then
        mockMvc.perform(post("/api/matches/1/complete"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.status", is("COMPLETED")))
            .andExpect(jsonPath("$.winnerId", is(1)))
            .andExpect(jsonPath("$.winnerName", is("John Doe")))
            .andExpect(jsonPath("$.athlete1Points", is(9)))
            .andExpect(jsonPath("$.athlete2Points", is(2)));

        verify(matchService).completeMatchAndReturn(1L);
    }

    @Test
    void completeMatch_WhenMatchNotInProgress_ThrowsException() throws Exception {
        // Given
        when(matchService.completeMatchAndReturn(1L))
            .thenThrow(new IllegalStateException("Only matches in progress can be completed"));

        // When/Then
        mockMvc.perform(post("/api/matches/1/complete"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void recordSubmission_RecordsSubmissionVictory() throws Exception {
        // Given
        testMatchDTO.setStatus(MatchStatus.COMPLETED);
        testMatchDTO.setFinishedBySubmission(true);
        testMatchDTO.setSubmissionType("Rear Naked Choke");
        testMatchDTO.setWinnerId(1L);
        testMatchDTO.setWinnerName("John Doe");
        when(matchService.recordSubmissionAndReturn(eq(1L), eq(1L), eq("Rear Naked Choke")))
            .thenReturn(testMatchDTO);

        String requestBody = """
            {
                "winnerId": 1,
                "submissionType": "Rear Naked Choke"
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/matches/1/submission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.status", is("COMPLETED")))
            .andExpect(jsonPath("$.finishedBySubmission", is(true)))
            .andExpect(jsonPath("$.submissionType", is("Rear Naked Choke")))
            .andExpect(jsonPath("$.winnerId", is(1)));

        verify(matchService).recordSubmissionAndReturn(1L, 1L, "Rear Naked Choke");
    }

    @Test
    void recordSubmission_WithInvalidWinner_ThrowsException() throws Exception {
        // Given
        when(matchService.recordSubmissionAndReturn(eq(1L), eq(999L), any()))
            .thenThrow(new IllegalArgumentException("Winner athlete not found"));

        String requestBody = """
            {
                "winnerId": 999,
                "submissionType": "Triangle Choke"
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/matches/1/submission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void recordWalkover_RecordsWalkoverVictory() throws Exception {
        // Given
        testMatchDTO.setStatus(MatchStatus.WALKOVER);
        testMatchDTO.setWinnerId(2L);
        testMatchDTO.setWinnerName("Jane Smith");
        when(matchService.recordWalkoverAndReturn(1L, 2L)).thenReturn(testMatchDTO);

        String requestBody = """
            {
                "winnerId": 2
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/matches/1/walkover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.status", is("WALKOVER")))
            .andExpect(jsonPath("$.winnerId", is(2)))
            .andExpect(jsonPath("$.winnerName", is("Jane Smith")));

        verify(matchService).recordWalkoverAndReturn(1L, 2L);
    }

    @Test
    void recordWalkover_WithInvalidWinner_ThrowsException() throws Exception {
        // Given
        when(matchService.recordWalkoverAndReturn(eq(1L), eq(999L)))
            .thenThrow(new IllegalArgumentException("Winner must be one of the match participants"));

        String requestBody = """
            {
                "winnerId": 999
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/matches/1/walkover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void resetMatch_ResetsAllScoresAndStatus() throws Exception {
        // Given - Match that was completed with scores
        MatchResponseDTO resetMatchDTO = new MatchResponseDTO();
        resetMatchDTO.setId(1L);
        resetMatchDTO.setDivisionId(1L);
        resetMatchDTO.setDivisionName("Adult Male Blue Belt");
        resetMatchDTO.setAthlete1Id(1L);
        resetMatchDTO.setAthlete1Name("John Doe");
        resetMatchDTO.setAthlete1Team("Team A");
        resetMatchDTO.setAthlete2Id(2L);
        resetMatchDTO.setAthlete2Name("Jane Smith");
        resetMatchDTO.setAthlete2Team("Team B");
        resetMatchDTO.setStatus(MatchStatus.PENDING);
        resetMatchDTO.setRoundNumber(1);
        resetMatchDTO.setMatchPosition(1);
        // All scores reset to 0
        resetMatchDTO.setAthlete1Points(0);
        resetMatchDTO.setAthlete2Points(0);
        resetMatchDTO.setAthlete1Advantages(0);
        resetMatchDTO.setAthlete2Advantages(0);
        resetMatchDTO.setAthlete1Penalties(0);
        resetMatchDTO.setAthlete2Penalties(0);
        resetMatchDTO.setAthlete1TotalScore(0);
        resetMatchDTO.setAthlete2TotalScore(0);
        resetMatchDTO.setFinishedBySubmission(false);
        resetMatchDTO.setWinnerId(null);
        resetMatchDTO.setWinnerName(null);

        when(matchService.resetMatch(1L)).thenReturn(resetMatchDTO);

        // When/Then
        mockMvc.perform(post("/api/matches/1/reset"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andExpect(jsonPath("$.athlete1Points", is(0)))
            .andExpect(jsonPath("$.athlete2Points", is(0)))
            .andExpect(jsonPath("$.athlete1Advantages", is(0)))
            .andExpect(jsonPath("$.athlete2Advantages", is(0)))
            .andExpect(jsonPath("$.athlete1Penalties", is(0)))
            .andExpect(jsonPath("$.athlete2Penalties", is(0)))
            .andExpect(jsonPath("$.winnerId").doesNotExist())
            .andExpect(jsonPath("$.winnerName").doesNotExist())
            .andExpect(jsonPath("$.finishedBySubmission", is(false)));

        verify(matchService).resetMatch(1L);
    }

    @Test
    void resetMatch_WhenMatchNotFound_ThrowsException() throws Exception {
        // Given
        when(matchService.resetMatch(999L))
            .thenThrow(new IllegalArgumentException("Match not found with ID: 999"));

        // When/Then
        mockMvc.perform(post("/api/matches/999/reset"))
            .andExpect(status().is4xxClientError());

        verify(matchService).resetMatch(999L);
    }

    @Test
    void resetMatch_ClearsSubmissionInformation() throws Exception {
        // Given - Match that was won by submission
        MatchResponseDTO resetMatchDTO = new MatchResponseDTO();
        resetMatchDTO.setId(1L);
        resetMatchDTO.setStatus(MatchStatus.PENDING);
        resetMatchDTO.setFinishedBySubmission(false);
        resetMatchDTO.setSubmissionType(null);
        resetMatchDTO.setWinnerId(null);
        resetMatchDTO.setAthlete1Points(0);
        resetMatchDTO.setAthlete2Points(0);
        resetMatchDTO.setAthlete1Advantages(0);
        resetMatchDTO.setAthlete2Advantages(0);
        resetMatchDTO.setAthlete1Penalties(0);
        resetMatchDTO.setAthlete2Penalties(0);

        when(matchService.resetMatch(1L)).thenReturn(resetMatchDTO);

        // When/Then
        mockMvc.perform(post("/api/matches/1/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andExpect(jsonPath("$.finishedBySubmission", is(false)))
            .andExpect(jsonPath("$.submissionType").doesNotExist())
            .andExpect(jsonPath("$.winnerId").doesNotExist());

        verify(matchService).resetMatch(1L);
    }
}