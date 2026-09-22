package io.github.breakerchap.skyblock.bridge;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public final class VoidTrowelManager implements Listener {
    private final RecipeManager recipes;
    private final ProgressStore store;
    private final ProgressionService progression;

    public VoidTrowelManager(
        SkyblockPlugin plugin,
        RecipeManager recipes,
        ProgressStore store,
        ProgressionService progression
    ) {
        this.recipes = recipes;
        this.store = store;
        this.progression = progression;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        if (!recipes.isVoidTrowel(player.getInventory().getItemInOffHand())) {
            return;
        }

        ItemStack blocks = player.getInventory().getItemInMainHand();
        Material material = blocks.getType();
        if (!material.isBlock() || material.isAir() || blocks.getItemMeta() instanceof BlockStateMeta) {
            return;
        }

        Block standingOn = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!standingOn.getType().isSolid()) {
            return;
        }

        BlockFace facing = horizontalFacing(player.getFacing());
        Block target = standingOn.getRelative(facing);
        if (!target.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        target.setType(material, true);

        if (player.getGameMode() != GameMode.CREATIVE) {
            blocks.setAmount(blocks.getAmount() - 1);
        }

        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_PLACE, 0.5f, 1.25f);

        long count = store.incrementPlayer(player.getUniqueId(), "trowel-blocks", 1);
        long placed = store.incrementPlayer(player.getUniqueId(), "blocks-placed", 1);
        progression.incrementCommunity("blocks-placed", 1);
        progression.grant(player, "engineering/void_trowel");
        if (placed >= 100) {
            progression.grant(player, "engineering/place_100");
        }
        if (placed >= 1000) {
            progression.grant(player, "engineering/place_1000");
        }
        if (count >= 64) {
            progression.grant(player, "engineering/bridge_64");
        }
        if (count >= 500) {
            progression.grant(player, "engineering/bridge_500");
        }

        boolean noArmour = true;
        for (ItemStack armour : player.getInventory().getArmorContents()) {
            if (armour != null && !armour.getType().isAir()) {
                noArmour = false;
                break;
            }
        }
        if (noArmour) {
            progression.grant(player, "oddities/safety_third");
        }
        if (player.isSprinting()) {
            progression.grant(player, "engineering/speed_bridge");
        }
    }

    private BlockFace horizontalFacing(BlockFace facing) {
        if (Math.abs(facing.getModX()) >= Math.abs(facing.getModZ())) {
            return facing.getModX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return facing.getModZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }
}
