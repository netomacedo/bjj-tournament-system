package com.bjj.tournament.controller;

import com.bjj.tournament.dto.DivisionCreateDTO;
import com.bjj.tournament.dto.DivisionResponseDTO;
import com.bjj.tournament.dto.DivisionUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.service.DivisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for division operations
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DivisionController {

    private final DivisionService divisionService;

    /**
     * Create a new division for a tournament
     * POST /api/tournaments/{tournamentId}/divisions
     */
    @PostMapping("/tournaments/{tournamentId}/divisions")
    public ResponseEntity<DivisionResponseDTO> createDivision(
            @PathVariable Long tournamentId,
            @Valid @RequestBody DivisionCreateDTO createDTO) {
        log.info("REST request to create division for tournament ID: {}", tournamentId);
        DivisionResponseDTO division = divisionService.createDivision(tournamentId, createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(division);
    }

    /**
     * Get all divisions for a tournament
     * GET /api/tournaments/{tournamentId}/divisions
     */
    @GetMapping("/tournaments/{tournamentId}/divisions")
    public ResponseEntity<List<DivisionResponseDTO>> getDivisionsByTournament(@PathVariable Long tournamentId) {
        log.info("REST request to get divisions for tournament ID: {}", tournamentId);
        List<DivisionResponseDTO> divisions = divisionService.getDivisionsByTournament(tournamentId);
        return ResponseEntity.ok(divisions);
    }

    /**
     * Get division by ID
     * GET /api/divisions/{divisionId}
     */
    @GetMapping("/divisions/{divisionId}")
    public ResponseEntity<DivisionResponseDTO> getDivisionById(@PathVariable Long divisionId) {
        log.info("REST request to get division by ID: {}", divisionId);
        DivisionResponseDTO division = divisionService.getDivisionById(divisionId);
        return ResponseEntity.ok(division);
    }

    /**
     * Update division
     * PUT /api/divisions/{divisionId}
     */
    @PutMapping("/divisions/{divisionId}")
    public ResponseEntity<DivisionResponseDTO> updateDivision(
            @PathVariable Long divisionId,
            @Valid @RequestBody DivisionUpdateDTO updateDTO) {
        log.info("REST request to update division ID: {}", divisionId);
        DivisionResponseDTO division = divisionService.updateDivision(divisionId, updateDTO);
        return ResponseEntity.ok(division);
    }

    /**
     * Delete division
     * DELETE /api/divisions/{divisionId}
     */
    @DeleteMapping("/divisions/{divisionId}")
    public ResponseEntity<Void> deleteDivision(@PathVariable Long divisionId) {
        log.info("REST request to delete division ID: {}", divisionId);
        divisionService.deleteDivision(divisionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enroll athlete into a division
     * POST /api/divisions/{divisionId}/athletes/{athleteId}
     */
    @PostMapping("/divisions/{divisionId}/athletes/{athleteId}")
    public ResponseEntity<DivisionResponseDTO> enrollAthlete(
            @PathVariable Long divisionId,
            @PathVariable Long athleteId) {
        log.info("REST request to enroll athlete {} into division {}", athleteId, divisionId);
        DivisionResponseDTO division = divisionService.enrollAthlete(divisionId, athleteId);
        return ResponseEntity.ok(division);
    }

    /**
     * Remove athlete from a division
     * DELETE /api/divisions/{divisionId}/athletes/{athleteId}
     */
    @DeleteMapping("/divisions/{divisionId}/athletes/{athleteId}")
    public ResponseEntity<DivisionResponseDTO> removeAthlete(
            @PathVariable Long divisionId,
            @PathVariable Long athleteId) {
        log.info("REST request to remove athlete {} from division {}", athleteId, divisionId);
        DivisionResponseDTO division = divisionService.removeAthlete(divisionId, athleteId);
        return ResponseEntity.ok(division);
    }

    /**
     * Get all athletes in a division
     * GET /api/divisions/{divisionId}/athletes
     */
    @GetMapping("/divisions/{divisionId}/athletes")
    public ResponseEntity<List<Athlete>> getAthletesByDivision(@PathVariable Long divisionId) {
        log.info("REST request to get athletes for division ID: {}", divisionId);
        List<Athlete> athletes = divisionService.getAthletesByDivision(divisionId);
        return ResponseEntity.ok(athletes);
    }

    /**
     * Get divisions ready for match generation
     * GET /api/divisions/ready-for-matches
     */
    @GetMapping("/divisions/ready-for-matches")
    public ResponseEntity<List<DivisionResponseDTO>> getDivisionsReadyForMatchGeneration() {
        log.info("REST request to get divisions ready for match generation");
        List<DivisionResponseDTO> divisions = divisionService.getDivisionsReadyForMatchGeneration();
        return ResponseEntity.ok(divisions);
    }
}