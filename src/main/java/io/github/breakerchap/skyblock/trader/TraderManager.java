package io.github.breakerchap.skyblock.trader;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TraderManager implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final RecipeManager recipes;

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

    public TraderManager(SkyblockPlugin plugin, ProgressStore store, RecipeManager recipes) {
        this.plugin = plugin;
        this.store = store;
        this.recipes = recipes;
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

    public boolean summonFor(Player player, boolean ignoreCooldown) {
        long now = System.currentTimeMillis();
        long cooldownUntil = store.getTraderCooldownUntil(player.getUniqueId());

        if (!ignoreCooldown && cooldownUntil > now) {
            long minutes = Math.max(1L, (cooldownUntil - now + 59_999L) / 60_000L);
            player.sendMessage(Component.text(
                "The bell stays quiet. Try again in about " + minutes + " minute" + (minutes == 1 ? "" : "s") + ".",
                NamedTextColor.GRAY
            ));
            return false;
        }

        boolean nearbyTrader = player.getWorld().getNearbyEntities(
            player.getLocation(), 64, 32, 64,
            entity -> entity instanceof WanderingTrader
        ).stream().findAny().isPresent();

        if (nearbyTrader) {
            player.sendMessage(Component.text("A wandering trader is already nearby.", NamedTextColor.YELLOW));
            return false;
        }

        Location spawn = findSpawn(player);
        WanderingTrader trader = player.getWorld().spawn(spawn, WanderingTrader.class);
        configureTrader(trader);

        long cooldownSeconds = plugin.getConfig().getLong("trader.summon-cooldown-seconds", 3600L);
        store.setTraderCooldownUntil(player.getUniqueId(), now + cooldownSeconds * 1000L);
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
        List<Trade> shuffled = new ArrayList<>(tradePool);
        Collections.shuffle(shuffled);

        int count = Math.max(4, Math.min(
            plugin.getConfig().getInt("trader.trade-count", 8),
            shuffled.size()
        ));

        List<MerchantRecipe> offers = new ArrayList<>();
        for (Trade trade : shuffled.subList(0, count)) {
            MerchantRecipe recipe = new MerchantRecipe(
                new ItemStack(trade.result(), trade.resultAmount()),
                trade.maxUses()
            );
            recipe.addIngredient(new ItemStack(Material.EMERALD, trade.emeraldCost()));
            recipe.setExperienceReward(false);
            recipe.setPriceMultiplier(0.0f);
            recipe.setIgnoreDiscounts(true);
            offers.add(recipe);
        }

        trader.setRecipes(offers);
    }

    private record Trade(Material result, int resultAmount, int emeraldCost, int maxUses) {
    }
}
