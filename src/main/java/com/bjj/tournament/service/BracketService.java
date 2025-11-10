package com.bjj.tournament.service;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for generating and managing tournament brackets
 * Supports both automatic and manual (coach-driven) match generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BracketService {
    
    private final DivisionRepository divisionRepository;
    private final MatchRepository matchRepository;
    
    /**
     * Generate matches automatically for a division
     * Uses seeding and bracket logic to create fair matchups
     * 
     * @param divisionId ID of the division
     * @return List of generated matches
     */
    @Transactional
    public List<Match> generateMatchesAutomatically(Long divisionId) {
        log.info("Generating matches automatically for division ID: {}", divisionId);
        
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));
        
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Matches have already been generated for this division");
        }
        
        List<Athlete> athletes = division.getAthletes();
        
        if (athletes.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 athletes to generate matches");
        }
        
        log.info("Division has {} athletes", athletes.size());
        
        // Shuffle athletes for random seeding (in real scenario, you might seed by ranking)
        List<Athlete> shuffledAthletes = new ArrayList<>(athletes);
        Collections.shuffle(shuffledAthletes);
        
        List<Match> matches = new ArrayList<>();
        
        // Generate matches based on bracket type
        switch (division.getBracketType()) {
            case SINGLE_ELIMINATION:
                matches = generateSingleEliminationBracket(division, shuffledAthletes);
                break;
            case DOUBLE_ELIMINATION:
                matches = generateDoubleEliminationBracket(division, shuffledAthletes);
                break;
            case ROUND_ROBIN:
                matches = generateRoundRobinBracket(division, shuffledAthletes);
                break;
        }
        
        // Save all matches
        List<Match> savedMatches = matchRepository.saveAll(matches);
        
        // Mark division as having matches generated
        division.setMatchesGenerated(true);
        divisionRepository.save(division);
        
        log.info("Successfully generated {} matches for division ID: {}", savedMatches.size(), divisionId);
        
        return savedMatches;
    }
    
    /**
     * Generate matches manually with coach input
     * Coach can specify exact matchups based on experience/skill level
     * 
     * @param divisionId ID of the division
     * @param matchPairs List of athlete ID pairs for matchups
     * @return List of generated matches
     */
    @Transactional
    public List<Match> generateMatchesManually(Long divisionId, List<long[]> matchPairs) {
        log.info("Generating matches manually for division ID: {}", divisionId);
        
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));
        
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Matches have already been generated for this division");
        }
        
        List<Match> matches = new ArrayList<>();
        List<Athlete> athletes = division.getAthletes();
        Map<Long, Athlete> athleteMap = new HashMap<>();
        
        for (Athlete athlete : athletes) {
            athleteMap.put(athlete.getId(), athlete);
        }
        
        int matchPosition = 1;
        for (long[] pair : matchPairs) {
            if (pair.length != 2) {
                throw new IllegalArgumentException("Each match pair must contain exactly 2 athlete IDs");
            }
            
            Athlete athlete1 = athleteMap.get(pair[0]);
            Athlete athlete2 = athleteMap.get(pair[1]);
            
            if (athlete1 == null || athlete2 == null) {
                throw new IllegalArgumentException("Athlete not found in division");
            }
            
            Match match = new Match();
            match.setDivision(division);
            match.setAthlete1(athlete1);
            match.setAthlete2(athlete2);
            match.setStatus(MatchStatus.PENDING);
            match.setRoundNumber(1); // First round for manual generation
            match.setMatchPosition(matchPosition++);
            
            matches.add(match);
        }
        
        // Save all matches
        List<Match> savedMatches = matchRepository.saveAll(matches);
        
        // Mark division as having matches generated
        division.setMatchesGenerated(true);
        divisionRepository.save(division);
        
        log.info("Successfully generated {} manual matches for division ID: {}", savedMatches.size(), divisionId);
        
        return savedMatches;
    }
    
    /**
     * Generate single elimination bracket
     * Standard tournament bracket where one loss eliminates the athlete
     */
    private List<Match> generateSingleEliminationBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();
        
        // Calculate number of matches needed for first round
        int numAthletes = athletes.size();
        int numFirstRoundMatches = numAthletes / 2;
        
        // Create first round matches
        int matchPosition = 1;
        for (int i = 0; i < numFirstRoundMatches * 2; i += 2) {
            Match match = new Match();
            match.setDivision(division);
            match.setAthlete1(athletes.get(i));
            
            // Handle odd number of athletes (one gets a bye)
            if (i + 1 < athletes.size()) {
                match.setAthlete2(athletes.get(i + 1));
            } else {
                // Bye - athlete1 automatically advances
                match.setStatus(MatchStatus.WALKOVER);
                match.setWinner(athletes.get(i));
            }
            
            match.setRoundNumber(1);
            match.setMatchPosition(matchPosition++);
            match.setStatus(match.getStatus() == null ? MatchStatus.PENDING : match.getStatus());
            
            matches.add(match);
        }
        
        // Create placeholder matches for subsequent rounds
        // These will be filled as winners are determined
        int currentRoundSize = (numAthletes + 1) / 2; // Round up for byes
        int roundNumber = 2;
        matchPosition = 1;
        
        while (currentRoundSize > 1) {
            int nextRoundSize = (currentRoundSize + 1) / 2;
            
            for (int i = 0; i < nextRoundSize; i++) {
                Match match = new Match();
                match.setDivision(division);
                match.setRoundNumber(roundNumber);
                match.setMatchPosition(matchPosition++);
                match.setStatus(MatchStatus.PENDING);
                // Athletes will be set as previous matches complete
                
                matches.add(match);
            }
            
            currentRoundSize = nextRoundSize;
            roundNumber++;
            matchPosition = 1;
        }
        
        return matches;
    }
    
    /**
     * Generate double elimination bracket
     * Losers get a second chance in the losers bracket
     */
    private List<Match> generateDoubleEliminationBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();
        
        // First, generate winners bracket (same as single elimination)
        matches.addAll(generateSingleEliminationBracket(division, athletes));
        
        // Then add losers bracket structure
        // Losers from winners bracket drop down to losers bracket
        // This is a simplified version - full implementation would be more complex
        
        log.warn("Double elimination bracket generation is simplified. Full losers bracket not implemented yet.");
        
        return matches;
    }
    
    /**
     * Generate round robin bracket
     * Every athlete fights every other athlete
     */
    private List<Match> generateRoundRobinBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();
        
        int numAthletes = athletes.size();
        int matchPosition = 1;
        
        // Generate all possible matchups
        for (int i = 0; i < numAthletes; i++) {
            for (int j = i + 1; j < numAthletes; j++) {
                Match match = new Match();
                match.setDivision(division);
                match.setAthlete1(athletes.get(i));
                match.setAthlete2(athletes.get(j));
                match.setRoundNumber(1); // All matches are in "round 1" for round robin
                match.setMatchPosition(matchPosition++);
                match.setStatus(MatchStatus.PENDING);
                
                matches.add(match);
            }
        }
        
        return matches;
    }
    
    /**
     * Advance winner to next round in single elimination bracket
     * 
     * @param matchId ID of the completed match
     * @param winnerId ID of the winning athlete
     */
    @Transactional
    public void advanceWinnerToNextRound(Long matchId, Long winnerId) {
        log.info("Advancing winner from match ID: {} to next round", matchId);
        
        Match completedMatch = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));
        
        if (completedMatch.getStatus() != MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match must be completed before advancing winner");
        }
        
        Athlete winner = completedMatch.getWinner();
        if (winner == null || !winner.getId().equals(winnerId)) {
            throw new IllegalArgumentException("Invalid winner ID");
        }
        
        Division division = completedMatch.getDivision();
        
        // Find next round match where this winner should compete
        int nextRound = completedMatch.getRoundNumber() + 1;
        int nextMatchPosition = (completedMatch.getMatchPosition() + 1) / 2;
        
        List<Match> nextRoundMatches = matchRepository.findByDivisionIdAndRoundNumber(
            division.getId(), nextRound);
        
        if (nextRoundMatches.isEmpty()) {
            log.info("No next round - this was the final match");
            
            // Mark division as completed
            division.setCompleted(true);
            divisionRepository.save(division);
            return;
        }
        
        // Find the appropriate next match
        Match nextMatch = nextRoundMatches.stream()
            .filter(m -> m.getMatchPosition() == nextMatchPosition)
            .findFirst()
            .orElse(null);
        
        if (nextMatch != null) {
            // Determine if winner goes to athlete1 or athlete2 slot
            if (completedMatch.getMatchPosition() % 2 == 1) {
                // Odd match position goes to athlete1 slot
                nextMatch.setAthlete1(winner);
            } else {
                // Even match position goes to athlete2 slot
                nextMatch.setAthlete2(winner);
            }
            
            matchRepository.save(nextMatch);
            log.info("Winner advanced to next round match ID: {}", nextMatch.getId());
        }
    }
}
