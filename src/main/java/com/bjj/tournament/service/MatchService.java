package com.bjj.tournament.service;

import com.bjj.tournament.dto.MatchResponseDTO;
import com.bjj.tournament.dto.MatchUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing match operations
 * Handles match updates, scoring, and status changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {
    
    private final MatchRepository matchRepository;
    private final AthleteRepository athleteRepository;
    private final BracketService bracketService;
    
    /**
     * Get match entity by ID
     */
    @Transactional(readOnly = true)
    public Match getMatchEntityById(Long id) {
        return matchRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + id));
    }

    /**
     * Get match entities for a division
     */
    @Transactional(readOnly = true)
    public List<Match> getMatchEntitiesByDivision(Long divisionId) {
        return matchRepository.findByDivisionId(divisionId);
    }
    
    /**
     * Get matches for a specific round in a division
     */
    @Transactional(readOnly = true)
    public List<Match> getMatchesByDivisionAndRound(Long divisionId, Integer roundNumber) {
        return matchRepository.findByDivisionIdAndRoundNumber(divisionId, roundNumber);
    }
    
    /**
     * Get all matches for a specific athlete
     */
    @Transactional(readOnly = true)
    public List<Match> getMatchesByAthlete(Long athleteId) {
        return matchRepository.findMatchesByAthleteId(athleteId);
    }
    
    /**
     * Get pending matches for a division
     */
    @Transactional(readOnly = true)
    public List<Match> getPendingMatches(Long divisionId) {
        return matchRepository.findPendingMatchesByDivision(divisionId);
    }
    
    /**
     * Update match scores and status
     * Used during competition to record results
     */
    @Transactional
    public Match updateMatchEntity(Long matchId, MatchUpdateDTO updateDTO) {
        log.info("Updating match ID: {}", matchId);

        Match match = getMatchEntityById(matchId);
        
        // Update scores if provided
        if (updateDTO.getAthlete1Points() != null) {
            match.setAthlete1Points(updateDTO.getAthlete1Points());
        }
        if (updateDTO.getAthlete2Points() != null) {
            match.setAthlete2Points(updateDTO.getAthlete2Points());
        }
        if (updateDTO.getAthlete1Advantages() != null) {
            match.setAthlete1Advantages(updateDTO.getAthlete1Advantages());
        }
        if (updateDTO.getAthlete2Advantages() != null) {
            match.setAthlete2Advantages(updateDTO.getAthlete2Advantages());
        }
        if (updateDTO.getAthlete1Penalties() != null) {
            match.setAthlete1Penalties(updateDTO.getAthlete1Penalties());
        }
        if (updateDTO.getAthlete2Penalties() != null) {
            match.setAthlete2Penalties(updateDTO.getAthlete2Penalties());
        }
        
        // Update submission info if provided
        if (updateDTO.getFinishedBySubmission() != null) {
            match.setFinishedBySubmission(updateDTO.getFinishedBySubmission());
        }
        if (updateDTO.getSubmissionType() != null) {
            match.setSubmissionType(updateDTO.getSubmissionType());
        }
        
        // Update status if provided
        if (updateDTO.getStatus() != null) {
            match.setStatus(updateDTO.getStatus());
        }
        
        // Update notes if provided
        if (updateDTO.getNotes() != null) {
            match.setNotes(updateDTO.getNotes());
        }
        
        // If match is being completed, determine winner
        if (updateDTO.getStatus() == MatchStatus.COMPLETED || 
            (match.getStatus() == MatchStatus.COMPLETED && updateDTO.getWinnerId() != null)) {
            
            if (updateDTO.getWinnerId() != null) {
                // Manual winner selection
                Athlete winner = athleteRepository.findById(updateDTO.getWinnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Winner athlete not found"));
                match.setWinner(winner);
            } else {
                // Automatic winner determination based on IBJJF rules
                match.determineWinner();
            }
            
            match.setStatus(MatchStatus.COMPLETED);
        }
        
        Match savedMatch = matchRepository.save(match);
        log.info("Successfully updated match ID: {}", matchId);
        
        // If match is completed and has a winner, advance to next round
        if (savedMatch.getStatus() == MatchStatus.COMPLETED && savedMatch.getWinner() != null) {
            try {
                bracketService.advanceWinnerToNextRound(matchId, savedMatch.getWinner().getId());
            } catch (Exception e) {
                log.error("Error advancing winner to next round: {}", e.getMessage());
                // Don't fail the match update if bracket advancement fails
            }
        }
        
        return savedMatch;
    }
    
    /**
     * Start a match (change status to IN_PROGRESS)
     */
    @Transactional
    public Match startMatch(Long matchId) {
        log.info("Starting match ID: {}", matchId);
        
        Match match = getMatchEntityById(matchId);
        
        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalStateException("Only pending matches can be started");
        }
        
        if (match.getAthlete1() == null || match.getAthlete2() == null) {
            throw new IllegalStateException("Both athletes must be assigned before match can start");
        }
        
        match.setStatus(MatchStatus.IN_PROGRESS);
        
        Match savedMatch = matchRepository.save(match);
        log.info("Successfully started match ID: {}", matchId);
        
        return savedMatch;
    }
    
    /**
     * Record a submission victory
     */
    @Transactional
    public Match recordSubmission(Long matchId, Long winnerId, String submissionType) {
        log.info("Recording submission for match ID: {} by athlete ID: {}", matchId, winnerId);
        
        Match match = getMatchEntityById(matchId);
        
        Athlete winner = athleteRepository.findById(winnerId)
            .orElseThrow(() -> new IllegalArgumentException("Winner athlete not found"));
        
        // Verify winner is in this match
        if (!winner.equals(match.getAthlete1()) && !winner.equals(match.getAthlete2())) {
            throw new IllegalArgumentException("Winner must be one of the match participants");
        }
        
        match.setFinishedBySubmission(true);
        match.setSubmissionType(submissionType);
        match.setWinner(winner);
        match.setStatus(MatchStatus.COMPLETED);
        
        Match savedMatch = matchRepository.save(match);
        
        // Advance winner to next round
        try {
            bracketService.advanceWinnerToNextRound(matchId, winnerId);
        } catch (Exception e) {
            log.error("Error advancing winner to next round: {}", e.getMessage());
        }
        
        log.info("Successfully recorded submission for match ID: {}", matchId);
        
        return savedMatch;
    }
    
    /**
     * Record a walkover (one athlete didn't show up)
     */
    @Transactional
    public Match recordWalkover(Long matchId, Long winnerId) {
        log.info("Recording walkover for match ID: {} - winner ID: {}", matchId, winnerId);
        
        Match match = getMatchEntityById(matchId);
        
        Athlete winner = athleteRepository.findById(winnerId)
            .orElseThrow(() -> new IllegalArgumentException("Winner athlete not found"));
        
        // Verify winner is in this match
        if (!winner.equals(match.getAthlete1()) && !winner.equals(match.getAthlete2())) {
            throw new IllegalArgumentException("Winner must be one of the match participants");
        }
        
        match.setWinner(winner);
        match.setStatus(MatchStatus.WALKOVER);
        match.setNotes("Walkover - opponent did not show up");
        
        Match savedMatch = matchRepository.save(match);
        
        // Advance winner to next round
        try {
            bracketService.advanceWinnerToNextRound(matchId, winnerId);
        } catch (Exception e) {
            log.error("Error advancing winner to next round: {}", e.getMessage());
        }
        
        log.info("Successfully recorded walkover for match ID: {}", matchId);
        
        return savedMatch;
    }
    
    /**
     * Assign match to a mat/ring
     */
    @Transactional
    public Match assignToMat(Long matchId, Integer matNumber) {
        log.info("Assigning match ID: {} to mat number: {}", matchId, matNumber);

        Match match = getMatchEntityById(matchId);
        match.setMatNumber(matNumber);

        Match savedMatch = matchRepository.save(match);
        log.info("Successfully assigned match to mat");

        return savedMatch;
    }

    // DTO conversion methods

    /**
     * Get match by ID as DTO
     */
    @Transactional(readOnly = true)
    public MatchResponseDTO getMatchById(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + id));
        return convertToDTO(match);
    }

    /**
     * Get all matches for a division as DTOs
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDTO> getMatchesByDivision(Long divisionId) {
        List<Match> matches = matchRepository.findByDivisionId(divisionId);
        return matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update match and return as DTO
     */
    @Transactional
    public MatchResponseDTO updateMatch(Long matchId, MatchUpdateDTO updateDTO) {
        Match updatedMatch = this.updateMatchInternal(matchId, updateDTO);
        return convertToDTO(updatedMatch);
    }

    /**
     * Generate matches for a division
     */
    @Transactional
    public List<MatchResponseDTO> generateMatches(Long divisionId) {
        log.info("Generating matches for division ID: {}", divisionId);
        List<Match> matches = bracketService.generateMatchesAutomatically(divisionId);
        return matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
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

    /**
     * Internal method for updating match (used by both public methods)
     */
    private Match updateMatchInternal(Long matchId, MatchUpdateDTO updateDTO) {
        log.info("Updating match ID: {}", matchId);

        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Update scores if provided
        if (updateDTO.getAthlete1Points() != null) {
            match.setAthlete1Points(updateDTO.getAthlete1Points());
        }
        if (updateDTO.getAthlete2Points() != null) {
            match.setAthlete2Points(updateDTO.getAthlete2Points());
        }
        if (updateDTO.getAthlete1Advantages() != null) {
            match.setAthlete1Advantages(updateDTO.getAthlete1Advantages());
        }
        if (updateDTO.getAthlete2Advantages() != null) {
            match.setAthlete2Advantages(updateDTO.getAthlete2Advantages());
        }
        if (updateDTO.getAthlete1Penalties() != null) {
            match.setAthlete1Penalties(updateDTO.getAthlete1Penalties());
        }
        if (updateDTO.getAthlete2Penalties() != null) {
            match.setAthlete2Penalties(updateDTO.getAthlete2Penalties());
        }

        // Update submission info if provided
        if (updateDTO.getFinishedBySubmission() != null) {
            match.setFinishedBySubmission(updateDTO.getFinishedBySubmission());
        }
        if (updateDTO.getSubmissionType() != null) {
            match.setSubmissionType(updateDTO.getSubmissionType());
        }

        // Update status if provided
        if (updateDTO.getStatus() != null) {
            match.setStatus(updateDTO.getStatus());
        }

        // Update notes if provided
        if (updateDTO.getNotes() != null) {
            match.setNotes(updateDTO.getNotes());
        }

        // If match is being completed, determine winner
        if (updateDTO.getStatus() == MatchStatus.COMPLETED ||
            (match.getStatus() == MatchStatus.COMPLETED && updateDTO.getWinnerId() != null)) {

            if (updateDTO.getWinnerId() != null) {
                // Manual winner selection
                Athlete winner = athleteRepository.findById(updateDTO.getWinnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Winner athlete not found"));
                match.setWinner(winner);
            } else {
                // Automatic winner determination based on IBJJF rules
                match.determineWinner();
            }

            match.setStatus(MatchStatus.COMPLETED);
        }

        Match savedMatch = matchRepository.save(match);
        log.info("Successfully updated match ID: {}", matchId);

        // If match is completed and has a winner, advance to next round
        if (savedMatch.getStatus() == MatchStatus.COMPLETED && savedMatch.getWinner() != null) {
            try {
                bracketService.advanceWinnerToNextRound(matchId, savedMatch.getWinner().getId());
            } catch (Exception e) {
                log.error("Error advancing winner to next round: {}", e.getMessage());
                // Don't fail the match update if bracket advancement fails
            }
        }

        return savedMatch;
    }
}
