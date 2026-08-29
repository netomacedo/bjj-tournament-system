package com.bjj.tournament;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.*;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import com.bjj.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for concurrency protection mechanisms
 * Tests unique constraints and optimistic locking (@Version) across all entities
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyProtectionTest {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private MatchRepository matchRepository;

    private Tournament testTournament;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        matchRepository.deleteAll();
        divisionRepository.deleteAll();
        athleteRepository.deleteAll();
        tournamentRepository.deleteAll();

        // Create a test tournament
        testTournament = new Tournament();
        testTournament.setName("Test Tournament");
        testTournament.setLocation("Test Location");
        testTournament.setTournamentDate(LocalDate.now().plusDays(30));
        testTournament.setRegistrationDeadline(LocalDate.now().plusDays(15));
        testTournament.setRegistrationOpen(true);
        testTournament = tournamentRepository.save(testTournament);
    }

    // ==================== DIVISION UNIQUE CONSTRAINT TESTS ====================

    @Test
    @Transactional
    @DisplayName("Should prevent duplicate divisions with same criteria")
    void testDivisionUniqueConstraint_PreventsDuplicates() {
        // Create first division
        Division division1 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        divisionRepository.saveAndFlush(division1);

        // Try to create duplicate division
        Division division2 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );

        // Should throw DataIntegrityViolationException due to unique constraint
        assertThatThrownBy(() -> divisionRepository.saveAndFlush(division2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("Unique index");
    }

    @Test
    @Transactional
    @DisplayName("Should allow divisions with different belt ranks")
    void testDivisionUniqueConstraint_AllowsDifferentBeltRanks() {
        Division division1 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        divisionRepository.saveAndFlush(division1);

        Division division2 = createDivision(
            testTournament,
            BeltRank.PURPLE,  // Different belt rank
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );

        // Should NOT throw - different belt rank
        assertThatCode(() -> divisionRepository.saveAndFlush(division2))
            .doesNotThrowAnyException();
    }

    @Test
    @Transactional
    @DisplayName("Should allow divisions with different genders")
    void testDivisionUniqueConstraint_AllowsDifferentGenders() {
        Division division1 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        divisionRepository.saveAndFlush(division1);

        Division division2 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.FEMALE,  // Different gender
            null
        );

        // Should NOT throw - different gender
        assertThatCode(() -> divisionRepository.saveAndFlush(division2))
            .doesNotThrowAnyException();
    }

    @Test
    @Transactional
    @DisplayName("Should allow same criteria in different tournaments")
    void testDivisionUniqueConstraint_AllowsSameCriteriaInDifferentTournaments() {
        // Create another tournament
        Tournament tournament2 = new Tournament();
        tournament2.setName("Another Tournament");
        tournament2.setLocation("Another Location");
        tournament2.setTournamentDate(LocalDate.now().plusDays(60));
        tournament2.setRegistrationDeadline(LocalDate.now().plusDays(45));
        tournament2.setRegistrationOpen(true);
        tournament2 = tournamentRepository.save(tournament2);

        // Create division in first tournament
        Division division1 = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        divisionRepository.saveAndFlush(division1);

        // Create same division criteria in second tournament
        Division division2 = createDivision(
            tournament2,  // Different tournament
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );

        // Should NOT throw - different tournament
        assertThatCode(() -> divisionRepository.saveAndFlush(division2))
            .doesNotThrowAnyException();
    }

    // ==================== DIVISION-ATHLETE UNIQUE CONSTRAINT TESTS ====================

    @Test
    @Transactional
    @DisplayName("Should prevent duplicate athlete enrollment in same division")
    void testDivisionAthlete_PreventsDuplicateEnrollment() {
        Division division = createDivision(
            testTournament,
            BeltRank.PURPLE,  // Use different belt rank to avoid conflict with other tests
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        Division savedDivision = divisionRepository.save(division);

        Athlete athlete = createAthlete("John Doe", "john.enrollment@test.com", BeltRank.PURPLE, Gender.MALE);
        athlete = athleteRepository.save(athlete);

        // Add athlete to division
        savedDivision.addAthlete(athlete);
        divisionRepository.saveAndFlush(savedDivision);

        // Try to add same athlete again
        savedDivision.addAthlete(athlete);

        // Should throw DataIntegrityViolationException due to unique division-athlete constraint
        assertThatThrownBy(() -> divisionRepository.saveAndFlush(savedDivision))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("Unique index");
    }

    @Test
    @Transactional
    @DisplayName("Should allow same athlete in different divisions")
    void testDivisionAthlete_AllowsSameAthleteInDifferentDivisions() {
        // Create two divisions with DIFFERENT weight classes (same belt/age/gender but different weight = different divisions)
        Division division1 = createDivision(
            testTournament,
            BeltRank.BROWN,  // Use different belt to avoid conflicts
            AgeCategory.ADULT,
            Gender.MALE,
            WeightClass.ADULT_MALE_LIGHT
        );
        Division savedDivision1 = divisionRepository.save(division1);

        // Create another division for absolute category (different age category to avoid constraint violation)
        Division division2 = createDivision(
            testTournament,
            BeltRank.BROWN,
            AgeCategory.MASTER_1,  // Different age category
            Gender.MALE,
            null  // Open weight
        );
        final Division savedDivision2 = divisionRepository.save(division2);

        // Create athlete
        Athlete athlete = createAthlete("John Doe", "john.multidivision@test.com", BeltRank.BROWN, Gender.MALE);
        athlete = athleteRepository.save(athlete);

        // Add athlete to first division
        savedDivision1.addAthlete(athlete);
        divisionRepository.saveAndFlush(savedDivision1);

        // Add same athlete to second division (absolute/open weight)
        savedDivision2.addAthlete(athlete);

        // Should NOT throw - different divisions
        assertThatCode(() -> divisionRepository.saveAndFlush(savedDivision2))
            .doesNotThrowAnyException();
    }

    // ==================== ATHLETE UNIQUE CONSTRAINT TESTS ====================

    @Test
    @Transactional
    @DisplayName("Should prevent duplicate athletes with same email")
    void testAthleteUniqueConstraint_PreventsDuplicateEmails() {
        // Create first athlete
        Athlete athlete1 = createAthlete("John Doe", "duplicate@test.com", BeltRank.BLUE, Gender.MALE);
        athleteRepository.saveAndFlush(athlete1);

        // Try to create athlete with same email
        Athlete athlete2 = createAthlete("John Smith", "duplicate@test.com", BeltRank.PURPLE, Gender.MALE);

        // Should throw DataIntegrityViolationException due to unique email constraint
        assertThatThrownBy(() -> athleteRepository.saveAndFlush(athlete2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("Unique index");
    }

    @Test
    @Transactional
    @DisplayName("Should allow athletes with different emails")
    void testAthleteUniqueConstraint_AllowsDifferentEmails() {
        Athlete athlete1 = createAthlete("John Doe", "john.different@test.com", BeltRank.BLUE, Gender.MALE);
        athleteRepository.saveAndFlush(athlete1);

        Athlete athlete2 = createAthlete("Jane Doe", "jane.different@test.com", BeltRank.BLUE, Gender.FEMALE);

        // Should NOT throw - different email
        assertThatCode(() -> athleteRepository.saveAndFlush(athlete2))
            .doesNotThrowAnyException();
    }

    // ==================== OPTIMISTIC LOCKING TESTS ====================

    @Test
    @DisplayName("Division optimistic locking should detect concurrent updates")
    void testDivisionOptimisticLocking() {
        Division division = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        division = divisionRepository.saveAndFlush(division);
        Long divisionId = division.getId();

        // Simulate two concurrent users reading the same division
        Division user1Division = divisionRepository.findById(divisionId).orElseThrow();
        Division user2Division = divisionRepository.findById(divisionId).orElseThrow();

        // User 1 updates and saves
        user1Division.setBracketType(BracketType.ROUND_ROBIN);
        divisionRepository.saveAndFlush(user1Division);

        // User 2 tries to update (with stale version)
        user2Division.setBracketType(BracketType.DOUBLE_ELIMINATION);

        // Should throw ObjectOptimisticLockingFailureException (Spring wraps OptimisticLockException)
        assertThatThrownBy(() -> divisionRepository.saveAndFlush(user2Division))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Tournament optimistic locking should detect concurrent state changes")
    void testTournamentOptimisticLocking() {
        Long tournamentId = testTournament.getId();

        // Simulate two concurrent users
        Tournament user1Tournament = tournamentRepository.findById(tournamentId).orElseThrow();
        Tournament user2Tournament = tournamentRepository.findById(tournamentId).orElseThrow();

        // User 1 starts tournament
        user1Tournament.setStarted(true);
        tournamentRepository.saveAndFlush(user1Tournament);

        // User 2 tries to complete tournament (with stale version)
        user2Tournament.setCompleted(true);

        // Should throw ObjectOptimisticLockingFailureException (Spring wraps OptimisticLockException)
        assertThatThrownBy(() -> tournamentRepository.saveAndFlush(user2Tournament))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Athlete optimistic locking should detect concurrent updates")
    void testAthleteOptimisticLocking() {
        Athlete athlete = createAthlete("John Doe", "john.locking@test.com", BeltRank.BLUE, Gender.MALE);
        athlete = athleteRepository.saveAndFlush(athlete);
        Long athleteId = athlete.getId();

        // Simulate two concurrent users
        Athlete user1Athlete = athleteRepository.findById(athleteId).orElseThrow();
        Athlete user2Athlete = athleteRepository.findById(athleteId).orElseThrow();

        // User 1 updates weight
        user1Athlete.setWeight(75.0);
        athleteRepository.saveAndFlush(user1Athlete);

        // User 2 tries to update belt rank (with stale version)
        user2Athlete.setBeltRank(BeltRank.PURPLE);

        // Should throw ObjectOptimisticLockingFailureException (Spring wraps OptimisticLockException)
        assertThatThrownBy(() -> athleteRepository.saveAndFlush(user2Athlete))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Match optimistic locking should detect concurrent score updates")
    void testMatchOptimisticLocking() {
        // Create division and athletes
        Division division = createDivision(
            testTournament,
            BeltRank.BLACK,  // Use different belt to avoid conflicts
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        division = divisionRepository.save(division);

        Athlete athlete1 = createAthlete("John Doe", "john.match@test.com", BeltRank.BLACK, Gender.MALE);
        athlete1 = athleteRepository.save(athlete1);

        Athlete athlete2 = createAthlete("Jane Doe", "jane.match@test.com", BeltRank.BLACK, Gender.FEMALE);
        athlete2 = athleteRepository.save(athlete2);

        // Create match
        Match match = new Match();
        match.setDivision(division);
        match.setAthlete1(athlete1);
        match.setAthlete2(athlete2);
        match.setStatus(MatchStatus.PENDING);
        match.setRoundNumber(1);
        match = matchRepository.saveAndFlush(match);
        Long matchId = match.getId();

        // Simulate two concurrent score updates
        Match user1Match = matchRepository.findById(matchId).orElseThrow();
        Match user2Match = matchRepository.findById(matchId).orElseThrow();

        // User 1 updates athlete1 score
        user1Match.setAthlete1Points(2);
        matchRepository.saveAndFlush(user1Match);

        // User 2 tries to update athlete2 score (with stale version)
        user2Match.setAthlete2Points(4);

        // Should throw ObjectOptimisticLockingFailureException (Spring wraps OptimisticLockException)
        assertThatThrownBy(() -> matchRepository.saveAndFlush(user2Match))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Version field should increment on each update")
    void testVersionFieldIncrement() {
        Division division = createDivision(
            testTournament,
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE,
            null
        );
        division = divisionRepository.saveAndFlush(division);

        // Initial version should be 0
        assertThat(division.getVersion()).isEqualTo(0L);

        // Update and save
        division.setBracketType(BracketType.ROUND_ROBIN);
        division = divisionRepository.saveAndFlush(division);

        // Version should increment to 1
        assertThat(division.getVersion()).isEqualTo(1L);

        // Update again
        division.setCompleted(true);
        division = divisionRepository.saveAndFlush(division);

        // Version should increment to 2
        assertThat(division.getVersion()).isEqualTo(2L);
    }

    // ==================== HELPER METHODS ====================

    private Division createDivision(Tournament tournament, BeltRank beltRank,
                                   AgeCategory ageCategory, Gender gender,
                                   WeightClass weightClass) {
        Division division = new Division();
        division.setTournament(tournament);
        division.setBeltRank(beltRank);
        division.setAgeCategory(ageCategory);
        division.setGender(gender);
        division.setWeightClass(weightClass);
        division.setBracketType(BracketType.SINGLE_ELIMINATION);
        return division;
    }

    private Athlete createAthlete(String name, String email, BeltRank beltRank, Gender gender) {
        Athlete athlete = new Athlete();
        athlete.setName(name);
        athlete.setEmail(email);
        athlete.setDateOfBirth(LocalDate.now().minusYears(25));
        athlete.setAge(25);
        athlete.setGender(gender);
        athlete.setBeltRank(beltRank);
        athlete.setWeight(70.0);
        athlete.setTeam("Test Team");
        return athlete;
    }
}
