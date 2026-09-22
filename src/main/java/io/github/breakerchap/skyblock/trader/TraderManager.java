package io.github.breakerchap.skyblock.trader;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.island.IslandDefinition;
import io.github.breakerchap.skyblock.island.IslandManager;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TraderManager implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final RecipeManager recipes;
    private final IslandManager islands;
    private final ProgressionService progression;

    private final List<Trade> tradePool = List.of(
        new Trade(Material.OAK_SAPLING, 1, 2, 8),
        new Trade(Material.SPRUCE_SAPLING, 1, 2, 8),
        new Trade(Material.BIRCH_SAPLING, 1, 2, 8),
        new Trade(Material.JUNGLE_SAPLING, 1, 3, 6),
        new Trade(Material.ACACIA_SAPLING, 1, 2, 8),
        new Trade(Material.DARK_OAK_SAPLING, 4, 4, 4),
        new Trade(Material.CHERRY_SAPLING, 1, 3, 6),
        new Trade(Material.MANGROVE_PROPAGULE, 1, 3, 6),
        new Trade(Material.BAMBOO, 4, 2, 8),
        new Trade(Material.CACTUS, 1, 2, 8),
        new Trade(Material.SUGAR_CANE, 3, 2, 8),
        new Trade(Material.KELP, 4, 2, 8),
        new Trade(Material.COCOA_BEANS, 3, 2, 8),
        new Trade(Material.MOSS_BLOCK, 2, 2, 8),
        new Trade(Material.POINTED_DRIPSTONE, 2, 3, 8),
        new Trade(Material.SMALL_DRIPLEAF, 2, 3, 6),
        new Trade(Material.GLOW_BERRIES, 3, 2, 8),
        new Trade(Material.SWEET_BERRIES, 3, 2, 8),
        new Trade(Material.LILY_PAD, 2, 2, 8),
        new Trade(Material.RED_MUSHROOM, 2, 2, 8),
        new Trade(Material.BROWN_MUSHROOM, 2, 2, 8),
        new Trade(Material.PUMPKIN_SEEDS, 4, 2, 8),
        new Trade(Material.MELON_SEEDS, 4, 2, 8),
        new Trade(Material.SEA_PICKLE, 2, 3, 6)
    );

    private final List<RareEgg> eggPool = List.of(
        new RareEgg(Material.GOAT_SPAWN_EGG, 12),
        new RareEgg(Material.AXOLOTL_SPAWN_EGG, 16),
        new RareEgg(Material.FROG_SPAWN_EGG, 16),
        new RareEgg(Material.TURTLE_SPAWN_EGG, 18),
        new RareEgg(Material.ARMADILLO_SPAWN_EGG, 18),
        new RareEgg(Material.CAMEL_SPAWN_EGG, 20),
        new RareEgg(Material.ALLAY_SPAWN_EGG, 28),
        new RareEgg(Material.SNIFFER_SPAWN_EGG, 32)
    );

    public TraderManager(
        SkyblockPlugin plugin,
        ProgressStore store,
        RecipeManager recipes,
        IslandManager islands,
        ProgressionService progression
    ) {
        this.plugin = plugin;
        this.store = store;
        this.recipes = recipes;
        this.islands = islands;
        this.progression = progression;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof WanderingTrader trader) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (trader.isValid()) {
                    configureTrader(trader);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBellUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        if (!recipes.isWayfarerBell(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        summonFor(event.getPlayer(), false);
    }

    public boolean summonFor(Player player, boolean adminOverride) {
        // Deliberately unlimited: every ring may summon another trader.
        Location spawn = findSpawn(player);
        WanderingTrader trader = player.getWorld().spawn(spawn, WanderingTrader.class);
        configureTrader(trader);

        long summons = store.incrementPlayer(player.getUniqueId(), "traders-summoned", 1);
        progression.grant(player, "engineering/wayfarer_call");
        if (summons >= 5) {
            progression.grant(player, "engineering/trader_5");
        }
        if (summons >= 25) {
            progression.grant(player, "engineering/trader_25");
        }
        store.saveIfDirty();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.8f);
        player.getWorld().playSound(spawn, Sound.ENTITY_WANDERING_TRADER_YES, 1.0f, 1.0f);
        player.sendMessage(Component.text("Someone heard the bell.", NamedTextColor.GOLD));
        return true;
    }

    private Location findSpawn(Player player) {
        World world = player.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int baseY = player.getLocation().getBlockY();

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            int distance = random.nextInt(10, 23);
            int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

            for (int y = baseY + 6; y >= baseY - 8; y--) {
                if (world.getBlockAt(x, y - 1, z).getType().isSolid()
                    && world.getBlockAt(x, y, z).isEmpty()
                    && world.getBlockAt(x, y + 1, z).isEmpty()) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }

        return player.getLocation().clone().add(2, 0, 2);
    }

    private void configureTrader(WanderingTrader trader) {
        List<MerchantRecipe> offers = new ArrayList<>();

        List<Trade> resources = new ArrayList<>(tradePool);
        Collections.shuffle(resources);
        int resourceCount = Math.max(2, Math.min(
            plugin.getConfig().getInt("trader.resource-trade-count", 6),
            resources.size()
        ));
        for (Trade trade : resources.subList(0, resourceCount)) {
            addOffer(offers, new ItemStack(trade.result(), trade.resultAmount()), trade.emeraldCost(), trade.maxUses());
        }

        List<IslandDefinition> structureChoices = new ArrayList<>(islands.definitions());
        Collections.shuffle(structureChoices);
        int mapCount = Math.max(1, Math.min(
            plugin.getConfig().getInt("trader.map-trade-count", 3),
            structureChoices.size()
        ));
        for (IslandDefinition definition : structureChoices.subList(0, mapCount)) {
            Location location = islands.location(definition);
            int distance = (int) Math.round(Math.hypot(definition.offsetX(), definition.offsetZ()));
            int cost = Math.max(4, Math.min(12, 3 + distance / 100));
            addOffer(offers, createStructureMap(definition, location), cost, 4);
        }

        List<RareEgg> eggs = new ArrayList<>(eggPool);
        Collections.shuffle(eggs);
        int eggCount = Math.max(1, Math.min(
            plugin.getConfig().getInt("trader.spawn-egg-trade-count", 2),
            eggs.size()
        ));
        for (RareEgg egg : eggs.subList(0, eggCount)) {
            addOffer(offers, new ItemStack(egg.material()), egg.emeraldCost(), 2);
        }

        trader.setRecipes(offers);
    }

    private ItemStack createStructureMap(IslandDefinition definition, Location target) {
        MapView view = Bukkit.createMap(target.getWorld());
        view.setCenterX(target.getBlockX());
        view.setCenterZ(target.getBlockZ());
        view.setScale(MapView.Scale.FAR);
        view.setTrackingPosition(true);
        view.setUnlimitedTracking(true);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        if (map.getItemMeta() instanceof MapMeta meta) {
            meta.setMapView(view);
            meta.displayName(Component.text("Map to " + definition.displayName(), NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("The marked land should be near the centre.", NamedTextColor.GRAY),
                Component.text("No coordinates. That would be too easy.", NamedTextColor.DARK_GRAY)
            ));
            map.setItemMeta(meta);
        }
        return map;
    }

    private void addOffer(List<MerchantRecipe> offers, ItemStack result, int emeraldCost, int maxUses) {
        MerchantRecipe recipe = new MerchantRecipe(result, maxUses);
        recipe.addIngredient(new ItemStack(Material.EMERALD, emeraldCost));
        recipe.setExperienceReward(false);
        recipe.setPriceMultiplier(0.0f);
        recipe.setIgnoreDiscounts(true);
        offers.add(recipe);
    }

    private record Trade(Material result, int resultAmount, int emeraldCost, int maxUses) {
    }

    private record RareEgg(Material material, int emeraldCost) {
    }
}
