package com.bjj.tournament.entity;

import com.bjj.tournament.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a competition division
 * A division groups athletes by belt rank, age category, gender, and weight class
 * Example: "Adult Male Blue Belt Light Division"
 */
@Entity
@Table(name = "divisions", indexes = {
    @Index(name = "idx_division_tournament", columnList = "tournament_id"),
    @Index(name = "idx_division_belt", columnList = "belt_rank"),
    @Index(name = "idx_division_age", columnList = "age_category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Division {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The tournament this division belongs to
     * Many-to-One relationship: Many divisions belong to one tournament
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @NotNull(message = "Tournament is required")
    @JsonIgnore
    private Tournament tournament;
    
    /**
     * Name of the division (auto-generated based on criteria)
     * Example: "Adult Male Blue Belt Light"
     */
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * Belt rank for this division
     */
    @NotNull(message = "Belt rank is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "belt_rank", length = 30)
    private BeltRank beltRank;
    
    /**
     * Age category for this division
     */
    @NotNull(message = "Age category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "age_category", length = 30)
    private AgeCategory ageCategory;
    
    /**
     * Gender for this division (NOT_APPLICABLE for kids under 10)
     */
    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;
    
    /**
     * Weight class for this division
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "weight_class", length = 50)
    private WeightClass weightClass;
    
    /**
     * Type of bracket/tournament format for this division
     */
    @NotNull(message = "Bracket type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "bracket_type", length = 30)
    private BracketType bracketType = BracketType.SINGLE_ELIMINATION;
    
    /**
     * Athletes registered in this division
     * Many-to-Many relationship: An athlete can be in multiple divisions (absolute/open weight)
     */
    @ManyToMany
    @JoinTable(
        name = "division_athletes",
        joinColumns = @JoinColumn(name = "division_id"),
        inverseJoinColumns = @JoinColumn(name = "athlete_id")
    )
    private List<Athlete> athletes = new ArrayList<>();
    
    /**
     * Matches in this division
     * One-to-Many relationship: One division has many matches
     */
    @OneToMany(mappedBy = "division", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Match> matches = new ArrayList<>();
    
    /**
     * Whether matches for this division have been generated
     */
    @Column(nullable = false, name = "matches_generated")
    private Boolean matchesGenerated = false;
    
    /**
     * Whether this division has been completed
     */
    @Column(nullable = false)
    private Boolean completed = false;
    
    /**
     * Timestamp when the division was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the division was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to add an athlete to this division
     */
    public void addAthlete(Athlete athlete) {
        athletes.add(athlete);
    }
    
    /**
     * Helper method to remove an athlete from this division
     */
    public void removeAthlete(Athlete athlete) {
        athletes.remove(athlete);
    }
    
    /**
     * Helper method to add a match to this division
     */
    public void addMatch(Match match) {
        matches.add(match);
        match.setDivision(this);
    }
    
    /**
     * Helper method to remove a match from this division
     */
    public void removeMatch(Match match) {
        matches.remove(match);
        match.setDivision(null);
    }
    
    /**
     * Generate division name based on criteria
     */
    @PrePersist
    @PreUpdate
    private void generateName() {
        StringBuilder nameBuilder = new StringBuilder();
        
        // Add age category
        nameBuilder.append(ageCategory.getDisplayName());
        
        // Add gender if applicable (not for kids under 10)
        if (gender != Gender.NOT_APPLICABLE) {
            nameBuilder.append(" ").append(gender.getDisplayName());
        }
        
        // Add belt rank
        nameBuilder.append(" ").append(beltRank.getDisplayName());
        
        // Add weight class if specified
        if (weightClass != null) {
            nameBuilder.append(" ").append(weightClass.getDisplayName());
        }
        
        this.name = nameBuilder.toString();
    }
}
