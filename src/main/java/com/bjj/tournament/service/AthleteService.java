package com.bjj.tournament.service;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.repository.AthleteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Service for managing athlete operations
 * Handles registration, updates, and athlete queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AthleteService {
    
    private final AthleteRepository athleteRepository;
    
    /**
     * Register a new athlete for the tournament
     * 
     * @param registrationDTO Athlete registration data
     * @return Registered athlete entity
     */
    @Transactional
    public Athlete registerAthlete(AthleteRegistrationDTO registrationDTO) {
        log.info("Registering new athlete: {}", registrationDTO.getName());
        
        // Check if athlete already exists by email
        if (athleteRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Athlete with email " + registrationDTO.getEmail() + " already exists");
        }
        
        // Calculate age from date of birth
        int age = Period.between(registrationDTO.getDateOfBirth(), LocalDate.now()).getYears();
        
        // Validate minimum age (IBJJF minimum is 4 years)
        if (age < 4) {
            throw new IllegalArgumentException("Athlete must be at least 4 years old to compete");
        }
        
        // Set gender to NOT_APPLICABLE for kids under 10 if not provided
        Gender gender = registrationDTO.getGender();
        if (age < 10 && (gender == null || gender == Gender.MALE || gender == Gender.FEMALE)) {
            gender = Gender.NOT_APPLICABLE;
            log.info("Setting gender to NOT_APPLICABLE for athlete under 10 years old");
        } else if (age >= 10 && gender == Gender.NOT_APPLICABLE) {
            throw new IllegalArgumentException("Gender must be specified for athletes 10 years or older");
        } else if (gender == null) {
            throw new IllegalArgumentException("Gender is required for athletes 10 years or older");
        }
        
        // Validate belt rank for age (kids can't have adult belts)
        if (age < 16 && !registrationDTO.getBeltRank().isKidsBelt() 
            && registrationDTO.getBeltRank() != BeltRank.WHITE) {
            throw new IllegalArgumentException("Invalid belt rank for age. Kids under 16 should have kids belts or white belt");
        }
        
        // Create and save athlete
        Athlete athlete = new Athlete();
        athlete.setName(registrationDTO.getName());
        athlete.setDateOfBirth(registrationDTO.getDateOfBirth());
        athlete.setAge(age);
        athlete.setGender(gender);
        athlete.setBeltRank(registrationDTO.getBeltRank());
        athlete.setWeight(registrationDTO.getWeight());
        athlete.setTeam(registrationDTO.getTeam());
        athlete.setCoachName(registrationDTO.getCoachName());
        athlete.setEmail(registrationDTO.getEmail());
        athlete.setPhone(registrationDTO.getPhone());
        athlete.setExperienceNotes(registrationDTO.getExperienceNotes());
        
        Athlete savedAthlete = athleteRepository.save(athlete);
        log.info("Successfully registered athlete with ID: {}", savedAthlete.getId());
        
        return savedAthlete;
    }
    
    /**
     * Get athlete by ID
     */
    @Transactional(readOnly = true)
    public Athlete getAthleteById(Long id) {
        return athleteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Athlete not found with ID: " + id));
    }
    
    /**
     * Get all athletes
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAllAthletes() {
        return athleteRepository.findAll();
    }
    
    /**
     * Find athletes by belt rank
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAthletesByBeltRank(BeltRank beltRank) {
        return athleteRepository.findByBeltRank(beltRank);
    }
    
    /**
     * Find athletes by age range
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAthletesByAgeRange(Integer minAge, Integer maxAge) {
        return athleteRepository.findByAgeBetween(minAge, maxAge);
    }
    
    /**
     * Find athletes by belt rank, gender, and age range
     * Used for division creation
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAthletesForDivision(BeltRank beltRank, Gender gender, Integer minAge, Integer maxAge) {
        if (gender == Gender.NOT_APPLICABLE) {
            // For kids under 10, don't filter by gender
            return athleteRepository.findByBeltRankAndAgeBetween(beltRank, minAge, maxAge);
        } else {
            return athleteRepository.findByBeltRankAndGenderAndAgeBetween(beltRank, gender, minAge, maxAge);
        }
    }
    
    /**
     * Search athletes by name
     */
    @Transactional(readOnly = true)
    public List<Athlete> searchAthletesByName(String name) {
        return athleteRepository.searchByName(name);
    }
    
    /**
     * Find athletes by team
     */
    @Transactional(readOnly = true)
    public List<Athlete> getAthletesByTeam(String team) {
        return athleteRepository.findByTeam(team);
    }
    
    /**
     * Update athlete information
     */
    @Transactional
    public Athlete updateAthlete(Long id, AthleteRegistrationDTO updateDTO) {
        log.info("Updating athlete with ID: {}", id);
        
        Athlete athlete = getAthleteById(id);
        
        // Update fields
        if (updateDTO.getName() != null) {
            athlete.setName(updateDTO.getName());
        }
        if (updateDTO.getWeight() != null) {
            athlete.setWeight(updateDTO.getWeight());
        }
        if (updateDTO.getTeam() != null) {
            athlete.setTeam(updateDTO.getTeam());
        }
        if (updateDTO.getCoachName() != null) {
            athlete.setCoachName(updateDTO.getCoachName());
        }
        if (updateDTO.getPhone() != null) {
            athlete.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getExperienceNotes() != null) {
            athlete.setExperienceNotes(updateDTO.getExperienceNotes());
        }
        
        Athlete updatedAthlete = athleteRepository.save(athlete);
        log.info("Successfully updated athlete with ID: {}", id);
        
        return updatedAthlete;
    }
    
    /**
     * Delete athlete
     */
    @Transactional
    public void deleteAthlete(Long id) {
        log.info("Deleting athlete with ID: {}", id);
        
        if (!athleteRepository.existsById(id)) {
            throw new IllegalArgumentException("Athlete not found with ID: " + id);
        }
        
        athleteRepository.deleteById(id);
        log.info("Successfully deleted athlete with ID: {}", id);
    }
}
