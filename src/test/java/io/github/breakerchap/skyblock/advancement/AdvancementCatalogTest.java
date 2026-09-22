package io.github.breakerchap.skyblock.advancement;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementCatalogTest {
    @Test
    void catalogIsLargeAndInternallyLinked() {
        var definitions = AdvancementCatalog.definitions();
        assertTrue(definitions.size() >= 100, "expected at least 100 custom advancements");

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
