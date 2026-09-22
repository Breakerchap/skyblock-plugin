# Custom recipes

All recipes here are registered as ordinary Paper recipes in the `skyblock` recipe group. On startup, `RecipeManager` checks all seven recipe keys with `Bukkit.getRecipe(...)`; if Paper rejected or lost one, the plugin throws instead of pretending the recipe works.

## Always available

### Bell — `skyblock:bell`

Produces **1 Bell**.

```text
Gold Ingot | Iron Ingot | Gold Ingot
           | Stick      |
```

Exact ingredients: **2 gold ingots + 1 iron ingot + 1 stick**.

### Wayfarer's Bell — `skyblock:wayfarer_bell`

Produces **1 Wayfarer's Bell**.

```text
Emerald    | Gold Ingot | Emerald
Gold Ingot | Bell       | Gold Ingot
Emerald    | Gold Ingot | Emerald
```

Exact ingredients: **4 emeralds + 4 gold ingots + 1 bell**.

Every valid use can summon another trader; there is no cooldown and no nearby-trader cap.

### Void Trowel — `skyblock:void_trowel`

Produces **1 Void Trowel**.

```text
Iron Ingot
Stick
```

Exact ingredients: **1 iron ingot + 1 stick**.

## Progression recipes

These recipes exist in the server recipe registry at all times, but crafting is blocked until their progression condition is met.

### Gravel from Cobblestone — `skyblock:gravel_from_cobblestone`

Shapeless: **4 cobblestone -> 1 gravel**.

Unlock: personal advancement `getting_started/cobblestone`.

### Sand from Gravel — `skyblock:sand_from_gravel`

Shapeless: **2 gravel -> 1 sand**.

Unlock: communal cobblestone goal.

### Dirt Cultivation — `skyblock:dirt_cultivation`

Produces **2 dirt**.

```text
Rotten Flesh | Rotten Flesh | Rotten Flesh
Rotten Flesh | Dirt         | Rotten Flesh
Rotten Flesh | Rotten Flesh | Rotten Flesh
```

Exact ingredients: **8 rotten flesh + 1 dirt**.

Unlock: communal breeding/life goal.

### Calcite from Quartz — `skyblock:calcite_from_quartz`

Shapeless: **2 quartz + 2 bone meal -> 2 calcite**.

Unlock: personal `nether/root` advancement.

## Recipe book and JEI

Bell, Wayfarer's Bell and Void Trowel are discovered for every player on progression sync, so they appear in the vanilla recipe book immediately. The four progression recipes are discovered when their unlock condition is satisfied and are also server-side blocked before then.

JEI on Minecraft 1.21.2+ needs the server's full recipe data over a recipe-sync channel; client-only JEI cannot reconstruct plugin-added recipes. This plugin keeps every recipe in Paper's normal recipe registry and soft-depends on `JEIRecipeFix`, so a compatible recipe-sync plugin can transmit them without special-case Skyblock recipe definitions.
