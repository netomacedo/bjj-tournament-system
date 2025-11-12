package com.bjj.tournament;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.repository.AthleteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the BJJ Tournament System
 * Tests the full stack: Controller → Service → Repository → Database
 * 
 * @SpringBootTest loads the complete application context
 * @AutoConfigureMockMvc configures MockMvc for testing REST endpoints
 * @Transactional ensures each test rolls back database changes
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test") // Use 'test' profile for testing with H2 DB
class TournamentApplicationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AthleteRepository athleteRepository;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        athleteRepository.deleteAll();
    }
    
    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
        assertThat(mockMvc).isNotNull();
        assertThat(athleteRepository).isNotNull();
    }
    
    @Test
    void testCompleteAthleteRegistrationFlow() throws Exception {
        // Given - Athlete registration data
        AthleteRegistrationDTO registrationDTO = new AthleteRegistrationDTO();
        registrationDTO.setName("Integration Test Athlete");
        registrationDTO.setDateOfBirth(LocalDate.of(1995, 6, 15));
        registrationDTO.setGender(Gender.MALE);
        registrationDTO.setBeltRank(BeltRank.BLUE);
        registrationDTO.setWeight(75.0);
        registrationDTO.setEmail("integration@test.com");
        registrationDTO.setTeam("Integration Team");
        registrationDTO.setCoachName("Integration Coach");
        registrationDTO.setPhone("+1234567890");
        
        // When - Register athlete via API
        String response = mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Integration Test Athlete"))
            .andExpect(jsonPath("$.email").value("integration@test.com"))
            .andExpect(jsonPath("$.age").value(greaterThanOrEqualTo(28)))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Parse response to get athlete ID
        Athlete createdAthlete = objectMapper.readValue(response, Athlete.class);
        Long athleteId = createdAthlete.getId();
        
        // Then - Verify athlete is in database
        Athlete savedAthlete = athleteRepository.findById(athleteId).orElse(null);
        assertThat(savedAthlete).isNotNull();
        assertThat(savedAthlete.getName()).isEqualTo("Integration Test Athlete");
        assertThat(savedAthlete.getGender()).isEqualTo(Gender.MALE);
        
        // When - Retrieve athlete via API
        mockMvc.perform(get("/api/athletes/" + athleteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Integration Test Athlete"))
            .andExpect(jsonPath("$.beltRank").value("BLUE"));
        
        // When - Update athlete
        registrationDTO.setWeight(80.0);
        mockMvc.perform(put("/api/athletes/" + athleteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDTO)))
            .andExpect(status().isOk());
        
        // Then - Verify update in database
        Athlete updatedAthlete = athleteRepository.findById(athleteId).orElse(null);
        assertThat(updatedAthlete).isNotNull();
        assertThat(updatedAthlete.getWeight()).isEqualTo(80.0);
        
        // When - Search for athlete
        mockMvc.perform(get("/api/athletes/search")
                .param("name", "Integration"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Integration Test Athlete"));
        
        // When - Delete athlete
        mockMvc.perform(delete("/api/athletes/" + athleteId))
            .andExpect(status().isNoContent());
        
        // Then - Verify deletion
        assertThat(athleteRepository.findById(athleteId)).isEmpty();
    }
    
    @Test
    void testMultipleAthletesRegistrationAndFiltering() throws Exception {
        // Register multiple athletes
        for (int i = 1; i <= 3; i++) {
            AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
            dto.setName("Athlete " + i);
            dto.setDateOfBirth(LocalDate.of(1990 + i, 1, 1));
            dto.setGender(i % 2 == 0 ? Gender.FEMALE : Gender.MALE);
            dto.setBeltRank(i == 1 ? BeltRank.BLUE : BeltRank.PURPLE);
            dto.setWeight(70.0 + i);
            dto.setEmail("athlete" + i + "@test.com");
            dto.setTeam("Team " + (i % 2 == 0 ? "A" : "B"));
            
            mockMvc.perform(post("/api/athletes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
        }
        
        // Verify all athletes were created
        mockMvc.perform(get("/api/athletes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
        
        // Filter by belt rank
        mockMvc.perform(get("/api/athletes/belt/BLUE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].beltRank").value("BLUE"));
        
        // Filter by team
        mockMvc.perform(get("/api/athletes/team/Team A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }
    
    @Test
    void testKidAthleteUnder10_AutoGenderHandling() throws Exception {
        // Given - Kid under 10 without gender
        AthleteRegistrationDTO kidDTO = new AthleteRegistrationDTO();
        kidDTO.setName("Little Champion");
        kidDTO.setDateOfBirth(LocalDate.now().minusYears(8));
        kidDTO.setBeltRank(BeltRank.GREY);
        kidDTO.setWeight(30.0);
        kidDTO.setEmail("kid@test.com");
        
        // When - Register kid
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(kidDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gender").value("NOT_APPLICABLE"))
            .andExpect(jsonPath("$.age").value(lessThan(10)));
    }
    
    @Test
    void testDuplicateEmailRejection() throws Exception {
        // Given - First athlete
        AthleteRegistrationDTO dto1 = createValidAthleteDTO("athlete1@test.com");
        
        // When - Register first athlete
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
            .andExpect(status().isCreated());
        
        // When - Try to register athlete with same email
        AthleteRegistrationDTO dto2 = createValidAthleteDTO("athlete1@test.com");
        dto2.setName("Different Name");
        
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isConflict()) // 409 - much better than 500!
                .andExpect(jsonPath("$.message").value("Athlete with email athlete1@test.com already exists")); // Business logic exception
    }
    
    @Test
    void testInvalidDataRejection() throws Exception {
        // Test invalid email
        AthleteRegistrationDTO invalidEmail = createValidAthleteDTO("test@test.com");
        invalidEmail.setEmail("invalid-email");
        
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)))
            .andExpect(status().isBadRequest());
        
        // Test missing required fields
        AthleteRegistrationDTO missingFields = new AthleteRegistrationDTO();
        missingFields.setName("Test");
        
        mockMvc.perform(post("/api/athletes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingFields)))
            .andExpect(status().isBadRequest());
    }
    
    private AthleteRegistrationDTO createValidAthleteDTO(String email) {
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Test Athlete");
        dto.setDateOfBirth(LocalDate.of(1995, 6, 15));
        dto.setGender(Gender.MALE);
        dto.setBeltRank(BeltRank.BLUE);
        dto.setWeight(75.0);
        dto.setEmail(email);
        return dto;
    }
}
