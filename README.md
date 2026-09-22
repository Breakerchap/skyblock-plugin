# Skyblock Progression

A self-contained Paper 26.3 Skyblock plugin for a small vanilla-client server.

**Clients need no mods or resource pack.**

## Included

- A generated **true void overworld** named `skyblock` by default.
- A starter island with a tree, dirt/grass, and the essentials for a cobblestone generator.
- **250 custom advancements** across Skyblock progression, farming, engineering, combat, exploration, community, collection and deliberately silly side goals.
- Vanilla Minecraft advancement progress is suppressed, so players use the custom advancement system instead.
- Persistent personal counters and eight communal goals.
- Progression-gated renewable-resource recipes.
- A craftable **Wayfarer's Bell** whose recipe is visible to every player from the start.
- A cheap **Void Trowel** whose recipe is also visible from the start.
- Reworked wandering traders that sell renewable resources, maps to progression structures and expensive spawn eggs for awkward/impossible Skyblock mobs.
- Twelve hand-built exploration islands/structures, now placed a few hundred blocks away instead of well over a thousand:
  - Lush Outcrop
  - Dripstone Spire
  - Witch's Moor
  - Ruined Portal
  - Monument Shard
  - Desert Shrine
  - Frozen Observatory
  - Mushroom Colony
  - Amethyst Geode
  - Void Apiary
  - End Shrine
  - Little Village

The Little Village contains two persistent villagers, beds, farms and a bell. The End Shrine contains a working portal.

## Void Trowel / Bedrock-style bridging

Craft the **Void Trowel** from two cobblestone and a stick. Put it in your **offhand** and put normal placeable blocks in your main hand.

While standing on the end of a bridge, right-click while facing where you want to go. The plugin places the next block directly in front of the block under your feet, so you do not need to look down at the edge like normal Java bridging.

Container blocks such as shulker boxes are intentionally excluded from this shortcut so their stored data cannot be lost.

## Wandering traders

The Wayfarer's Bell has **no cooldown**. Ringing it summons a trader as long as another wandering trader is not already nearby.

Each trader gets a random mix of:

- renewable Skyblock resources,
- several filled maps centred on random exploration structures,
- several rare spawn eggs.

The rare egg pool currently includes goats, axolotls, frogs, turtles, armadillos, camels, allays and sniffers. These are intentionally expensive and have limited uses.

## Advancement direction

The advancement tree is not intended to be a crafting checklist. Basic resource milestones remain where obtaining that resource is actually meaningful in Skyblock, but low-value entries such as “make a dropper”, “make a comparator”, “make a repeater”, and similar component-by-component achievements were removed.

Situation-based goals include:

- **Whatever Floats Your Goat** — put a goat in a boat.
- **Buzz Cruise** — put a bee in a boat.
- **Village People** — get five villagers together.
- **Skeleton Crew** — kill a skeleton while it is riding in a boat.
- **Air Superiority** — kill a hostile mob with no block beneath you.
- **What Did You Expect?** — try to sleep in the Nether.
- **Bedrock at Home** — use the Void Trowel.
- **Don't Look Down** — place 64 bridge blocks with it.
- **Bedrock Bridger** — use it while sprinting.
- **Frequent Caller** — summon five wandering traders.
- **He Knows Your Number** — summon 25.
- **Safety Third** — use the trowel while wearing no armour.
- **The Void Has Layers** — fall into the void in the End.

The existing Questionable Decisions tab still contains things such as eating rotten flesh, carrying 16 beds, making a netherite hoe and repeatedly verifying that gravity still works.

## World generation

On first start the plugin creates the configured `world:` as a void world and builds the starter island at its spawn. First-time players are teleported there automatically. Nether return portals and End return/respawn paths are redirected back into the Skyblock world.

### Updating an existing test world

Structure coordinates are stored only in code, while the generated blocks remain in the world. After changing to this version, the cleanest test is to stop the server and delete:

- `skyblock/`
- `plugins/SkyblockProgression/data.yml`

Then restart. That regenerates the world with the new, closer structure layout and Little Village instead of leaving the old distant structures behind.

## Requirements

- Paper 26.3
- Java 25

## Build

    mvn verify

The jar is produced under `target/`.

## Commands

- `/skyblock progress` — communal counters and your custom advancement count.
- `/skyblock islands` — normal players see the number of hidden structures; operators see exact coordinates.
- `/skyblock islands generate [force]` — generate/regenerate structures; op only.
- `/skyblock trader` — summon a test trader immediately; op only.
- `/skyblock grant <player> <advancement-id>` — admin/debug grant.
