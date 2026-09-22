# Skyblock Progression

A self-contained Paper 26.3 Skyblock plugin for a small vanilla-client server.

**Clients need no mods or resource pack.**

## Included

- A generated **true void overworld** named `skyblock` by default.
- A starter island with a tree, dirt/grass, and a deliberately small starter chest containing the essentials for a cobblestone generator.
- **100+ custom advancements** split across custom tabs:
  - Skybound core progression
  - Farming
  - Engineering
  - Combat
  - Exploration
  - Community
  - Museum of Stuff
  - Questionable Decisions
- Vanilla Minecraft advancement progress is suppressed, so players use the custom advancement system instead.
- Persistent personal counters and **8 communal goals**.
- Progression-gated custom recipes.
- Reworked wandering traders and the craftable **Wayfarer's Bell** used to summon one.
- **11 hand-built exploration islands/structures**:
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

The End Shrine contains a working portal so a void overworld does not make End progression impossible. Structures provide narrow resource unlocks rather than giant loot dumps.

## Funny advancements

The custom tree includes deliberately stupid side goals alongside real progression, including:

- **Fine Dining** — eat rotten flesh.
- **It Says Poisonous** — eat a poisonous potato anyway.
- **Gravity Still Works** — die in the void.
- **Research Confirmed** — do that ten times.
- **I Could Have Played Normal Minecraft** — mine 10,000 cobblestone.
- **Capitalism** — carry 64 emeralds.
- **Landlord** — carry 16 beds.
- **Touch Grassn't** — carry 64 cactus.
- **Terrible Financial Decision** — make a netherite hoe.
- **Optimist** — make a boat in a sky world.
- **OSHA Has Left the Server** — obtain scaffolding.

## World generation

On first start the plugin creates the configured `world:` as a void world and builds the starter island at its spawn. First-time players are teleported there automatically.

The server's ordinary default `world` folder may still exist because Bukkit loads its configured default worlds before normal plugins enable, but it is not used for Skyblock gameplay. The plugin also exposes its void `ChunkGenerator`, so it can be selected as a Bukkit world generator if you want the server's configured default world itself to use it.

## Requirements

- Paper 26.3
- Java 25

## Build

    mvn verify

The jar is produced under `target/`.

## Commands

- `/skyblock progress` — communal counters and your custom advancement count.
- `/skyblock islands` — tells normal players how many hidden structures exist; operators see exact coordinates.
- `/skyblock islands generate [force]` — generate/regenerate structures; op only.
- `/skyblock trader` — summon a test trader immediately; op only.
- `/skyblock grant <player> <advancement-id>` — admin/debug grant.

## Progression design

The plugin avoids the common Skyblock failure mode where one upgraded cobblestone generator eventually creates every material. Resources instead come from different systems: farming, mobs, traders, exploration structures, Nether/End progression, community milestones and selected recipes.

Communal completions persist across restarts and are granted to players who join later. Personal milestone counters such as cobblestone mined, mobs killed, trees grown, deaths, breeding and fishing persist in `data.yml`.
