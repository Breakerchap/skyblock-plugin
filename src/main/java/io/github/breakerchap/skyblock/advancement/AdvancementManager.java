package io.github.breakerchap.skyblock.advancement;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        validateCatalog();

        List<NamespacedKey> existing = new ArrayList<>();
        Bukkit.advancementIterator().forEachRemaining(advancement -> {
            if (NAMESPACE.equals(advancement.getKey().getNamespace())) {
                existing.add(advancement.getKey());
            }
        });

        /*
         * loadAdvancement(key, json) persists its JSON into Paper's Bukkit datapack.
         * On the next boot Paper loads those files before this plugin enables. The old
         * code deleted the files with removeAdvancement(), but immediately tried to
         * register the still-live in-memory advancements again. Paper correctly
         * rejected every duplicate as "already exists".
         *
         * On a normal server start nobody is online yet, so migrate any old persisted
         * copies away and reload Minecraft data to actually remove them from memory.
         * If this plugin is hot-reloaded while players are online, keep the live
         * advancements for that session rather than risk disturbing player progress;
         * the next full restart will perform the migration safely.
         */
        if (!existing.isEmpty() && Bukkit.getOnlinePlayers().isEmpty()) {
            int removedPersistent = 0;
            for (NamespacedKey key : existing) {
                try {
                    if (Bukkit.getUnsafe().removeAdvancement(key)) {
                        removedPersistent++;
                    }
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning(
                        "Could not remove persisted custom advancement " + key + ": " + ex.getMessage()
                    );
                }
            }

            try {
                Bukkit.reloadData();
                plugin.getLogger().info(
                    "Refreshed Skyblock advancement data"
                        + (removedPersistent > 0 ? " and removed " + removedPersistent + " old persisted definition(s)." : ".")
                );
            } catch (RuntimeException ex) {
                plugin.getLogger().severe(
                    "Could not reload Minecraft data while refreshing Skyblock advancements: " + ex.getMessage()
                );
            }
        } else if (!existing.isEmpty()) {
            plugin.getLogger().warning(
                "Skyblock advancements are already loaded while players are online; "
                    + "reusing them for this session. Restart the server to refresh their definitions safely."
            );
        }

        int registered = 0;
        int reused = 0;
        for (AdvancementDefinition definition : definitions) {
            NamespacedKey key = key(definition.id());

            if (Bukkit.getAdvancement(key) != null) {
                reused++;
                continue;
            }

            try {
                // Do not persist these into Paper's Bukkit datapack. The plugin is the
                // source of truth and registers the current catalogue on every boot.
                Advancement loaded = Bukkit.getUnsafe().loadAdvancement(key, definition.toJson(), false);
                if (loaded == null) {
                    plugin.getLogger().warning("Minecraft rejected advancement " + key);
                } else {
                    registered++;
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("Failed to load advancement " + key + ": " + ex.getMessage());
            }
        }

        if (registered > 0) {
            Bukkit.updateResources();
        }

        List<String> missing = definitions.stream()
            .map(AdvancementDefinition::id)
            .filter(id -> Bukkit.getAdvancement(key(id)) == null)
            .toList();

        if (missing.isEmpty()) {
            plugin.getLogger().info(
                "Skyblock advancements ready: " + definitions.size()
                    + " total (" + registered + " registered, " + reused + " already present)."
            );
        } else {
            int shown = Math.min(10, missing.size());
            String suffix = missing.size() > shown ? " (and " + (missing.size() - shown) + " more)" : "";
            plugin.getLogger().severe(
                "Only " + (definitions.size() - missing.size()) + "/" + definitions.size()
                    + " Skyblock advancements are available. Missing: "
                    + String.join(", ", missing.subList(0, shown)) + suffix
            );
        }
    }

    private void validateCatalog() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (AdvancementDefinition definition : definitions) {
            if (!ids.add(definition.id())) {
                throw new IllegalStateException("Duplicate Skyblock advancement id: " + definition.id());
            }
        }

        for (AdvancementDefinition definition : definitions) {
            if (definition.parentId() != null && !ids.contains(definition.parentId())) {
                throw new IllegalStateException(
                    "Skyblock advancement " + definition.id()
                        + " references missing parent " + definition.parentId()
                );
            }
        }
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
