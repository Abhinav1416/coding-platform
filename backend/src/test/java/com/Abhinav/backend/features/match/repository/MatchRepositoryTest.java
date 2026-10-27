package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    private static final Set<MatchStatus> ACTIVE_STATUSES = EnumSet.of(
            MatchStatus.WAITING_FOR_OPPONENT,
            MatchStatus.SCHEDULED,
            MatchStatus.ACTIVE
    );

    @Test
    @DisplayName("Should return true when user is playerOne in WAITING_FOR_OPPONENT status")
    void shouldReturnTrue_WhenUserIsPlayerOneInWaitingStatus() {
        Long userId = 10L;

        Match match = Match.builder()
                .roomCode("ROOM01")
                .playerOneId(userId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .difficultyMin(1000)
                .difficultyMax(1500)
                .startDelayInMinutes(5)
                .durationInMinutes(30)
                .build();
        matchRepository.save(match);

        boolean exists = matchRepository.existsActiveMatchForUser(userId, ACTIVE_STATUSES);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return true when user is playerTwo in ACTIVE status")
    void shouldReturnTrue_WhenUserIsPlayerTwoInActiveStatus() {
        Long userOne = 10L;
        Long userTwo = 20L;

        Match match = Match.builder()
                .roomCode("ROOM02")
                .playerOneId(userOne)
                .playerTwoId(userTwo)
                .status(MatchStatus.ACTIVE)
                .difficultyMin(1000)
                .difficultyMax(1500)
                .startDelayInMinutes(5)
                .durationInMinutes(30)
                .build();
        matchRepository.save(match);

        boolean existsUserTwo = matchRepository.existsActiveMatchForUser(userTwo, ACTIVE_STATUSES);
        assertThat(existsUserTwo).isTrue();

        boolean existsUserOne = matchRepository.existsActiveMatchForUser(userOne, ACTIVE_STATUSES);
        assertThat(existsUserOne).isTrue();
    }

    @Test
    @DisplayName("Should return false when user only has past completed matches")
    void shouldReturnFalse_WhenUserOnlyHasCompletedMatches() {
        Long userId = 10L;

        Match match = Match.builder()
                .roomCode("ROOM03")
                .playerOneId(userId)
                .status(MatchStatus.COMPLETED)
                .difficultyMin(1000)
                .difficultyMax(1500)
                .startDelayInMinutes(5)
                .durationInMinutes(30)
                .build();
        matchRepository.save(match);

        boolean exists = matchRepository.existsActiveMatchForUser(userId, ACTIVE_STATUSES);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false when user has no matches at all")
    void shouldReturnFalse_WhenUserHasNoMatches() {
        Long userId = 999L;
        boolean exists = matchRepository.existsActiveMatchForUser(userId, ACTIVE_STATUSES);
        assertThat(exists).isFalse();
    }
}
