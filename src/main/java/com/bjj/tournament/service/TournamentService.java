package com.bjj.tournament.service;

import com.bjj.tournament.dto.TournamentCreateDTO;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.repository.TournamentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing tournament operations
 * Handles tournament creation, updates, and queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final EntityManager entityManager;
    
    /**
     * Create a new tournament
     */
    @Transactional
    public Tournament createTournament(TournamentCreateDTO createDTO) {
        log.info("Creating new tournament: {}", createDTO.getName());
        
        // Validate tournament date is in the future
        if (createDTO.getTournamentDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Tournament date must be in the future");
        }
        
        // Validate registration deadline if provided
        if (createDTO.getRegistrationDeadline() != null && 
            createDTO.getRegistrationDeadline().isAfter(createDTO.getTournamentDate())) {
            throw new IllegalArgumentException("Registration deadline must be before tournament date");
        }
        
        Tournament tournament = new Tournament();
        tournament.setName(createDTO.getName());
        tournament.setDescription(createDTO.getDescription());
        tournament.setLocation(createDTO.getLocation());
        tournament.setTournamentDate(createDTO.getTournamentDate());
        tournament.setRegistrationDeadline(createDTO.getRegistrationDeadline());
        tournament.setOrganizer(createDTO.getOrganizer());
        tournament.setContactEmail(createDTO.getContactEmail());
        tournament.setRules(createDTO.getRules());
        
        Tournament savedTournament = tournamentRepository.save(tournament);
        log.info("Successfully created tournament with ID: {}", savedTournament.getId());
        
        return savedTournament;
    }
    
    /**
     * Get tournament by ID
     */
    @Transactional(readOnly = true)
    public Tournament getTournamentById(Long id) {
        return tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + id));
    }
    
    /**
     * Get all tournaments
     */
    @Transactional(readOnly = true)
    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }
    
    /**
     * Get upcoming tournaments
     */
    @Transactional(readOnly = true)
    public List<Tournament> getUpcomingTournaments() {
        return tournamentRepository.findUpcomingTournaments(LocalDate.now());
    }
    
    /**
     * Get tournaments with open registration
     */
    @Transactional(readOnly = true)
    public List<Tournament> getTournamentsWithOpenRegistration() {
        return tournamentRepository.findByRegistrationOpen(true);
    }
    
    /**
     * Close tournament registration
     */
    @Transactional
    public Tournament closeRegistration(Long tournamentId) {
        log.info("Closing registration for tournament ID: {}", tournamentId);
        
        Tournament tournament = getTournamentById(tournamentId);
        tournament.setRegistrationOpen(false);
        
        Tournament updatedTournament = tournamentRepository.save(tournament);
        log.info("Successfully closed registration for tournament ID: {}", tournamentId);
        
        return updatedTournament;
    }
    
    /**
     * Start tournament
     */
    @Transactional
    public Tournament startTournament(Long tournamentId) {
        log.info("Starting tournament ID: {}", tournamentId);
        
        Tournament tournament = getTournamentById(tournamentId);
        
        if (tournament.getStarted()) {
            throw new IllegalStateException("Tournament has already started");
        }
        
        tournament.setStarted(true);
        tournament.setRegistrationOpen(false); // Automatically close registration
        
        Tournament updatedTournament = tournamentRepository.save(tournament);
        log.info("Successfully started tournament ID: {}", tournamentId);
        
        return updatedTournament;
    }
    
    /**
     * Complete tournament
     */
    @Transactional
    public Tournament completeTournament(Long tournamentId) {
        log.info("Completing tournament ID: {}", tournamentId);
        
        Tournament tournament = getTournamentById(tournamentId);
        
        if (!tournament.getStarted()) {
            throw new IllegalStateException("Tournament has not started yet");
        }
        
        if (tournament.getCompleted()) {
            throw new IllegalStateException("Tournament is already completed");
        }
        
        tournament.setCompleted(true);
        
        Tournament updatedTournament = tournamentRepository.save(tournament);
        log.info("Successfully completed tournament ID: {}", tournamentId);
        
        return updatedTournament;
    }
    
    /**
     * Delete tournament
     */
    @Transactional
    public void deleteTournament(Long id) {
        log.info("==========================================");
        log.info("TOURNAMENT DELETION STARTED - ID: {}", id);
        log.info("==========================================");

        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + id));

        log.info("Tournament found: {}", tournament.getName());

        // Step 1: Delete all matches for this tournament using JPQL (bypasses @Version)
        int deletedMatches = entityManager.createQuery(
            "DELETE FROM Match m WHERE m.division.tournament.id = :tournamentId")
            .setParameter("tournamentId", id)
            .executeUpdate();
        log.info("Deleted {} matches", deletedMatches);

        // Step 2: Delete all divisions using JPQL (bypasses @Version)
        int deletedDivisions = entityManager.createQuery(
            "DELETE FROM Division d WHERE d.tournament.id = :tournamentId")
            .setParameter("tournamentId", id)
            .executeUpdate();
        log.info("Deleted {} divisions", deletedDivisions);

        // Step 3: Delete the tournament using JPQL
        log.info("Deleting tournament...");
        entityManager.createQuery("DELETE FROM Tournament t WHERE t.id = :tournamentId")
            .setParameter("tournamentId", id)
            .executeUpdate();
        log.info("Tournament deleted successfully");

        log.info("==========================================");
        log.info("TOURNAMENT DELETION COMPLETED - ID: {}", id);
        log.info("==========================================");
    }
}
