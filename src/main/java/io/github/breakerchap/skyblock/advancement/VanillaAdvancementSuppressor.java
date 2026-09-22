package io.github.breakerchap.skyblock.advancement;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Iterator;

public final class VanillaAdvancementSuppressor implements Listener {
    private final SkyblockPlugin plugin;

    public VanillaAdvancementSuppressor(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaCriterion(PlayerAdvancementCriterionGrantEvent event) {
        if ("minecraft".equals(event.getAdvancement().getKey().getNamespace())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> revokeVanilla(event.getPlayer()));
    }

    public void revokeVanilla(Player player) {
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            if (!"minecraft".equals(advancement.getKey().getNamespace())) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criterion : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criterion);
            }
        }
    }
}
