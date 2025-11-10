package com.bjj.tournament.dto;

import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for athlete response
 * Used when returning athlete information to the client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class AthleteResponseDTO {
    
    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private BeltRank beltRank;
    private Double weight;
    private String team;
    private String coachName;
    private String email;
    private String phone;
    private String experienceNotes;
}
