# Skyblock Progression

A self-contained Paper 26.3 Skyblock plugin for a small vanilla-client server.

**Vanilla clients need no mods or resource pack.**

## Included

- A generated true-void overworld named `skyblock`.
- A starter island with a tree and a one-time starter chest containing the lava/ice/sapling/seeds needed to get moving.
- **251 custom advancements**, with most titles written as puns, references or jokes rather than plain item names.
- Vanilla advancement progress suppressed in favour of the Skyblock advancement trees.
- Persistent personal counters and communal milestones.
- Seven registered custom crafting recipes, all checked at plugin startup.
- An unlimited-use **Wayfarer's Bell** for summoning wandering traders.
- A cheap **Void Trowel** for Bedrock-style forward bridging in Java.
- Wandering traders with renewable resources, structure maps, and rare Skyblock mob spawn eggs.
- Twelve hand-built progression structures with environmental resources instead of loot chests.

## Structures

The exploration landmarks use **hand-authored block layouts**, not generic sphere/ellipsoid generators. Their silhouettes, terrain layers, paths, caves and architectural details are placed deliberately. Where Minecraft already has a strong vanilla building, the plugin loads the server's built-in structure template at runtime instead of copying third-party builds.

Currently reused vanilla templates:
- `minecraft:village/plains/houses/plains_small_house_4`
- `minecraft:village/plains/houses/plains_library_1`
- `minecraft:ruined_portal/portal_7`

The plugin does not redistribute third-party builder schematics. If a built-in template cannot be loaded, a plugin-authored fallback is used.


The structures are small destinations/biomes rather than floating loot boxes.

- **Lush Hollow** — a hollow cave with moss, rooted dirt, clay, a water pond, glow-berry vines, dripleaf, spore blossoms and axolotls. The area uses the Lush Caves biome.
- **Dripstone Cathedral** — a deepslate/tuff cavern with stalactites, stalagmites, water, lava and exposed copper.
- **Witch's Moor** — muddy mangrove wetland, lily pads, a stilt hut, frogs and a resident witch.
- **Ruined Portal** — blackstone/netherrack ruin with a working portal, magma, crying obsidian and gilded blackstone.
- **Drowned Monument** — broken prismarine arch surrounding a water court, sea lanterns, sponge and guardians.
- **Desert Oasis** — sandstone island with an oasis, palms, sugar cane, cactus, a ruined arch and a camel.
- **Frozen Observatory** — snowy/icy island with a little calcite/copper observatory, blue ice, powder snow, spruce and goats.
- **Mushroom Colony** — giant mushrooms, mycelium and mooshrooms.
- **Broken Geode** — walk-in basalt/calcite/amethyst geode with budding amethyst and clusters.
- **Void Apiary** — flower meadow, oak tree, bee nests/hives, honey blocks and bees.
- **End Shrine** — purpur/end-stone temple with a working End portal.
- **Little Village** — two houses, farms, bell, two persistent villagers and the only exploration-structure loot chest.

Outside the starter island and village, progression resources come from the structure itself rather than chests.

## Bells and wandering traders

Vanilla bells are normally obtained from villages or villager trading rather than through a normal crafting recipe. This plugin adds one:

```text
G I G
  S
```

where `G` is a gold ingot, `I` an iron ingot and `S` a stick.

The Wayfarer's Bell recipe is visible from the start. It has **no cooldown and no nearby-trader limit**: every valid ring may summon another trader.

## Void Trowel

Recipe:

```text
I
S
```

where `I` is an iron ingot and `S` a stick.

Put the Void Trowel in your **offhand** and ordinary placeable blocks in your main hand. While standing on the end of a bridge, right-click while facing where you want to go. The plugin places the next block in front of the block under your feet, so you do not need to aim at its edge.

Container blocks are excluded from this shortcut so their data cannot be accidentally destroyed.

## Recipes

All custom recipes are ordinary Paper/Bukkit shaped or shapeless recipes in the `skyblock` recipe group. The plugin verifies after registration that every recipe can be retrieved from Paper's recipe registry; startup fails loudly instead of silently dropping a broken recipe.

Exact ingredients and unlock conditions are documented in `docs/recipes.md`.

### JEI / recipe-viewer compatibility

The recipes themselves are standard server recipes, rather than recipes implemented in a custom GUI, so they are suitable for recipe-viewer synchronisation.

There is an important modern-Minecraft limitation: since 1.21.2 the vanilla server no longer sends the complete recipe data set to clients, so a client-only JEI installation cannot discover server-added recipes by itself. For JEI, use a compatible server-side recipe-sync plugin such as **JEIRecipeFix**; this plugin declares it as a soft dependency so the sync plugin can load first. The vanilla recipe book works without JEI, and always-available recipes such as Bell, Wayfarer's Bell and Void Trowel are discovered for every player.

At the time this was written, JEIRecipeFix publicly listed support through Paper 26.2, so **do not assume its current build is safe on 26.3 until it explicitly supports 26.3**. The Skyblock recipe definitions themselves need no JEI-specific changes once a compatible recipe-sync layer is present.

## Advancements

The advancement tree is deliberately not named like an item checklist. Even collection entries use titles such as **Granite Expectations**, **Another Brick in the Void**, **Coal Me Maybe**, **Prismarine Without the Marine**, **Shell Company**, **Forklift Certified** and **Key Performance Indicator**.

Situation-based goals include **Whatever Floats Your Goat**, **Buzz Cruise**, **Skeleton Crew**, **What Did You Expect?**, **Don't Look Down**, **Bedrock Bridger**, **He Knows Your Number**, **Safety Third** and **The Void Has Layers**.

## Updating an existing test world

The structures are written into world blocks. To see the rebuilt structures cleanly after updating the jar, stop the server and delete:

- `skyblock/`
- `plugins/SkyblockProgression/data.yml`

Then restart. Keep the plugin jar.

## Requirements

- Paper 26.3
- Java 25

## Build

```text
mvn verify
```

The jar is produced under `target/`.

## Commands

- `/skyblock progress` — communal counters and your custom advancement count.
- `/skyblock islands` — normal players see how many hidden structures exist; operators see coordinates.
- `/skyblock islands generate [force]` — regenerate structures; op only.
- `/skyblock trader` — summon a trader immediately; op only.
- `/skyblock grant <player> <advancement-id>` — admin/debug grant.


## Structure authoring

Exploration structures are being migrated to the repo's purpose-built **SkyStruct** authoring tool under `tools/structure_editor`. It provides explicit voxel blueprints, structural diagnostics, headless four-angle PNG renders and compilation to real vanilla Minecraft `.nbt` structure templates. See `tools/structure_editor/README.md`.
