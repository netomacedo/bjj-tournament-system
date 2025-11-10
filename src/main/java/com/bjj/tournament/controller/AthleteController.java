package com.bjj.tournament.controller;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.service.AthleteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for athlete operations
 * Provides endpoints for athlete registration and management
 */
@RestController
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow CORS for React frontend
public class AthleteController {
    
    private final AthleteService athleteService;
    
    /**
     * Register a new athlete
     * POST /api/athletes
     */
    @PostMapping
    public ResponseEntity<Athlete> registerAthlete(@Valid @RequestBody AthleteRegistrationDTO registrationDTO) {
        log.info("REST request to register athlete: {}", registrationDTO.getName());
        Athlete athlete = athleteService.registerAthlete(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(athlete);
    }
    
    /**
     * Get all athletes
     * GET /api/athletes
     */
    @GetMapping
    public ResponseEntity<List<Athlete>> getAllAthletes() {
        log.info("REST request to get all athletes");
        List<Athlete> athletes = athleteService.getAllAthletes();
        return ResponseEntity.ok(athletes);
    }
    
    /**
     * Get athlete by ID
     * GET /api/athletes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Athlete> getAthleteById(@PathVariable Long id) {
        log.info("REST request to get athlete by ID: {}", id);
        Athlete athlete = athleteService.getAthleteById(id);
        return ResponseEntity.ok(athlete);
    }
    
    /**
     * Get athletes by belt rank
     * GET /api/athletes/belt/{beltRank}
     */
    @GetMapping("/belt/{beltRank}")
    public ResponseEntity<List<Athlete>> getAthletesByBeltRank(@PathVariable BeltRank beltRank) {
        log.info("REST request to get athletes by belt rank: {}", beltRank);
        List<Athlete> athletes = athleteService.getAthletesByBeltRank(beltRank);
        return ResponseEntity.ok(athletes);
    }
    
    /**
     * Get athletes by age range
     * GET /api/athletes/age?min=18&max=29
     */
    @GetMapping("/age")
    public ResponseEntity<List<Athlete>> getAthletesByAgeRange(
            @RequestParam Integer min,
            @RequestParam Integer max) {
        log.info("REST request to get athletes by age range: {}-{}", min, max);
        List<Athlete> athletes = athleteService.getAthletesByAgeRange(min, max);
        return ResponseEntity.ok(athletes);
    }
    
    /**
     * Search athletes by name
     * GET /api/athletes/search?name=John
     */
    @GetMapping("/search")
    public ResponseEntity<List<Athlete>> searchAthletesByName(@RequestParam String name) {
        log.info("REST request to search athletes by name: {}", name);
        List<Athlete> athletes = athleteService.searchAthletesByName(name);
        return ResponseEntity.ok(athletes);
    }
    
    /**
     * Get athletes by team
     * GET /api/athletes/team/{team}
     */
    @GetMapping("/team/{team}")
    public ResponseEntity<List<Athlete>> getAthletesByTeam(@PathVariable String team) {
        log.info("REST request to get athletes by team: {}", team);
        List<Athlete> athletes = athleteService.getAthletesByTeam(team);
        return ResponseEntity.ok(athletes);
    }
    
    /**
     * Update athlete
     * PUT /api/athletes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Athlete> updateAthlete(
            @PathVariable Long id,
            @Valid @RequestBody AthleteRegistrationDTO updateDTO) {
        log.info("REST request to update athlete ID: {}", id);
        Athlete athlete = athleteService.updateAthlete(id, updateDTO);
        return ResponseEntity.ok(athlete);
    }
    
    /**
     * Delete athlete
     * DELETE /api/athletes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAthlete(@PathVariable Long id) {
        log.info("REST request to delete athlete ID: {}", id);
        athleteService.deleteAthlete(id);
        return ResponseEntity.noContent().build();
    }
}
