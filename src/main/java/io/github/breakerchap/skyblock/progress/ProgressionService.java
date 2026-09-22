package io.github.breakerchap.skyblock.progress;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.advancement.AdvancementManager;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class ProgressionService {
    private static final List<String> EXPLORATION_IDS = List.of(
        "lush", "dripstone", "moor", "portal", "monument",
        "desert", "frozen", "mushroom", "geode", "apiary", "end_shrine", "village"
    );

    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final AdvancementManager advancements;
    private RecipeManager recipes;

    public ProgressionService(SkyblockPlugin plugin, ProgressStore store, AdvancementManager advancements) {
        this.plugin = plugin;
        this.store = store;
        this.advancements = advancements;
    }

    public void setRecipeManager(RecipeManager recipes) {
        this.recipes = recipes;
    }

    public boolean grant(Player player, String id) {
        boolean newlyGranted = advancements.grant(player, id);
        if (newlyGranted && recipes != null) {
            recipes.syncPlayer(player, true);
        }
        if (newlyGranted && id.startsWith("exploration/") && !id.equals("exploration/all") && !id.equals("exploration/root")) {
            boolean all = EXPLORATION_IDS.stream().allMatch(name -> advancements.has(player, "exploration/" + name));
            if (all) {
                advancements.grant(player, "exploration/all");
            }
        }
        return newlyGranted;
    }

    public boolean has(Player player, String id) {
        return advancements.has(player, id);
    }

    public void syncPlayer(Player player) {
        for (String root : advancements.rootIds()) {
            advancements.grant(player, root);
        }
        for (CommunityGoal goal : CommunityGoal.values()) {
            if (store.isCommunityComplete(goal)) {
                advancements.grant(player, goal.advancementId());
            }
        }
        if (recipes != null) {
            recipes.syncPlayer(player, false);
        }
    }

    public void incrementCommunity(String metric, long amount) {
        long value = store.increment(metric, amount);

        for (CommunityGoal goal : CommunityGoal.values()) {
            if (!goal.metric().equals(metric) || store.isCommunityComplete(goal)) {
                continue;
            }

            long threshold = threshold(goal);
            if (value < threshold) {
                continue;
            }

            store.markCommunityComplete(goal);
            store.save();

            Bukkit.getOnlinePlayers().forEach(player -> advancements.grant(player, goal.advancementId()));
            Bukkit.broadcast(
                Component.text("Community milestone complete: ", NamedTextColor.GOLD)
                    .append(Component.text(goal.displayName(), NamedTextColor.YELLOW))
            );

            if (recipes != null) {
                recipes.syncAll(true);
            }
        }
    }

    public long threshold(CommunityGoal goal) {
        return plugin.getConfig().getLong(
            "community." + goal.id() + ".threshold",
            goal.defaultThreshold()
        );
    }

    public long counter(CommunityGoal goal) {
        return store.getCounter(goal.metric());
    }

    public boolean isCommunityComplete(CommunityGoal goal) {
        return store.isCommunityComplete(goal);
    }

    public AdvancementManager advancements() {
        return advancements;
    }
}
