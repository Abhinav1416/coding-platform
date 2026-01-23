package com.Abhinav.backend.features.duel.repository;

import com.Abhinav.backend.features.duel.model.DuelHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime; // <--- CHANGED IMPORT
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DuelRepositoryTest {

    @Autowired
    private DuelRepository duelRepository;

    @Test
    @DisplayName("Should find duels where user is Player 1 OR Player 2")
    void testFindAllByPlayer1IdOrPlayer2Id() {
        Long id1 = 100L;
        Long id2 = 200L;
        Long randomId = 300L;

        DuelHistory match1 = new DuelHistory();
        match1.setDuelId(UUID.randomUUID());
        match1.setPlayer1Id(id1);
        match1.setPlayer2Id(id2);
        match1.setPlayer1Handle("Me");
        match1.setPlayer2Handle("Opponent");
        match1.setStartedAt(LocalDateTime.now());
        duelRepository.save(match1);

        DuelHistory match2 = new DuelHistory();
        match2.setDuelId(UUID.randomUUID());
        match2.setPlayer1Id(id2);
        match2.setPlayer2Id(id1);
        match2.setPlayer1Handle("Opponent");
        match2.setPlayer2Handle("Me");
        match2.setStartedAt(LocalDateTime.now());
        duelRepository.save(match2);

        DuelHistory match3 = new DuelHistory();
        match3.setDuelId(UUID.randomUUID());
        match3.setPlayer1Id(id2);
        match3.setPlayer2Id(randomId);
        match3.setPlayer1Handle("Opponent");
        match3.setPlayer2Handle("Random");
        match3.setStartedAt(LocalDateTime.now());
        duelRepository.save(match3);

        List<DuelHistory> results = duelRepository.findAllByPlayer1IdOrPlayer2Id(id1, id1);

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(DuelHistory::getDuelId)
                .containsExactlyInAnyOrder(match1.getDuelId(), match2.getDuelId());
    }
}