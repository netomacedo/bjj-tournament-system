package com.bjj.tournament.dto;

import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.Gender;
import com.bjj.tournament.enums.WeightClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a division
 * Allows updating all division fields (belt rank, age category, gender, weight class, bracket type)
 * Updates are only allowed before matches have been generated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionUpdateDTO {

    private BeltRank beltRank;
    private AgeCategory ageCategory;
    private Gender gender;
    private WeightClass weightClass;
    private BracketType bracketType;
}