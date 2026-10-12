package com.bjj.tournament.entity;

import com.bjj.tournament.enums.BeltRank;
import com.bjj.tournament.enums.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Athlete entity's own logic: the two eligibility helper
 * methods, and the @PrePersist/@PreUpdate lifecycle callbacks that derive
 * age/dateOfBirth/gender/team/coach defaults. Repository query methods are
 * covered separately in AthleteRepositoryTest.
 */
@DataJpaTest
class AthleteTest {

    @Autowired
    private TestEntityManager entityManager;

    // ---- requiresGenderSeparation() ----

    @Test
    void requiresGenderSeparation_AtExactlyTen_ReturnsTrue() {
        Athlete athlete = new Athlete();
        athlete.setAge(10);

        assertThat(athlete.requiresGenderSeparation()).isTrue();
    }

    @Test
    void requiresGenderSeparation_UnderTen_ReturnsFalse() {
        Athlete athlete = new Athlete();
        athlete.setAge(9);

        assertThat(athlete.requiresGenderSeparation()).isFalse();
    }

    // ---- isKids() ----

    @Test
    void isKids_AtFifteen_ReturnsTrue() {
        Athlete athlete = new Athlete();
        athlete.setAge(15);

        assertThat(athlete.isKids()).isTrue();
    }

    @Test
    void isKids_AtSixteen_ReturnsFalse() {
        Athlete athlete = new Athlete();
        athlete.setAge(16);

        assertThat(athlete.isKids()).isFalse();
    }

    // ---- @PrePersist: calculateAgeAndDefaults() ----

    @Test
    void onPersist_WithDateOfBirthOnly_CalculatesAgeFromIt() {
        Athlete athlete = newMinimalAthlete();
        athlete.setAge(null);
        athlete.setDateOfBirth(LocalDate.now().minusYears(20));

        entityManager.persistAndFlush(athlete);

        assertThat(athlete.getAge()).isEqualTo(20);
    }

    @Test
    void onPersist_WithAgeOnly_EstimatesDateOfBirthFromIt() {
        Athlete athlete = newMinimalAthlete();
        athlete.setAge(25);
        athlete.setDateOfBirth(null);

        entityManager.persistAndFlush(athlete);

        assertThat(athlete.getDateOfBirth()).isEqualTo(LocalDate.now().minusYears(25));
    }

    @Test
    void onPersist_UnderTenWithNoGenderSet_DefaultsGenderToNotApplicable() {
        Athlete athlete = newMinimalAthlete();
        athlete.setAge(7);
        athlete.setGender(null);

        entityManager.persistAndFlush(athlete);

        assertThat(athlete.getGender()).isEqualTo(Gender.NOT_APPLICABLE);
    }

    @Test
    void onPersist_UnderTenWithGenderAlreadySet_DoesNotOverrideIt() {
        Athlete athlete = newMinimalAthlete();
        athlete.setAge(7);
        athlete.setGender(Gender.FEMALE);

        entityManager.persistAndFlush(athlete);

        assertThat(athlete.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void onPersist_BlankTeamAndCoach_FallBackToDefaults() {
        Athlete athlete = newMinimalAthlete();
        athlete.setTeam("   ");
        athlete.setCoachName("");

        entityManager.persistAndFlush(athlete);

        assertThat(athlete.getTeam()).isEqualTo("Takedown Martial Arts");
        assertThat(athlete.getCoachName()).isEqualTo("Pedro Monteiro");
    }

    // ---- @PreUpdate: updateDerivedFields() ----

    @Test
    void onUpdate_ChangingAge_RecalculatesDateOfBirthToMatch() {
        Athlete athlete = newMinimalAthlete();
        athlete.setAge(20);
        entityManager.persistAndFlush(athlete);

        athlete.setAge(21);
        entityManager.flush();

        assertThat(athlete.getDateOfBirth()).isEqualTo(LocalDate.now().minusYears(21));
    }

    private Athlete newMinimalAthlete() {
        Athlete athlete = new Athlete();
        athlete.setName("Test Athlete");
        athlete.setAge(25);
        athlete.setGender(Gender.MALE);
        athlete.setBeltRank(BeltRank.WHITE);
        athlete.setWeight(70.0);
        return athlete;
    }
}
