package com.bjj.tournament.service;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.repository.AthleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AthleteService
 * Uses Mockito to mock repository dependencies
 */
@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {
    
    @Mock
    private AthleteRepository athleteRepository;
    
    @InjectMocks
    private AthleteService athleteService;
    
    private AthleteRegistrationDTO validRegistrationDTO;
    private Athlete savedAthlete;
    
    @BeforeEach
    void setUp() {
        validRegistrationDTO = new AthleteRegistrationDTO();
        validRegistrationDTO.setName("Test Athlete");
        validRegistrationDTO.setDateOfBirth(LocalDate.of(1995, 6, 15));
        validRegistrationDTO.setGender(Gender.MALE);
        validRegistrationDTO.setBeltRank(BeltRank.BLUE);
        validRegistrationDTO.setWeight(75.0);
        validRegistrationDTO.setEmail("test@example.com");
        validRegistrationDTO.setTeam("Test Team");
        validRegistrationDTO.setCoachName("Test Coach");
        validRegistrationDTO.setPhone("+1234567890");
        
        savedAthlete = new Athlete();
        savedAthlete.setId(1L);
        savedAthlete.setName("Test Athlete");
        savedAthlete.setDateOfBirth(LocalDate.of(1995, 6, 15));
        savedAthlete.setAge(29);
        savedAthlete.setGender(Gender.MALE);
        savedAthlete.setBeltRank(BeltRank.BLUE);
        savedAthlete.setWeight(75.0);
        savedAthlete.setEmail("test@example.com");
    }
    
    @Test
    void testRegisterAthlete_WithValidData_ShouldSucceed() {
        // Given
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenReturn(savedAthlete);
        
        // When
        Athlete result = athleteService.registerAthlete(validRegistrationDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Athlete");
        
        verify(athleteRepository, times(1)).existsByEmail("test@example.com");
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }
    
    @Test
    void testRegisterAthlete_WithDuplicateEmail_ShouldThrowException() {
        // Given
        when(athleteRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> athleteService.registerAthlete(validRegistrationDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
        
        verify(athleteRepository, times(1)).existsByEmail("test@example.com");
        verify(athleteRepository, never()).save(any(Athlete.class));
    }
    
    @Test
    void testRegisterAthlete_WithKidUnder10_ShouldSetGenderNotApplicable() {
        // Given
        validRegistrationDTO.setDateOfBirth(LocalDate.now().minusYears(8));
        validRegistrationDTO.setGender(null);
        
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(invocation -> {
            Athlete athlete = invocation.getArgument(0);
            athlete.setId(1L);
            return athlete;
        });
        
        // When
        Athlete result = athleteService.registerAthlete(validRegistrationDTO);
        
        // Then
        assertThat(result.getGender()).isEqualTo(Gender.NOT_APPLICABLE);
        assertThat(result.getAge()).isLessThan(10);
        
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }
    
    @Test
    void testRegisterAthlete_WithAthleteUnder4_ShouldThrowException() {
        // Given
        validRegistrationDTO.setDateOfBirth(LocalDate.now().minusYears(3));
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> athleteService.registerAthlete(validRegistrationDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 4 years old");
        
        verify(athleteRepository, never()).save(any(Athlete.class));
    }
    
    @Test
    void testRegisterAthlete_WithKidAndAdultBelt_ShouldThrowException() {
        // Given
        validRegistrationDTO.setDateOfBirth(LocalDate.now().minusYears(12));
        validRegistrationDTO.setBeltRank(BeltRank.PURPLE); // Adult belt for kid
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> athleteService.registerAthlete(validRegistrationDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid belt rank for age");
        
        verify(athleteRepository, never()).save(any(Athlete.class));
    }
    
    @Test
    void testGetAthleteById_WhenExists_ShouldReturnAthlete() {
        // Given
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(savedAthlete));
        
        // When
        Athlete result = athleteService.getAthleteById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(athleteRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetAthleteById_WhenNotExists_ShouldThrowException() {
        // Given
        when(athleteRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> athleteService.getAthleteById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
        
        verify(athleteRepository, times(1)).findById(999L);
    }
    
    @Test
    void testGetAllAthletes_ShouldReturnList() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete, new Athlete());
        when(athleteRepository.findAll()).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.getAllAthletes();
        
        // Then
        assertThat(result).hasSize(2);
        
        verify(athleteRepository, times(1)).findAll();
    }
    
    @Test
    void testGetAthletesByBeltRank_ShouldReturnFilteredList() {
        // Given
        List<Athlete> bluebelts = Arrays.asList(savedAthlete);
        when(athleteRepository.findByBeltRank(BeltRank.BLUE)).thenReturn(bluebelts);
        
        // When
        List<Athlete> result = athleteService.getAthletesByBeltRank(BeltRank.BLUE);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBeltRank()).isEqualTo(BeltRank.BLUE);
        
        verify(athleteRepository, times(1)).findByBeltRank(BeltRank.BLUE);
    }
    
    @Test
    void testGetAthletesByAgeRange_ShouldReturnFilteredList() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete);
        when(athleteRepository.findByAgeBetween(25, 35)).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.getAthletesByAgeRange(25, 35);
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(athleteRepository, times(1)).findByAgeBetween(25, 35);
    }
    
    @Test
    void testGetAthletesForDivision_WithGenderNotApplicable_ShouldNotFilterByGender() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete);
        when(athleteRepository.findByBeltRankAndAgeBetween(
            BeltRank.GREY, 4, 9)).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.getAthletesForDivision(
            BeltRank.GREY, Gender.NOT_APPLICABLE, 4, 9);
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(athleteRepository, times(1))
            .findByBeltRankAndAgeBetween(BeltRank.GREY, 4, 9);
        verify(athleteRepository, never())
            .findByBeltRankAndGenderAndAgeBetween(any(), any(), anyInt(), anyInt());
    }
    
    @Test
    void testGetAthletesForDivision_WithGender_ShouldFilterByGender() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete);
        when(athleteRepository.findByBeltRankAndGenderAndAgeBetween(
            BeltRank.BLUE, Gender.MALE, 18, 29)).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.getAthletesForDivision(
            BeltRank.BLUE, Gender.MALE, 18, 29);
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(athleteRepository, times(1))
            .findByBeltRankAndGenderAndAgeBetween(BeltRank.BLUE, Gender.MALE, 18, 29);
    }
    
    @Test
    void testSearchAthletesByName_ShouldReturnResults() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete);
        when(athleteRepository.searchByName("Test")).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.searchAthletesByName("Test");
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(athleteRepository, times(1)).searchByName("Test");
    }
    
    @Test
    void testGetAthletesByTeam_ShouldReturnTeamMembers() {
        // Given
        List<Athlete> athletes = Arrays.asList(savedAthlete);
        when(athleteRepository.findByTeam("Test Team")).thenReturn(athletes);
        
        // When
        List<Athlete> result = athleteService.getAthletesByTeam("Test Team");
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(athleteRepository, times(1)).findByTeam("Test Team");
    }
    
    @Test
    void testUpdateAthlete_WithValidData_ShouldSucceed() {
        // Given
        AthleteRegistrationDTO updateDTO = new AthleteRegistrationDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setWeight(80.0);
        
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(savedAthlete));
        when(athleteRepository.save(any(Athlete.class))).thenReturn(savedAthlete);
        
        // When
        Athlete result = athleteService.updateAthlete(1L, updateDTO);
        
        // Then
        assertThat(result).isNotNull();
        
        verify(athleteRepository, times(1)).findById(1L);
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }
    
    @Test
    void testDeleteAthlete_WhenExists_ShouldSucceed() {
        // Given
        when(athleteRepository.existsById(1L)).thenReturn(true);
        doNothing().when(athleteRepository).deleteById(1L);
        
        // When
        athleteService.deleteAthlete(1L);
        
        // Then
        verify(athleteRepository, times(1)).existsById(1L);
        verify(athleteRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void testDeleteAthlete_WhenNotExists_ShouldThrowException() {
        // Given
        when(athleteRepository.existsById(999L)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> athleteService.deleteAthlete(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
        
        verify(athleteRepository, times(1)).existsById(999L);
        verify(athleteRepository, never()).deleteById(anyLong());
    }
}
