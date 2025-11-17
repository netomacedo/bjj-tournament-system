package com.bjj.tournament.controller;

import com.bjj.tournament.dto.MatchResponseDTO;
import com.bjj.tournament.dto.TournamentCreateDTO;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.service.TournamentService;
import com.bjj.tournament.service.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for tournament operations
 */
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TournamentController {
    
    private final TournamentService tournamentService;
    private final BracketService bracketService;
    
    /**
     * Create a new tournament
     * POST /api/tournaments
     */
    @PostMapping
    public ResponseEntity<Tournament> createTournament(@Valid @RequestBody TournamentCreateDTO createDTO) {
        log.info("REST request to create tournament: {}", createDTO.getName());
        Tournament tournament = tournamentService.createTournament(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(tournament);
    }
    
    /**
     * Get all tournaments
     * GET /api/tournaments
     */
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        log.info("REST request to get all tournaments");
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        return ResponseEntity.ok(tournaments);
    }
    
    /**
     * Get tournament by ID
     * GET /api/tournaments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable Long id) {
        log.info("REST request to get tournament by ID: {}", id);
        Tournament tournament = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(tournament);
    }
    
    /**
     * Get upcoming tournaments
     * GET /api/tournaments/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Tournament>> getUpcomingTournaments() {
        log.info("REST request to get upcoming tournaments");
        List<Tournament> tournaments = tournamentService.getUpcomingTournaments();
        return ResponseEntity.ok(tournaments);
    }
    
    /**
     * Start tournament
     * POST /api/tournaments/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Tournament> startTournament(@PathVariable Long id) {
        log.info("REST request to start tournament ID: {}", id);
        Tournament tournament = tournamentService.startTournament(id);
        return ResponseEntity.ok(tournament);
    }
    
    /**
     * Close tournament registration
     * POST /api/tournaments/{id}/close-registration
     */
    @PostMapping("/{id}/close-registration")
    public ResponseEntity<Tournament> closeRegistration(@PathVariable Long id) {
        log.info("REST request to close registration for tournament ID: {}", id);
        Tournament tournament = tournamentService.closeRegistration(id);
        return ResponseEntity.ok(tournament);
    }
    
    /**
     * Complete tournament
     * POST /api/tournaments/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Tournament> completeTournament(@PathVariable Long id) {
        log.info("REST request to complete tournament ID: {}", id);
        Tournament tournament = tournamentService.completeTournament(id);
        return ResponseEntity.ok(tournament);
    }
    
    /**
     * Generate matches automatically for a division
     * POST /api/tournaments/divisions/{divisionId}/generate-matches
     */
    @PostMapping("/divisions/{divisionId}/generate-matches")
    public ResponseEntity<List<MatchResponseDTO>> generateMatches(@PathVariable Long divisionId) {
        log.info("REST request to generate matches for division ID: {}", divisionId);
        List<Match> matches = bracketService.generateMatchesAutomatically(divisionId);
        List<MatchResponseDTO> dtos = matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Generate matches manually for a division
     * POST /api/tournaments/divisions/{divisionId}/generate-matches-manual
     * Body: [[athlete1Id, athlete2Id], [athlete3Id, athlete4Id], ...]
     */
    @PostMapping("/divisions/{divisionId}/generate-matches-manual")
    public ResponseEntity<List<MatchResponseDTO>> generateMatchesManually(
            @PathVariable Long divisionId,
            @RequestBody List<long[]> matchPairs) {
        log.info("REST request to generate manual matches for division ID: {}", divisionId);
        List<Match> matches = bracketService.generateMatchesManually(divisionId, matchPairs);
        List<MatchResponseDTO> dtos = matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Delete tournament
     * DELETE /api/tournaments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        log.info("REST request to delete tournament ID: {}", id);
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convert Match entity to MatchResponseDTO
     */
    private MatchResponseDTO convertToDTO(Match match) {
        MatchResponseDTO dto = new MatchResponseDTO();

        dto.setId(match.getId());
        dto.setDivisionId(match.getDivision() != null ? match.getDivision().getId() : null);
        dto.setDivisionName(match.getDivision() != null ? match.getDivision().getName() : null);

        // Athlete 1 details
        if (match.getAthlete1() != null) {
            dto.setAthlete1Id(match.getAthlete1().getId());
            dto.setAthlete1Name(match.getAthlete1().getName());
            dto.setAthlete1Team(match.getAthlete1().getTeam());
        }

        // Athlete 2 details
        if (match.getAthlete2() != null) {
            dto.setAthlete2Id(match.getAthlete2().getId());
            dto.setAthlete2Name(match.getAthlete2().getName());
            dto.setAthlete2Team(match.getAthlete2().getTeam());
        }

        // Winner details
        if (match.getWinner() != null) {
            dto.setWinnerId(match.getWinner().getId());
            dto.setWinnerName(match.getWinner().getName());
        }

        dto.setStatus(match.getStatus());
        dto.setRoundNumber(match.getRoundNumber());
        dto.setMatchPosition(match.getMatchPosition());
        dto.setMatNumber(match.getMatNumber());

        // Scores
        dto.setAthlete1Points(match.getAthlete1Points());
        dto.setAthlete2Points(match.getAthlete2Points());
        dto.setAthlete1Advantages(match.getAthlete1Advantages());
        dto.setAthlete2Advantages(match.getAthlete2Advantages());
        dto.setAthlete1Penalties(match.getAthlete1Penalties());
        dto.setAthlete2Penalties(match.getAthlete2Penalties());
        dto.setAthlete1TotalScore(match.getAthlete1TotalScore());
        dto.setAthlete2TotalScore(match.getAthlete2TotalScore());

        dto.setFinishedBySubmission(match.getFinishedBySubmission());
        dto.setSubmissionType(match.getSubmissionType());
        dto.setDurationSeconds(match.getDurationSeconds());
        dto.setNotes(match.getNotes());

        return dto;
    }
}
