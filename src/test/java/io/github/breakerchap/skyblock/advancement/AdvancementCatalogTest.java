package io.github.breakerchap.skyblock.advancement;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementCatalogTest {
    @Test
    void catalogIsLargeAndInternallyLinked() {
        var definitions = AdvancementCatalog.definitions();
        assertTrue(definitions.size() >= 200, "expected at least 200 custom advancements");
        assertTrue(definitions.stream().anyMatch(d -> d.id().equals("farming/goat_boat")));
        assertTrue(definitions.stream().anyMatch(d -> d.id().equals("engineering/bridge_64")));
        assertTrue(definitions.stream().anyMatch(d -> d.id().equals("exploration/village")));
        assertTrue(definitions.stream().noneMatch(d -> d.id().equals("engineering/dropper")));
        assertTrue(definitions.stream().noneMatch(d -> d.id().equals("engineering/comparator")));

        var ids = new HashSet<String>();
        definitions.forEach(definition ->
            assertTrue(ids.add(definition.id()), "duplicate advancement id: " + definition.id())
        );

        definitions.stream()
            .filter(definition -> definition.parentId() != null)
            .forEach(definition ->
                assertTrue(ids.contains(definition.parentId()),
                    "missing parent " + definition.parentId() + " for " + definition.id())
            );
    }
}
