package com.bjj.tournament.controller;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.exception.AthleteNotFoundException;
import com.bjj.tournament.service.AthleteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AthleteController
 * Uses MockMvc to test REST endpoints
 */
@WebMvcTest(AthleteController.class)
class AthleteControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AthleteService athleteService;
    
    private Athlete athlete1;
    private Athlete athlete2;
    private AthleteRegistrationDTO registrationDTO;
    
    @BeforeEach
    void setUp() {
        athlete1 = new Athlete();
        athlete1.setId(1L);
        athlete1.setName("John Silva");
        athlete1.setDateOfBirth(LocalDate.of(1995, 5, 15));
        athlete1.setAge(29);
        athlete1.setGender(Gender.MALE);
        athlete1.setBeltRank(BeltRank.BLUE);
        athlete1.setWeight(75.0);
        athlete1.setEmail("john@test.com");
        athlete1.setTeam("Team Alpha");
        
        athlete2 = new Athlete();
        athlete2.setId(2L);
        athlete2.setName("Maria Santos");
        athlete2.setDateOfBirth(LocalDate.of(1998, 8, 20));
        athlete2.setAge(26);
        athlete2.setGender(Gender.FEMALE);
        athlete2.setBeltRank(BeltRank.PURPLE);
        athlete2.setWeight(60.0);
        athlete2.setEmail("maria@test.com");
        
        registrationDTO = new AthleteRegistrationDTO();
        registrationDTO.setName("Test Athlete");
        registrationDTO.setDateOfBirth(LocalDate.of(1995, 6, 15));
        registrationDTO.setGender(Gender.MALE);
        registrationDTO.setBeltRank(BeltRank.BLUE);
        registrationDTO.setWeight(75.0);
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPhone("+1234567890");
    }
    
    @Test
    void testRegisterAthlete_WithValidData_ShouldReturn201Created() throws Exception {
        // Given
        when(athleteService.registerAthlete(any(AthleteRegistrationDTO.class)))
            .thenReturn(athlete1);
        
        // When/Then
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("John Silva"))
            .andExpect(jsonPath("$.email").value("john@test.com"))
            .andExpect(jsonPath("$.beltRank").value("BLUE"));
        
        verify(athleteService, times(1)).registerAthlete(any(AthleteRegistrationDTO.class));
    }
    
    @Test
    void testRegisterAthlete_WithInvalidData_ShouldReturn400BadRequest() throws Exception {
        // Given - Invalid DTO (missing required fields)
        AthleteRegistrationDTO invalidDTO = new AthleteRegistrationDTO();
        invalidDTO.setName(""); // Empty name
        
        // When/Then
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
            .andExpect(status().isBadRequest());
        
        verify(athleteService, never()).registerAthlete(any(AthleteRegistrationDTO.class));
    }
    
    @Test
    void testGetAllAthletes_ShouldReturnList() throws Exception {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete1, athlete2);
        when(athleteService.getAllAthletes()).thenReturn(athletes);
        
        // When/Then
        mockMvc.perform(get("/api/athletes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("John Silva"))
            .andExpect(jsonPath("$[1].name").value("Maria Santos"));
        
        verify(athleteService, times(1)).getAllAthletes();
    }
    
    @Test
    void testGetAthleteById_WhenExists_ShouldReturn200Ok() throws Exception {
        // Given
        when(athleteService.getAthleteById(1L)).thenReturn(athlete1);
        
        // When/Then
        mockMvc.perform(get("/api/athletes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("John Silva"))
            .andExpect(jsonPath("$.email").value("john@test.com"));
        
        verify(athleteService, times(1)).getAthleteById(1L);
    }
    
    @Test
    void testGetAthleteById_WhenNotExists_ShouldReturn404NotFound() throws Exception {
        // Given
        when(athleteService.getAthleteById(999L))
            .thenThrow(new AthleteNotFoundException("Athlete not found"));
        
        // When/Then
        mockMvc.perform(get("/api/athletes/999"))
            .andExpect(status().isNotFound()); // Spring default behavior
        
        verify(athleteService, times(1)).getAthleteById(999L);
    }
    
    @Test
    void testGetAthletesByBeltRank_ShouldReturnFilteredList() throws Exception {
        // Given
        List<Athlete> bluebelts = Arrays.asList(athlete1);
        when(athleteService.getAthletesByBeltRank(BeltRank.BLUE)).thenReturn(bluebelts);
        
        // When/Then
        mockMvc.perform(get("/api/athletes/belt/BLUE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].beltRank").value("BLUE"));
        
        verify(athleteService, times(1)).getAthletesByBeltRank(BeltRank.BLUE);
    }
    
    @Test
    void testGetAthletesByAgeRange_ShouldReturnFilteredList() throws Exception {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete1, athlete2);
        when(athleteService.getAthletesByAgeRange(25, 35)).thenReturn(athletes);
        
        // When/Then
        mockMvc.perform(get("/api/athletes/age")
                .param("min", "25")
                .param("max", "35"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
        
        verify(athleteService, times(1)).getAthletesByAgeRange(25, 35);
    }
    
    @Test
    void testSearchAthletesByName_ShouldReturnResults() throws Exception {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete1);
        when(athleteService.searchAthletesByName("John")).thenReturn(athletes);
        
        // When/Then
        mockMvc.perform(get("/api/athletes/search")
                .param("name", "John"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("John Silva"));
        
        verify(athleteService, times(1)).searchAthletesByName("John");
    }
    
    @Test
    void testGetAthletesByTeam_ShouldReturnTeamMembers() throws Exception {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete1);
        when(athleteService.getAthletesByTeam("Team Alpha")).thenReturn(athletes);
        
        // When/Then
        mockMvc.perform(get("/api/athletes/team/Team Alpha"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].team").value("Team Alpha"));
        
        verify(athleteService, times(1)).getAthletesByTeam("Team Alpha");
    }
    
    @Test
    void testUpdateAthlete_WithValidData_ShouldReturn200Ok() throws Exception {
        // Given
        when(athleteService.updateAthlete(eq(1L), any(AthleteRegistrationDTO.class)))
            .thenReturn(athlete1);
        
        // When/Then
        mockMvc.perform(put("/api/athletes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L));
        
        verify(athleteService, times(1))
            .updateAthlete(eq(1L), any(AthleteRegistrationDTO.class));
    }
    
    @Test
    void testDeleteAthlete_ShouldReturn204NoContent() throws Exception {
        // Given
        doNothing().when(athleteService).deleteAthlete(1L);
        
        // When/Then
        mockMvc.perform(delete("/api/athletes/1"))
            .andExpect(status().isNoContent());
        
        verify(athleteService, times(1)).deleteAthlete(1L);
    }
    
    @Test
    void testDeleteAthlete_WhenNotExists_ShouldReturnError() throws Exception {
        // Given
        doThrow(new AthleteNotFoundException("Athlete not found"))
            .when(athleteService).deleteAthlete(999L);
        
        // When/Then
        mockMvc.perform(delete("/api/athletes/999"))
            .andExpect(status().isNotFound());
        
        verify(athleteService, times(1)).deleteAthlete(999L);
    }
    
    @Test
    void testRegisterAthlete_WithInvalidEmail_ShouldReturn400BadRequest() throws Exception {
        // Given
        registrationDTO.setEmail("invalid-email");
        
        // When/Then
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isBadRequest());
        
        verify(athleteService, never()).registerAthlete(any(AthleteRegistrationDTO.class));
    }
    
    @Test
    void testRegisterAthlete_WithInvalidPhoneNumber_ShouldReturn400BadRequest() throws Exception {
        // Given
        registrationDTO.setPhone("123"); // Too short
        
        // When/Then
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isBadRequest());
        
        verify(athleteService, never()).registerAthlete(any(AthleteRegistrationDTO.class));
    }
    
    @Test
    void testRegisterAthlete_WithFutureDateOfBirth_ShouldReturn400BadRequest() throws Exception {
        // Given
        registrationDTO.setDateOfBirth(LocalDate.now().plusYears(1));
        
        // When/Then
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isBadRequest());
        
        verify(athleteService, never()).registerAthlete(any(AthleteRegistrationDTO.class));
    }
}
