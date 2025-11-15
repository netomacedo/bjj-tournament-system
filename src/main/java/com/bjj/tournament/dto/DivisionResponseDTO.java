package com.bjj.tournament.dto;

import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.enums.WeightClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for division response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionResponseDTO {

    private Long id;
    private Long tournamentId;
    private String name;
    private BeltRank beltRank;
    private AgeCategory ageCategory;
    private Gender gender;
    private WeightClass weightClass;
    private BracketType bracketType;
    private Integer athleteCount;
    private Integer matchCount;
    private Boolean matchesGenerated;
    private Boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}