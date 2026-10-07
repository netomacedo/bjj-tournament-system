package com.bjj.tournament.service;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.enums.AgeCategory;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BracketService, focused on round-robin match ordering
 */
@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private DivisionRepository divisionRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private BracketService bracketService;

    private Division division;
    private List<Athlete> athletes;

    @BeforeEach
    void setUp() {
        division = new Division();
        division.setId(1L);
        division.setBracketType(BracketType.ROUND_ROBIN);
        division.setAgeCategory(AgeCategory.ADULT);
        division.setMatchesGenerated(false);

        athletes = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            Athlete athlete = new Athlete();
            athlete.setId(i);
            athlete.setName("Athlete " + i);
            athletes.add(athlete);
        }
        division.setAthletes(athletes);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generateMatchesAutomatically_RoundRobin_NoAthleteFightsTwiceInARowMoreThanUnavoidable() {
        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        // Every unique pair must play exactly once
        assertThat(matches).hasSize(6);

        // Count back-to-back repeats: an athlete appearing in both match i and match i+1
        int backToBackRepeats = 0;
        for (int i = 0; i < matches.size() - 1; i++) {
            Match current = matches.get(i);
            Match next = matches.get(i + 1);
            boolean sharesAthlete = next.getAthlete1().getId().equals(current.getAthlete1().getId())
                || next.getAthlete1().getId().equals(current.getAthlete2().getId())
                || next.getAthlete2().getId().equals(current.getAthlete1().getId())
                || next.getAthlete2().getId().equals(current.getAthlete2().getId());
            if (sharesAthlete) {
                backToBackRepeats++;
            }
        }

        // With 4 athletes and no bye, 2 back-to-back repeats (one per round transition)
        // is the mathematical minimum - the naive pairing order produces 3 in a row
        // for the first athlete alone, so this asserts the greedy scheduler achieves optimum.
        assertThat(backToBackRepeats).isEqualTo(2);
    }

    @Test
    void generateMatchesAutomatically_RoundRobin_EveryPairPlaysExactlyOnce() {
        List<Match> matches = bracketService.generateMatchesAutomatically(1L);

        List<String> pairs = matches.stream()
            .map(m -> {
                long a = m.getAthlete1().getId();
                long b = m.getAthlete2().getId();
                return Math.min(a, b) + "-" + Math.max(a, b);
            })
            .toList();

        assertThat(pairs).containsExactlyInAnyOrder("1-2", "1-3", "1-4", "2-3", "2-4", "3-4");
        assertThat(pairs).doesNotHaveDuplicates();
    }
}
