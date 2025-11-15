package com.bjj.tournament.service;

import com.bjj.tournament.dto.DivisionCreateDTO;
import com.bjj.tournament.dto.DivisionResponseDTO;
import com.bjj.tournament.dto.DivisionUpdateDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.repository.AthleteRepository;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing division operations
 * Handles division creation, athlete enrollment, and queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DivisionService {

    private final DivisionRepository divisionRepository;
    private final TournamentRepository tournamentRepository;
    private final AthleteRepository athleteRepository;

    /**
     * Create a new division for a tournament
     */
    @Transactional
    public DivisionResponseDTO createDivision(Long tournamentId, DivisionCreateDTO createDTO) {
        log.info("Creating new division for tournament ID: {}", tournamentId);

        // Validate tournament exists and registration is open
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found with ID: " + tournamentId));

        if (!tournament.getRegistrationOpen()) {
            throw new IllegalStateException("Tournament registration is closed");
        }

        if (tournament.getStarted()) {
            throw new IllegalStateException("Cannot create divisions after tournament has started");
        }

        // Check if division with same criteria already exists
        divisionRepository.findByTournamentIdAndBeltRankAndAgeCategoryAndGender(
            tournamentId,
            createDTO.getBeltRank(),
            createDTO.getAgeCategory(),
            createDTO.getGender()
        ).ifPresent(d -> {
            throw new IllegalArgumentException(
                "Division already exists for this tournament with belt rank: " +
                createDTO.getBeltRank() + ", age category: " + createDTO.getAgeCategory() +
                ", and gender: " + createDTO.getGender()
            );
        });

        // Create division
        Division division = new Division();
        division.setTournament(tournament);
        division.setBeltRank(createDTO.getBeltRank());
        division.setAgeCategory(createDTO.getAgeCategory());
        division.setGender(createDTO.getGender());
        division.setWeightClass(createDTO.getWeightClass());
        division.setBracketType(createDTO.getBracketType());

        Division savedDivision = divisionRepository.save(division);
        log.info("Successfully created division with ID: {} - {}", savedDivision.getId(), savedDivision.getName());

        return convertToResponseDTO(savedDivision);
    }

    /**
     * Get division by ID
     */
    @Transactional(readOnly = true)
    public DivisionResponseDTO getDivisionById(Long divisionId) {
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        return convertToResponseDTO(division);
    }

    /**
     * Get all divisions for a tournament
     */
    @Transactional(readOnly = true)
    public List<DivisionResponseDTO> getDivisionsByTournament(Long tournamentId) {
        // Validate tournament exists
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new IllegalArgumentException("Tournament not found with ID: " + tournamentId);
        }

        List<Division> divisions = divisionRepository.findByTournamentId(tournamentId);
        return divisions.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update division (only bracket type and weight class can be updated)
     */
    @Transactional
    public DivisionResponseDTO updateDivision(Long divisionId, DivisionUpdateDTO updateDTO) {
        log.info("Updating division ID: {}", divisionId);

        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        // Cannot update if matches have been generated
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Cannot update division after matches have been generated");
        }

        // Update allowed fields
        if (updateDTO.getBracketType() != null) {
            division.setBracketType(updateDTO.getBracketType());
        }

        if (updateDTO.getWeightClass() != null) {
            division.setWeightClass(updateDTO.getWeightClass());
        }

        Division updatedDivision = divisionRepository.save(division);
        log.info("Successfully updated division ID: {}", divisionId);

        return convertToResponseDTO(updatedDivision);
    }

    /**
     * Delete division
     */
    @Transactional
    public void deleteDivision(Long divisionId) {
        log.info("Deleting division ID: {}", divisionId);

        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        // Cannot delete if matches have been generated
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Cannot delete division after matches have been generated");
        }

        divisionRepository.delete(division);
        log.info("Successfully deleted division ID: {}", divisionId);
    }

    /**
     * Enroll athlete into a division
     */
    @Transactional
    public DivisionResponseDTO enrollAthlete(Long divisionId, Long athleteId) {
        log.info("Enrolling athlete {} into division {}", athleteId, divisionId);

        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        Athlete athlete = athleteRepository.findById(athleteId)
            .orElseThrow(() -> new IllegalArgumentException("Athlete not found with ID: " + athleteId));

        // Validate tournament registration is open
        if (!division.getTournament().getRegistrationOpen()) {
            throw new IllegalStateException("Tournament registration is closed");
        }

        // Cannot enroll after matches have been generated
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Cannot enroll athletes after matches have been generated");
        }

        // Check if athlete is already enrolled
        if (division.getAthletes().contains(athlete)) {
            throw new IllegalArgumentException("Athlete is already enrolled in this division");
        }

        // Validate athlete meets division criteria
        validateAthleteEligibility(athlete, division);

        // Enroll athlete
        division.addAthlete(athlete);
        Division savedDivision = divisionRepository.save(division);

        log.info("Successfully enrolled athlete {} into division {}", athleteId, divisionId);
        return convertToResponseDTO(savedDivision);
    }

    /**
     * Remove athlete from a division
     */
    @Transactional
    public DivisionResponseDTO removeAthlete(Long divisionId, Long athleteId) {
        log.info("Removing athlete {} from division {}", athleteId, divisionId);

        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        Athlete athlete = athleteRepository.findById(athleteId)
            .orElseThrow(() -> new IllegalArgumentException("Athlete not found with ID: " + athleteId));

        // Cannot remove after matches have been generated
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Cannot remove athletes after matches have been generated");
        }

        // Check if athlete is enrolled
        if (!division.getAthletes().contains(athlete)) {
            throw new IllegalArgumentException("Athlete is not enrolled in this division");
        }

        // Remove athlete
        division.removeAthlete(athlete);
        Division savedDivision = divisionRepository.save(division);

        log.info("Successfully removed athlete {} from division {}", athleteId, divisionId);
        return convertToResponseDTO(savedDivision);
    }

    /**
     * Get all athletes in a division
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAthletesByDivision(Long divisionId) {
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));

        return division.getAthletes();
    }

    /**
     * Get divisions ready for match generation (2+ athletes, no matches generated yet)
     */
    @Transactional(readOnly = true)
    public List<DivisionResponseDTO> getDivisionsReadyForMatchGeneration() {
        List<Division> divisions = divisionRepository.findDivisionsReadyForMatchGeneration();
        return divisions.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Validate athlete meets division eligibility criteria
     */
    private void validateAthleteEligibility(Athlete athlete, Division division) {
        // Check belt rank
        if (!athlete.getBeltRank().equals(division.getBeltRank())) {
            throw new IllegalArgumentException(
                "Athlete belt rank (" + athlete.getBeltRank() +
                ") does not match division requirement (" + division.getBeltRank() + ")"
            );
        }

        // Check gender (if applicable)
        if (athlete.requiresGenderSeparation() &&
            division.getGender() != com.bjj.tournament.enums.Gender.NOT_APPLICABLE) {
            if (!athlete.getGender().equals(division.getGender())) {
                throw new IllegalArgumentException(
                    "Athlete gender (" + athlete.getGender() +
                    ") does not match division requirement (" + division.getGender() + ")"
                );
            }
        }

        // Check age category - use the enum's built-in age range validation
        int athleteAge = athlete.getAge();
        int minAge = division.getAgeCategory().getMinAge();
        int maxAge = division.getAgeCategory().getMaxAge();

        if (athleteAge < minAge || athleteAge > maxAge) {
            throw new IllegalArgumentException(
                "Athlete age (" + athleteAge +
                ") does not match division age category (" + division.getAgeCategory().getDisplayName() +
                ": " + minAge + "-" + maxAge + " years)"
            );
        }

        // Check weight class if specified
        // WeightClass enum only stores maxWeight, so we just check if athlete is under the max
        if (division.getWeightClass() != null) {
            double maxWeight = division.getWeightClass().getMaxWeightKg();

            // If max weight is 999.9, it's unlimited (ultra heavy/super heavy)
            if (maxWeight < 999.0 && athlete.getWeight() > maxWeight) {
                throw new IllegalArgumentException(
                    "Athlete weight (" + athlete.getWeight() + " kg) exceeds division weight class maximum (" +
                    maxWeight + " kg)"
                );
            }
        }
    }

    /**
     * Convert Division entity to DivisionResponseDTO
     */
    private DivisionResponseDTO convertToResponseDTO(Division division) {
        DivisionResponseDTO dto = new DivisionResponseDTO();
        dto.setId(division.getId());
        dto.setTournamentId(division.getTournament().getId());
        dto.setName(division.getName());
        dto.setBeltRank(division.getBeltRank());
        dto.setAgeCategory(division.getAgeCategory());
        dto.setGender(division.getGender());
        dto.setWeightClass(division.getWeightClass());
        dto.setBracketType(division.getBracketType());

        // Force lazy loading of collections by accessing them
        int athleteCount = 0;
        int matchCount = 0;

        if (division.getAthletes() != null) {
            athleteCount = division.getAthletes().size(); // This triggers lazy loading
        }

        if (division.getMatches() != null) {
            matchCount = division.getMatches().size(); // This triggers lazy loading
        }

        dto.setAthleteCount(athleteCount);
        dto.setMatchCount(matchCount);
        dto.setMatchesGenerated(division.getMatchesGenerated());
        dto.setCompleted(division.getCompleted());
        dto.setCreatedAt(division.getCreatedAt());
        dto.setUpdatedAt(division.getUpdatedAt());
        return dto;
    }
}