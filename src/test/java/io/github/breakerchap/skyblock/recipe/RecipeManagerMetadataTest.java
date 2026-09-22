package io.github.breakerchap.skyblock.recipe;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeManagerMetadataTest {
    @Test
    void customRecipeIdsAreCompleteAndUnique() {
        assertEquals(7, RecipeManager.REGISTERED_RECIPE_IDS.size());
        assertEquals(
            RecipeManager.REGISTERED_RECIPE_IDS.size(),
            new HashSet<>(RecipeManager.REGISTERED_RECIPE_IDS).size()
        );
        assertTrue(RecipeManager.REGISTERED_RECIPE_IDS.contains("bell"));
        assertTrue(RecipeManager.REGISTERED_RECIPE_IDS.contains("wayfarer_bell"));
        assertTrue(RecipeManager.REGISTERED_RECIPE_IDS.contains("void_trowel"));
    }
}
