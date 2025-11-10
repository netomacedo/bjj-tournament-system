package com.bjj.tournament.dto;

import com.bjj.tournament.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for match response
 * Used to return match information including athlete details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class MatchResponseDTO {

    private Long id;
    private Long divisionId;
    private String divisionName;

    // Athlete 1 details
    private Long athlete1Id;
    private String athlete1Name;
    private String athlete1Team;

    // Athlete 2 details
    private Long athlete2Id;
    private String athlete2Name;
    private String athlete2Team;

    // Winner details
    private Long winnerId;
    private String winnerName;

    private MatchStatus status;
    private Integer roundNumber;
    private Integer matchPosition;
    private Integer matNumber;

    // Scores
    private Integer athlete1Points;
    private Integer athlete2Points;
    private Integer athlete1Advantages;
    private Integer athlete2Advantages;
    private Integer athlete1Penalties;
    private Integer athlete2Penalties;
    private Integer athlete1TotalScore;
    private Integer athlete2TotalScore;

    private Boolean finishedBySubmission;
    private String submissionType;
    private Integer durationSeconds;
    private String notes;
}
