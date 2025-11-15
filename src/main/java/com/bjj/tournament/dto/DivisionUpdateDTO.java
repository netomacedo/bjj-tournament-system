package com.bjj.tournament.dto;

import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.WeightClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a division
 * Only allows updating bracket type and weight class
 * Belt rank, age category, and gender cannot be changed after creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionUpdateDTO {

    private WeightClass weightClass;
    private BracketType bracketType;
}