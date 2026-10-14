package com.bjj.tournament.repository;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Athlete entity
 * Provides database operations for athlete management
 */
@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    
    /**
     * Find every athlete on one organization's roster
     */
    List<Athlete> findByOrganizationId(Long organizationId);

    /**
     * Find athletes by belt rank, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndBeltRank(Long organizationId, BeltRank beltRank);

    /**
     * Find athletes by gender, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndGender(Long organizationId, Gender gender);

    /**
     * Find athletes by age range, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndAgeBetween(Long organizationId, Integer minAge, Integer maxAge);

    /**
     * Find athletes by belt rank and age range, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndBeltRankAndAgeBetween(
        Long organizationId, BeltRank beltRank, Integer minAge, Integer maxAge);

    /**
     * Find athletes by belt rank, gender, and age range, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndBeltRankAndGenderAndAgeBetween(
        Long organizationId,
        BeltRank beltRank,
        Gender gender,
        Integer minAge,
        Integer maxAge
    );

    /**
     * Find athlete by email, scoped to one organization (the same email can
     * be registered independently by two different organizations - only
     * username/email on User, the login identity, is globally unique)
     */
    Optional<Athlete> findByOrganizationIdAndEmail(Long organizationId, String email);

    /**
     * Check if athlete exists by email, scoped to one organization
     */
    boolean existsByOrganizationIdAndEmail(Long organizationId, String email);

    /**
     * Find athletes by team, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndTeam(Long organizationId, String team);

    /**
     * Find athletes by coach name, scoped to one organization
     */
    List<Athlete> findByOrganizationIdAndCoachName(Long organizationId, String coachName);

    /**
     * Search athletes by name (case-insensitive, partial match), scoped to one organization
     */
    @Query("SELECT a FROM Athlete a WHERE a.organizationId = :organizationId "
        + "AND LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Athlete> searchByName(Long organizationId, String name);
}
