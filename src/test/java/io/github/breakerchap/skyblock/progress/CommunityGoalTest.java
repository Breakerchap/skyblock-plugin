package io.github.breakerchap.skyblock.progress;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityGoalTest {
    @Test
    void idsAndMetricsAreUsable() {
        var ids = new HashSet<String>();
        for (CommunityGoal goal : CommunityGoal.values()) {
            assertTrue(ids.add(goal.id()), "duplicate community goal id: " + goal.id());
            assertTrue(goal.defaultThreshold() > 0);
            assertTrue(goal.advancementId().startsWith("community/"));
            assertTrue(!goal.metric().isBlank());
        }
        assertEquals(CommunityGoal.values().length, ids.size());
    }
}
