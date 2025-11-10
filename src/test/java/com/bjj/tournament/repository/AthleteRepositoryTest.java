package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
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
 * Unit tests for AthleteRepository
 * Uses @DataJpaTest for in-memory database testing
 */
@DataJpaTest
class AthleteRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private AthleteRepository athleteRepository;
    
    private Athlete athlete1;
    private Athlete athlete2;
    private Athlete athlete3;
    
    @BeforeEach
    void setUp() {
        // Create test athletes
        athlete1 = new Athlete();
        athlete1.setName("John Silva");
        athlete1.setDateOfBirth(LocalDate.of(1995, 5, 15));
        athlete1.setAge(29);
        athlete1.setGender(Gender.MALE);
        athlete1.setBeltRank(BeltRank.BLUE);
        athlete1.setWeight(75.0);
        athlete1.setEmail("john@test.com");
        athlete1.setTeam("Team Alpha");
        athlete1.setCoachName("Coach Mike");
        
        athlete2 = new Athlete();
        athlete2.setName("Maria Santos");
        athlete2.setDateOfBirth(LocalDate.of(1998, 8, 20));
        athlete2.setAge(26);
        athlete2.setGender(Gender.FEMALE);
        athlete2.setBeltRank(BeltRank.PURPLE);
        athlete2.setWeight(60.0);
        athlete2.setEmail("maria@test.com");
        athlete2.setTeam("Team Beta");
        athlete2.setCoachName("Coach Ana");
        
        athlete3 = new Athlete();
        athlete3.setName("Pedro Costa");
        athlete3.setDateOfBirth(LocalDate.of(1992, 3, 10));
        athlete3.setAge(32);
        athlete3.setGender(Gender.MALE);
        athlete3.setBeltRank(BeltRank.BLUE);
        athlete3.setWeight(82.0);
        athlete3.setEmail("pedro@test.com");
        athlete3.setTeam("Team Alpha");
        athlete3.setCoachName("Coach Mike");
        
        entityManager.persist(athlete1);
        entityManager.persist(athlete2);
        entityManager.persist(athlete3);
        entityManager.flush();
    }
    
    @Test
    void testFindByBeltRank_ShouldReturnAthletesWithBlueBelt() {
        // When
        List<Athlete> bluebelts = athleteRepository.findByBeltRank(BeltRank.BLUE);
        
        // Then
        assertThat(bluebelts).hasSize(2);
        assertThat(bluebelts).extracting(Athlete::getName)
            .containsExactlyInAnyOrder("John Silva", "Pedro Costa");
    }
    
    @Test
    void testFindByGender_ShouldReturnMaleAthletes() {
        // When
        List<Athlete> males = athleteRepository.findByGender(Gender.MALE);
        
        // Then
        assertThat(males).hasSize(2);
        assertThat(males).allMatch(a -> a.getGender() == Gender.MALE);
    }
    
    @Test
    void testFindByAgeBetween_ShouldReturnAthletesInAgeRange() {
        // When
        List<Athlete> athletes = athleteRepository.findByAgeBetween(25, 30);
        
        // Then
        assertThat(athletes).hasSize(2);
        assertThat(athletes).extracting(Athlete::getName)
            .containsExactlyInAnyOrder("John Silva", "Maria Santos");
    }
    
    @Test
    void testFindByBeltRankAndAgeBetween_ShouldReturnMatchingAthletes() {
        // When
        List<Athlete> athletes = athleteRepository.findByBeltRankAndAgeBetween(
            BeltRank.BLUE, 25, 35);
        
        // Then
        assertThat(athletes).hasSize(2);
        assertThat(athletes).allMatch(a -> a.getBeltRank() == BeltRank.BLUE);
    }
    
    @Test
    void testFindByBeltRankAndGenderAndAgeBetween_ShouldReturnSpecificAthletes() {
        // When
        List<Athlete> athletes = athleteRepository.findByBeltRankAndGenderAndAgeBetween(
            BeltRank.BLUE, Gender.MALE, 25, 35);
        
        // Then
        assertThat(athletes).hasSize(2);
        assertThat(athletes).allMatch(a -> 
            a.getBeltRank() == BeltRank.BLUE && a.getGender() == Gender.MALE);
    }
    
    @Test
    void testFindByEmail_ShouldReturnAthlete() {
        // When
        Optional<Athlete> found = athleteRepository.findByEmail("john@test.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Silva");
    }
    
    @Test
    void testFindByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
        // When
        Optional<Athlete> found = athleteRepository.findByEmail("nonexistent@test.com");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void testExistsByEmail_ShouldReturnTrue() {
        // When
        boolean exists = athleteRepository.existsByEmail("maria@test.com");
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    void testExistsByEmail_WithNonExistentEmail_ShouldReturnFalse() {
        // When
        boolean exists = athleteRepository.existsByEmail("fake@test.com");
        
        // Then
        assertThat(exists).isFalse();
    }
    
    @Test
    void testFindByTeam_ShouldReturnTeamMembers() {
        // When
        List<Athlete> teamAlpha = athleteRepository.findByTeam("Team Alpha");
        
        // Then
        assertThat(teamAlpha).hasSize(2);
        assertThat(teamAlpha).extracting(Athlete::getName)
            .containsExactlyInAnyOrder("John Silva", "Pedro Costa");
    }
    
    @Test
    void testFindByCoachName_ShouldReturnCoachAthletes() {
        // When
        List<Athlete> coachMikeAthletes = athleteRepository.findByCoachName("Coach Mike");
        
        // Then
        assertThat(coachMikeAthletes).hasSize(2);
        assertThat(coachMikeAthletes).allMatch(a -> 
            "Coach Mike".equals(a.getCoachName()));
    }
    
    @Test
    void testSearchByName_ShouldFindAthletesByPartialName() {
        // When
        List<Athlete> results = athleteRepository.searchByName("Silva");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("John Silva");
    }
    
    @Test
    void testSearchByName_WithLowercaseQuery_ShouldBeCaseInsensitive() {
        // When
        List<Athlete> results = athleteRepository.searchByName("maria");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Maria Santos");
    }
    
    @Test
    void testSearchByName_WithPartialMatch_ShouldFindMultipleResults() {
        // When
        List<Athlete> results = athleteRepository.searchByName("a");
        
        // Then
        assertThat(results.size()).isGreaterThan(0);
    }
}
