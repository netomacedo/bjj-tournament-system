package com.bjj.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a BJJ Tournament
 * A tournament can have multiple divisions based on belt, age, gender, and weight
 */
@Entity
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Name of the tournament (e.g., "Summer BJJ Championship 2025")
     */
    @NotBlank(message = "Tournament name is required")
    @Size(min = 3, max = 200, message = "Tournament name must be between 3 and 200 characters")
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * Description of the tournament
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * Location/venue of the tournament
     */
    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be less than 200 characters")
    @Column(nullable = false, length = 200)
    private String location;
    
    /**
     * Date of the tournament
     */
    @NotNull(message = "Tournament date is required")
    @Column(nullable = false, name = "tournament_date")
    private LocalDate tournamentDate;
    
    /**
     * Registration deadline
     */
    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;
    
    /**
     * Whether registration is currently open
     */
    @Column(nullable = false, name = "registration_open")
    private Boolean registrationOpen = true;
    
    /**
     * Organizer name or organization
     */
    @Size(max = 100, message = "Organizer name must be less than 100 characters")
    @Column(length = 100)
    private String organizer;
    
    /**
     * Contact email for tournament inquiries
     */
    @Column(name = "contact_email", length = 100)
    private String contactEmail;
    
    /**
     * List of divisions in this tournament
     * One-to-Many relationship: One tournament has many divisions
     */
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Division> divisions = new ArrayList<>();
    
    /**
     * Tournament rules or special notes
     */
    @Column(length = 2000)
    private String rules;
    
    /**
     * Whether the tournament has started
     */
    @Column(nullable = false)
    private Boolean started = false;
    
    /**
     * Whether the tournament is completed
     */
    @Column(nullable = false)
    private Boolean completed = false;
    
    /**
     * Timestamp when the tournament was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the tournament was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to add a division to this tournament
     */
    public void addDivision(Division division) {
        divisions.add(division);
        division.setTournament(this);
    }
    
    /**
     * Helper method to remove a division from this tournament
     */
    public void removeDivision(Division division) {
        divisions.remove(division);
        division.setTournament(null);
    }
}
