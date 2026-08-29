package com.bjj.tournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for athlete ranking/medal positions in a division
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteRankingDTO {
    private Long athleteId;
    private String athleteName;
    private String team;
    private Integer position; // 1 = Gold, 2 = Silver, 3 = Bronze, 4 = 4th place
    private String medal; // "GOLD", "SILVER", "BRONZE", null for 4th
    private Integer wins;
    private Integer losses;
    private Integer totalPoints;
}
