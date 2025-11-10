package com.bjj.tournament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new tournament
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentCreateDTO {

    @NotBlank(message = "Tournament name is required")
    @Size(min = 3, max = 200, message = "Tournament name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be less than 200 characters")
    private String location;

    @NotNull(message = "Tournament date is required")
    private LocalDate tournamentDate;

    private LocalDate registrationDeadline;

    @Size(max = 100, message = "Organizer name must be less than 100 characters")
    private String organizer;

    private String contactEmail;

    @Size(max = 2000, message = "Rules must be less than 2000 characters")
    private String rules;
}
