# Exploration structure design

The Skyblock exploration landmarks are intentionally authored as individual destinations rather than generated from generic ellipsoids, spheres, flat roofs or perimeter fences.

## Source policy

The plugin may place **Minecraft's own built-in structure templates** from the running server. This keeps familiar vanilla architecture where it fits without bundling copied community builds.

Currently used:
- `minecraft:village/plains/houses/plains_small_house_4`
- `minecraft:village/plains/houses/plains_library_1`
- `minecraft:ruined_portal/portal_7`

No third-party builder schematic or NBT file is redistributed by this project.

## Landmarks

- **Lush Hollow** — open cave face, moss shelves, clay-bottom pond, rooted dirt, glow berries, dripleaf, spore blossoms and axolotls.
- **Dripstone Cathedral** — deepslate/tuff cavern with tall authored ribs, pointed dripstone, copper, a drip pool and a lava pocket.
- **Witch's Moor** — muddy wetland with broken pools, twisted mangrove, stilt witch hut, frogs and a witch.
- **Ruined Portal** — hand-authored corrupted island using a built-in ruined-portal template as one ruin, plus a reliable working portal. Any template loot containers are removed.
- **Drowned Monument** — broken prismarine gate, unequal towers, water court, sea lanterns, sponge and guardians.
- **Desert Oasis** — stepped sandstone underside, dune shelves, pond, clay, bent palms, reeds, cactus, red-sand details, broken arch and camel.
- **Frozen Observatory** — rocky/icy island with a custom calcite-and-stone observatory, copper telescope, spruce and goats.
- **Mushroom Colony** — mycelium island with irregular giant mushrooms, podzol and mooshrooms.
- **Broken Geode** — fractured basalt/calcite shell with a walk-in amethyst interior, budding amethyst and oriented crystal growths.
- **Void Apiary** — meadow, irregular oak, timber hive shelter, bee nests/hives, flowers, honey blocks and bees.
- **End Shrine** — broken causeway, uneven obsidian/purpur pylons, fragmented arches and a working End portal.
- **Little Village** — irregular grassy/rocky island with real vanilla plains house templates, paths, village green, crooked oak, small farm, pond, flowers and two persistent villagers.

Only the **starter island** and **Little Village** intentionally contain loot chests.

## Regenerating after structure changes

These structures are written directly into the world. To see a redesign cleanly, stop the server, delete the `skyblock` world and `plugins/SkyblockProgression` data folder, then restart with the updated plugin jar.
