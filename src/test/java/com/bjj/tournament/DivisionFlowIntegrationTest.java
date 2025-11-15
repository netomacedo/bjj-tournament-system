package com.bjj.tournament;

import com.bjj.tournament.dto.*;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.*;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.TournamentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Division management flow
 * Tests the complete tournament setup: Tournament → Division → Athletes → Matches
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class DivisionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    private Tournament tournament;
    private Athlete athlete1;
    private Athlete athlete2;
    private Athlete athlete3;

    @BeforeEach
    void setUp() {
        // Clean database
        divisionRepository.deleteAll();
        athleteRepository.deleteAll();
        tournamentRepository.deleteAll();

        // Create test tournament
        tournament = new Tournament();
        tournament.setName("Test Championship");
        tournament.setLocation("Los Angeles");
        tournament.setTournamentDate(LocalDate.now().plusDays(30));
        tournament.setRegistrationDeadline(LocalDate.now().plusDays(15));
        tournament.setRegistrationOpen(true);
        tournament = tournamentRepository.save(tournament);

        // Create test athletes
        athlete1 = createAthlete("John Silva", BeltRank.BLUE, Gender.MALE, 75.0, 29, "john@test.com");
        athlete2 = createAthlete("Mike Jones", BeltRank.BLUE, Gender.MALE, 74.0, 28, "mike@test.com");
        athlete3 = createAthlete("Tom Brown", BeltRank.BLUE, Gender.MALE, 76.0, 27, "tom@test.com");
    }

    private Athlete createAthlete(String name, BeltRank beltRank, Gender gender,
                                  double weight, int age, String email) {
        Athlete athlete = new Athlete();
        athlete.setName(name);
        athlete.setDateOfBirth(LocalDate.now().minusYears(age));
        athlete.setAge(age);
        athlete.setGender(gender);
        athlete.setBeltRank(beltRank);
        athlete.setWeight(weight);
        athlete.setEmail(email);
        return athleteRepository.save(athlete);
    }

    @Test
    void testCompleteDivisionWorkflow_CreateDivisionAndEnrollAthletes() throws Exception {
        // Step 1: Create Division
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.BLUE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        MvcResult createResult = mockMvc.perform(post("/api/tournaments/" + tournament.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(containsString("Adult")))
            .andExpect(jsonPath("$.name").value(containsString("Male")))
            .andExpect(jsonPath("$.name").value(containsString("Blue")))
            .andExpect(jsonPath("$.athleteCount").value(0))
            .andExpect(jsonPath("$.matchesGenerated").value(false))
            .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        DivisionResponseDTO divisionResponse = objectMapper.readValue(responseJson, DivisionResponseDTO.class);
        Long divisionId = divisionResponse.getId();

        // Step 2: Enroll Athletes
        mockMvc.perform(post("/api/divisions/" + divisionId + "/athletes/" + athlete1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.athleteCount").value(1));

        mockMvc.perform(post("/api/divisions/" + divisionId + "/athletes/" + athlete2.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.athleteCount").value(2));

        mockMvc.perform(post("/api/divisions/" + divisionId + "/athletes/" + athlete3.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.athleteCount").value(3));

        // Step 3: Verify athletes are enrolled
        mockMvc.perform(get("/api/divisions/" + divisionId + "/athletes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].name", containsInAnyOrder("John Silva", "Mike Jones", "Tom Brown")));

        // Step 4: Verify division appears in ready-for-matches list
        mockMvc.perform(get("/api/divisions/ready-for-matches"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(divisionId));

        // Step 5: Verify in database
        Division savedDivision = divisionRepository.findById(divisionId).orElseThrow();
        assertThat(savedDivision.getAthletes()).hasSize(3);
        assertThat(savedDivision.getMatchesGenerated()).isFalse();
    }

    @Test
    void testDivisionCreation_PreventsDuplicates() throws Exception {
        // Given - First division created successfully
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.PURPLE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_MIDDLE);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        mockMvc.perform(post("/api/tournaments/" + tournament.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated());

        // When - Try to create duplicate division
        // Then - Should fail with conflict error
        mockMvc.perform(post("/api/tournaments/" + tournament.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isConflict());
    }

    @Test
    void testAthleteEnrollment_ValidatesEligibility() throws Exception {
        // Given - Create division
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.PURPLE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        MvcResult result = mockMvc.perform(post("/api/tournaments/" + tournament.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andReturn();

        DivisionResponseDTO divisionResponse = objectMapper.readValue(
            result.getResponse().getContentAsString(), DivisionResponseDTO.class);

        // When - Try to enroll athlete with wrong belt rank (BLUE instead of PURPLE)
        // Then - Should fail with conflict error
        mockMvc.perform(post("/api/divisions/" + divisionResponse.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isConflict());
    }

    @Test
    void testAthleteEnrollment_PreventsDuplicates() throws Exception {
        // Given - Division with one athlete enrolled
        Division division = createDivisionWithAthletes(0);
        division = divisionRepository.save(division);

        mockMvc.perform(post("/api/divisions/" + division.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isOk());

        // When - Try to enroll same athlete again
        // Then - Should fail
        mockMvc.perform(post("/api/divisions/" + division.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isConflict());
    }

    @Test
    void testDivisionUpdate_AllowsBracketTypeChange() throws Exception {
        // Given - Division created
        Division division = createDivisionWithAthletes(0);
        division = divisionRepository.save(division);

        // When - Update bracket type
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.ROUND_ROBIN);

        mockMvc.perform(put("/api/divisions/" + division.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bracketType").value("ROUND_ROBIN"));

        // Then - Verify in database
        Division updated = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(updated.getBracketType()).isEqualTo(BracketType.ROUND_ROBIN);
    }

    @Test
    void testDivisionUpdate_BlockedAfterMatchGeneration() throws Exception {
        // Given - Division with matches generated
        Division division = createDivisionWithAthletes(2);
        division.setMatchesGenerated(true);
        division = divisionRepository.save(division);

        // When - Try to update bracket type
        DivisionUpdateDTO updateDTO = new DivisionUpdateDTO();
        updateDTO.setBracketType(BracketType.DOUBLE_ELIMINATION);

        // Then - Should fail
        mockMvc.perform(put("/api/divisions/" + division.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testDivisionDeletion_AllowedBeforeMatchGeneration() throws Exception {
        // Given - Division without matches
        Division division = createDivisionWithAthletes(0);
        division = divisionRepository.save(division);

        // When - Delete division
        mockMvc.perform(delete("/api/divisions/" + division.getId()))
            .andExpect(status().isNoContent());

        // Then - Verify deleted
        assertThat(divisionRepository.findById(division.getId())).isEmpty();
    }

    @Test
    void testDivisionDeletion_BlockedAfterMatchGeneration() throws Exception {
        // Given - Division with matches generated
        Division division = createDivisionWithAthletes(0);
        division.setMatchesGenerated(true);
        division = divisionRepository.save(division);

        // When - Try to delete
        // Then - Should fail
        mockMvc.perform(delete("/api/divisions/" + division.getId()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDivisionsByTournament_ReturnsAllDivisions() throws Exception {
        // Given - Multiple divisions for tournament
        Division division1 = createDivisionWithAthletes(0); // BLUE, ADULT
        divisionRepository.save(division1);

        Division division2 = new Division();
        division2.setTournament(tournament);
        division2.setBeltRank(BeltRank.PURPLE);
        division2.setAgeCategory(AgeCategory.ADULT);
        division2.setGender(Gender.MALE);
        division2.setWeightClass(WeightClass.ADULT_MALE_MIDDLE);
        division2.setBracketType(BracketType.SINGLE_ELIMINATION);
        divisionRepository.save(division2);

        Division division3 = new Division();
        division3.setTournament(tournament);
        division3.setBeltRank(BeltRank.BLUE);
        division3.setAgeCategory(AgeCategory.MASTER_1);
        division3.setGender(Gender.MALE);
        division3.setWeightClass(WeightClass.ADULT_MALE_HEAVY);
        division3.setBracketType(BracketType.ROUND_ROBIN);
        divisionRepository.save(division3);

        // When - Get all divisions
        mockMvc.perform(get("/api/tournaments/" + tournament.getId() + "/divisions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].beltRank",
                containsInAnyOrder("BLUE", "PURPLE", "BLUE")));
    }

    @Test
    void testRemoveAthlete_SuccessfullyRemoves() throws Exception {
        // Given - Division with enrolled athletes
        Division division = createDivisionWithAthletes(2);
        division = divisionRepository.save(division);

        // When - Remove one athlete
        mockMvc.perform(delete("/api/divisions/" + division.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.athleteCount").value(1));

        // Then - Verify in database
        Division updated = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(updated.getAthletes()).hasSize(1);
        assertThat(updated.getAthletes()).doesNotContain(athlete1);
    }

    @Test
    void testRemoveAthlete_BlockedAfterMatchGeneration() throws Exception {
        // Given - Division with matches generated
        Division division = createDivisionWithAthletes(2);
        division.setMatchesGenerated(true);
        division = divisionRepository.save(division);

        // When - Try to remove athlete
        // Then - Should fail
        mockMvc.perform(delete("/api/divisions/" + division.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testEnrollmentBlockedWhenRegistrationClosed() throws Exception {
        // Given - Tournament with closed registration
        tournament.setRegistrationOpen(false);
        tournament = tournamentRepository.save(tournament);

        Division division = createDivisionWithAthletes(0);
        division = divisionRepository.save(division);

        // When - Try to enroll athlete
        // Then - Should fail
        mockMvc.perform(post("/api/divisions/" + division.getId() + "/athletes/" + athlete1.getId()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMultipleTournaments_DivisionsIsolated() throws Exception {
        // Given - Two tournaments
        Tournament tournament2 = new Tournament();
        tournament2.setName("Second Tournament");
        tournament2.setLocation("New York");
        tournament2.setTournamentDate(LocalDate.now().plusDays(60));
        tournament2.setRegistrationOpen(true);
        tournament2 = tournamentRepository.save(tournament2);

        // Create division for tournament 1
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.BLUE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        mockMvc.perform(post("/api/tournaments/" + tournament.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated());

        // Create same division for tournament 2 (should succeed - different tournament)
        mockMvc.perform(post("/api/tournaments/" + tournament2.getId() + "/divisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated());

        // Verify both tournaments have their divisions
        List<Division> tournament1Divisions = divisionRepository.findByTournamentId(tournament.getId());
        List<Division> tournament2Divisions = divisionRepository.findByTournamentId(tournament2.getId());

        assertThat(tournament1Divisions).hasSize(1);
        assertThat(tournament2Divisions).hasSize(1);
        assertThat(tournament1Divisions.get(0).getId()).isNotEqualTo(tournament2Divisions.get(0).getId());
    }

    // Helper method to create a division with specified number of athletes
    private Division createDivisionWithAthletes(int athleteCount) {
        Division division = new Division();
        division.setTournament(tournament);
        division.setBeltRank(BeltRank.BLUE);
        division.setAgeCategory(AgeCategory.ADULT);
        division.setGender(Gender.MALE);
        division.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        division.setBracketType(BracketType.SINGLE_ELIMINATION);

        if (athleteCount >= 1) division.getAthletes().add(athlete1);
        if (athleteCount >= 2) division.getAthletes().add(athlete2);
        if (athleteCount >= 3) division.getAthletes().add(athlete3);

        return division;
    }
}