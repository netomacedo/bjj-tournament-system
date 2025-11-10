package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Match;
import com.bjj.tournament.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Match entity
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    /**
     * Find matches by division ID
     */
    List<Match> findByDivisionId(Long divisionId);
    
    /**
     * Find matches by division ID and round number
     */
    List<Match> findByDivisionIdAndRoundNumber(Long divisionId, Integer roundNumber);
    
    /**
     * Find matches by status
     */
    List<Match> findByStatus(MatchStatus status);
    
    /**
     * Find matches by mat number
     */
    List<Match> findByMatNumber(Integer matNumber);
    
    /**
     * Find all matches for a specific athlete
     */
    @Query("SELECT m FROM Match m WHERE m.athlete1.id = :athleteId OR m.athlete2.id = :athleteId")
    List<Match> findMatchesByAthleteId(Long athleteId);
    
    /**
     * Find pending matches by division
     */
    @Query("SELECT m FROM Match m WHERE m.division.id = :divisionId AND m.status = 'PENDING' ORDER BY m.roundNumber, m.matchPosition")
    List<Match> findPendingMatchesByDivision(Long divisionId);
    
    /**
     * Count completed matches in a division
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE m.division.id = :divisionId AND m.status = 'COMPLETED'")
    Long countCompletedMatchesByDivision(Long divisionId);
}
