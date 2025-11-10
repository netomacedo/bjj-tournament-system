package com.bjj.tournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for tournament response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class TournamentResponseDTO {
    
    private Long id;
    private String name;
    private String description;
    private String location;
    private LocalDate tournamentDate;
    private LocalDate registrationDeadline;
    private Boolean registrationOpen;
    private String organizer;
    private String contactEmail;
    private String rules;
    private Boolean started;
    private Boolean completed;
    private Integer totalDivisions;
    private Integer totalAthletes;
}
