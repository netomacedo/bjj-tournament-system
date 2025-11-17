package com.bjj.tournament.controller;

import com.bjj.tournament.dto.MatchResponseDTO;
import com.bjj.tournament.dto.MatchUpdateDTO;
import com.bjj.tournament.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for match operations
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchService matchService;

    /**
     * Get all matches for a division
     * GET /api/divisions/{divisionId}/matches
     */
    @GetMapping("/divisions/{divisionId}/matches")
    public ResponseEntity<List<MatchResponseDTO>> getMatchesByDivision(@PathVariable Long divisionId) {
        log.info("REST request to get matches for division ID: {}", divisionId);
        List<MatchResponseDTO> matches = matchService.getMatchesByDivision(divisionId);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get all matches for a division (backwards compatible endpoint)
     * GET /api/matches/division/{divisionId}
     */
    @GetMapping("/matches/division/{divisionId}")
    public ResponseEntity<List<MatchResponseDTO>> getMatchesByDivisionLegacy(@PathVariable Long divisionId) {
        log.info("REST request to get matches for division ID: {} (legacy endpoint)", divisionId);
        List<MatchResponseDTO> matches = matchService.getMatchesByDivision(divisionId);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get match by ID
     * GET /api/matches/{matchId}
     */
    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchResponseDTO> getMatchById(@PathVariable Long matchId) {
        log.info("REST request to get match by ID: {}", matchId);
        MatchResponseDTO match = matchService.getMatchById(matchId);
        return ResponseEntity.ok(match);
    }

    /**
     * Update match result
     * PUT /api/matches/{matchId}
     */
    @PutMapping("/matches/{matchId}")
    public ResponseEntity<MatchResponseDTO> updateMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchUpdateDTO updateDTO) {
        log.info("REST request to update match ID: {}", matchId);
        MatchResponseDTO match = matchService.updateMatch(matchId, updateDTO);
        return ResponseEntity.ok(match);
    }

    /**
     * Generate matches for a division
     * POST /api/divisions/{divisionId}/generate-matches
     */
    @PostMapping("/divisions/{divisionId}/generate-matches")
    public ResponseEntity<List<MatchResponseDTO>> generateMatches(@PathVariable Long divisionId) {
        log.info("REST request to generate matches for division ID: {}", divisionId);
        List<MatchResponseDTO> matches = matchService.generateMatches(divisionId);
        return ResponseEntity.ok(matches);
    }
}