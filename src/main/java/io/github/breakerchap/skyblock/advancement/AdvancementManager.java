package io.github.breakerchap.skyblock.advancement;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.List;

public final class AdvancementManager {
    private static final String NAMESPACE = "skyblock";

    private final SkyblockPlugin plugin;
    private final List<AdvancementDefinition> definitions = AdvancementCatalog.definitions();

    public AdvancementManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public void registerAll() {
        for (int i = definitions.size() - 1; i >= 0; i--) {
            NamespacedKey key = key(definitions.get(i).id());
            if (Bukkit.getAdvancement(key) != null) {
                try {
                    Bukkit.getUnsafe().removeAdvancement(key);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Could not remove stale custom advancement " + key + ": " + ex.getMessage());
                }
            }
        }

        boolean loadedAny = false;
        for (AdvancementDefinition definition : definitions) {
            NamespacedKey key = key(definition.id());
            try {
                Advancement loaded = Bukkit.getUnsafe().loadAdvancement(key, definition.toJson());
                if (loaded == null) {
                    plugin.getLogger().warning("Minecraft rejected advancement " + key);
                } else {
                    loadedAny = true;
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("Failed to load advancement " + key + ": " + ex.getMessage());
            }
        }

        if (loadedAny) {
            Bukkit.updateResources();
        }
        plugin.getLogger().info("Loaded " + definitions.size() + " custom Skyblock advancements.");
    }

    public boolean grant(Player player, String id) {
        Advancement advancement = Bukkit.getAdvancement(key(id));
        if (advancement == null) {
            plugin.getLogger().warning("Cannot grant missing advancement skyblock:" + id);
            return false;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            return false;
        }

        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
        return progress.isDone();
    }

    public boolean has(Player player, String id) {
        Advancement advancement = Bukkit.getAdvancement(key(id));
        return advancement != null && player.getAdvancementProgress(advancement).isDone();
    }

    public int completedCount(Player player) {
        int count = 0;
        for (AdvancementDefinition definition : definitions) {
            if (has(player, definition.id())) {
                count++;
            }
        }
        return count;
    }

    public int totalCount() {
        return definitions.size();
    }

    public List<String> rootIds() {
        return definitions.stream()
            .filter(definition -> definition.parentId() == null)
            .map(AdvancementDefinition::id)
            .toList();
    }

    public boolean exists(String id) {
        return definitions.stream().anyMatch(definition -> definition.id().equals(id));
    }

    public List<String> ids() {
        return definitions.stream().map(AdvancementDefinition::id).toList();
    }

    public NamespacedKey key(String id) {
        return new NamespacedKey(NAMESPACE, id);
    }
}
