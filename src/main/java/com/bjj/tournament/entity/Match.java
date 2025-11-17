package com.bjj.tournament.entity;

import com.bjj.tournament.enums.MatchStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a match between two athletes
 * Follows IBJJF scoring system with points, advantages, and penalties
 */
@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_match_division", columnList = "division_id"),
    @Index(name = "idx_match_status", columnList = "status"),
    @Index(name = "idx_match_round", columnList = "round_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The division this match belongs to
     * Many-to-One relationship: Many matches belong to one division
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id", nullable = false)
    @JsonIgnore
    private Division division;

    /**
     * First athlete (Blue corner in IBJJF)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete1_id")
    @JsonIgnore
    private Athlete athlete1;

    /**
     * Second athlete (White corner in IBJJF)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete2_id")
    @JsonIgnore
    private Athlete athlete2;

    /**
     * Winner of the match (set after match completion)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    @JsonIgnore
    private Athlete winner;
    
    /**
     * Current status of the match
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.PENDING;
    
    /**
     * Round number in the bracket (1 = First round, 2 = Quarter-finals, etc.)
     */
    @Min(value = 1, message = "Round number must be at least 1")
    @Column(nullable = false, name = "round_number")
    private Integer roundNumber;
    
    /**
     * Position in the round (used for bracket positioning)
     */
    @Column(name = "match_position")
    private Integer matchPosition;
    
    /**
     * Mat/ring number where the match is scheduled
     */
    @Column(name = "mat_number")
    private Integer matNumber;
    
    /**
     * Scheduled start time for the match
     */
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;
    
    // IBJJF Scoring System
    // Points are awarded for: Takedown (2), Knee on belly (2), Mount (4), Back control (4)
    // Guard pass (3), Sweep (2)
    
    /**
     * Points scored by athlete 1
     */
    @Min(value = 0, message = "Points cannot be negative")
    @Column(nullable = false, name = "athlete1_points")
    private Integer athlete1Points = 0;
    
    /**
     * Points scored by athlete 2
     */
    @Min(value = 0, message = "Points cannot be negative")
    @Column(nullable = false, name = "athlete2_points")
    private Integer athlete2Points = 0;
    
    /**
     * Advantages for athlete 1 (used when points are tied)
     */
    @Min(value = 0, message = "Advantages cannot be negative")
    @Column(nullable = false, name = "athlete1_advantages")
    private Integer athlete1Advantages = 0;
    
    /**
     * Advantages for athlete 2 (used when points are tied)
     */
    @Min(value = 0, message = "Advantages cannot be negative")
    @Column(nullable = false, name = "athlete2_advantages")
    private Integer athlete2Advantages = 0;
    
    /**
     * Penalties for athlete 1
     * Penalties in IBJJF: 2 points to opponent per penalty, 4 penalties = disqualification
     */
    @Min(value = 0, message = "Penalties cannot be negative")
    @Column(nullable = false, name = "athlete1_penalties")
    private Integer athlete1Penalties = 0;
    
    /**
     * Penalties for athlete 2
     */
    @Min(value = 0, message = "Penalties cannot be negative")
    @Column(nullable = false, name = "athlete2_penalties")
    private Integer athlete2Penalties = 0;
    
    /**
     * Whether the match ended by submission
     */
    @Column(nullable = false, name = "finished_by_submission")
    private Boolean finishedBySubmission = false;
    
    /**
     * Type of submission if applicable (e.g., "Armbar", "Triangle", "Rear Naked Choke")
     */
    @Column(name = "submission_type", length = 100)
    private String submissionType;
    
    /**
     * Match duration in seconds
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    /**
     * Notes about the match (injuries, controversies, referee decisions, etc.)
     */
    @Column(length = 1000)
    private String notes;
    
    /**
     * Timestamp when the match was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the match was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Calculate total score for athlete 1 including penalty points given to them
     * In IBJJF, opponent gets 2 points for each penalty
     */
    public Integer getAthlete1TotalScore() {
        return athlete1Points + (athlete2Penalties * 2);
    }
    
    /**
     * Calculate total score for athlete 2 including penalty points given to them
     */
    public Integer getAthlete2TotalScore() {
        return athlete2Points + (athlete1Penalties * 2);
    }
    
    /**
     * Determine winner based on IBJJF rules
     * 1. Submission wins immediately
     * 2. Points (including penalty points)
     * 3. Advantages (if points are tied)
     * 4. Referee decision (if advantages are tied)
     */
    public void determineWinner() {
        if (status != MatchStatus.COMPLETED) {
            return;
        }
        
        // Check for disqualification (4 penalties)
        if (athlete1Penalties >= 4) {
            winner = athlete2;
            return;
        }
        if (athlete2Penalties >= 4) {
            winner = athlete1;
            return;
        }
        
        // Submission wins
        if (finishedBySubmission) {
            // Winner should already be set
            return;
        }
        
        // Compare total scores
        int score1 = getAthlete1TotalScore();
        int score2 = getAthlete2TotalScore();
        
        if (score1 > score2) {
            winner = athlete1;
        } else if (score2 > score1) {
            winner = athlete2;
        } else {
            // Scores are tied, check advantages
            if (athlete1Advantages > athlete2Advantages) {
                winner = athlete1;
            } else if (athlete2Advantages > athlete1Advantages) {
                winner = athlete2;
            }
            // If still tied, winner should be determined by referee decision
            // and set manually
        }
    }
}
