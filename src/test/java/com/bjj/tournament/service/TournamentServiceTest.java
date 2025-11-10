package com.bjj.tournament.service;

import com.bjj.tournament.dto.TournamentCreateDTO;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.repository.TournamentRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for TournamentService
 */
@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {
    
    @Mock
    private TournamentRepository tournamentRepository;
    
    @InjectMocks
    private TournamentService tournamentService;
    
    private TournamentCreateDTO validTournamentDTO;
    private Tournament savedTournament;
    
    @BeforeEach
    void setUp() {
        validTournamentDTO = new TournamentCreateDTO();
        validTournamentDTO.setName("Test Tournament");
        validTournamentDTO.setLocation("Test Arena");
        validTournamentDTO.setTournamentDate(LocalDate.now().plusMonths(2));
        validTournamentDTO.setRegistrationDeadline(LocalDate.now().plusMonths(1));
        validTournamentDTO.setOrganizer("Test Org");
        validTournamentDTO.setContactEmail("contact@test.com");
        
        savedTournament = new Tournament();
        savedTournament.setId(1L);
        savedTournament.setName("Test Tournament");
        savedTournament.setLocation("Test Arena");
        savedTournament.setTournamentDate(LocalDate.now().plusMonths(2));
        savedTournament.setRegistrationOpen(true);
        savedTournament.setStarted(false);
        savedTournament.setCompleted(false);
    }
    
    @Test
    void testCreateTournament_WithValidData_ShouldSucceed() {
        // Given
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);
        
        // When
        Tournament result = tournamentService.createTournament(validTournamentDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Tournament");
        
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }
    
    @Test
    void testCreateTournament_WithPastDate_ShouldThrowException() {
        // Given
        validTournamentDTO.setTournamentDate(LocalDate.now().minusDays(1));
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.createTournament(validTournamentDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be in the future");
        
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }
    
    @Test
    void testCreateTournament_WithInvalidDeadline_ShouldThrowException() {
        // Given
        validTournamentDTO.setRegistrationDeadline(
            validTournamentDTO.getTournamentDate().plusDays(1));
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.createTournament(validTournamentDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("before tournament date");
        
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }
    
    @Test
    void testGetTournamentById_WhenExists_ShouldReturnTournament() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        
        // When
        Tournament result = tournamentService.getTournamentById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(tournamentRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetTournamentById_WhenNotExists_ShouldThrowException() {
        // Given
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.getTournamentById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
        
        verify(tournamentRepository, times(1)).findById(999L);
    }
    
    @Test
    void testGetAllTournaments_ShouldReturnList() {
        // Given
        List<Tournament> tournaments = Arrays.asList(savedTournament, new Tournament());
        when(tournamentRepository.findAll()).thenReturn(tournaments);
        
        // When
        List<Tournament> result = tournamentService.getAllTournaments();
        
        // Then
        assertThat(result).hasSize(2);
        
        verify(tournamentRepository, times(1)).findAll();
    }
    
    @Test
    void testGetUpcomingTournaments_ShouldReturnList() {
        // Given
        List<Tournament> tournaments = Arrays.asList(savedTournament);
        when(tournamentRepository.findUpcomingTournaments(any(LocalDate.class)))
            .thenReturn(tournaments);
        
        // When
        List<Tournament> result = tournamentService.getUpcomingTournaments();
        
        // Then
        assertThat(result).hasSize(1);
        
        verify(tournamentRepository, times(1))
            .findUpcomingTournaments(any(LocalDate.class));
    }
    
    @Test
    void testCloseRegistration_ShouldUpdateStatus() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);
        
        // When
        Tournament result = tournamentService.closeRegistration(1L);
        
        // Then
        assertThat(result.getRegistrationOpen()).isFalse();
        
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }
    
    @Test
    void testStartTournament_WhenNotStarted_ShouldSucceed() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);
        
        // When
        Tournament result = tournamentService.startTournament(1L);
        
        // Then
        assertThat(result.getStarted()).isTrue();
        assertThat(result.getRegistrationOpen()).isFalse();
        
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }
    
    @Test
    void testStartTournament_WhenAlreadyStarted_ShouldThrowException() {
        // Given
        savedTournament.setStarted(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.startTournament(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already started");
        
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }
    
    @Test
    void testCompleteTournament_WhenStarted_ShouldSucceed() {
        // Given
        savedTournament.setStarted(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);
        
        // When
        Tournament result = tournamentService.completeTournament(1L);
        
        // Then
        assertThat(result.getCompleted()).isTrue();
        
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }
    
    @Test
    void testCompleteTournament_WhenNotStarted_ShouldThrowException() {
        // Given
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.completeTournament(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not started");
        
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }
    
    @Test
    void testCompleteTournament_WhenAlreadyCompleted_ShouldThrowException() {
        // Given
        savedTournament.setStarted(true);
        savedTournament.setCompleted(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(savedTournament));
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.completeTournament(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already completed");
        
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }
    
    @Test
    void testDeleteTournament_WhenExists_ShouldSucceed() {
        // Given
        when(tournamentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(tournamentRepository).deleteById(1L);
        
        // When
        tournamentService.deleteTournament(1L);
        
        // Then
        verify(tournamentRepository, times(1)).existsById(1L);
        verify(tournamentRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void testDeleteTournament_WhenNotExists_ShouldThrowException() {
        // Given
        when(tournamentRepository.existsById(999L)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> tournamentService.deleteTournament(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
        
        verify(tournamentRepository, never()).deleteById(anyLong());
    }
}
