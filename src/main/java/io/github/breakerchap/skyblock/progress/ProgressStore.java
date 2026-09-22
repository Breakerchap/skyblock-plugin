package io.github.breakerchap.skyblock.progress;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class ProgressStore {
    private final SkyblockPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean dirty;

    public ProgressStore(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public long getCounter(String metric) {
        return data.getLong("community.counters." + metric, 0L);
    }

    public long increment(String metric, long amount) {
        long value = getCounter(metric) + amount;
        data.set("community.counters." + metric, value);
        dirty = true;
        return value;
    }

    public long getPlayerCounter(UUID uuid, String metric) {
        return data.getLong("players." + uuid + ".counters." + metric, 0L);
    }

    public long incrementPlayer(UUID uuid, String metric, long amount) {
        long value = getPlayerCounter(uuid, metric) + amount;
        data.set("players." + uuid + ".counters." + metric, value);
        dirty = true;
        return value;
    }

    public boolean isCommunityComplete(CommunityGoal goal) {
        return data.getBoolean("community.completed." + goal.id(), false);
    }

    public void markCommunityComplete(CommunityGoal goal) {
        data.set("community.completed." + goal.id(), true);
        dirty = true;
    }

    public boolean areIslandsGenerated() {
        return data.getBoolean("islands.generated", false);
    }

    public void markIslandsGenerated() {
        data.set("islands.generated", true);
        dirty = true;
    }

    public boolean isStarterIslandGenerated() {
        return data.getBoolean("world.starter-island-generated", false);
    }

    public void markStarterIslandGenerated() {
        data.set("world.starter-island-generated", true);
        dirty = true;
    }

    public long getTraderCooldownUntil(UUID uuid) {
        return data.getLong("trader.cooldowns." + uuid, 0L);
    }

    public void setTraderCooldownUntil(UUID uuid, long epochMillis) {
        data.set("trader.cooldowns." + uuid, epochMillis);
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data directory.");
                return;
            }
            data.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }
}
