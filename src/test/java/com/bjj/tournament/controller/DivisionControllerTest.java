package com.bjj.tournament.controller;

import com.bjj.tournament.dto.DivisionCreateDTO;
import com.bjj.tournament.dto.DivisionResponseDTO;
import com.bjj.tournament.dto.DivisionUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.*;
import com.bjj.tournament.service.DivisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DivisionController
 * Uses MockMvc to test REST endpoints
 */
@WebMvcTest(DivisionController.class)
class DivisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DivisionService divisionService;

    private DivisionResponseDTO divisionResponseDTO;
    private DivisionCreateDTO createDTO;
    private Athlete athlete;

    @BeforeEach
    void setUp() {
        // Setup division response DTO
        divisionResponseDTO = new DivisionResponseDTO();
        divisionResponseDTO.setId(1L);
        divisionResponseDTO.setTournamentId(1L);
        divisionResponseDTO.setName("Adult Male Blue Belt Light");
        divisionResponseDTO.setBeltRank(BeltRank.BLUE);
        divisionResponseDTO.setAgeCategory(AgeCategory.ADULT);
        divisionResponseDTO.setGender(Gender.MALE);
        divisionResponseDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        divisionResponseDTO.setBracketType(BracketType.SINGLE_ELIMINATION);
        divisionResponseDTO.setAthleteCount(0);
        divisionResponseDTO.setMatchCount(0);
        divisionResponseDTO.setMatchesGenerated(false);
        divisionResponseDTO.setCompleted(false);
        divisionResponseDTO.setCreatedAt(LocalDateTime.now());
        divisionResponseDTO.setUpdatedAt(LocalDateTime.now());

        // Setup create DTO
        createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.BLUE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        // Setup athlete
        athlete = new Athlete();
        athlete.setId(1L);
        athlete.setName("Test Athlete");
        athlete.setDateOfBirth(LocalDate.of(1995, 1, 1));
        athlete.setAge(29);
        athlete.setGender(Gender.MALE);
        athlete.setBeltRank(BeltRank.BLUE);
        athlete.setWeight(75.0);
        athlete.setEmail("athlete@example.com");
    }

    // ============ CREATE DIVISION TESTS ============

    @Test
    void testCreateDivision_WithValidData_ShouldReturn201Created() throws Exception {
        // Given
        when(divisionService.createDivision(anyLong(), any(DivisionCreateDTO.class)))
            .thenReturn(divisionResponseDTO);

        // When/Then
        mockMvc.perform(post("/api/tournaments/1/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Adult Male Blue Belt Light"))
            .andExpect(jsonPath("$.beltRank").value("BLUE"))
            .andExpect(jsonPath("$.ageCategory").value("ADULT"))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.bracketType").value("SINGLE_ELIMINATION"));

        verify(divisionService, times(1)).createDivision(eq(1L), any(DivisionCreateDTO.class));
    }

    @Test
    void testCreateDivision_WithInvalidData_ShouldReturn400BadRequest() throws Exception {
        // Given - Invalid DTO (missing required fields)
        DivisionCreateDTO invalidDTO = new DivisionCreateDTO();
        // Missing belt rank, age category, etc.

        // When/Then
        mockMvc.perform(post("/api/tournaments/1/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
            .andExpect(status().isBadRequest());

        verify(divisionService, never()).createDivision(anyLong(), any(DivisionCreateDTO.class));
    }

    @Test
    void testCreateDivision_WithNonExistentTournament_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.createDivision(anyLong(), any(DivisionCreateDTO.class)))
            .thenThrow(new IllegalArgumentException("Tournament not found"));

        // When/Then
        mockMvc.perform(post("/api/tournaments/999/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).createDivision(eq(999L), any(DivisionCreateDTO.class));
    }

    // ============ GET DIVISION TESTS ============

    @Test
    void testGetDivisionById_WithValidId_ShouldReturn200Ok() throws Exception {
        // Given
        when(divisionService.getDivisionById(1L)).thenReturn(divisionResponseDTO);

        // When/Then
        mockMvc.perform(get("/api/divisions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Adult Male Blue Belt Light"))
            .andExpect(jsonPath("$.athleteCount").value(0));

        verify(divisionService, times(1)).getDivisionById(1L);
    }

    @Test
    void testGetDivisionById_WithInvalidId_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.getDivisionById(anyLong()))
            .thenThrow(new IllegalArgumentException("Division not found"));

        // When/Then
        mockMvc.perform(get("/api/divisions/999"))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).getDivisionById(999L);
    }

    @Test
    void testGetDivisionsByTournament_ShouldReturnList() throws Exception {
        // Given
        List<DivisionResponseDTO> divisions = Arrays.asList(divisionResponseDTO);
        when(divisionService.getDivisionsByTournament(1L)).thenReturn(divisions);

        // When/Then
        mockMvc.perform(get("/api/tournaments/1/divisions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Adult Male Blue Belt Light"));

        verify(divisionService, times(1)).getDivisionsByTournament(1L);
    }

    // ============ UPDATE DIVISION TESTS ============

    @Test
    void testUpdateDivision_WithValidData_ShouldReturn200Ok() throws Exception {
        // Given
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.ROUND_ROBIN);

        divisionResponseDTO.setBracketType(BracketType.ROUND_ROBIN);
        when(divisionService.updateDivision(eq(1L), any(DivisionUpdateDTO.class)))
            .thenReturn(divisionResponseDTO);

        // When/Then
        mockMvc.perform(put("/api/divisions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.bracketType").value("ROUND_ROBIN"));

        verify(divisionService, times(1)).updateDivision(eq(1L), any(DivisionUpdateDTO.class));
    }

    @Test
    void testUpdateDivision_AfterMatchesGenerated_ShouldReturn400() throws Exception {
        // Given
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.ROUND_ROBIN);

        when(divisionService.updateDivision(eq(1L), any(DivisionUpdateDTO.class)))
            .thenThrow(new IllegalStateException("Cannot update division after matches have been generated"));

        // When/Then
        mockMvc.perform(put("/api/divisions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isBadRequest());

        verify(divisionService, times(1)).updateDivision(eq(1L), any(DivisionUpdateDTO.class));
    }

    // ============ DELETE DIVISION TESTS ============

    @Test
    void testDeleteDivision_WithValidId_ShouldReturn204NoContent() throws Exception {
        // Given
        doNothing().when(divisionService).deleteDivision(1L);

        // When/Then
        mockMvc.perform(delete("/api/divisions/1"))
            .andExpect(status().isNoContent());

        verify(divisionService, times(1)).deleteDivision(1L);
    }

    @Test
    void testDeleteDivision_AfterMatchesGenerated_ShouldReturn400() throws Exception {
        // Given
        doThrow(new IllegalStateException("Cannot delete division after matches have been generated"))
            .when(divisionService).deleteDivision(1L);

        // When/Then
        mockMvc.perform(delete("/api/divisions/1"))
            .andExpect(status().isBadRequest());

        verify(divisionService, times(1)).deleteDivision(1L);
    }

    // ============ ENROLL ATHLETE TESTS ============

    @Test
    void testEnrollAthlete_WithValidData_ShouldReturn200Ok() throws Exception {
        // Given
        divisionResponseDTO.setAthleteCount(1);
        when(divisionService.enrollAthlete(1L, 1L)).thenReturn(divisionResponseDTO);

        // When/Then
        mockMvc.perform(post("/api/divisions/1/athletes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.athleteCount").value(1));

        verify(divisionService, times(1)).enrollAthlete(1L, 1L);
    }

    @Test
    void testEnrollAthlete_WithInvalidAthlete_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.enrollAthlete(1L, 999L))
            .thenThrow(new IllegalArgumentException("Athlete not found"));

        // When/Then
        mockMvc.perform(post("/api/divisions/1/athletes/999"))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).enrollAthlete(1L, 999L);
    }

    @Test
    void testEnrollAthlete_WithAlreadyEnrolledAthlete_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.enrollAthlete(1L, 1L))
            .thenThrow(new IllegalArgumentException("Athlete is already enrolled"));

        // When/Then
        mockMvc.perform(post("/api/divisions/1/athletes/1"))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).enrollAthlete(1L, 1L);
    }

    @Test
    void testEnrollAthlete_WithIneligibleAthlete_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.enrollAthlete(1L, 1L))
            .thenThrow(new IllegalArgumentException("Athlete belt rank does not match"));

        // When/Then
        mockMvc.perform(post("/api/divisions/1/athletes/1"))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).enrollAthlete(1L, 1L);
    }

    // ============ REMOVE ATHLETE TESTS ============

    @Test
    void testRemoveAthlete_WithValidData_ShouldReturn200Ok() throws Exception {
        // Given
        when(divisionService.removeAthlete(1L, 1L)).thenReturn(divisionResponseDTO);

        // When/Then
        mockMvc.perform(delete("/api/divisions/1/athletes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L));

        verify(divisionService, times(1)).removeAthlete(1L, 1L);
    }

    @Test
    void testRemoveAthlete_WithNotEnrolledAthlete_ShouldReturn409() throws Exception {
        // Given
        when(divisionService.removeAthlete(1L, 1L))
            .thenThrow(new IllegalArgumentException("Athlete is not enrolled"));

        // When/Then
        mockMvc.perform(delete("/api/divisions/1/athletes/1"))
            .andExpect(status().isConflict());

        verify(divisionService, times(1)).removeAthlete(1L, 1L);
    }

    // ============ GET ATHLETES BY DIVISION TESTS ============

    @Test
    void testGetAthletesByDivision_ShouldReturnList() throws Exception {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete);
        when(divisionService.getAthletesByDivision(1L)).thenReturn(athletes);

        // When/Then
        mockMvc.perform(get("/api/divisions/1/athletes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Test Athlete"));

        verify(divisionService, times(1)).getAthletesByDivision(1L);
    }

    // ============ GET DIVISIONS READY FOR MATCH GENERATION TESTS ============

    @Test
    void testGetDivisionsReadyForMatchGeneration_ShouldReturnList() throws Exception {
        // Given
        divisionResponseDTO.setAthleteCount(3);
        List<DivisionResponseDTO> divisions = Arrays.asList(divisionResponseDTO);
        when(divisionService.getDivisionsReadyForMatchGeneration()).thenReturn(divisions);

        // When/Then
        mockMvc.perform(get("/api/divisions/ready-for-matches"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].athleteCount").value(3));

        verify(divisionService, times(1)).getDivisionsReadyForMatchGeneration();
    }
}