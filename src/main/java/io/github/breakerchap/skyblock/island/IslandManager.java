package io.github.breakerchap.skyblock.island;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class IslandManager implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final ProgressionService progression;

    private final List<IslandDefinition> islands = List.of(
        new IslandDefinition("lush", "Lush Outcrop", 650, 150, 8, 18),
        new IslandDefinition("dripstone", "Dripstone Spire", -850, 300, 18, 18),
        new IslandDefinition("moor", "Witch's Moor", 150, -1100, -4, 20),
        new IslandDefinition("portal", "Ruined Portal", 1400, 700, 10, 24),
        new IslandDefinition("monument", "Monument Shard", -1500, -850, -8, 22)
    );

    public IslandManager(SkyblockPlugin plugin, ProgressStore store, ProgressionService progression) {
        this.plugin = plugin;
        this.store = store;
        this.progression = progression;
    }

    public List<IslandDefinition> definitions() {
        return islands;
    }

    public World targetWorld() {
        String configured = plugin.getConfig().getString("world", "").trim();
        if (!configured.isEmpty()) {
            World world = plugin.getServer().getWorld(configured);
            if (world != null) {
                return world;
            }
            plugin.getLogger().warning("Configured Skyblock world '" + configured + "' is not loaded.");
        }

        return plugin.getServer().getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
            .findFirst()
            .orElse(plugin.getServer().getWorlds().getFirst());
    }

    public Location location(IslandDefinition definition) {
        World world = targetWorld();
        Location spawn = world.getSpawnLocation();
        int y = plugin.getConfig().contains("islands.base-y")
            ? plugin.getConfig().getInt("islands.base-y")
            : spawn.getBlockY();
        return new Location(
            world,
            spawn.getBlockX() + definition.offsetX(),
            y + definition.yOffset(),
            spawn.getBlockZ() + definition.offsetZ()
        );
    }

    public int generateAll(boolean force) {
        if (store.areIslandsGenerated() && !force) {
            return 0;
        }

        if (!force && !allTargetsLookVoid()) {
            plugin.getLogger().warning(
                "Exploration islands were NOT generated because one or more target areas contain terrain. " +
                "Set the correct 'world' in config.yml, or run /skyblock islands generate force after checking the locations."
            );
            return 0;
        }

        for (IslandDefinition definition : islands) {
            Location center = location(definition);
            switch (definition.id()) {
                case "lush" -> buildLush(center);
                case "dripstone" -> buildDripstone(center);
                case "moor" -> buildMoor(center);
                case "portal" -> buildPortal(center);
                case "monument" -> buildMonument(center);
                default -> throw new IllegalStateException("Unknown island " + definition.id());
            }
        }

        store.markIslandsGenerated();
        store.save();
        plugin.getLogger().info("Generated " + islands.size() + " progression islands in " + targetWorld().getName() + ".");
        return islands.size();
    }

    private boolean allTargetsLookVoid() {
        for (IslandDefinition definition : islands) {
            if (!looksVoid(location(definition))) {
                return false;
            }
        }
        return true;
    }

    private boolean looksVoid(Location center) {
        World world = center.getWorld();
        int[][] samples = {{0, 0}, {12, 0}, {-12, 0}, {0, 12}, {0, -12}};
        for (int[] sample : samples) {
            int highest = world.getHighestBlockYAt(
                center.getBlockX() + sample[0],
                center.getBlockZ() + sample[1],
                HeightMap.MOTION_BLOCKING_NO_LEAVES
            );
            if (highest > world.getMinHeight() + 1) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null
            || (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.getWorld().equals(targetWorld())) {
            return;
        }

        for (IslandDefinition definition : islands) {
            Location centre = location(definition);
            if (player.getLocation().distanceSquared(centre) <= definition.discoveryRadius() * definition.discoveryRadius()) {
                if (progression.grant(player, "exploration/" + definition.id())) {
                    player.sendMessage(
                        Component.text("Discovered: ", NamedTextColor.AQUA)
                            .append(Component.text(definition.displayName(), NamedTextColor.WHITE))
                    );
                }
            }
        }
    }

    private void buildLush(Location center) {
        ellipsoid(center, 8, 3, 7, Material.STONE);
        cap(center, 7, 6, Material.MOSS_BLOCK);
        set(center, 0, 2, 0, Material.FLOWERING_AZALEA);
        set(center, 3, 2, -2, Material.AZALEA);
        set(center, -3, 2, 2, Material.MOSS_CARPET);
        set(center, 1, 2, 3, Material.MOSS_CARPET);
        chest(center.clone().add(0, 2, -2),
            new ItemStack(Material.GLOW_BERRIES, 6),
            new ItemStack(Material.ROOTED_DIRT, 4),
            new ItemStack(Material.SMALL_DRIPLEAF, 2),
            new ItemStack(Material.SPORE_BLOSSOM, 1)
        );
    }

    private void buildDripstone(Location center) {
        ellipsoid(center, 6, 4, 6, Material.DEEPSLATE);
        ellipsoid(center.clone().add(0, 3, 0), 3, 6, 3, Material.DRIPSTONE_BLOCK);
        set(center, 0, 10, 0, Material.DRIPSTONE_BLOCK);
        chest(center.clone().add(2, 5, 0),
            new ItemStack(Material.POINTED_DRIPSTONE, 8),
            new ItemStack(Material.DRIPSTONE_BLOCK, 4),
            new ItemStack(Material.COPPER_INGOT, 3)
        );
    }

    private void buildMoor(Location center) {
        ellipsoid(center, 9, 3, 8, Material.MUD);
        cap(center, 8, 7, Material.MUDDY_MANGROVE_ROOTS);
        set(center, -2, 2, 1, Material.CAULDRON);
        set(center, 3, 2, -2, Material.MANGROVE_ROOTS);
        chest(center.clone().add(0, 2, 2),
            new ItemStack(Material.MANGROVE_PROPAGULE, 2),
            new ItemStack(Material.LILY_PAD, 3),
            new ItemStack(Material.SLIME_BALL, 2),
            new ItemStack(Material.BROWN_MUSHROOM, 2),
            new ItemStack(Material.RED_MUSHROOM, 2)
        );
    }

    private void buildPortal(Location center) {
        ellipsoid(center, 9, 3, 7, Material.NETHERRACK);
        cap(center, 8, 6, Material.SOUL_SOIL);

        int baseY = 2;
        for (int y = 1; y <= 3; y++) {
            set(center, -1, baseY + y, 0, y == 2 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
            set(center, 2, baseY + y, 0, Material.OBSIDIAN);
        }
        set(center, 0, baseY, 0, Material.OBSIDIAN);
        set(center, 1, baseY, 0, Material.OBSIDIAN);

        chest(center.clone().add(4, 2, 1),
            new ItemStack(Material.OBSIDIAN, 2),
            new ItemStack(Material.FIRE_CHARGE, 2),
            new ItemStack(Material.GOLDEN_CARROT, 2)
        );
    }

    private void buildMonument(Location center) {
        ellipsoid(center, 9, 3, 9, Material.PRISMARINE);
        ellipsoid(center.clone().add(0, 2, 0), 5, 2, 5, Material.PRISMARINE_BRICKS);
        set(center, 4, 4, 0, Material.SEA_LANTERN);
        set(center, -4, 4, 0, Material.SEA_LANTERN);
        set(center, 0, 4, 4, Material.SEA_LANTERN);
        set(center, 0, 4, -4, Material.SEA_LANTERN);
        chest(center.clone().add(0, 5, 0),
            new ItemStack(Material.SPONGE, 1),
            new ItemStack(Material.PRISMARINE_CRYSTALS, 4),
            new ItemStack(Material.SEA_PICKLE, 2)
        );
    }

    private void ellipsoid(Location center, int radiusX, int radiusY, int radiusZ, Material material) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double value =
                        (x * x) / (double) (radiusX * radiusX)
                            + (y * y) / (double) (radiusY * radiusY)
                            + (z * z) / (double) (radiusZ * radiusZ);
                    if (value <= 1.0) {
                        set(center, x, y, z, material);
                    }
                }
            }
        }
    }

    private void cap(Location center, int radiusX, int radiusZ, Material material) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double value = (x * x) / (double) (radiusX * radiusX) + (z * z) / (double) (radiusZ * radiusZ);
                if (value <= 1.0) {
                    set(center, x, 1, z, material);
                }
            }
        }
    }

    private void set(Location center, int dx, int dy, int dz, Material material) {
        center.getWorld().getBlockAt(
            center.getBlockX() + dx,
            center.getBlockY() + dy,
            center.getBlockZ() + dz
        ).setType(material, false);
    }

    private void chest(Location location, ItemStack... items) {
        Block block = location.getBlock();
        block.setType(Material.CHEST, false);
        if (block.getState() instanceof Chest chest) {
            chest.getBlockInventory().clear();
            chest.getBlockInventory().addItem(items);
            chest.update(true, false);
        }
    }
}
