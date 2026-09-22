package io.github.breakerchap.skyblock.advancement;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.List;

public final class AdvancementManager {
    private static final String NAMESPACE = "skyblock";

    private final SkyblockPlugin plugin;
    private final List<AdvancementDefinition> definitions = List.of(
        new AdvancementDefinition("root", null, "Skybound", "Build a world out of almost nothing.", Material.GRASS_BLOCK, "task", false, 0),

        new AdvancementDefinition("getting_started/cobblestone", "root", "One Block at a Time", "Obtain cobblestone.", Material.COBBLESTONE, "task", false, 3),
        new AdvancementDefinition("getting_started/tree", "getting_started/cobblestone", "Arborist", "Grow a tree on the island.", Material.OAK_SAPLING, "task", false, 5),
        new AdvancementDefinition("getting_started/iron", "getting_started/cobblestone", "Industry Begins", "Obtain an iron ingot.", Material.IRON_INGOT, "goal", false, 8),
        new AdvancementDefinition("getting_started/lava", "getting_started/iron", "Hot Property", "Obtain a lava bucket.", Material.LAVA_BUCKET, "goal", false, 8),
        new AdvancementDefinition("civilisation/villager", "getting_started/iron", "Civilisation", "Breed two villagers.", Material.EMERALD, "goal", false, 12),

        new AdvancementDefinition("nether/root", "getting_started/lava", "Beyond the Void", "Enter the Nether.", Material.OBSIDIAN, "goal", false, 10),
        new AdvancementDefinition("nether/blaze", "nether/root", "Firepower", "Obtain a blaze rod.", Material.BLAZE_ROD, "task", false, 10),
        new AdvancementDefinition("nether/netherite", "nether/blaze", "Overengineered", "Obtain a netherite ingot.", Material.NETHERITE_INGOT, "challenge", false, 30),

        new AdvancementDefinition("end/root", "nether/blaze", "The Last Horizon", "Enter the End.", Material.END_STONE, "goal", false, 15),
        new AdvancementDefinition("end/dragon", "end/root", "Nothing Underneath", "Defeat the Ender Dragon.", Material.DRAGON_HEAD, "challenge", false, 40),
        new AdvancementDefinition("end/beacon", "end/dragon", "A Light in the Void", "Place a beacon.", Material.BEACON, "challenge", false, 30),

        new AdvancementDefinition("exploration/lush", "root", "A Speck of Green", "Find the Lush Outcrop.", Material.MOSS_BLOCK, "goal", true, 8),
        new AdvancementDefinition("exploration/dripstone", "root", "Stone Teeth", "Find the Dripstone Spire.", Material.POINTED_DRIPSTONE, "goal", true, 8),
        new AdvancementDefinition("exploration/moor", "root", "Mud in the Sky", "Find the Witch's Moor.", Material.MUD, "goal", true, 8),
        new AdvancementDefinition("exploration/portal", "root", "Who Built This?", "Find the Ruined Portal.", Material.CRYING_OBSIDIAN, "goal", true, 10),
        new AdvancementDefinition("exploration/monument", "root", "Sea Without an Ocean", "Find the Monument Shard.", Material.PRISMARINE, "goal", true, 12),

        new AdvancementDefinition("community/cobble", "root", "Stone by Stone", "As a server, mine 5,000 cobblestone.", Material.COBBLESTONE, "challenge", false, 20),
        new AdvancementDefinition("community/builder", "root", "Somewhere to Live", "As a server, place 3,000 blocks.", Material.BRICKS, "challenge", false, 20),
        new AdvancementDefinition("community/hunter", "root", "Night Shift", "As a server, kill 250 hostile mobs.", Material.IRON_SWORD, "challenge", false, 20),
        new AdvancementDefinition("community/life", "root", "It Takes a Village", "As a server, breed 50 creatures.", Material.WHEAT, "challenge", false, 20)
    );

    public AdvancementManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public void registerAll() {
        boolean loadedAny = false;
        for (AdvancementDefinition definition : definitions) {
            NamespacedKey key = key(definition.id());
            if (Bukkit.getAdvancement(key) != null) {
                continue;
            }

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
