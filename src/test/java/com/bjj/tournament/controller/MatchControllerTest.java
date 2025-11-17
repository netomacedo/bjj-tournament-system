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
@WebMvcTest(MatchController.class)
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
}