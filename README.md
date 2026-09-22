# Skyblock Progression

A Paper 26.3 plugin for a small, vanilla-client Skyblock server. It deliberately avoids generic economy/crate/shop systems and instead makes normal Minecraft mechanics work as a progression game over the void.

**Clients need no mods or resource pack.**

## What it adds

- A real custom **Skybound** advancement tab with personal, exploration and communal milestones.
- Four persistent server-wide goals:
  - **Stone by Stone** — mine 5,000 cobblestone.
  - **Somewhere to Live** — place 3,000 blocks.
  - **Night Shift** — kill 250 hostile mobs.
  - **It Takes a Village** — breed 50 creatures.
- Progression-gated recipes that fix Skyblock resource dead ends:
  - 4 cobblestone → 1 gravel after obtaining cobblestone.
  - 2 gravel → 1 sand after the communal cobblestone goal.
  - 8 rotten flesh + 1 dirt → 2 dirt after the communal breeding goal.
  - 2 quartz + 2 bone meal → 2 calcite after reaching the Nether.
  - A reusable **Wayfarer's Bell** after the communal hostile-mob goal.
- Reworked wandering traders with a pool focused on otherwise awkward renewable resources: saplings, bamboo, cactus, sugar cane, kelp, cocoa, moss, dripstone, dripleaf, berries, mushrooms, seeds and sea pickles.
- The Wayfarer's Bell summons a wandering trader near its user, with a configurable cooldown.
- Five hand-built floating exploration targets:
  - **Lush Outcrop**
  - **Dripstone Spire**
  - **Witch's Moor**
  - **Ruined Portal**
  - **Monument Shard**

Each island has a discovery advancement and a small, deliberately limited resource cache.

## Requirements

- Paper 26.3
- Java 25

Paper 26.x requires Java 25.

## Build

Run:

    mvn verify

The plugin jar is produced under target/.

## Install

1. Build the jar or download a build artifact.
2. Put the jar in the Paper server's plugins/ directory.
3. Start the server once.
4. Check plugins/SkyblockProgression/config.yml.
5. If your Skyblock overworld is not the first overworld, set world: to its exact world name.

Exploration islands auto-generate only if all remote target areas look like void. This is a safety guard: the plugin will not silently punch structures into a normal terrain world. Operators can inspect /skyblock islands and deliberately use /skyblock islands generate force if needed.

## Commands

- /skyblock progress — communal counters and your custom advancement count.
- /skyblock islands — list exploration island coordinates.
- /skyblock islands generate [force] — generate/regenerate islands; op only.
- /skyblock trader — summon a test trader immediately; op only.
- /skyblock grant <player> <advancement-id> — admin/debug grant.

## Progression philosophy

The plugin is intentionally conservative. A cobblestone generator does not eventually produce every resource in the game. Different resources come from different activities: community goals, Nether progression, exploration islands and wandering traders.

Custom recipes are registered on the server but cannot be crafted until their prerequisite is completed. Locked recipes are also removed from the player's recipe book; when a prerequisite is completed the recipe is discovered automatically.

Communal completions are permanent and are synchronised to players who join later.

## Notes

Custom advancements are loaded through Paper/Bukkit's current advancement-loading API so they appear in the normal vanilla advancement UI. Vanilla advancements are left intact rather than aggressively deleting/revoking them; the custom **Skybound** tab is the server's intended progression path.
