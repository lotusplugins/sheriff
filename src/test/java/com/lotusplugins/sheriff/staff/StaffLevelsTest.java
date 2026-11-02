package com.lotusplugins.sheriff.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class StaffLevelsTest {
    @Test
    void noNodeIsLevelZero() {
        assertEquals(0, StaffLevels.levelOf(node -> false));
    }

    @Test
    void singleNodeIsThatLevel() {
        assertEquals(4, StaffLevels.levelOf(Set.of("sheriff.level.4")::contains));
    }

    @Test
    void highestNodeWins() {
        assertEquals(7, StaffLevels.levelOf(Set.of("sheriff.level.2", "sheriff.level.7", "sheriff.level.5")::contains));
    }

    @Test
    void wildcardIsMaxLevel() {
        assertEquals(StaffLevels.MAX_LEVEL, StaffLevels.levelOf(node -> true));
    }

    @Test
    void nodeAboveMaxIsIgnored() {
        assertEquals(0, StaffLevels.levelOf(Set.of("sheriff.level.11")::contains));
    }
}
