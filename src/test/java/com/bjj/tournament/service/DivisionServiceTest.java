package com.bjj.tournament.service;

import com.bjj.tournament.dto.DivisionCreateDTO;
import com.bjj.tournament.dto.DivisionResponseDTO;
import com.bjj.tournament.dto.DivisionUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.*;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DivisionService
 * Uses Mockito to mock repository dependencies
 */
@ExtendWith(MockitoExtension.class)
class DivisionServiceTest {

    @Mock
    private DivisionRepository divisionRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private DivisionService divisionService;

    private Tournament tournament;
    private Division division;
    private Athlete athlete;
    private DivisionCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        // Setup tournament
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Test Tournament");
        tournament.setRegistrationOpen(true);
        tournament.setStarted(false);
        tournament.setTournamentDate(LocalDate.now().plusDays(30));

        // Setup division
        division = new Division();
        division.setId(1L);
        division.setTournament(tournament);
        division.setBeltRank(BeltRank.BLUE);
        division.setAgeCategory(AgeCategory.ADULT);
        division.setGender(Gender.MALE);
        division.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        division.setBracketType(BracketType.SINGLE_ELIMINATION);
        division.setMatchesGenerated(false);
        division.setAthletes(new ArrayList<>());
        division.setMatches(new ArrayList<>());

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

        // Setup create DTO
        createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.BLUE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);
    }

    // ============ CREATE DIVISION TESTS ============

    @Test
    void testCreateDivision_WithValidData_ShouldSucceed() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            anyLong(), any(), any(), any())).thenReturn(Optional.empty());
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        // When
        DivisionResponseDTO result = divisionService.createDivision(1L, createDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBeltRank()).isEqualTo(BeltRank.BLUE);
        assertThat(result.getAgeCategory()).isEqualTo(AgeCategory.ADULT);
        assertThat(result.getGender()).isEqualTo(Gender.MALE);

        verify(tournamentRepository, times(1)).findById(1L);
        verify(divisionRepository, times(1)).save(any(Division.class));
    }

    @Test
    void testCreateDivision_WithNonExistentTournament_ShouldThrowException() {
        // Given
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> divisionService.createDivision(1L, createDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tournament not found");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testCreateDivision_WithClosedRegistration_ShouldThrowException() {
        // Given
        tournament.setRegistrationOpen(false);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        // When/Then
        assertThatThrownBy(() -> divisionService.createDivision(1L, createDTO))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("registration is closed");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testCreateDivision_WithStartedTournament_ShouldThrowException() {
        // Given
        tournament.setStarted(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        // When/Then
        assertThatThrownBy(() -> divisionService.createDivision(1L, createDTO))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot create divisions after tournament has started");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testCreateDivision_WithDuplicateCriteria_ShouldThrowException() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            anyLong(), any(), any(), any())).thenReturn(Optional.of(division));

        // When/Then
        assertThatThrownBy(() -> divisionService.createDivision(1L, createDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Division already exists");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    // ============ GET DIVISION TESTS ============

    @Test
    void testGetDivisionById_WithValidId_ShouldReturnDivision() {
        // Given
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When
        DivisionResponseDTO result = divisionService.getDivisionById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(divisionRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDivisionById_WithInvalidId_ShouldThrowException() {
        // Given
        when(divisionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> divisionService.getDivisionById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Division not found");
    }

    @Test
    void testGetDivisionsByTournament_WithValidTournamentId_ShouldReturnList() {
        // Given
        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(divisionRepository.findByTournamentId(1L)).thenReturn(Arrays.asList(division));

        // When
        List<DivisionResponseDTO> result = divisionService.getDivisionsByTournament(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(divisionRepository, times(1)).findByTournamentId(1L);
    }

    @Test
    void testGetDivisionsByTournament_WithInvalidTournamentId_ShouldThrowException() {
        // Given
        when(tournamentRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> divisionService.getDivisionsByTournament(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tournament not found");
    }

    // ============ UPDATE DIVISION TESTS ============

    @Test
    void testUpdateDivision_WithValidData_ShouldSucceed() {
        // Given
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.ROUND_ROBIN);
        updateDTO.setWeightClass(WeightClass.ADULT_MALE_MIDDLE);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        // When
        DivisionResponseDTO result = divisionService.updateDivision(1L, updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(divisionRepository, times(1)).save(any(Division.class));
    }

    @Test
    void testUpdateDivision_AfterMatchesGenerated_ShouldThrowException() {
        // Given
        division.setMatchesGenerated(true);
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.ROUND_ROBIN);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When/Then
        assertThatThrownBy(() -> divisionService.updateDivision(1L, updateDTO))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot update division after matches have been generated");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    // ============ DELETE DIVISION TESTS ============

    @Test
    void testDeleteDivision_WithValidId_ShouldSucceed() {
        // Given
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When
        divisionService.deleteDivision(1L);

        // Then
        verify(divisionRepository, times(1)).delete(division);
    }

    @Test
    void testDeleteDivision_AfterMatchesGenerated_ShouldThrowException() {
        // Given
        division.setMatchesGenerated(true);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When/Then
        assertThatThrownBy(() -> divisionService.deleteDivision(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete division after matches have been generated");

        verify(divisionRepository, never()).delete(any(Division.class));
    }

    // ============ ENROLL ATHLETE TESTS ============

    @Test
    void testEnrollAthlete_WithValidData_ShouldSucceed() {
        // Given
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        // When
        DivisionResponseDTO result = divisionService.enrollAthlete(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(divisionRepository, times(1)).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithClosedRegistration_ShouldThrowException() {
        // Given
        tournament.setRegistrationOpen(false);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tournament registration is closed");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_AfterMatchesGenerated_ShouldThrowException() {
        // Given
        division.setMatchesGenerated(true);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot enroll athletes after matches have been generated");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithAlreadyEnrolledAthlete_ShouldThrowException() {
        // Given
        division.getAthletes().add(athlete);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Athlete is already enrolled");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithWrongBeltRank_ShouldThrowException() {
        // Given
        athlete.setBeltRank(BeltRank.PURPLE);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("belt rank");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithWrongGender_ShouldThrowException() {
        // Given
        athlete.setGender(Gender.FEMALE);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("gender");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithWrongAge_ShouldThrowException() {
        // Given
        athlete.setAge(17); // Too young for ADULT category
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("age");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testEnrollAthlete_WithExcessiveWeight_ShouldThrowException() {
        // Given
        athlete.setWeight(80.0); // Over 76kg limit for LIGHT weight class
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.enrollAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("weight");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    // ============ REMOVE ATHLETE TESTS ============

    @Test
    void testRemoveAthlete_WithValidData_ShouldSucceed() {
        // Given
        division.getAthletes().add(athlete);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));
        when(divisionRepository.save(any(Division.class))).thenReturn(division);

        // When
        DivisionResponseDTO result = divisionService.removeAthlete(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(divisionRepository, times(1)).save(any(Division.class));
    }

    @Test
    void testRemoveAthlete_AfterMatchesGenerated_ShouldThrowException() {
        // Given
        division.setMatchesGenerated(true);
        division.getAthletes().add(athlete);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.removeAthlete(1L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot remove athletes after matches have been generated");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    @Test
    void testRemoveAthlete_WithNotEnrolledAthlete_ShouldThrowException() {
        // Given
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When/Then
        assertThatThrownBy(() -> divisionService.removeAthlete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Athlete is not enrolled");

        verify(divisionRepository, never()).save(any(Division.class));
    }

    // ============ GET ATHLETES BY DIVISION TESTS ============

    @Test
    void testGetAthletesByDivision_WithValidId_ShouldReturnList() {
        // Given
        division.getAthletes().add(athlete);
        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));

        // When
        List<Athlete> result = divisionService.getAthletesByDivision(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Athlete");
    }

    // ============ GET DIVISIONS READY FOR MATCH GENERATION TESTS ============

    @Test
    void testGetDivisionsReadyForMatchGeneration_ShouldReturnList() {
        // Given
        when(divisionRepository.findDivisionsReadyForMatchGeneration()).thenReturn(Arrays.asList(division));

        // When
        List<DivisionResponseDTO> result = divisionService.getDivisionsReadyForMatchGeneration();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(divisionRepository, times(1)).findDivisionsReadyForMatchGeneration();
    }
}