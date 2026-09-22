package io.github.breakerchap.skyblock.island;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Witch;
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
        new IslandDefinition("lush", "Lush Hollow", 260, 90, 8, 22),
        new IslandDefinition("dripstone", "Dripstone Cathedral", -330, 160, 18, 22),
        new IslandDefinition("moor", "Witch's Moor", 110, -420, -4, 22),
        new IslandDefinition("portal", "Ruined Portal", 460, 260, 10, 24),
        new IslandDefinition("monument", "Drowned Monument", -500, -280, -8, 24),
        new IslandDefinition("desert", "Desert Oasis", 330, -310, 4, 24),
        new IslandDefinition("frozen", "Frozen Observatory", -270, -520, 14, 24),
        new IslandDefinition("mushroom", "Mushroom Colony", 600, -120, 2, 24),
        new IslandDefinition("geode", "Broken Geode", -650, 170, -2, 22),
        new IslandDefinition("apiary", "Void Apiary", 180, 620, 6, 22),
        new IslandDefinition("end_shrine", "End Shrine", 760, 470, 12, 26),
        new IslandDefinition("village", "Little Village", -100, 390, 4, 28)
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
                "Use a fresh Skyblock world or run /skyblock islands generate force after checking the locations."
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
        plugin.getLogger().info("Generated " + islands.size() + " detailed progression structures in " + targetWorld().getName() + ".");
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
        int[][] samples = {{0, 0}, {14, 0}, {-14, 0}, {0, 14}, {0, -14}};
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
        ellipsoid(center, 12, 7, 10, Material.STONE);
        ellipsoid(center.clone().add(0, 1, 0), 9, 5, 7, Material.AIR);
        carveCrossEntrance(center, 10, 4);

        for (int x = -8; x <= 8; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = x * x / 64.0 + z * z / 36.0;
                if (d <= 1.0) {
                    set(center, x, -4, z, ((x + z) & 3) == 0 ? Material.CLAY : Material.MOSS_BLOCK);
                }
            }
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x * x + z * z <= 14) {
                    set(center, x, -3, z, Material.WATER);
                    set(center, x, -4, z, Material.CLAY);
                }
            }
        }

        set(center, -6, -3, 2, Material.ROOTED_DIRT);
        set(center, 6, -3, -1, Material.ROOTED_DIRT);
        set(center, -5, -2, -2, Material.FLOWERING_AZALEA);
        set(center, 5, -2, 2, Material.AZALEA);
        set(center, -3, -2, 5, Material.SMALL_DRIPLEAF);
        set(center, 3, -2, 5, Material.BIG_DRIPLEAF);
        set(center, 0, 5, 0, Material.SPORE_BLOSSOM);
        placeGlowVine(center, -5, 5, -2, 4);
        placeGlowVine(center, 4, 5, 1, 3);
        placeGlowVine(center, 1, 5, 5, 4);

        setBiomeCube(center, 13, 8, 11, Biome.LUSH_CAVES);

        spawnIfFewer(center, Axolotl.class, 2, 12, 8, 10,
            center.clone().add(-1.5, -2.7, 0.5));
        spawnIfFewer(center, Axolotl.class, 2, 12, 8, 10,
            center.clone().add(1.5, -2.7, -0.5));
    }

    private void buildDripstone(Location center) {
        ellipsoid(center, 10, 8, 9, Material.DEEPSLATE);
        ellipsoid(center.clone().add(0, 1, 0), 7, 6, 6, Material.AIR);
        carveCrossEntrance(center, 9, 4);

        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 5; z++) {
                if (x * x / 36.0 + z * z / 25.0 <= 1.0) {
                    set(center, x, -6, z, ((x - z) & 2) == 0 ? Material.TUFF : Material.DRIPSTONE_BLOCK);
                }
            }
        }

        int[][] spikes = {{0,0},{-4,2},{4,-2},{-2,-4},{3,4}};
        for (int[] p : spikes) {
            for (int y = -5; y <= -2; y++) set(center, p[0], y, p[1], Material.DRIPSTONE_BLOCK);
            set(center, p[0], -1, p[1], Material.POINTED_DRIPSTONE);
            for (int y = 6; y >= 3; y--) set(center, p[0] + 1, y, p[1] - 1, Material.DRIPSTONE_BLOCK);
            set(center, p[0] + 1, 2, p[1] - 1, Material.POINTED_DRIPSTONE);
        }

        set(center, -5, -5, -3, Material.WATER);
        set(center, -4, -5, -3, Material.WATER);
        set(center, 5, -5, 3, Material.LAVA);
        set(center, 4, -5, 3, Material.LAVA);
        set(center, -6, -3, 0, Material.RAW_COPPER_BLOCK);
        set(center, 6, -3, 0, Material.COPPER_ORE);
    }

    private void buildMoor(Location center) {
        ellipsoid(center, 11, 4, 10, Material.MUD);
        cap(center, 10, 9, Material.MUDDY_MANGROVE_ROOTS);

        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x * x + z * z <= 22) {
                    set(center, x, 2, z, Material.WATER);
                }
            }
        }
        set(center, -3, 3, 1, Material.LILY_PAD);
        set(center, 2, 3, -2, Material.LILY_PAD);
        set(center, 4, 3, 1, Material.LILY_PAD);
        set(center, -7, 2, 2, Material.RED_MUSHROOM);
        set(center, 7, 2, -1, Material.BROWN_MUSHROOM);
        buildMangrove(center.clone().add(-6, 2, -4));
        buildWitchHut(center.clone().add(5, 2, 4));

        spawnIfFewer(center, Frog.class, 2, 14, 8, 12, center.clone().add(1.5, 3, 0.5));
        spawnIfFewer(center, Witch.class, 1, 14, 8, 12, center.clone().add(5.5, 4, 4.5));
    }

    private void buildPortal(Location center) {
        ellipsoid(center, 11, 4, 9, Material.BLACKSTONE);
        cap(center, 10, 8, Material.NETHERRACK);
        for (int x = -7; x <= 7; x += 2) {
            set(center, x, 2, -5, Material.MAGMA_BLOCK);
        }
        set(center, -7, 2, 4, Material.CRYING_OBSIDIAN);
        set(center, 7, 2, 4, Material.CRYING_OBSIDIAN);
        set(center, -6, 2, -1, Material.GILDED_BLACKSTONE);
        set(center, 6, 2, 1, Material.GILDED_BLACKSTONE);

        // Complete, working 4x5 portal so Nether access does not depend on hidden chest loot.
        for (int x = -2; x <= 1; x++) {
            set(center, x, 2, 0, Material.OBSIDIAN);
            set(center, x, 6, 0, Material.OBSIDIAN);
        }
        for (int y = 3; y <= 5; y++) {
            set(center, -2, y, 0, Material.OBSIDIAN);
            set(center, 1, y, 0, Material.OBSIDIAN);
            set(center, -1, y, 0, Material.NETHER_PORTAL);
            set(center, 0, y, 0, Material.NETHER_PORTAL);
        }
    }

    private void buildMonument(Location center) {
        ellipsoid(center, 12, 4, 11, Material.PRISMARINE);
        cap(center, 11, 10, Material.PRISMARINE_BRICKS);

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x * x + z * z <= 34) set(center, x, 2, z, Material.WATER);
            }
        }
        for (int y = 2; y <= 8; y++) {
            set(center, -7, y, 0, Material.DARK_PRISMARINE);
            set(center, 7, y, 0, Material.DARK_PRISMARINE);
        }
        for (int x = -7; x <= 7; x++) set(center, x, 8, 0, Material.PRISMARINE_BRICKS);
        set(center, -7, 6, 0, Material.SEA_LANTERN);
        set(center, 7, 6, 0, Material.SEA_LANTERN);
        set(center, 0, 8, 0, Material.SEA_LANTERN);
        set(center, 0, 1, 0, Material.WET_SPONGE);
        set(center, 3, 1, 2, Material.SPONGE);
        set(center, -3, 1, -2, Material.SPONGE);

        spawnIfFewer(center, Guardian.class, 2, 15, 10, 14, center.clone().add(2.5, 3, 0.5));
        spawnIfFewer(center, Guardian.class, 2, 15, 10, 14, center.clone().add(-2.5, 3, 0.5));
    }

    private void buildDesert(Location center) {
        ellipsoid(center, 13, 4, 11, Material.SANDSTONE);
        cap(center, 12, 10, Material.SAND);
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x * x + z * z <= 20) {
                    set(center, x, 2, z, Material.WATER);
                    set(center, x, 1, z, Material.CLAY);
                }
            }
        }

        buildPalm(center.clone().add(-6, 2, -3));
        buildPalm(center.clone().add(6, 2, 4));
        for (int y = 2; y <= 4; y++) set(center, -10, y, 2, Material.CACTUS);
        for (int y = 2; y <= 3; y++) set(center, 10, y, -3, Material.CACTUS);
        set(center, -5, 2, 5, Material.DEAD_BUSH);
        set(center, 7, 2, -6, Material.DEAD_BUSH);
        set(center, -4, 3, 1, Material.SUGAR_CANE);
        set(center, -3, 3, 1, Material.SUGAR_CANE);
        set(center, 4, 3, -1, Material.SUGAR_CANE);
        set(center, 5, 3, -1, Material.SUGAR_CANE);
        buildSandstoneArch(center.clone().add(0, 2, 7));

        spawnIfFewer(center, Camel.class, 1, 16, 8, 14, center.clone().add(8.5, 3, 0.5));
    }

    private void buildFrozen(Location center) {
        ellipsoid(center, 11, 5, 10, Material.PACKED_ICE);
        cap(center, 10, 9, Material.SNOW_BLOCK);
        for (int y = 2; y <= 7; y++) set(center, 0, y, 0, Material.CALCITE);
        for (int x = -5; x <= 5; x++) {
            set(center, x, 2, -3, Material.CALCITE);
            if (Math.abs(x) >= 3) set(center, x, 3, -3, Material.CALCITE);
        }
        set(center, 0, 8, 0, Material.COPPER_BLOCK);
        set(center, 0, 9, 0, Material.LIGHTNING_ROD);
        set(center, 1, 8, 0, Material.LIGHTNING_ROD);
        set(center, 5, 2, 4, Material.BLUE_ICE);
        set(center, -5, 2, -4, Material.POWDER_SNOW);
        buildSpruce(center.clone().add(-6, 2, 3));

        spawnIfFewer(center, Goat.class, 2, 14, 10, 13, center.clone().add(5.5, 3, -2.5));
        spawnIfFewer(center, Goat.class, 2, 14, 10, 13, center.clone().add(-4.5, 3, 2.5));
    }

    private void buildMushroom(Location center) {
        ellipsoid(center, 12, 4, 11, Material.DIRT);
        cap(center, 11, 10, Material.MYCELIUM);
        giantMushroom(center.clone().add(-5, 2, -2), true, 6);
        giantMushroom(center.clone().add(5, 2, 2), false, 5);
        giantMushroom(center.clone().add(0, 2, 6), true, 4);
        set(center, -2, 2, 5, Material.RED_MUSHROOM);
        set(center, 3, 2, -5, Material.BROWN_MUSHROOM);

        spawnIfFewer(center, MushroomCow.class, 2, 15, 8, 14, center.clone().add(1.5, 3, 0.5));
        spawnIfFewer(center, MushroomCow.class, 2, 15, 8, 14, center.clone().add(-1.5, 3, -0.5));
    }

    private void buildGeode(Location center) {
        ellipsoid(center, 10, 8, 10, Material.SMOOTH_BASALT);
        ellipsoid(center, 8, 6, 8, Material.CALCITE);
        ellipsoid(center, 6, 5, 6, Material.AMETHYST_BLOCK);
        ellipsoid(center, 4, 4, 4, Material.AIR);
        for (int z = -10; z <= -4; z++) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) set(center, x, y, z, Material.AIR);
            }
        }
        int[][] buds = {{0,-5,0},{4,0,0},{-4,1,1},{1,3,3},{-2,-2,3}};
        for (int[] p : buds) set(center, p[0], p[1], p[2], Material.BUDDING_AMETHYST);
        set(center, 0, 2, -4, Material.AMETHYST_CLUSTER);
        set(center, 3, 0, -2, Material.LARGE_AMETHYST_BUD);
        set(center, -3, 1, 1, Material.MEDIUM_AMETHYST_BUD);
    }

    private void buildApiary(Location center) {
        ellipsoid(center, 11, 4, 10, Material.DIRT);
        cap(center, 10, 9, Material.GRASS_BLOCK);
        buildOak(center.clone().add(0, 2, 0), 6);
        set(center, 2, 5, 0, Material.BEE_NEST);
        set(center, -2, 5, 1, Material.BEEHIVE);
        set(center, 4, 2, 2, Material.HONEY_BLOCK);
        set(center, 5, 2, 2, Material.HONEYCOMB_BLOCK);

        Material[] flowers = {
            Material.DANDELION, Material.POPPY, Material.OXEYE_DAISY,
            Material.CORNFLOWER, Material.ALLIUM, Material.AZURE_BLUET
        };
        int i = 0;
        for (int x = -8; x <= 8; x += 2) {
            for (int z = -7; z <= 7; z += 2) {
                if (x * x / 81.0 + z * z / 64.0 < 0.8 && Math.abs(x) + Math.abs(z) > 4) {
                    set(center, x, 2, z, flowers[i++ % flowers.length]);
                }
            }
        }
        spawnIfFewer(center, Bee.class, 4, 14, 10, 13, center.clone().add(3.5, 4, 1.5));
        spawnIfFewer(center, Bee.class, 4, 14, 10, 13, center.clone().add(-3.5, 4, -1.5));
    }

    private void buildEndShrine(Location center) {
        ellipsoid(center, 12, 4, 11, Material.END_STONE);
        cap(center, 11, 10, Material.END_STONE_BRICKS);
        for (int x = -4; x <= 4; x++) {
            set(center, x, 2, -4, Material.PURPUR_BLOCK);
            set(center, x, 2, 4, Material.PURPUR_BLOCK);
        }
        for (int z = -3; z <= 3; z++) {
            set(center, -4, 2, z, Material.PURPUR_BLOCK);
            set(center, 4, 2, z, Material.PURPUR_BLOCK);
        }
        for (int y = 3; y <= 8; y++) {
            set(center, -6, y, -6, Material.OBSIDIAN);
            set(center, 6, y, -6, Material.OBSIDIAN);
            set(center, -6, y, 6, Material.OBSIDIAN);
            set(center, 6, y, 6, Material.OBSIDIAN);
        }
        set(center, -6, 9, -6, Material.END_ROD);
        set(center, 6, 9, -6, Material.END_ROD);
        set(center, -6, 9, 6, Material.END_ROD);
        set(center, 6, 9, 6, Material.END_ROD);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) set(center, x, 2, z, Material.END_PORTAL);
        }
        set(center, -8, 2, 0, Material.CHORUS_FLOWER);
        set(center, 8, 2, 0, Material.CHORUS_FLOWER);
    }

    private void buildVillage(Location center) {
        ellipsoid(center, 14, 4, 12, Material.DIRT);
        cap(center, 13, 11, Material.GRASS_BLOCK);
        buildVillageHouse(center.clone().add(-7, 2, -3), Material.OAK_PLANKS, Material.OAK_LOG, Material.OAK_SLAB);
        buildVillageHouse(center.clone().add(7, 2, -3), Material.COBBLESTONE, Material.STONE_BRICKS, Material.STONE_BRICK_SLAB);
        set(center, 0, 2, 0, Material.BELL);

        for (int x = -5; x <= 5; x++) {
            set(center, x, 2, 6, Material.FARMLAND);
            set(center, x, 2, 7, Material.FARMLAND);
            set(center, x, 3, 6, x % 2 == 0 ? Material.WHEAT : Material.CARROTS);
            set(center, x, 3, 7, x % 2 == 0 ? Material.POTATOES : Material.WHEAT);
        }
        set(center, 0, 2, 8, Material.WATER);
        set(center, -8, 2, 5, Material.HAY_BLOCK);
        set(center, -8, 3, 5, Material.HAY_BLOCK);
        set(center, 8, 2, 5, Material.COMPOSTER);

        for (int x = -11; x <= 11; x++) {
            set(center, x, 2, -9, Material.OAK_FENCE);
            set(center, x, 2, 9, Material.OAK_FENCE);
        }
        for (int z = -8; z <= 8; z++) {
            set(center, -12, 2, z, Material.OAK_FENCE);
            set(center, 12, 2, z, Material.OAK_FENCE);
        }

        long villagers = center.getWorld().getNearbyEntities(center, 20, 12, 20, entity -> entity instanceof Villager).size();
        if (villagers < 2) {
            Villager farmer = center.getWorld().spawn(center.clone().add(-3.5, 3, 1.5), Villager.class);
            farmer.setProfession(Villager.Profession.FARMER);
            farmer.setPersistent(true);
            villagers++;
        }
        if (villagers < 2) {
            Villager librarian = center.getWorld().spawn(center.clone().add(3.5, 3, 1.5), Villager.class);
            librarian.setProfession(Villager.Profession.LIBRARIAN);
            librarian.setPersistent(true);
        }

        villageChest(center.clone().add(0, 3, -7),
            new ItemStack(Material.BREAD, 6),
            new ItemStack(Material.EMERALD, 4),
            new ItemStack(Material.POTATO, 4),
            new ItemStack(Material.CARROT, 4),
            new ItemStack(Material.BEETROOT_SEEDS, 4)
        );
    }

    private void carveCrossEntrance(Location center, int radius, int halfWidth) {
        for (int i = radius - 4; i <= radius + 1; i++) {
            for (int w = -halfWidth / 2; w <= halfWidth / 2; w++) {
                for (int y = -1; y <= 4; y++) {
                    set(center, w, y, i, Material.AIR);
                    set(center, w, y, -i, Material.AIR);
                    set(center, i, y, w, Material.AIR);
                    set(center, -i, y, w, Material.AIR);
                }
            }
        }
    }

    private void placeGlowVine(Location center, int x, int y, int z, int length) {
        for (int i = 0; i < length - 1; i++) set(center, x, y - i, z, Material.CAVE_VINES_PLANT);
        Block tip = block(center, x, y - length + 1, z);
        tip.setBlockData(Bukkit.createBlockData("minecraft:cave_vines[berries=true]"), false);
    }

    private void setBiomeCube(Location center, int rx, int ry, int rz, Biome biome) {
        World world = center.getWorld();
        for (int x = -rx; x <= rx; x += 4) {
            for (int y = -ry; y <= ry; y += 4) {
                for (int z = -rz; z <= rz; z += 4) {
                    world.setBiome(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z, biome);
                }
            }
        }
    }

    private void buildPalm(Location base) {
        for (int y = 0; y < 6; y++) base.clone().add(0, y, 0).getBlock().setType(Material.JUNGLE_LOG, false);
        for (int x = -3; x <= 3; x++) base.clone().add(x, 5, 0).getBlock().setType(Material.JUNGLE_LEAVES, false);
        for (int z = -3; z <= 3; z++) base.clone().add(0, 5, z).getBlock().setType(Material.JUNGLE_LEAVES, false);
        base.clone().add(0, 6, 0).getBlock().setType(Material.JUNGLE_LEAVES, false);
    }

    private void buildMangrove(Location base) {
        for (int y = 0; y < 5; y++) base.clone().add(0, y, 0).getBlock().setType(Material.MANGROVE_LOG, false);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x * x + z * z <= 6) base.clone().add(x, 5, z).getBlock().setType(Material.MANGROVE_LEAVES, false);
            }
        }
        base.clone().add(-1, 0, 0).getBlock().setType(Material.MANGROVE_ROOTS, false);
        base.clone().add(1, 0, 0).getBlock().setType(Material.MANGROVE_ROOTS, false);
    }

    private void buildWitchHut(Location base) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) base.clone().add(x, 2, z).getBlock().setType(Material.SPRUCE_PLANKS, false);
        }
        for (int[] p : new int[][]{{-2,-2},{-2,2},{2,-2},{2,2}}) {
            for (int y = 0; y <= 4; y++) base.clone().add(p[0], y, p[1]).getBlock().setType(Material.SPRUCE_LOG, false);
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) base.clone().add(x, 5, z).getBlock().setType(Material.DARK_OAK_SLAB, false);
        }
        base.clone().add(0, 3, 0).getBlock().setType(Material.CAULDRON, false);
    }

    private void buildSandstoneArch(Location base) {
        for (int y = 0; y <= 5; y++) {
            base.clone().add(-3, y, 0).getBlock().setType(Material.CUT_SANDSTONE, false);
            base.clone().add(3, y, 0).getBlock().setType(Material.CUT_SANDSTONE, false);
        }
        for (int x = -3; x <= 3; x++) base.clone().add(x, 5, 0).getBlock().setType(Material.CHISELED_SANDSTONE, false);
    }

    private void giantMushroom(Location base, boolean red, int height) {
        for (int y = 0; y < height; y++) base.clone().add(0, y, 0).getBlock().setType(Material.MUSHROOM_STEM, false);
        Material cap = red ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK;
        int radius = red ? 3 : 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius + 2) base.clone().add(x, height, z).getBlock().setType(cap, false);
            }
        }
    }

    private void buildSpruce(Location base) {
        for (int y = 0; y < 6; y++) base.clone().add(0, y, 0).getBlock().setType(Material.SPRUCE_LOG, false);
        for (int y = 3; y <= 6; y++) {
            int r = Math.max(1, 4 - (y - 3));
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) + Math.abs(z) <= r + 1) base.clone().add(x, y, z).getBlock().setType(Material.SPRUCE_LEAVES, false);
                }
            }
        }
    }

    private void buildOak(Location base, int height) {
        for (int y = 0; y < height; y++) base.clone().add(0, y, 0).getBlock().setType(Material.OAK_LOG, false);
        for (int y = height - 2; y <= height; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x + z * z <= 6) base.clone().add(x, y, z).getBlock().setType(Material.OAK_LEAVES, false);
                }
            }
        }
    }

    private void buildVillageHouse(Location base, Material floor, Material wall, Material roof) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) base.clone().add(x, 0, z).getBlock().setType(floor, false);
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = -3; x <= 3; x++) {
                base.clone().add(x, y, -3).getBlock().setType(wall, false);
                base.clone().add(x, y, 3).getBlock().setType(wall, false);
            }
            for (int z = -2; z <= 2; z++) {
                base.clone().add(-3, y, z).getBlock().setType(wall, false);
                base.clone().add(3, y, z).getBlock().setType(wall, false);
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) base.clone().add(x, 5, z).getBlock().setType(roof, false);
        }
        base.clone().add(0, 1, 3).getBlock().setType(Material.AIR, false);
        base.clone().add(0, 2, 3).getBlock().setType(Material.AIR, false);
        base.clone().add(-1, 1, 0).getBlock().setType(Material.RED_BED, false);
        base.clone().add(1, 1, 0).getBlock().setType(Material.YELLOW_BED, false);
    }

    private <T extends org.bukkit.entity.LivingEntity> void spawnIfFewer(
        Location center, Class<T> type, int target, double rx, double ry, double rz, Location spawn
    ) {
        long count = center.getWorld().getNearbyEntities(center, rx, ry, rz, type::isInstance).size();
        if (count < target) {
            T entity = center.getWorld().spawn(spawn, type);
            entity.setPersistent(true);
        }
    }

    private void ellipsoid(Location center, int radiusX, int radiusY, int radiusZ, Material material) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double value =
                        (x * x) / (double) (radiusX * radiusX)
                            + (y * y) / (double) (radiusY * radiusY)
                            + (z * z) / (double) (radiusZ * radiusZ);
                    if (value <= 1.0) set(center, x, y, z, material);
                }
            }
        }
    }

    private void cap(Location center, int radiusX, int radiusZ, Material material) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double value = (x * x) / (double) (radiusX * radiusX) + (z * z) / (double) (radiusZ * radiusZ);
                if (value <= 1.0) set(center, x, 1, z, material);
            }
        }
    }

    private Block block(Location center, int dx, int dy, int dz) {
        return center.getWorld().getBlockAt(
            center.getBlockX() + dx,
            center.getBlockY() + dy,
            center.getBlockZ() + dz
        );
    }

    private void set(Location center, int dx, int dy, int dz, Material material) {
        block(center, dx, dy, dz).setType(material, false);
    }

    private void villageChest(Location location, ItemStack... items) {
        Block block = location.getBlock();
        block.setType(Material.CHEST, false);
        if (block.getState() instanceof Chest chest) {
            chest.getBlockInventory().clear();
            chest.getBlockInventory().addItem(items);
            chest.update(true, false);
        }
    }
}
