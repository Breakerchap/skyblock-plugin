package io.github.breakerchap.skyblock.progress;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

public final class ProgressListener implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressionService progression;

    public ProgressListener(SkyblockPlugin plugin, ProgressionService progression) {
        this.plugin = plugin;
        this.progression = progression;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> progression.syncPlayer(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItem(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        checkItem(player, result.getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent event) {
        if (event.getPlayer() != null) {
            progression.grant(event.getPlayer(), "getting_started/tree");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.COBBLESTONE) {
            progression.incrementCommunity("cobblestone-mined", 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        progression.incrementCommunity("blocks-placed", 1);
        if (event.getBlockPlaced().getType() == Material.BEACON) {
            progression.grant(event.getPlayer(), "end/beacon");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            progression.incrementCommunity("creatures-bred", 1);
            if (event.getEntity() instanceof Villager) {
                progression.grant(player, "civilisation/villager");
            }
        }
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World.Environment environment = player.getWorld().getEnvironment();
        if (environment == World.Environment.NETHER) {
            progression.grant(player, "nether/root");
        } else if (environment == World.Environment.THE_END) {
            progression.grant(player, "end/root");
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (event.getEntity() instanceof Enemy) {
            progression.incrementCommunity("hostiles-killed", 1);
        }

        if (event.getEntity() instanceof EnderDragon) {
            progression.grant(killer, "end/dragon");
        }
    }

    private void checkItem(Player player, Material material) {
        switch (material) {
            case COBBLESTONE -> progression.grant(player, "getting_started/cobblestone");
            case IRON_INGOT -> progression.grant(player, "getting_started/iron");
            case LAVA_BUCKET -> progression.grant(player, "getting_started/lava");
            case BLAZE_ROD -> progression.grant(player, "nether/blaze");
            case NETHERITE_INGOT -> progression.grant(player, "nether/netherite");
            default -> {
            }
        }
    }
}
