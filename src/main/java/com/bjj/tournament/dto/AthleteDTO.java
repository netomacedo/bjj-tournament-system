package com.bjj.tournament.dto;

import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for athlete registration request
 * Used when registering a new athlete for a tournament
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteRegistrationDTO {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    private Gender gender; // Optional, will be set to NOT_APPLICABLE for kids under 10
    
    @NotNull(message = "Belt rank is required")
    private BeltRank beltRank;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
    @DecimalMax(value = "250.0", message = "Weight must be less than 250 kg")
    private Double weight;
    
    @Size(max = 100, message = "Team name must be less than 100 characters")
    private String team;
    
    @Size(max = 100, message = "Coach name must be less than 100 characters")
    private String coachName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;
    
    @Size(max = 500, message = "Experience notes must be less than 500 characters")
    private String experienceNotes;
}

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
