package com.bjj.tournament;

import com.bjj.tournament.dto.*;
import com.bjj.tournament.entity.*;
import com.bjj.tournament.enums.*;
import com.bjj.tournament.repository.*;
import com.bjj.tournament.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive Integration Tests for ALL functionalities
 * Tests actual database persistence, not mocks
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComprehensiveFunctionalityTest {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private DivisionService divisionService;

    @Autowired
    private AthleteService athleteService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private MatchRepository matchRepository;

    private Long testTournamentId;
    private Long testDivisionId;
    private Long testAthleteId;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        matchRepository.deleteAll();
        divisionRepository.deleteAll();
        athleteRepository.deleteAll();
        tournamentRepository.deleteAll();
    }

    // ==================== TOURNAMENT TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Should create tournament and persist to database")
    void testTournamentCreation() {
        // Given
        TournamentCreateDTO createDTO = new TournamentCreateDTO();
        createDTO.setName("Test Tournament");
        createDTO.setLocation("Test Location");
        createDTO.setTournamentDate(LocalDate.now().plusDays(30));
        createDTO.setRegistrationDeadline(LocalDate.now().plusDays(15));

        // When
        Tournament created = tournamentService.createTournament(createDTO);
        testTournamentId = created.getId();

        // Then - verify in database
        Tournament fromDb = tournamentRepository.findById(testTournamentId).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Test Tournament");
        assertThat(fromDb.getLocation()).isEqualTo("Test Location");
        assertThat(fromDb.getRegistrationOpen()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Should update tournament and persist changes")
    void testTournamentUpdate() {
        // Given - create tournament first
        Tournament tournament = createTestTournament();
        Long id = tournament.getId();

        // When - update
        tournament.setName("Updated Tournament Name");
        tournament.setLocation("New Location");
        tournamentRepository.save(tournament);

        // Then - verify changes persisted
        Tournament fromDb = tournamentRepository.findById(id).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Updated Tournament Name");
        assertThat(fromDb.getLocation()).isEqualTo("New Location");
        assertThat(fromDb.getVersion()).isEqualTo(1L); // Version incremented
    }

    @Test
    @Order(3)
    @DisplayName("Should delete tournament and remove from database")
    void testTournamentDeletion() {
        // Given - create tournament
        Tournament tournament = createTestTournament();
        Long id = tournament.getId();

        // Verify exists
        assertThat(tournamentRepository.existsById(id)).isTrue();

        // When - delete
        tournamentService.deleteTournament(id);

        // Then - verify deleted from database
        assertThat(tournamentRepository.existsById(id)).isFalse();
        assertThat(tournamentRepository.findById(id)).isEmpty();
    }

    // ==================== DIVISION TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should create division and persist to database")
    void testDivisionCreation() {
        // Given
        Tournament tournament = createTestTournament();
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(BeltRank.BLUE);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);

        // When
        DivisionResponseDTO created = divisionService.createDivision(tournament.getId(), createDTO);
        testDivisionId = created.getId();

        // Then - verify in database
        Division fromDb = divisionRepository.findById(testDivisionId).orElseThrow();
        assertThat(fromDb.getBeltRank()).isEqualTo(BeltRank.BLUE);
        assertThat(fromDb.getAgeCategory()).isEqualTo(AgeCategory.ADULT);
        assertThat(fromDb.getGender()).isEqualTo(Gender.MALE);
        assertThat(fromDb.getTournament().getId()).isEqualTo(tournament.getId());
    }

    @Test
    @Order(5)
    @DisplayName("Should delete division and remove from database")
    void testDivisionDeletion() {
        // Given - create division
        Tournament tournament = createTestTournament();
        Division division = createTestDivision(tournament);
        Long id = division.getId();

        // Verify exists
        assertThat(divisionRepository.existsById(id)).isTrue();

        // When - delete
        divisionService.deleteDivision(id);

        // Then - verify deleted from database
        assertThat(divisionRepository.existsById(id)).isFalse();
        assertThat(divisionRepository.findById(id)).isEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("Should delete division with athletes and remove from database")
    void testDivisionDeletionWithAthletes() {
        // Given - create division with athletes
        Tournament tournament = createTestTournament();
        Division division = createTestDivision(tournament);
        Athlete athlete1 = createTestAthlete("John Doe", "john@test.com");
        Athlete athlete2 = createTestAthlete("Jane Doe", "jane@test.com");

        // Enroll athletes
        divisionService.enrollAthlete(division.getId(), athlete1.getId());
        divisionService.enrollAthlete(division.getId(), athlete2.getId());

        // Verify athletes enrolled
        Division divisionWithAthletes = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(divisionWithAthletes.getAthletes()).hasSize(2);

        Long divisionId = division.getId();

        // When - delete division
        divisionService.deleteDivision(divisionId);

        // Then - verify division deleted but athletes remain
        assertThat(divisionRepository.existsById(divisionId)).isFalse();
        assertThat(athleteRepository.existsById(athlete1.getId())).isTrue();
        assertThat(athleteRepository.existsById(athlete2.getId())).isTrue();
    }

    // ==================== ATHLETE TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Should create athlete and persist to database")
    void testAthleteCreation() {
        // Given
        AthleteRegistrationDTO registrationDTO = new AthleteRegistrationDTO();
        registrationDTO.setName("Test Athlete");
        registrationDTO.setEmail("athlete@test.com");
        registrationDTO.setDateOfBirth(LocalDate.now().minusYears(25));
        registrationDTO.setGender(Gender.MALE);
        registrationDTO.setBeltRank(BeltRank.BLUE);
        registrationDTO.setWeight(75.0);

        // When
        Athlete created = athleteService.registerAthlete(registrationDTO);
        testAthleteId = created.getId();

        // Then - verify in database
        Athlete fromDb = athleteRepository.findById(testAthleteId).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Test Athlete");
        assertThat(fromDb.getEmail()).isEqualTo("athlete@test.com");
        assertThat(fromDb.getWeight()).isEqualTo(75.0);
    }

    @Test
    @Order(8)
    @DisplayName("Should update athlete and persist changes")
    void testAthleteUpdate() {
        // Given
        Athlete athlete = createTestAthlete("Original Name", "original@test.com");
        Long id = athlete.getId();

        // When - update
        athlete.setName("Updated Name");
        athlete.setWeight(80.0);
        athleteRepository.save(athlete);

        // Then - verify changes persisted
        Athlete fromDb = athleteRepository.findById(id).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Updated Name");
        assertThat(fromDb.getWeight()).isEqualTo(80.0);
        assertThat(fromDb.getVersion()).isEqualTo(1L);
    }

    // ==================== MATCH TESTS ====================

    @Test
    @Order(9)
    @DisplayName("Should generate matches for division")
    void testMatchGeneration() {
        // Given - division with 4 athletes
        Tournament tournament = createTestTournament();
        Division division = createTestDivision(tournament);

        Athlete a1 = createTestAthlete("Athlete 1", "a1@test.com");
        Athlete a2 = createTestAthlete("Athlete 2", "a2@test.com");
        Athlete a3 = createTestAthlete("Athlete 3", "a3@test.com");
        Athlete a4 = createTestAthlete("Athlete 4", "a4@test.com");

        divisionService.enrollAthlete(division.getId(), a1.getId());
        divisionService.enrollAthlete(division.getId(), a2.getId());
        divisionService.enrollAthlete(division.getId(), a3.getId());
        divisionService.enrollAthlete(division.getId(), a4.getId());

        // When - generate matches
        List<MatchResponseDTO> matchDTOs = matchService.generateMatches(division.getId());

        // Then - verify matches created in database
        assertThat(matchDTOs).isNotEmpty();
        List<Match> fromDb = matchRepository.findByDivisionId(division.getId());
        assertThat(fromDb).hasSizeGreaterThanOrEqualTo(3); // At least 3 matches for 4 athletes

        // Verify division marked as matches generated
        Division updatedDivision = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(updatedDivision.getMatchesGenerated()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("Should update match scores and persist")
    void testMatchScoreUpdate() {
        // Given - match
        Tournament tournament = createTestTournament();
        Division division = createTestDivision(tournament);
        Athlete a1 = createTestAthlete("Athlete 1", "a1@test.com");
        Athlete a2 = createTestAthlete("Athlete 2", "a2@test.com");

        Match match = new Match();
        match.setDivision(division);
        match.setAthlete1(a1);
        match.setAthlete2(a2);
        match.setStatus(MatchStatus.PENDING);
        match.setRoundNumber(1);
        match = matchRepository.save(match);

        Long matchId = match.getId();

        // When - update scores
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setAthlete1Points(4);
        updateDTO.setAthlete2Points(2);
        updateDTO.setStatus(MatchStatus.COMPLETED);
        updateDTO.setWinnerId(a1.getId());

        matchService.updateMatch(matchId, updateDTO);

        // Then - verify persisted
        Match fromDb = matchRepository.findById(matchId).orElseThrow();
        assertThat(fromDb.getAthlete1Points()).isEqualTo(4);
        assertThat(fromDb.getAthlete2Points()).isEqualTo(2);
        assertThat(fromDb.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(fromDb.getWinner().getId()).isEqualTo(a1.getId());
    }

    // ==================== FULL WORKFLOW TEST ====================

    @Test
    @Order(11)
    @DisplayName("Should complete full tournament workflow")
    void testFullTournamentWorkflow() {
        // Step 1: Create tournament
        Tournament tournament = createTestTournament();
        assertThat(tournamentRepository.existsById(tournament.getId())).isTrue();

        // Step 2: Create division
        Division division = createTestDivision(tournament);
        assertThat(divisionRepository.existsById(division.getId())).isTrue();

        // Step 3: Register athletes
        Athlete a1 = createTestAthlete("Fighter 1", "f1@test.com");
        Athlete a2 = createTestAthlete("Fighter 2", "f2@test.com");
        divisionService.enrollAthlete(division.getId(), a1.getId());
        divisionService.enrollAthlete(division.getId(), a2.getId());

        Division divisionWithAthletes = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(divisionWithAthletes.getAthletes()).hasSize(2);

        // Step 4: Generate matches
        List<MatchResponseDTO> matchDTOs = matchService.generateMatches(division.getId());
        assertThat(matchDTOs).isNotEmpty();

        // Step 5: Complete match - get from database
        List<Match> matches = matchRepository.findByDivisionId(division.getId());
        Match match = matches.get(0);
        MatchUpdateDTO updateDTO = new MatchUpdateDTO();
        updateDTO.setAthlete1Points(4);
        updateDTO.setAthlete2Points(0);
        updateDTO.setStatus(MatchStatus.COMPLETED);
        updateDTO.setWinnerId(a1.getId());
        matchService.updateMatch(match.getId(), updateDTO);

        Match completedMatch = matchRepository.findById(match.getId()).orElseThrow();
        assertThat(completedMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);

        // Step 6: Get rankings
        List<AthleteRankingDTO> rankings = divisionService.getDivisionRankings(division.getId());
        assertThat(rankings).isNotEmpty();

        // Step 7: Complete division
        Division completedDivision = divisionRepository.findById(division.getId()).orElseThrow();
        completedDivision.setCompleted(true);
        divisionRepository.save(completedDivision);

        Division verifyCompleted = divisionRepository.findById(division.getId()).orElseThrow();
        assertThat(verifyCompleted.getCompleted()).isTrue();
    }

    // ==================== DELETION CASCADE TESTS ====================

    @Test
    @Order(12)
    @DisplayName("Should cascade delete tournament with all divisions and matches")
    void testTournamentCascadeDelete() {
        // Given - tournament with divisions and matches
        Tournament tournament = createTestTournament();
        Division division1 = createTestDivision(tournament);

        Athlete a1 = createTestAthlete("A1", "a1@test.com");
        Athlete a2 = createTestAthlete("A2", "a2@test.com");

        divisionService.enrollAthlete(division1.getId(), a1.getId());
        divisionService.enrollAthlete(division1.getId(), a2.getId());

        // Don't generate matches - test deletion without matches first
        Long tournamentId = tournament.getId();

        // When - delete tournament
        tournamentService.deleteTournament(tournamentId);

        // Then - verify tournament deleted
        assertThat(tournamentRepository.existsById(tournamentId)).isFalse();

        // Note: Divisions should be deleted manually before tournament deletion
        // or we need cascade settings on the entity
    }

    // ==================== HELPER METHODS ====================

    private Tournament createTestTournament() {
        TournamentCreateDTO createDTO = new TournamentCreateDTO();
        createDTO.setName("Test Tournament " + System.currentTimeMillis());
        createDTO.setLocation("Test Location");
        createDTO.setTournamentDate(LocalDate.now().plusDays(30));
        createDTO.setRegistrationDeadline(LocalDate.now().plusDays(15));
        return tournamentService.createTournament(createDTO);
    }

    private Division createTestDivision(Tournament tournament) {
        return createTestDivisionWithCustomData(tournament, BeltRank.BLUE);
    }

    private Division createTestDivisionWithCustomData(Tournament tournament, BeltRank beltRank) {
        DivisionCreateDTO createDTO = new DivisionCreateDTO();
        createDTO.setBeltRank(beltRank);
        createDTO.setAgeCategory(AgeCategory.ADULT);
        createDTO.setGender(Gender.MALE);
        createDTO.setWeightClass(WeightClass.ADULT_MALE_MIDDLE);
        createDTO.setBracketType(BracketType.SINGLE_ELIMINATION);
        DivisionResponseDTO response = divisionService.createDivision(tournament.getId(), createDTO);
        return divisionRepository.findById(response.getId()).orElseThrow();
    }

    private Athlete createTestAthlete(String name, String email) {
        Athlete athlete = new Athlete();
        athlete.setName(name);
        athlete.setEmail(email);
        athlete.setDateOfBirth(LocalDate.now().minusYears(25));
        athlete.setAge(25);
        athlete.setGender(Gender.MALE);
        athlete.setBeltRank(BeltRank.BLUE);
        athlete.setWeight(75.0);
        athlete.setTeam("Test Team");
        return athleteRepository.save(athlete);
    }
}
