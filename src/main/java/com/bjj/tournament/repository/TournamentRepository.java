package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Tournament entity
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    
    /**
     * Find tournaments by registration status
     */
    List<Tournament> findByRegistrationOpen(Boolean registrationOpen);
    
    /**
     * Find upcoming tournaments (future date, not completed)
     */
    @Query("SELECT t FROM Tournament t WHERE t.tournamentDate >= :currentDate AND t.completed = false ORDER BY t.tournamentDate ASC")
    List<Tournament> findUpcomingTournaments(LocalDate currentDate);
    
    /**
     * Find completed tournaments
     */
    List<Tournament> findByCompleted(Boolean completed);
    
    /**
     * Find tournaments by date range
     */
    List<Tournament> findByTournamentDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find tournaments by organizer
     */
    List<Tournament> findByOrganizer(String organizer);
}
