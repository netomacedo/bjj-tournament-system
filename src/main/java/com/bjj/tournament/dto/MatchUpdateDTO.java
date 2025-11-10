package com.bjj.tournament.dto;

import com.bjj.tournament.enums.MatchStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating match scores during competition
 * Used by coaches/referees to update match results in real-time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchUpdateDTO {
    
    @Min(value = 0, message = "Points cannot be negative")
    private Integer athlete1Points;
    
    @Min(value = 0, message = "Points cannot be negative")
    private Integer athlete2Points;
    
    @Min(value = 0, message = "Advantages cannot be negative")
    private Integer athlete1Advantages;
    
    @Min(value = 0, message = "Advantages cannot be negative")
    private Integer athlete2Advantages;
    
    @Min(value = 0, message = "Penalties cannot be negative")
    private Integer athlete1Penalties;
    
    @Min(value = 0, message = "Penalties cannot be negative")
    private Integer athlete2Penalties;
    
    private MatchStatus status;
    
    private Boolean finishedBySubmission;
    
    private String submissionType;
    
    private Long winnerId; // ID of the winning athlete
    
    private String notes;
}

