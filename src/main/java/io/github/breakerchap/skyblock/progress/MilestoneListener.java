package io.github.breakerchap.skyblock.progress;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.advancement.AdvancementCatalog;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public final class MilestoneListener implements Listener {
    private static final Set<Material> CROPS = EnumSet.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.NETHER_WART
    );

    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final ProgressionService progression;
    private final RecipeManager recipes;

    public MilestoneListener(
        SkyblockPlugin plugin,
        ProgressStore store,
        ProgressionService progression,
        RecipeManager recipes
    ) {
        this.plugin = plugin;
        this.store = store;
        this.progression = progression;
        this.recipes = recipes;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkMaterial(player, event.getItem().getItemStack().getType());
            plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        checkMaterial(player, result.getType());

        if (recipes.isWayfarerBell(result)) {
            progression.grant(player, "engineering/wayfarer_bell");
        }
        if (recipes.isVoidTrowel(result)) {
            progression.grant(player, "engineering/void_trowel");
        }

        store.incrementPlayer(player.getUniqueId(), "craft-actions", 1);
        progression.incrementCommunity("craft-actions", 1);

        plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        if (material == Material.COBBLESTONE) {
            long cobble = store.incrementPlayer(player.getUniqueId(), "cobblestone-mined", 1);
            grantAt(player, cobble, 64, "engineering/cobble_64");
            grantAt(player, cobble, 1000, "engineering/cobble_1000");
            grantAt(player, cobble, 10000, "engineering/cobble_10000");
            grantAt(player, cobble, 10000, "oddities/cobble_10000");
        }

        if (CROPS.contains(material)) {
            progression.incrementCommunity("crops-harvested", 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        long placed = store.incrementPlayer(player.getUniqueId(), "blocks-placed", 1);
        grantAt(player, placed, 100, "engineering/place_100");
        grantAt(player, placed, 1000, "engineering/place_1000");

        if (event.getBlockPlaced().getType() == Material.BEACON) {
            progression.grant(player, "engineering/beacon");
            progression.grant(player, "end/beacon");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        long trees = store.incrementPlayer(player.getUniqueId(), "trees-grown", 1);
        progression.incrementCommunity("trees-grown", 1);
        progression.grant(player, "getting_started/tree");
        grantAt(player, trees, 10, "farming/trees_10");
        grantAt(player, trees, 100, "farming/trees_100");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }

        long bred = store.incrementPlayer(player.getUniqueId(), "creatures-bred", 1);
        progression.grant(player, "farming/breed");
        grantAt(player, bred, 10, "farming/breed_10");
        grantAt(player, bred, 100, "farming/breed_100");

        if (event.getEntity() instanceof Villager) {
            progression.grant(player, "civilisation/villager");
        }
        if (event.getEntityType() == EntityType.GOAT) {
            progression.grant(player, "farming/goat_breeder");
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            long villagers = player.getNearbyEntities(16, 8, 16).stream()
                .filter(entity -> entity instanceof Villager)
                .count();
            if (villagers >= 5) {
                progression.grant(player, "farming/village_people");
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Boat)) {
            return;
        }

        String id = switch (event.getEntered().getType()) {
            case GOAT -> "farming/goat_boat";
            case BEE -> "farming/bee_boat";
            default -> null;
        };
        if (id != null) {
            grantNearby(event.getVehicle().getLocation(), id);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (event.getEntity() instanceof Enemy) {
            long kills = store.incrementPlayer(killer.getUniqueId(), "hostile-kills", 1);
            grantAt(killer, kills, 10, "combat/kills_10");
            grantAt(killer, kills, 100, "combat/kills_100");
            grantAt(killer, kills, 1000, "combat/kills_1000");

            if (killer.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).isEmpty()) {
                progression.grant(killer, "combat/air_superiority");
            }
        }

        if (event.getEntityType() == EntityType.SKELETON && event.getEntity().getVehicle() instanceof Boat) {
            progression.grant(killer, "combat/skeleton_crew");
        }

        String advancement = switch (event.getEntityType()) {
            case ZOMBIE -> "combat/zombie";
            case SKELETON -> "combat/skeleton";
            case CREEPER -> "combat/creeper";
            case SPIDER, CAVE_SPIDER -> "combat/spider";
            case WITCH -> "combat/witch";
            case SLIME -> "combat/slime";
            case ENDERMAN -> "combat/enderman";
            case DROWNED -> "combat/drowned";
            case PHANTOM -> "combat/phantom";
            case BLAZE -> "combat/blaze";
            case GHAST -> "combat/ghast";
            case MAGMA_CUBE -> "combat/magma_cube";
            case WITHER_SKELETON -> "combat/wither_skeleton";
            case GUARDIAN, ELDER_GUARDIAN -> "combat/guardian";
            case SHULKER -> "combat/shulker";
            case PIGLIN_BRUTE -> "combat/piglin_brute";
            case WITHER -> "combat/wither";
            case ENDER_DRAGON -> "combat/dragon";
            default -> null;
        };

        if (advancement != null) {
            progression.grant(killer, advancement);
        }
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            progression.grant(killer, "end/dragon");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        long fish = store.incrementPlayer(player.getUniqueId(), "fish-caught", 1);
        progression.incrementCommunity("fish-caught", 1);
        progression.grant(player, "oddities/fish");
        grantAt(player, fish, 25, "oddities/fish_25");
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String id = switch (event.getItem().getType()) {
            case ROTTEN_FLESH -> "oddities/rotten_flesh";
            case SPIDER_EYE -> "oddities/spider_eye";
            case POISONOUS_POTATO -> "oddities/poisonous_potato";
            case PUFFERFISH -> "oddities/pufferfish";
            default -> null;
        };
        if (id != null) {
            progression.grant(event.getPlayer(), id);
        }
    }

    @EventHandler
    public void onBed(PlayerBedEnterEvent event) {
        if (event.getPlayer().getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            progression.grant(event.getPlayer(), "nether/bed_attempt");
            return;
        }

        String skyWorld = plugin.getConfig().getString("world", "skyblock");
        if (event.getPlayer().getWorld().getName().equals(skyWorld)) {
            progression.grant(event.getPlayer(), "oddities/sleep");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        long deaths = store.incrementPlayer(player.getUniqueId(), "deaths", 1);
        grantAt(player, deaths, 10, "oddities/deaths_10");
        grantAt(player, deaths, 50, "oddities/deaths_50");

        if (player.getLastDamageCause() != null
            && player.getLastDamageCause().getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) {
            long voidDeaths = store.incrementPlayer(player.getUniqueId(), "void-deaths", 1);
            progression.grant(player, "oddities/void_death");
            grantAt(player, voidDeaths, 10, "oddities/void_deaths_10");
            if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                progression.grant(player, "end/void_death");
            }
        }
    }

    private void checkInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                checkMaterial(player, item.getType());
            }
        }

        checkAmount(player, Material.DIRT, 64, "oddities/dirt_64");
        checkAmount(player, Material.EMERALD, 64, "oddities/emerald_64");
        checkAmount(player, Material.BREAD, 64, "oddities/bread_64");
        checkAmount(player, Material.TORCH, 64, "oddities/torches_64");
        checkAmount(player, Material.CACTUS, 64, "oddities/cactus_64");
        checkAmount(player, Material.EGG, 64, "oddities/eggs_64");
        checkAmount(player, Material.ROTTEN_FLESH, 64, "oddities/flesh_64");

        int beds = 0;
        boolean water = false;
        boolean lava = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            if (item.getType().name().endsWith("_BED")) {
                beds += item.getAmount();
            }
            water |= item.getType() == Material.WATER_BUCKET;
            lava |= item.getType() == Material.LAVA_BUCKET;
        }

        if (beds >= 16) {
            progression.grant(player, "oddities/beds_16");
        }
        if (water && lava) {
            progression.grant(player, "oddities/thermodynamics");
        }
    }

    private void checkAmount(Player player, Material material, int amount, String id) {
        if (player.getInventory().all(material).values().stream().mapToInt(ItemStack::getAmount).sum() >= amount) {
            progression.grant(player, id);
        }
    }

    private void checkMaterial(Player player, Material material) {
        if (AdvancementCatalog.collectionMaterials().contains(material)) {
            progression.grant(player, AdvancementCatalog.collectionId(material));
        }

        String id = switch (material) {
            case COBBLESTONE -> "getting_started/cobblestone";
            case STONE -> "getting_started/stone";
            case CRAFTING_TABLE -> "getting_started/crafting";
            case FURNACE -> "getting_started/furnace";
            case CHARCOAL -> "getting_started/charcoal";
            case IRON_INGOT -> "getting_started/iron";
            case BUCKET -> "getting_started/bucket";
            case LAVA_BUCKET -> "getting_started/lava";
            case WATER_BUCKET -> "getting_started/water";
            case GLASS -> "getting_started/glass";
            case CHEST -> "getting_started/chest";
            case TORCH -> "getting_started/torch";
            case SHIELD -> "getting_started/shield";
            case DIAMOND -> "getting_started/diamond";
            case ENCHANTING_TABLE -> "getting_started/enchanting";
            case EMERALD -> "getting_started/emerald";

            case WHEAT -> "farming/wheat";
            case CARROT -> "farming/carrot";
            case POTATO -> "farming/potato";
            case BEETROOT -> "farming/beetroot";
            case PUMPKIN -> "farming/pumpkin";
            case MELON_SLICE -> "farming/melon";
            case SUGAR_CANE -> "farming/sugar_cane";
            case CACTUS -> "farming/cactus";
            case BAMBOO -> "farming/bamboo";
            case COCOA_BEANS -> "farming/cocoa";
            case KELP -> "farming/kelp";
            case RED_MUSHROOM -> "farming/red_mushroom";
            case BROWN_MUSHROOM -> "farming/brown_mushroom";
            case GLOW_BERRIES -> "farming/glow_berries";
            case SWEET_BERRIES -> "farming/sweet_berries";
            case MOSS_BLOCK -> "farming/moss";
            case POINTED_DRIPSTONE -> "farming/dripstone";
            case HONEY_BOTTLE -> "farming/honey";
            case EGG -> "farming/egg";
            case BREAD -> "farming/bread";
            case CAKE -> "farming/cake";

            case REDSTONE -> "engineering/redstone";

            case QUARTZ -> "nether/quartz";
            case GLOWSTONE_DUST -> "nether/glowstone";
            case SOUL_SAND -> "nether/soul_sand";
            case NETHER_WART -> "nether/nether_wart";
            case BLAZE_ROD -> "nether/blaze";
            case MAGMA_CREAM -> "nether/magma_cream";
            case CRYING_OBSIDIAN -> "nether/crying_obsidian";
            case ANCIENT_DEBRIS -> "nether/ancient_debris";
            case NETHERITE_SCRAP -> "nether/netherite_scrap";
            case NETHERITE_INGOT -> "nether/netherite";
            case RESPAWN_ANCHOR -> "nether/respawn_anchor";
            case WITHER_SKELETON_SKULL -> "nether/wither_skull";
            case NETHER_STAR -> "nether/nether_star";

            case END_STONE -> "end/end_stone";
            case CHORUS_FRUIT -> "end/chorus";
            case PURPUR_BLOCK -> "end/purpur";
            case END_ROD -> "end/end_rod";
            case SHULKER_SHELL -> "end/shulker_shell";
            case ELYTRA -> "end/elytra";
            case DRAGON_HEAD -> "end/dragon_head";
            case DRAGON_BREATH -> "end/dragon_breath";
            case END_CRYSTAL -> "end/end_crystal";

            case DIAMOND_HOE -> "oddities/diamond_hoe";
            case NETHERITE_HOE -> "oddities/netherite_hoe";
            case COOKIE -> "oddities/cookie";
            case FISHING_ROD -> "oddities/fishing_rod";
            case SPYGLASS -> "oddities/spyglass";
            case CLOCK -> "oddities/clock";
            case COMPASS -> "oddities/compass";
            case SCAFFOLDING -> "oddities/scaffolding";
            default -> null;
        };

        if (id != null) {
            progression.grant(player, id);
        }

        if (material.name().endsWith("_BED")) {
            progression.grant(player, "getting_started/bed");
        }
        if (material.name().endsWith("_WOOL")) {
            progression.grant(player, "farming/wool");
        }
        if (material.name().endsWith("_BOAT")) {
            progression.grant(player, "oddities/boat");
        }
        if (material.name().endsWith("SHULKER_BOX")) {
            progression.grant(player, "end/shulker_box");
        }
    }

    private void grantNearby(org.bukkit.Location location, String id) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= 16 * 16) {
                progression.grant(player, id);
            }
        }
    }

    private void grantAt(Player player, long value, long target, String id) {
        if (value >= target) {
            progression.grant(player, id);
        }
    }
}
