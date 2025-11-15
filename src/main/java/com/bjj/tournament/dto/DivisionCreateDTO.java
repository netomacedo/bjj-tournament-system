package com.bjj.tournament.dto;

import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.enums.WeightClass;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new division
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionCreateDTO {

    @NotNull(message = "Belt rank is required")
    private BeltRank beltRank;

    @NotNull(message = "Age category is required")
    private AgeCategory ageCategory;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private WeightClass weightClass;

    @NotNull(message = "Bracket type is required")
    private BracketType bracketType;
}