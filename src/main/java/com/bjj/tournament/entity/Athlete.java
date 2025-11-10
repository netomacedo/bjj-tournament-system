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
@Table(name = "athletes", indexes = {
    @Index(name = "idx_athlete_belt", columnList = "belt_rank"),
    @Index(name = "idx_athlete_gender", columnList = "gender"),
    @Index(name = "idx_athlete_age", columnList = "age")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Athlete {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Athlete's full name
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Athlete's date of birth (used to calculate age and age category)
     */
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(nullable = false, name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    /**
     * Current age of the athlete (calculated from date of birth)
     */
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
     * Team/Academy name
     */
    @Size(max = 100, message = "Team name must be less than 100 characters")
    @Column(length = 100)
    private String team;
    
    /**
     * Coach's name who will manage matches
     */
    @Size(max = 100, message = "Coach name must be less than 100 characters")
    @Column(name = "coach_name", length = 100)
    private String coachName;
    
    /**
     * Contact email for athlete or parent/guardian
     */
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, length = 100)
    private String email;
    
    /**
     * Contact phone number
     */
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(length = 20)
    private String phone;
    
    /**
     * Experience level notes (for coach to create fair matches)
     * E.g., "Competition experience: 5 tournaments", "Training time at current belt: 6 months"
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
     * Calculate age from date of birth before persisting
     */
    @PrePersist
    @PreUpdate
    private void calculateAge() {
        if (this.dateOfBirth != null) {
            this.age = Period.between(this.dateOfBirth, LocalDate.now()).getYears();
            
            // Set gender to NOT_APPLICABLE for kids under 10 if not already set
            if (this.age < 10 && this.gender == null) {
                this.gender = Gender.NOT_APPLICABLE;
            }
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
