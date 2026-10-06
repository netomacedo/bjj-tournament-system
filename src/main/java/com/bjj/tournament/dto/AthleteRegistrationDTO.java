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
 * Simplified form: only essential fields required
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteRegistrationDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 4, message = "Athlete must be at least 4 years old")
    @Max(value = 150, message = "Invalid age")
    private Integer age;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth; // Optional - calculated from age if not provided

    private Gender gender; // Optional, will be set to NOT_APPLICABLE for kids under 10

    @NotNull(message = "Belt rank is required")
    private BeltRank beltRank;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
    @DecimalMax(value = "250.0", message = "Weight must be less than 250 kg")
    private Double weight;

    @Size(max = 100, message = "Team name must be less than 100 characters")
    private String team; // Defaults to "Takedown Martial Arts"

    @Size(max = 100, message = "Coach name must be less than 100 characters")
    private String coachName; // Defaults to "Pedro Monteiro"

    @Email(message = "Invalid email format")
    private String email; // Optional

    @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone; // Optional

    @Size(max = 500, message = "Experience notes must be less than 500 characters")
    private String experienceNotes; // Optional
}
