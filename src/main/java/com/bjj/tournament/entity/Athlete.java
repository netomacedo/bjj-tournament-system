package com.bjj.tournament.entity;

import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Entity representing a BJJ athlete registered for a tournament
 * Contains personal information, belt rank, and physical attributes
 */
@Entity
@Table(name = "athletes",
    indexes = {
        @Index(name = "idx_athlete_belt", columnList = "belt_rank"),
        @Index(name = "idx_athlete_gender", columnList = "gender"),
        @Index(name = "idx_athlete_age", columnList = "age")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Version for optimistic locking
     * Prevents concurrent modification conflicts
     */
    @Version
    private Long version;
    
    /**
     * Athlete's full name
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Athlete's date of birth (optional - can be calculated from age)
     */
    @Past(message = "Date of birth must be in the past")
    @Column(nullable = true, name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Current age of the athlete (primary field, date of birth is optional)
     */
    @NotNull(message = "Age is required")
    @Min(value = 4, message = "Athlete must be at least 4 years old")
    @Max(value = 150, message = "Invalid age")
    @Column(nullable = false)
    private Integer age;
    
    /**
     * Gender of the athlete
     * Note: For kids under 10, this can be NOT_APPLICABLE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;
    
    /**
     * Current belt rank following IBJJF system
     */
    @NotNull(message = "Belt rank is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "belt_rank", length = 30)
    private BeltRank beltRank;
    
    /**
     * Weight in kilograms (used for weight class assignment)
     */
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
    @DecimalMax(value = "250.0", message = "Weight must be less than 250 kg")
    @Column(nullable = false)
    private Double weight;
    
    /**
     * Team/Academy name (defaults to "Takedown Martial Arts")
     */
    @Size(max = 100, message = "Team name must be less than 100 characters")
    @Column(length = 100)
    private String team = "Takedown Martial Arts";

    /**
     * Coach's name who will manage matches (defaults to "Pedro Monteiro")
     */
    @Size(max = 100, message = "Coach name must be less than 100 characters")
    @Column(name = "coach_name", length = 100)
    private String coachName = "Pedro Monteiro";

    /**
     * Contact email for athlete or parent/guardian (optional)
     */
    @Email(message = "Invalid email format")
    @Column(nullable = true, length = 100)
    private String email;

    /**
     * Contact phone number (optional)
     */
    @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(length = 20)
    private String phone;

    /**
     * Experience level notes (optional, removed from form)
     */
    @Column(length = 500, name = "experience_notes")
    private String experienceNotes;
    
    /**
     * Timestamp when the athlete registered
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the athlete information was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Calculate age from date of birth (if provided) and set defaults
     */
    @PrePersist
    @PreUpdate
    private void calculateAgeAndDefaults() {
        // If dateOfBirth is provided, calculate age from it
        if (this.dateOfBirth != null) {
            this.age = Period.between(this.dateOfBirth, LocalDate.now()).getYears();
        }
        // If age is provided but no dateOfBirth, estimate dateOfBirth for database consistency
        else if (this.age != null && this.dateOfBirth == null) {
            this.dateOfBirth = LocalDate.now().minusYears(this.age);
        }

        // Set gender to NOT_APPLICABLE for kids under 10 if not already set
        if (this.age != null && this.age < 10 && this.gender == null) {
            this.gender = Gender.NOT_APPLICABLE;
        }

        // Set default team and coach if not provided
        if (this.team == null || this.team.trim().isEmpty()) {
            this.team = "Takedown Martial Arts";
        }
        if (this.coachName == null || this.coachName.trim().isEmpty()) {
            this.coachName = "Pedro Monteiro";
        }
    }
    
    /**
     * Check if this athlete requires gender-separated divisions
     */
    public boolean requiresGenderSeparation() {
        return this.age >= 10;
    }
    
    /**
     * Check if this is a kids athlete
     */
    public boolean isKids() {
        return this.age < 16;
    }
}
