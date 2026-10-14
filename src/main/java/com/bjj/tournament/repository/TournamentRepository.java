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
     * Find every tournament belonging to one organization
     */
    List<Tournament> findByOrganizationId(Long organizationId);

    /**
     * Find tournaments by registration status, scoped to one organization
     */
    List<Tournament> findByOrganizationIdAndRegistrationOpen(Long organizationId, Boolean registrationOpen);

    /**
     * Find upcoming tournaments (future date, not completed), scoped to one organization
     */
    @Query("SELECT t FROM Tournament t WHERE t.organizationId = :organizationId "
        + "AND t.tournamentDate >= :currentDate AND t.completed = false ORDER BY t.tournamentDate ASC")
    List<Tournament> findUpcomingTournaments(Long organizationId, LocalDate currentDate);

    /**
     * Find completed tournaments, scoped to one organization
     */
    List<Tournament> findByOrganizationIdAndCompleted(Long organizationId, Boolean completed);
    
    /**
     * Find tournaments by date range
     */
    List<Tournament> findByTournamentDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find tournaments by organizer
     */
    List<Tournament> findByOrganizer(String organizer);
}
