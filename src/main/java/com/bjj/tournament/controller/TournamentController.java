package com.bjj.tournament.controller;

import com.bjj.tournament.dto.TournamentCreateDTO;
import com.bjj.tournament.dto.MatchUpdateDTO;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.service.TournamentService;
import com.bjj.tournament.service.MatchService;
import com.bjj.tournament.service.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<Match>> generateMatches(@PathVariable Long divisionId) {
        log.info("REST request to generate matches for division ID: {}", divisionId);
        List<Match> matches = bracketService.generateMatchesAutomatically(divisionId);
        return ResponseEntity.ok(matches);
    }
    
    /**
     * Generate matches manually for a division
     * POST /api/tournaments/divisions/{divisionId}/generate-matches-manual
     * Body: [[athlete1Id, athlete2Id], [athlete3Id, athlete4Id], ...]
     */
    @PostMapping("/divisions/{divisionId}/generate-matches-manual")
    public ResponseEntity<List<Match>> generateMatchesManually(
            @PathVariable Long divisionId,
            @RequestBody List<long[]> matchPairs) {
        log.info("REST request to generate manual matches for division ID: {}", divisionId);
        List<Match> matches = bracketService.generateMatchesManually(divisionId, matchPairs);
        return ResponseEntity.ok(matches);
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
}

/**
 * REST Controller for match operations
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
class MatchController {
    
    private final MatchService matchService;
    
    /**
     * Get match by ID
     * GET /api/matches/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        log.info("REST request to get match by ID: {}", id);
        Match match = matchService.getMatchById(id);
        return ResponseEntity.ok(match);
    }
    
    /**
     * Get all matches for a division
     * GET /api/matches/division/{divisionId}
     */
    @GetMapping("/division/{divisionId}")
    public ResponseEntity<List<Match>> getMatchesByDivision(@PathVariable Long divisionId) {
        log.info("REST request to get matches for division ID: {}", divisionId);
        List<Match> matches = matchService.getMatchesByDivision(divisionId);
        return ResponseEntity.ok(matches);
    }
    
    /**
     * Get matches for a specific round in a division
     * GET /api/matches/division/{divisionId}/round/{roundNumber}
     */
    @GetMapping("/division/{divisionId}/round/{roundNumber}")
    public ResponseEntity<List<Match>> getMatchesByDivisionAndRound(
            @PathVariable Long divisionId,
            @PathVariable Integer roundNumber) {
        log.info("REST request to get matches for division {} round {}", divisionId, roundNumber);
        List<Match> matches = matchService.getMatchesByDivisionAndRound(divisionId, roundNumber);
        return ResponseEntity.ok(matches);
    }
    
    /**
     * Get all matches for an athlete
     * GET /api/matches/athlete/{athleteId}
     */
    @GetMapping("/athlete/{athleteId}")
    public ResponseEntity<List<Match>> getMatchesByAthlete(@PathVariable Long athleteId) {
        log.info("REST request to get matches for athlete ID: {}", athleteId);
        List<Match> matches = matchService.getMatchesByAthlete(athleteId);
        return ResponseEntity.ok(matches);
    }
    
    /**
     * Get pending matches for a division
     * GET /api/matches/division/{divisionId}/pending
     */
    @GetMapping("/division/{divisionId}/pending")
    public ResponseEntity<List<Match>> getPendingMatches(@PathVariable Long divisionId) {
        log.info("REST request to get pending matches for division ID: {}", divisionId);
        List<Match> matches = matchService.getPendingMatches(divisionId);
        return ResponseEntity.ok(matches);
    }
    
    /**
     * Start a match
     * POST /api/matches/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Match> startMatch(@PathVariable Long id) {
        log.info("REST request to start match ID: {}", id);
        Match match = matchService.startMatch(id);
        return ResponseEntity.ok(match);
    }
    
    /**
     * Update match scores and status
     * PUT /api/matches/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(
            @PathVariable Long id,
            @Valid @RequestBody MatchUpdateDTO updateDTO) {
        log.info("REST request to update match ID: {}", id);
        Match match = matchService.updateMatch(id, updateDTO);
        return ResponseEntity.ok(match);
    }
    
    /**
     * Record a submission victory
     * POST /api/matches/{id}/submission
     */
    @PostMapping("/{id}/submission")
    public ResponseEntity<Match> recordSubmission(
            @PathVariable Long id,
            @RequestParam Long winnerId,
            @RequestParam String submissionType) {
        log.info("REST request to record submission for match ID: {}", id);
        Match match = matchService.recordSubmission(id, winnerId, submissionType);
        return ResponseEntity.ok(match);
    }
    
    /**
     * Record a walkover
     * POST /api/matches/{id}/walkover
     */
    @PostMapping("/{id}/walkover")
    public ResponseEntity<Match> recordWalkover(
            @PathVariable Long id,
            @RequestParam Long winnerId) {
        log.info("REST request to record walkover for match ID: {}", id);
        Match match = matchService.recordWalkover(id, winnerId);
        return ResponseEntity.ok(match);
    }
    
    /**
     * Assign match to a mat/ring
     * POST /api/matches/{id}/assign-mat
     */
    @PostMapping("/{id}/assign-mat")
    public ResponseEntity<Match> assignToMat(
            @PathVariable Long id,
            @RequestParam Integer matNumber) {
        log.info("REST request to assign match {} to mat {}", id, matNumber);
        Match match = matchService.assignToMat(id, matNumber);
        return ResponseEntity.ok(match);
    }
}
