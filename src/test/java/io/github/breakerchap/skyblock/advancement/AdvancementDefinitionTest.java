package io.github.breakerchap.skyblock.advancement;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementDefinitionTest {
    @Test
    void rootContainsBackgroundAndModernIconSyntax() {
        AdvancementDefinition definition = new AdvancementDefinition(
            "root", null, "Skybound", "Test", Material.GRASS_BLOCK, "task", false, 0
        );

        String json = definition.toJson();
        assertTrue(json.contains("\"background\":\"minecraft:gui/advancements/backgrounds/stone\""));
        assertTrue(json.contains("\"icon\":{\"id\":\"minecraft:grass_block\"}"));
        assertTrue(json.contains("\"trigger\":\"minecraft:impossible\""));
        assertFalse(json.contains("\"parent\""));
    }

    @Test
    void childContainsParentAndReward() {
        AdvancementDefinition definition = new AdvancementDefinition(
            "child", "root", "Child", "Test", Material.COBBLESTONE, "goal", true, 12
        );

        String json = definition.toJson();
        assertTrue(json.contains("\"parent\":\"skyblock:root\""));
        assertTrue(json.contains("\"experience\":12"));
        assertTrue(json.contains("\"hidden\":true"));
    }
}
