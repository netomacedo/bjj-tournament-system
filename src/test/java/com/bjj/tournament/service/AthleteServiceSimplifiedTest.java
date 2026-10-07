package com.bjj.tournament.service;

import com.bjj.tournament.dto.AthleteRegistrationDTO;
import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for simplified athlete registration
 * - Age instead of date of birth
 * - No contact information required
 * - Default team and coach
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AthleteServiceSimplifiedTest {

    @Autowired
    private AthleteService athleteService;

    @Test
    @DisplayName("Should register athlete with only essential fields (age, name, belt, weight)")
    void testRegisterAthleteWithMinimalFields() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Test Kid");
        dto.setAge(8);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(30.0);

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getId()).isNotNull();
        assertThat(athlete.getName()).isEqualTo("Test Kid");
        assertThat(athlete.getAge()).isEqualTo(8);
        assertThat(athlete.getBeltRank()).isEqualTo(BeltRank.WHITE);
        assertThat(athlete.getWeight()).isEqualTo(30.0);

        // Check defaults are applied
        assertThat(athlete.getTeam()).isEqualTo("Takedown Martial Arts");
        assertThat(athlete.getCoachName()).isEqualTo("Pedro Monteiro");

        // Check gender auto-set for kids under 10
        assertThat(athlete.getGender()).isEqualTo(Gender.NOT_APPLICABLE);

        // Check optional fields are null
        assertThat(athlete.getEmail()).isNull();
        assertThat(athlete.getPhone()).isNull();
        assertThat(athlete.getExperienceNotes()).isNull();
    }

    @Test
    @DisplayName("Should allow custom team and coach if provided")
    void testRegisterAthleteWithCustomTeamAndCoach() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Custom Team Kid");
        dto.setAge(10);
        dto.setGender(Gender.MALE);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(35.0);
        dto.setTeam("Gracie Barra");
        dto.setCoachName("John Danaher");

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getTeam()).isEqualTo("Gracie Barra");
        assertThat(athlete.getCoachName()).isEqualTo("John Danaher");
    }

    @Test
    @DisplayName("Should accept athletes without email (email is optional)")
    void testRegisterAthleteWithoutEmail() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("No Email Kid");
        dto.setAge(7);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(28.0);
        // No email provided

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getId()).isNotNull();
        assertThat(athlete.getEmail()).isNull();
    }

    @Test
    @DisplayName("Should allow multiple athletes without email (no unique constraint)")
    void testRegisterMultipleAthletesWithoutEmail() {
        // Given
        AthleteRegistrationDTO dto1 = new AthleteRegistrationDTO();
        dto1.setName("Kid One");
        dto1.setAge(6);
        dto1.setBeltRank(BeltRank.WHITE);
        dto1.setWeight(25.0);

        AthleteRegistrationDTO dto2 = new AthleteRegistrationDTO();
        dto2.setName("Kid Two");
        dto2.setAge(7);
        dto2.setBeltRank(BeltRank.WHITE);
        dto2.setWeight(27.0);

        // When
        Athlete athlete1 = athleteService.registerAthlete(dto1);
        Athlete athlete2 = athleteService.registerAthlete(dto2);

        // Then
        assertThat(athlete1.getId()).isNotNull();
        assertThat(athlete2.getId()).isNotNull();
        assertThat(athlete1.getId()).isNotEqualTo(athlete2.getId());
    }

    @Test
    @DisplayName("Should auto-calculate dateOfBirth from age")
    void testAutoCalculateDateOfBirth() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Age Only Kid");
        dto.setAge(9);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(32.0);

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getDateOfBirth()).isNotNull();
        assertThat(athlete.getAge()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should set gender to NOT_APPLICABLE for kids under 10")
    void testAutoSetGenderForKids() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Young Kid");
        dto.setAge(6);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(24.0);
        // No gender specified

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getGender()).isEqualTo(Gender.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Should require gender for athletes 10 or older")
    void testGenderRequiredForOlderAthletes() {
        // Given
        AthleteRegistrationDTO dto = new AthleteRegistrationDTO();
        dto.setName("Older Kid");
        dto.setAge(12);
        dto.setGender(Gender.MALE);
        dto.setBeltRank(BeltRank.WHITE);
        dto.setWeight(45.0);

        // When
        Athlete athlete = athleteService.registerAthlete(dto);

        // Then
        assertThat(athlete.getGender()).isEqualTo(Gender.MALE);
    }
}
