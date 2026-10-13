package com.bjj.tournament.service;

import com.bjj.tournament.entity.Athlete;
import com.bjj.tournament.entity.Division;
import com.bjj.tournament.entity.Match;
import com.bjj.tournament.entity.Tournament;
import com.bjj.tournament.enums.BracketType;
import com.bjj.tournament.enums.MatchStatus;
import com.bjj.tournament.repository.DivisionRepository;
import com.bjj.tournament.repository.MatchRepository;
import com.bjj.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for generating and managing tournament brackets
 * Supports both automatic and manual (coach-driven) match generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BracketService {
    
    private final DivisionRepository divisionRepository;
    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    
    /**
     * Generate matches automatically for a division
     * Uses seeding and bracket logic to create fair matchups
     * 
     * @param divisionId ID of the division
     * @return List of generated matches
     */
    @Transactional
    public List<Match> generateMatchesAutomatically(Long divisionId) {
        log.info("Generating matches automatically for division ID: {}", divisionId);
        
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));
        
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Matches have already been generated for this division");
        }
        
        List<Athlete> athletes = division.getAthletes();
        int numAthletes = athletes.size();

        if (numAthletes < 1) {
            throw new IllegalArgumentException("Need at least 1 athlete to generate matches");
        }

        log.info("Division has {} athletes", numAthletes);

        // Shuffle athletes for random seeding (in real scenario, you might seed by ranking)
        List<Athlete> shuffledAthletes = new ArrayList<>(athletes);
        Collections.shuffle(shuffledAthletes);

        List<Match> matches;

        if (numAthletes == 1) {
            // IBJJF rule: a division with a single enrolled athlete is awarded
            // gold automatically - there's nobody left to fight.
            matches = generateSoloWalkoverBracket(division, shuffledAthletes.get(0));
        } else if (numAthletes == 3 && division.getBracketType() != BracketType.ROUND_ROBIN) {
            // IBJJF rule: divisions of exactly 3 athletes are run round-robin
            // (everyone fights twice), not single elimination with a bye
            // straight to the final. Override whatever bracket type was
            // selected so the ranking/advancement logic - which branches on
            // division.getBracketType() - treats these matches consistently
            // with how they're actually shaped.
            log.info("Division {} has exactly 3 athletes - using round-robin format per IBJJF rules instead of {}",
                division.getId(), division.getBracketType());
            division.setBracketType(BracketType.ROUND_ROBIN);
            matches = generateRoundRobinBracket(division, shuffledAthletes);
        } else {
            switch (division.getBracketType()) {
                case SINGLE_ELIMINATION:
                    matches = generateSingleEliminationBracket(division, shuffledAthletes);
                    break;
                case DOUBLE_ELIMINATION:
                    matches = generateDoubleEliminationBracket(division, shuffledAthletes);
                    break;
                case ROUND_ROBIN:
                    matches = generateRoundRobinBracket(division, shuffledAthletes);
                    break;
                default:
                    matches = new ArrayList<>();
            }
        }

        // Save all matches
        List<Match> savedMatches = matchRepository.saveAll(matches);

        // Immediately advance any bye winners (round-1 walkovers created above,
        // for an odd number of athletes) into their next-round slot, instead of
        // leaving them sitting unplaced until someone manually completes them.
        for (Match match : savedMatches) {
            if (match.getStatus() == MatchStatus.WALKOVER && match.getWinner() != null) {
                advanceWinnerToNextRound(match.getId(), match.getWinner().getId());
            }
        }

        // Mark division as having matches generated
        division.setMatchesGenerated(true);
        divisionRepository.save(division);
        
        log.info("Successfully generated {} matches for division ID: {}", savedMatches.size(), divisionId);
        
        return savedMatches;
    }
    
    /**
     * Generate matches manually with coach input
     * Coach can specify exact matchups based on experience/skill level
     * 
     * @param divisionId ID of the division
     * @param matchPairs List of athlete ID pairs for matchups
     * @return List of generated matches
     */
    @Transactional
    public List<Match> generateMatchesManually(Long divisionId, List<long[]> matchPairs) {
        log.info("Generating matches manually for division ID: {}", divisionId);
        
        Division division = divisionRepository.findById(divisionId)
            .orElseThrow(() -> new IllegalArgumentException("Division not found with ID: " + divisionId));
        
        if (division.getMatchesGenerated()) {
            throw new IllegalStateException("Matches have already been generated for this division");
        }
        
        List<Match> matches = new ArrayList<>();
        List<Athlete> athletes = division.getAthletes();
        Map<Long, Athlete> athleteMap = new HashMap<>();
        
        for (Athlete athlete : athletes) {
            athleteMap.put(athlete.getId(), athlete);
        }
        
        int matchPosition = 1;
        for (long[] pair : matchPairs) {
            if (pair.length != 2) {
                throw new IllegalArgumentException("Each match pair must contain exactly 2 athlete IDs");
            }
            
            Athlete athlete1 = athleteMap.get(pair[0]);
            Athlete athlete2 = athleteMap.get(pair[1]);
            
            if (athlete1 == null || athlete2 == null) {
                throw new IllegalArgumentException("Athlete not found in division");
            }
            
            Match match = new Match();
            match.setDivision(division);
            match.setAthlete1(athlete1);
            match.setAthlete2(athlete2);
            match.setStatus(MatchStatus.PENDING);
            match.setRoundNumber(1); // First round for manual generation
            match.setMatchPosition(matchPosition++);
            match.setDurationSeconds(getMatchDurationSeconds(division));

            matches.add(match);
        }
        
        // Save all matches
        List<Match> savedMatches = matchRepository.saveAll(matches);
        
        // Mark division as having matches generated
        division.setMatchesGenerated(true);
        divisionRepository.save(division);
        
        log.info("Successfully generated {} manual matches for division ID: {}", savedMatches.size(), divisionId);
        
        return savedMatches;
    }
    
    /**
     * Generate single elimination bracket
     * Standard tournament bracket where one loss eliminates the athlete.
     *
     * Byes are minimized and cascaded rather than front-loaded: each round
     * pairs up as many real matches as possible, and only the single
     * leftover entrant (if the round's entrant count is odd) byes forward -
     * into whichever round first has a free slot for them, which is
     * sometimes the very next round and sometimes one round further (see
     * the queue-based routing below). This matches how organizers actually
     * expect a small field to look (e.g. 5 athletes = 2 real matches + 1
     * athlete standing by in round 1), as opposed to front-loading every
     * needed bye into round 1 alone.
     */
    private List<Match> generateSingleEliminationBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();
        int numAthletes = athletes.size();

        // Round-by-round composition (how many real matches, and whether a
        // bye exists) is a pure function of N - independent of who wins
        // anything. At most one bye per round, since n % 2 is never more
        // than 1. The routing of WHO lands where (below) is what depends on
        // match outcomes.
        List<int[]> roundSizes = new ArrayList<>(); // {realMatches, hasBye(0/1)}
        int n = numAthletes;
        while (n > 1) {
            int byes = n % 2;
            int realMatches = n / 2;
            roundSizes.add(new int[]{realMatches, byes});
            n = realMatches + byes;
        }

        // Round 1 is special: entrants are the shuffled athletes themselves,
        // all known immediately - rather than winners of earlier matches.
        int round1RealMatches = roundSizes.get(0)[0];
        boolean round1HasBye = roundSizes.get(0)[1] == 1;

        int matchPosition = 1;
        List<Match> previousRoundRealMatches = new ArrayList<>();
        for (int i = 0; i < round1RealMatches; i++) {
            Match match = new Match();
            match.setDivision(division);
            match.setRoundNumber(1);
            match.setMatchPosition(matchPosition++);
            match.setAthlete1(athletes.get(2 * i));
            match.setAthlete2(athletes.get(2 * i + 1));
            match.setStatus(MatchStatus.PENDING);
            match.setDurationSeconds(getMatchDurationSeconds(division));
            matches.add(match);
            previousRoundRealMatches.add(match);
        }

        // The single leftover athlete (if any) gets an explicit bye match,
        // auto-decided immediately - same representation byes have always
        // used. Unlike real matches, this doesn't get pre-filled into next
        // round's slot directly here; it's routed via advancesTo* below and
        // resolved by the existing auto-advance-after-save loop in
        // generateMatchesAutomatically, same mechanism as any other bye.
        Match byeCarryover = null;
        if (round1HasBye) {
            Athlete byeAthlete = athletes.get(athletes.size() - 1);
            Match byeMatch = new Match();
            byeMatch.setDivision(division);
            byeMatch.setRoundNumber(1);
            byeMatch.setMatchPosition(round1RealMatches + 1);
            byeMatch.setAthlete1(byeAthlete);
            byeMatch.setStatus(MatchStatus.WALKOVER);
            byeMatch.setWinner(byeAthlete);
            byeMatch.setDurationSeconds(getMatchDurationSeconds(division));
            matches.add(byeMatch);
            byeCarryover = byeMatch;
        }

        // Rounds 2+: entrants are always references to earlier matches
        // (never raw athletes) - a match reference here means "whoever wins
        // this match", whether that's already decided (a bye) or still
        // pending (a real match awaiting a result).
        for (int roundIndex = 1; roundIndex < roundSizes.size(); roundIndex++) {
            int roundNumber = roundIndex + 1;
            int realMatches = roundSizes.get(roundIndex)[0];
            boolean hasBye = roundSizes.get(roundIndex)[1] == 1;

            // Queue order matters: the carryover goes FIRST so it always
            // pairs into a real match this round rather than becoming the
            // leftover again - this is what guarantees nobody ever byes two
            // rounds in a row.
            List<Match> queue = new ArrayList<>();
            if (byeCarryover != null) {
                queue.add(byeCarryover);
            }
            queue.addAll(previousRoundRealMatches);

            List<Match> thisRoundRealMatches = new ArrayList<>();
            matchPosition = 1;
            for (int i = 0; i < realMatches; i++) {
                Match feederA = queue.get(2 * i);
                Match feederB = queue.get(2 * i + 1);

                Match match = new Match();
                match.setDivision(division);
                match.setRoundNumber(roundNumber);
                match.setMatchPosition(matchPosition);
                match.setStatus(MatchStatus.PENDING);
                match.setDurationSeconds(getMatchDurationSeconds(division));
                matches.add(match);
                thisRoundRealMatches.add(match);

                feederA.setAdvancesToRound(roundNumber);
                feederA.setAdvancesToPosition(matchPosition);
                feederA.setAdvancesToSlot("ATHLETE1");

                feederB.setAdvancesToRound(roundNumber);
                feederB.setAdvancesToPosition(matchPosition);
                feederB.setAdvancesToSlot("ATHLETE2");

                matchPosition++;
            }

            // The trailing queue entry (if the queue was odd-length) carries
            // forward unresolved - no row is created for it at this round,
            // it simply isn't paired yet. Its advancesTo* gets set whenever
            // it's finally consumed, which the proof above guarantees
            // happens in the very next round.
            byeCarryover = hasBye ? queue.get(queue.size() - 1) : null;
            previousRoundRealMatches = thisRoundRealMatches;
        }

        return matches;
    }
    
    /**
     * Generate a single-match "bracket" for a division with exactly one
     * enrolled athlete. Per IBJJF rules, a lone entrant is awarded gold
     * automatically - this is represented as a walkover with no opponent,
     * the same shape used for byes, so the existing ranking/advancement
     * logic (which already knows how to handle an opponent-less match)
     * assigns the medal with no special-casing needed downstream.
     */
    private List<Match> generateSoloWalkoverBracket(Division division, Athlete soleAthlete) {
        Match match = new Match();
        match.setDivision(division);
        match.setAthlete1(soleAthlete);
        match.setRoundNumber(1);
        match.setMatchPosition(1);
        match.setStatus(MatchStatus.WALKOVER);
        match.setWinner(soleAthlete);
        match.setDurationSeconds(getMatchDurationSeconds(division));
        return new ArrayList<>(List.of(match));
    }

    /**
     * Generate double elimination bracket
     * Losers get a second chance in the losers bracket
     */
    private List<Match> generateDoubleEliminationBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();
        
        // First, generate winners bracket (same as single elimination)
        matches.addAll(generateSingleEliminationBracket(division, athletes));
        
        // Then add losers bracket structure
        // Losers from winners bracket drop down to losers bracket
        // This is a simplified version - full implementation would be more complex
        
        log.warn("Double elimination bracket generation is simplified. Full losers bracket not implemented yet.");
        
        return matches;
    }
    
    /**
     * Generate round robin bracket
     * Every athlete fights every other athlete, ordered so athletes get a
     * rest between their own matches instead of fighting repeatedly in a row.
     */
    private List<Match> generateRoundRobinBracket(Division division, List<Athlete> athletes) {
        List<Match> matches = new ArrayList<>();

        List<Athlete[]> orderedPairs = orderRoundRobinPairsForRest(athletes);

        int matchPosition = 1;
        for (Athlete[] pair : orderedPairs) {
            Match match = new Match();
            match.setDivision(division);
            match.setAthlete1(pair[0]);
            match.setAthlete2(pair[1]);
            match.setRoundNumber(1); // All matches are in "round 1" for round robin
            match.setMatchPosition(matchPosition++);
            match.setStatus(MatchStatus.PENDING);
            match.setDurationSeconds(getMatchDurationSeconds(division));

            matches.add(match);
        }

        return matches;
    }

    /**
     * Orders every unique athlete pairing so that, at each step, we greedily pick
     * whichever remaining pair avoids repeating an athlete from the immediately
     * previous match; among equally valid options, we prefer whoever has rested
     * longest since their last match. A back-to-back repeat is sometimes
     * mathematically unavoidable (every "round" of a round-robin uses every
     * athlete, so round transitions can force an immediate rematch), but this
     * keeps such repeats to the minimum possible instead of letting one athlete
     * fight several matches in a row like naive pairing order does.
     */
    private List<Athlete[]> orderRoundRobinPairsForRest(List<Athlete> athletes) {
        List<Athlete[]> remainingPairs = new ArrayList<>();
        for (int i = 0; i < athletes.size(); i++) {
            for (int j = i + 1; j < athletes.size(); j++) {
                remainingPairs.add(new Athlete[]{athletes.get(i), athletes.get(j)});
            }
        }

        Map<Long, Integer> lastPlayedAtStep = new HashMap<>();
        Set<Long> lastMatchAthleteIds = Collections.emptySet();
        List<Athlete[]> orderedPairs = new ArrayList<>();

        int step = 0;
        while (!remainingPairs.isEmpty()) {
            Athlete[] best = null;
            int bestRest = Integer.MIN_VALUE;
            boolean bestConflicts = true;

            for (Athlete[] pair : remainingPairs) {
                boolean conflicts = lastMatchAthleteIds.contains(pair[0].getId())
                    || lastMatchAthleteIds.contains(pair[1].getId());
                int rest = step - Math.max(
                    lastPlayedAtStep.getOrDefault(pair[0].getId(), Integer.MIN_VALUE / 2),
                    lastPlayedAtStep.getOrDefault(pair[1].getId(), Integer.MIN_VALUE / 2)
                );

                boolean better = best == null
                    || (bestConflicts && !conflicts)
                    || (conflicts == bestConflicts && rest > bestRest);

                if (better) {
                    best = pair;
                    bestRest = rest;
                    bestConflicts = conflicts;
                }
            }

            orderedPairs.add(best);
            remainingPairs.remove(best);
            lastPlayedAtStep.put(best[0].getId(), step);
            lastPlayedAtStep.put(best[1].getId(), step);
            lastMatchAthleteIds = Set.of(best[0].getId(), best[1].getId());
            step++;
        }

        return orderedPairs;
    }
    
    /**
     * Advance winner to next round in single elimination bracket
     * 
     * @param matchId ID of the completed match
     * @param winnerId ID of the winning athlete
     */
    @Transactional
    public void advanceWinnerToNextRound(Long matchId, Long winnerId) {
        log.info("Advancing winner from match ID: {} to next round", matchId);
        
        Match completedMatch = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));
        
        if (completedMatch.getStatus() != MatchStatus.COMPLETED
                && completedMatch.getStatus() != MatchStatus.WALKOVER) {
            throw new IllegalStateException("Match must be completed before advancing winner");
        }
        
        Athlete winner = completedMatch.getWinner();
        if (winner == null || !winner.getId().equals(winnerId)) {
            throw new IllegalArgumentException("Invalid winner ID");
        }
        
        Division division = completedMatch.getDivision();

        // Round robin has no bracket progression - every match is already
        // scheduled upfront in a single round ("all fight all"), so there's no
        // "next round" to advance a winner into. The division is complete once
        // every one of its matches has a decided winner.
        if (division.getBracketType() == BracketType.ROUND_ROBIN) {
            List<Match> allMatches = matchRepository.findByDivisionIdAndRoundNumber(division.getId(), 1);
            boolean allDecided = allMatches.stream().allMatch(m -> m.getWinner() != null);
            if (allDecided) {
                division.setCompleted(true);
                divisionRepository.save(division);
                log.info("All round-robin matches decided - division {} marked completed", division.getId());
                checkAndCompleteTournament(division);
            }
            return;
        }

        // Find next round match where this winner should compete
        int nextRound;
        int nextMatchPosition;
        boolean toAthlete1Slot;

        if (completedMatch.getAdvancesToRound() != null) {
            // Explicit routing, computed at generation time - needed because
            // the cascading-bye bracket shape doesn't always advance exactly
            // one round forward (a bye-carryover's source match lands two
            // rounds ahead instead of one), so it can't be assumed generically.
            nextRound = completedMatch.getAdvancesToRound();
            nextMatchPosition = completedMatch.getAdvancesToPosition();
            toAthlete1Slot = "ATHLETE1".equals(completedMatch.getAdvancesToSlot());
        } else {
            // No explicit routing stored: either a bracket generated before
            // this field existed (always advanced exactly one round, so this
            // computation still applies), or a genuine final (nothing further
            // to advance to - correctly resolved below by finding no matches
            // at the naively-computed next round, same as the legacy case).
            nextRound = completedMatch.getRoundNumber() + 1;
            nextMatchPosition = (completedMatch.getMatchPosition() + 1) / 2;
            toAthlete1Slot = completedMatch.getMatchPosition() % 2 == 1;
        }

        List<Match> nextRoundMatches = matchRepository.findByDivisionIdAndRoundNumber(
            division.getId(), nextRound);

        if (nextRoundMatches.isEmpty()) {
            log.info("No next round - this was the final match");

            // Mark division as completed
            division.setCompleted(true);
            divisionRepository.save(division);
            checkAndCompleteTournament(division);
            return;
        }

        // Find the appropriate next match
        Match nextMatch = nextRoundMatches.stream()
            .filter(m -> m.getMatchPosition() == nextMatchPosition)
            .findFirst()
            .orElse(null);

        if (nextMatch != null) {
            if (toAthlete1Slot) {
                nextMatch.setAthlete1(winner);
            } else {
                nextMatch.setAthlete2(winner);
            }

            matchRepository.save(nextMatch);
            log.info("Winner advanced to next round match ID: {}", nextMatch.getId());
        }
    }

    /**
     * Marks the division's tournament as completed once every division in it
     * has finished (a tournament with no divisions yet is never auto-completed).
     */
    private void checkAndCompleteTournament(Division division) {
        Tournament tournament = division.getTournament();
        if (tournament == null || Boolean.TRUE.equals(tournament.getCompleted())) {
            return;
        }

        List<Division> divisions = divisionRepository.findByTournamentIdOrderByIdAsc(tournament.getId());
        boolean allDivisionsCompleted = !divisions.isEmpty()
            && divisions.stream().allMatch(d -> Boolean.TRUE.equals(d.getCompleted()));

        if (allDivisionsCompleted) {
            tournament.setCompleted(true);
            tournamentRepository.save(tournament);
            log.info("All divisions completed - tournament {} marked completed", tournament.getId());
        }
    }

    /**
     * Allotted match duration for a division, derived from its age category
     */
    private Integer getMatchDurationSeconds(Division division) {
        return division.getAgeCategory().getMatchDurationMinutes() * 60;
    }
}
