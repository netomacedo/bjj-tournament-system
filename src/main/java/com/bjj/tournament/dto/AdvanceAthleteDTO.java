package com.bjj.tournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for advancing athlete to next round
 * Used when manually managing the bracket tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class AdvanceAthleteDTO {

    private Long matchId;
    private Long winningAthleteId;
    private String reason; // e.g., "Submission", "Points", "Walkover", "Disqualification"
}
