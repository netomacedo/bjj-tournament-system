package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Athlete entity
 * Provides database operations for athlete management
 */
@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    
    /**
     * Find athletes by belt rank
     */
    List<Athlete> findByBeltRank(BeltRank beltRank);
    
    /**
     * Find athletes by gender
     */
    List<Athlete> findByGender(Gender gender);
    
    /**
     * Find athletes by age range
     */
    List<Athlete> findByAgeBetween(Integer minAge, Integer maxAge);
    
    /**
     * Find athletes by belt rank and age range
     */
    List<Athlete> findByBeltRankAndAgeBetween(BeltRank beltRank, Integer minAge, Integer maxAge);
    
    /**
     * Find athletes by belt rank, gender, and age range
     */
    List<Athlete> findByBeltRankAndGenderAndAgeBetween(
        BeltRank beltRank, 
        Gender gender, 
        Integer minAge, 
        Integer maxAge
    );
    
    /**
     * Find athlete by email
     */
    Optional<Athlete> findByEmail(String email);
    
    /**
     * Check if athlete exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Find athletes by team
     */
    List<Athlete> findByTeam(String team);
    
    /**
     * Find athletes by coach name
     */
    List<Athlete> findByCoachName(String coachName);
    
    /**
     * Search athletes by name (case-insensitive, partial match)
     */
    @Query("SELECT a FROM Athlete a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Athlete> searchByName(String name);
}
