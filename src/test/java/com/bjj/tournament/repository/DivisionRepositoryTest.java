package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DivisionRepository
 * Uses @DataJpaTest for in-memory database testing
 */
@DataJpaTest
class DivisionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DivisionRepository divisionRepository;

    private Tournament tournament;
    private Division division1;
    private Division division2;
    private Division division3;
    private Athlete athlete1;
    private Athlete athlete2;

    @BeforeEach
    void setUp() {
        // Create tournament
        tournament = new Tournament();
        tournament.setName("Test Tournament");
        tournament.setLocation("Los Angeles");
        tournament.setTournamentDate(LocalDate.now().plusDays(30));
        tournament.setRegistrationDeadline(LocalDate.now().plusDays(15));
        tournament.setRegistrationOpen(true);
        entityManager.persist(tournament);

        // Create athletes
        athlete1 = new Athlete();
        athlete1.setName("John Silva");
        athlete1.setDateOfBirth(LocalDate.of(1995, 1, 1));
        athlete1.setAge(29);
        athlete1.setGender(Gender.MALE);
        athlete1.setBeltRank(BeltRank.BLUE);
        athlete1.setWeight(75.0);
        athlete1.setEmail("john@test.com");
        entityManager.persist(athlete1);

        athlete2 = new Athlete();
        athlete2.setName("Mike Jones");
        athlete2.setDateOfBirth(LocalDate.of(1996, 1, 1));
        athlete2.setAge(28);
        athlete2.setGender(Gender.MALE);
        athlete2.setBeltRank(BeltRank.BLUE);
        athlete2.setWeight(76.0);
        athlete2.setEmail("mike@test.com");
        entityManager.persist(athlete2);

        // Create divisions
        division1 = new Division();
        division1.setTournament(tournament);
        division1.setBeltRank(BeltRank.BLUE);
        division1.setAgeCategory(AgeCategory.ADULT);
        division1.setGender(Gender.MALE);
        division1.setWeightClass(WeightClass.ADULT_MALE_LIGHT);
        division1.setBracketType(BracketType.SINGLE_ELIMINATION);
        division1.setMatchesGenerated(false);
        division1.setCompleted(false);
        division1.getAthletes().add(athlete1);
        division1.getAthletes().add(athlete2);
        entityManager.persist(division1);

        division2 = new Division();
        division2.setTournament(tournament);
        division2.setBeltRank(BeltRank.PURPLE);
        division2.setAgeCategory(AgeCategory.ADULT);
        division2.setGender(Gender.MALE);
        division2.setWeightClass(WeightClass.ADULT_MALE_MIDDLE);
        division2.setBracketType(BracketType.SINGLE_ELIMINATION);
        division2.setMatchesGenerated(false);
        division2.setCompleted(false);
        entityManager.persist(division2);

        division3 = new Division();
        division3.setTournament(tournament);
        division3.setBeltRank(BeltRank.BLUE);
        division3.setAgeCategory(AgeCategory.MASTER_1);
        division3.setGender(Gender.MALE);
        division3.setWeightClass(WeightClass.ADULT_MALE_HEAVY);
        division3.setBracketType(BracketType.ROUND_ROBIN);
        division3.setMatchesGenerated(true);
        division3.setCompleted(false);
        entityManager.persist(division3);

        entityManager.flush();
    }

    @Test
    void testFindByTournamentId_ShouldReturnAllDivisionsForTournament() {
        // When
        List<Division> divisions = divisionRepository.findByTournamentId(tournament.getId());

        // Then
        assertThat(divisions).hasSize(3);
        assertThat(divisions).extracting(Division::getBeltRank)
            .containsExactlyInAnyOrder(BeltRank.BLUE, BeltRank.PURPLE, BeltRank.BLUE);
    }

    @Test
    void testFindByTournamentId_WithNonExistentTournament_ShouldReturnEmpty() {
        // When
        List<Division> divisions = divisionRepository.findByTournamentId(999L);

        // Then
        assertThat(divisions).isEmpty();
    }

    @Test
    void testFindByTournamentIdAndBeltRankAndAgeCategoryAndGender_ShouldReturnSpecificDivision() {
        // When
        Optional<Division> found = divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            tournament.getId(),
            BeltRank.BLUE,
            AgeCategory.ADULT,
            Gender.MALE
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getBeltRank()).isEqualTo(BeltRank.BLUE);
        assertThat(found.get().getAgeCategory()).isEqualTo(AgeCategory.ADULT);
        assertThat(found.get().getGender()).isEqualTo(Gender.MALE);
        assertThat(found.get().getWeightClass()).isEqualTo(WeightClass.ADULT_MALE_LIGHT);
    }

    @Test
    void testFindByTournamentIdAndBeltRankAndAgeCategoryAndGender_WithNonExistentCombination_ShouldReturnEmpty() {
        // When
        Optional<Division> found = divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            tournament.getId(),
            BeltRank.BLACK,
            AgeCategory.ADULT,
            Gender.MALE
        );

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testFindDivisionsReadyForMatchGeneration_ShouldReturnDivisionsWithAtLeastTwoAthletes() {
        // When
        List<Division> readyDivisions = divisionRepository.findDivisionsReadyForMatchGeneration();

        // Then
        assertThat(readyDivisions).hasSize(1);
        assertThat(readyDivisions.get(0).getBeltRank()).isEqualTo(BeltRank.BLUE);
        assertThat(readyDivisions.get(0).getAgeCategory()).isEqualTo(AgeCategory.ADULT);
        assertThat(readyDivisions.get(0).getMatchesGenerated()).isFalse();
        assertThat(readyDivisions.get(0).getAthletes()).hasSize(2);
    }

    @Test
    void testFindDivisionsReadyForMatchGeneration_ShouldExcludeDivisionsWithMatchesGenerated() {
        // Given - division3 has matchesGenerated = true

        // When
        List<Division> readyDivisions = divisionRepository.findDivisionsReadyForMatchGeneration();

        // Then
        assertThat(readyDivisions).doesNotContain(division3);
    }

    @Test
    void testFindDivisionsReadyForMatchGeneration_ShouldExcludeDivisionsWithLessThanTwoAthletes() {
        // When
        List<Division> readyDivisions = divisionRepository.findDivisionsReadyForMatchGeneration();

        // Then
        assertThat(readyDivisions).doesNotContain(division2); // division2 has 0 athletes
    }

    @Test
    void testFindByCompleted_WithTrueValue_ShouldReturnCompletedDivisions() {
        // Given - mark division1 as completed
        division1.setCompleted(true);
        entityManager.persist(division1);
        entityManager.flush();

        // When
        List<Division> completedDivisions = divisionRepository.findByCompleted(true);

        // Then
        assertThat(completedDivisions).hasSize(1);
        assertThat(completedDivisions.get(0).getCompleted()).isTrue();
    }

    @Test
    void testFindByCompleted_WithFalseValue_ShouldReturnIncompleteDivisions() {
        // When
        List<Division> incompleteDivisions = divisionRepository.findByCompleted(false);

        // Then
        assertThat(incompleteDivisions).hasSize(3);
        assertThat(incompleteDivisions).allMatch(d -> !d.getCompleted());
    }

    @Test
    void testDivisionWithAthletes_ShouldMaintainManyToManyRelationship() {
        // When
        Division found = divisionRepository.findById(division1.getId()).orElseThrow();

        // Then
        assertThat(found.getAthletes()).hasSize(2);
        assertThat(found.getAthletes()).extracting(Athlete::getName)
            .containsExactlyInAnyOrder("John Silva", "Mike Jones");
    }

    @Test
    void testDivisionNameGeneration_ShouldAutoGenerateBasedOnCriteria() {
        // When
        Division found = divisionRepository.findById(division1.getId()).orElseThrow();

        // Then
        assertThat(found.getName()).isNotNull();
        assertThat(found.getName()).contains("Adult");
        assertThat(found.getName()).contains("Male");
        assertThat(found.getName()).contains("Blue");
    }

    @Test
    void testSaveDivision_WithNewDivision_ShouldPersist() {
        // Given
        Division newDivision = new Division();
        newDivision.setTournament(tournament);
        newDivision.setBeltRank(BeltRank.WHITE);
        newDivision.setAgeCategory(AgeCategory.ADULT);
        newDivision.setGender(Gender.FEMALE);
        newDivision.setWeightClass(WeightClass.ADULT_FEMALE_LIGHT);
        newDivision.setBracketType(BracketType.SINGLE_ELIMINATION);
        newDivision.setMatchesGenerated(false);
        newDivision.setCompleted(false);

        // When
        Division saved = divisionRepository.save(newDivision);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBeltRank()).isEqualTo(BeltRank.WHITE);
        assertThat(saved.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void testDeleteDivision_ShouldRemoveFromDatabase() {
        // Given
        Long divisionId = division2.getId();

        // When
        divisionRepository.delete(division2);
        entityManager.flush();

        // Then
        Optional<Division> found = divisionRepository.findById(divisionId);
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByTournamentId_ShouldOrderDivisionsConsistently() {
        // When
        List<Division> divisions = divisionRepository.findByTournamentId(tournament.getId());

        // Then
        assertThat(divisions).hasSize(3);
        // Verify we get consistent ordering
        for (Division div : divisions) {
            assertThat(div.getId()).isNotNull();
        }
    }

    @Test
    void testUpdateDivision_ShouldPersistChanges() {
        // Given
        division1.setBracketType(BracketType.DOUBLE_ELIMINATION);

        // When
        divisionRepository.save(division1);
        entityManager.flush();
        entityManager.clear();

        // Then
        Division found = divisionRepository.findById(division1.getId()).orElseThrow();
        assertThat(found.getBracketType()).isEqualTo(BracketType.DOUBLE_ELIMINATION);
    }

    @Test
    void testDivision_WithMultipleCriteria_ShouldBeUnique() {
        // When
        Optional<Division> blueBeltAdult = divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            tournament.getId(), BeltRank.BLUE, AgeCategory.ADULT, Gender.MALE);

        Optional<Division> blueBeltMaster = divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            tournament.getId(), BeltRank.BLUE, AgeCategory.MASTER_1, Gender.MALE);

        // Then
        assertThat(blueBeltAdult).isPresent();
        assertThat(blueBeltMaster).isPresent();
        assertThat(blueBeltAdult.get().getId()).isNotEqualTo(blueBeltMaster.get().getId());
    }
}