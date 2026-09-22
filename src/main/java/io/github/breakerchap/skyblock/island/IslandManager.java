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
import org.bukkit.entity.Villager;
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
        new IslandDefinition("lush", "Lush Outcrop", 260, 90, 8, 18),
        new IslandDefinition("dripstone", "Dripstone Spire", -330, 160, 18, 18),
        new IslandDefinition("moor", "Witch's Moor", 110, -420, -4, 20),
        new IslandDefinition("portal", "Ruined Portal", 460, 260, 10, 24),
        new IslandDefinition("monument", "Monument Shard", -500, -280, -8, 22),
        new IslandDefinition("desert", "Desert Shrine", 330, -310, 4, 20),
        new IslandDefinition("frozen", "Frozen Observatory", -270, -520, 14, 20),
        new IslandDefinition("mushroom", "Mushroom Colony", 600, -120, 2, 22),
        new IslandDefinition("geode", "Amethyst Geode", -650, 170, -2, 20),
        new IslandDefinition("apiary", "Void Apiary", 180, 620, 6, 20),
        new IslandDefinition("end_shrine", "End Shrine", 760, 470, 12, 24),
        new IslandDefinition("village", "Little Village", -100, 390, 4, 26)
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
                case "desert" -> buildDesert(center);
                case "frozen" -> buildFrozen(center);
                case "mushroom" -> buildMushroom(center);
                case "geode" -> buildGeode(center);
                case "apiary" -> buildApiary(center);
                case "end_shrine" -> buildEndShrine(center);
                case "village" -> buildVillage(center);
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
            set(center, -1, baseY + y, 0, Material.OBSIDIAN);
            set(center, 2, baseY + y, 0, Material.OBSIDIAN);
        }
        set(center, 0, baseY, 0, Material.OBSIDIAN);
        set(center, 1, baseY, 0, Material.OBSIDIAN);
        // The two top blocks are intentionally missing; the chest contains exactly enough
        // obsidian to finish the frame. Crying obsidian is decorative, not structural.
        set(center, -4, 2, -2, Material.CRYING_OBSIDIAN);

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

    private void buildDesert(Location center) {
        ellipsoid(center, 9, 3, 8, Material.SANDSTONE);
        cap(center, 8, 7, Material.SAND);
        set(center, -3, 2, 1, Material.CACTUS);
        set(center, -3, 3, 1, Material.CACTUS);
        set(center, 3, 2, -2, Material.DEAD_BUSH);
        set(center, 0, 2, -3, Material.CUT_SANDSTONE);
        chest(center.clone().add(2, 2, 2),
            new ItemStack(Material.CACTUS, 2),
            new ItemStack(Material.SAND, 8),
            new ItemStack(Material.RED_SAND, 4),
            new ItemStack(Material.SUGAR_CANE, 2)
        );
    }

    private void buildFrozen(Location center) {
        ellipsoid(center, 8, 3, 8, Material.PACKED_ICE);
        cap(center, 7, 7, Material.SNOW_BLOCK);
        set(center, 0, 2, 0, Material.BLUE_ICE);
        set(center, 3, 2, 2, Material.SPRUCE_SAPLING);
        set(center, -3, 2, -2, Material.POWDER_SNOW);
        chest(center.clone().add(0, 2, 3),
            new ItemStack(Material.ICE, 8),
            new ItemStack(Material.PACKED_ICE, 4),
            new ItemStack(Material.SPRUCE_SAPLING, 2),
            new ItemStack(Material.SNOWBALL, 8)
        );
    }

    private void buildMushroom(Location center) {
        ellipsoid(center, 10, 3, 9, Material.DIRT);
        cap(center, 9, 8, Material.MYCELIUM);
        set(center, -3, 2, 0, Material.RED_MUSHROOM);
        set(center, 3, 2, 0, Material.BROWN_MUSHROOM);
        set(center, 0, 2, 3, Material.RED_MUSHROOM_BLOCK);
        chest(center.clone().add(0, 2, -3),
            new ItemStack(Material.RED_MUSHROOM, 4),
            new ItemStack(Material.BROWN_MUSHROOM, 4),
            new ItemStack(Material.MYCELIUM, 2),
            new ItemStack(Material.MUSHROOM_STEW, 2)
        );
    }

    private void buildGeode(Location center) {
        ellipsoid(center, 8, 6, 8, Material.SMOOTH_BASALT);
        ellipsoid(center, 6, 5, 6, Material.CALCITE);
        ellipsoid(center, 4, 4, 4, Material.AMETHYST_BLOCK);
        ellipsoid(center, 3, 3, 3, Material.AIR);
        set(center, 0, 0, -4, Material.BUDDING_AMETHYST);
        set(center, 2, 1, -3, Material.AMETHYST_CLUSTER);
        chest(center.clone().add(0, 0, 0),
            new ItemStack(Material.AMETHYST_SHARD, 8),
            new ItemStack(Material.CALCITE, 6),
            new ItemStack(Material.SPYGLASS, 1)
        );
    }

    private void buildApiary(Location center) {
        ellipsoid(center, 9, 3, 8, Material.DIRT);
        cap(center, 8, 7, Material.GRASS_BLOCK);
        set(center, -2, 2, 0, Material.BEEHIVE);
        set(center, 2, 2, 1, Material.DANDELION);
        set(center, 3, 2, -1, Material.POPPY);
        set(center, 0, 2, 3, Material.OXEYE_DAISY);
        chest(center.clone().add(0, 2, -2),
            new ItemStack(Material.HONEYCOMB, 6),
            new ItemStack(Material.HONEY_BOTTLE, 2),
            new ItemStack(Material.BEEHIVE, 1),
            new ItemStack(Material.FLOWERING_AZALEA, 1)
        );
    }

    private void buildEndShrine(Location center) {
        ellipsoid(center, 10, 3, 9, Material.END_STONE);
        cap(center, 8, 7, Material.END_STONE_BRICKS);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(center, x, 2, z, Material.END_PORTAL);
            }
        }

        for (int x = -2; x <= 2; x++) {
            set(center, x, 2, -2, Material.OBSIDIAN);
            set(center, x, 2, 2, Material.OBSIDIAN);
        }
        for (int z = -1; z <= 1; z++) {
            set(center, -2, 2, z, Material.OBSIDIAN);
            set(center, 2, 2, z, Material.OBSIDIAN);
        }

        chest(center.clone().add(0, 2, 5),
            new ItemStack(Material.ENDER_PEARL, 2),
            new ItemStack(Material.END_STONE, 4),
            new ItemStack(Material.CHORUS_FRUIT, 2)
        );
    }

    private void buildVillage(Location center) {
        ellipsoid(center, 12, 3, 10, Material.DIRT);
        cap(center, 11, 9, Material.GRASS_BLOCK);

        // Tiny two-house village with beds, farms and a bell.
        for (int x = -7; x <= -2; x++) {
            for (int z = -4; z <= 1; z++) {
                set(center, x, 2, z, Material.OAK_PLANKS);
            }
        }
        for (int x = 2; x <= 7; x++) {
            for (int z = -4; z <= 1; z++) {
                set(center, x, 2, z, Material.COBBLESTONE);
            }
        }

        for (int y = 3; y <= 5; y++) {
            for (int x = -7; x <= -2; x++) {
                set(center, x, y, -4, Material.OAK_LOG);
                set(center, x, y, 1, Material.OAK_LOG);
            }
            for (int z = -3; z <= 0; z++) {
                set(center, -7, y, z, Material.OAK_LOG);
                set(center, -2, y, z, Material.OAK_LOG);
            }

            for (int x = 2; x <= 7; x++) {
                set(center, x, y, -4, Material.STONE_BRICKS);
                set(center, x, y, 1, Material.STONE_BRICKS);
            }
            for (int z = -3; z <= 0; z++) {
                set(center, 2, y, z, Material.STONE_BRICKS);
                set(center, 7, y, z, Material.STONE_BRICKS);
            }
        }

        for (int x = -8; x <= -1; x++) {
            for (int z = -5; z <= 2; z++) {
                set(center, x, 6, z, Material.OAK_SLAB);
            }
        }
        for (int x = 1; x <= 8; x++) {
            for (int z = -5; z <= 2; z++) {
                set(center, x, 6, z, Material.STONE_BRICK_SLAB);
            }
        }

        set(center, -4, 3, 1, Material.OAK_FENCE_GATE);
        set(center, 4, 3, 1, Material.OAK_FENCE_GATE);
        set(center, -5, 3, -2, Material.RED_BED);
        set(center, 5, 3, -2, Material.BLUE_BED);
        set(center, 0, 2, 0, Material.BELL);

        for (int x = -4; x <= 4; x++) {
            set(center, x, 2, 5, Material.FARMLAND);
            set(center, x, 2, 6, Material.FARMLAND);
            set(center, x, 3, 5, Material.WHEAT);
            set(center, x, 3, 6, Material.CARROTS);
        }
        set(center, 0, 2, 7, Material.WATER);

        // Keep the original villagers from casually walking off the edge.
        for (int x = -9; x <= 9; x++) {
            for (int z = -7; z <= 7; z++) {
                double here = (x * x) / 121.0 + (z * z) / 81.0;
                if (here > 0.82 || here < 0.62) {
                    continue;
                }
                boolean edge = ((x + 1) * (x + 1)) / 121.0 + (z * z) / 81.0 > 0.82
                    || ((x - 1) * (x - 1)) / 121.0 + (z * z) / 81.0 > 0.82
                    || (x * x) / 121.0 + ((z + 1) * (z + 1)) / 81.0 > 0.82
                    || (x * x) / 121.0 + ((z - 1) * (z - 1)) / 81.0 > 0.82;
                if (edge) {
                    set(center, x, 2, z, Material.OAK_FENCE);
                }
            }
        }

        long villagers = center.getWorld().getNearbyEntities(
            center, 18, 12, 18, entity -> entity instanceof Villager
        ).size();
        if (villagers < 2) {
            Villager farmer = center.getWorld().spawn(center.clone().add(-3.5, 3, -1.5), Villager.class);
            farmer.setProfession(Villager.Profession.FARMER);
            farmer.setPersistent(true);
            villagers++;
        }
        if (villagers < 2) {
            Villager librarian = center.getWorld().spawn(center.clone().add(3.5, 3, -1.5), Villager.class);
            librarian.setProfession(Villager.Profession.LIBRARIAN);
            librarian.setPersistent(true);
        }

        chest(center.clone().add(0, 2, -7),
            new ItemStack(Material.BREAD, 6),
            new ItemStack(Material.EMERALD, 4),
            new ItemStack(Material.POTATO, 4),
            new ItemStack(Material.CARROT, 4)
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
