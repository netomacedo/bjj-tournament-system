package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Division;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Division entity
 */
@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {
    
    /**
     * Find divisions by tournament ID
     */
    List<Division> findByTournamentId(Long tournamentId);
    
    /**
     * Find division by tournament ID, belt rank, age category, and gender
     */
    Optional<Division> findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
        Long tournamentId,
        BeltRank beltRank,
        AgeCategory ageCategory,
        Gender gender
    );
    
    /**
     * Find divisions that haven't generated matches yet
     */
    @Query("SELECT d FROM Division d WHERE d.matchesGenerated = false AND SIZE(d.athletes) >= 2")
    List<Division> findDivisionsReadyForMatchGeneration();
    
    /**
     * Find completed divisions
     */
    List<Division> findByCompleted(Boolean completed);
}


