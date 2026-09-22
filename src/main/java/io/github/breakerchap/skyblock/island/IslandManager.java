package io.github.breakerchap.skyblock.island;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
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
import org.bukkit.structure.Structure;

import java.util.List;
import java.util.Map;
import java.util.Random;

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
        Map<Character, Material> rock = Map.of(
            '#', Material.STONE,
            'd', Material.DEEPSLATE,
            'm', Material.MOSS_BLOCK,
            'r', Material.ROOTED_DIRT,
            'c', Material.CLAY,
            'w', Material.WATER
        );

        // Hand-authored cliff/cave slices. The south face is deliberately torn open so
        // the cave reads clearly from the bridge instead of looking like a floating ball.
        paintLayer(center, -7, rock,
            "       d####d       ",
            "    dd########d    ",
            "  dd############d  ",
            " d###############d ",
            "d#################d",
            "d#################d",
            " d###############d ",
            "  d#############d  ",
            "    d#########d    ",
            "       d###d        ");
        paintLayer(center, -6, rock,
            "     d########d     ",
            "   d############d   ",
            " d################d ",
            "d##################d",
            "d##################d",
            "d##################d",
            " d################d ",
            "  d##############d  ",
            "    d##########d    ",
            "       d####d       ");
        paintLayer(center, -5, rock,
            "    ############    ",
            "  ################  ",
            " ################## ",
            "####mmmmmmmmmmmm####",
            "###mmmmmmmmmmmmmm###",
            "###mmmmccccmmmmmm###",
            "####mmmmmmmmmmmm####",
            " ####mmmmmmmmmm#### ",
            "   ##############   ",
            "      ########       ");
        paintLayer(center, -4, rock,
            "    ############    ",
            "  ###mmmmmmmmmm###  ",
            " ##mmmmmmmmmmmmmm## ",
            "##mmmmccccccmmmmmm##",
            "##mmmccwwwwccmmmm###",
            "##mmmccwwwwccmmmm###",
            "##mmmmccccccmmmmmm##",
            " ##mmmmmmmmmmmmmm## ",
            "   ####mmmmmm#####  ",
            "      ########       ");
        paintLayer(center, -3, rock,
            "       #####         ",
            "   mmmmmmmmmmmmm     ",
            "  mmmmmmmmmmmmmmmm   ",
            " mmmmmccwwwwccmmmmm  ",
            " mmmmccwwwwwwccmmmm  ",
            " mmmmccwwwwwwccmmmm  ",
            " mmmmmccwwwwccmmmmm  ",
            "  mmmmmmmmmmmmmmmm   ",
            "    mmmmmmmmmmmm     ",
            "       mmmmm         ");

        // Cave walls/roof, built as authored ribs rather than a generated shell.
        int[][] ribs = {
            {-9,-2,-3},{-9,-1,-3},{-9,0,-2},{-8,1,-2},{-8,2,-1},{-7,3,-1},
            {9,-2,-3},{9,-1,-3},{9,0,-2},{8,1,-2},{8,2,-1},{7,3,-1},
            {-7,4,0},{-6,5,0},{-4,6,0},{-2,6,0},{0,7,0},{2,6,0},{4,6,0},{6,5,0},{7,4,0}
        };
        for (int[] p : ribs) {
            set(center, p[0], p[1], p[2], p[1] >= 4 ? Material.MOSS_BLOCK : Material.STONE);
        }
        // Broken roof shelves extend backwards into the cave.
        for (int z = -7; z <= 0; z++) {
            set(center, -7, 3, z, Material.STONE);
            set(center, -6, 4, z, Material.MOSS_BLOCK);
            set(center, 7, 3, z, Material.STONE);
            set(center, 6, 4, z, Material.MOSS_BLOCK);
        }
        for (int z = -6; z <= -1; z++) {
            set(center, -3, 6, z, Material.STONE);
            set(center, 0, 7, z, Material.MOSS_BLOCK);
            set(center, 3, 6, z, Material.STONE);
        }

        // Pond detail.
        set(center, -3, -2, 0, Material.SMALL_DRIPLEAF);
        set(center, 3, -2, 1, Material.BIG_DRIPLEAF);
        set(center, 5, -2, -4, Material.AZALEA);
        set(center, -5, -2, -5, Material.FLOWERING_AZALEA);
        set(center, -6, -2, 2, Material.MOSS_CARPET);
        set(center, 6, -2, 2, Material.MOSS_CARPET);
        set(center, -2, 5, -3, Material.SPORE_BLOSSOM);
        set(center, 3, 5, -5, Material.SPORE_BLOSSOM);
        placeGlowVine(center, -5, 5, -5, 4);
        placeGlowVine(center, 1, 6, -6, 5);
        placeGlowVine(center, 5, 4, -3, 3);

        // Hanging roots and little stone teeth make the silhouette less clean/geometric.
        for (int[] p : new int[][]{{-8,-4,5},{-6,-5,6},{6,-5,5},{8,-4,4},{-3,-6,7},{4,-6,7}}) {
            set(center, p[0], p[1], p[2], Material.POINTED_DRIPSTONE);
        }
        setBiomeCube(center, 14, 10, 12, Biome.LUSH_CAVES);

        spawnIfFewer(center, Axolotl.class, 2, 14, 10, 12, center.clone().add(-1.5, -2.6, 0.5));
        spawnIfFewer(center, Axolotl.class, 2, 14, 10, 12, center.clone().add(1.5, -2.6, -0.5));
    }

    private void buildDripstone(Location center) {
        Map<Character, Material> p = Map.of(
            'd', Material.DEEPSLATE,
            't', Material.TUFF,
            'r', Material.DRIPSTONE_BLOCK,
            'c', Material.COPPER_ORE,
            'w', Material.WATER,
            'l', Material.LAVA
        );

        paintLayer(center, -8, p,
            "       ddddd       ",
            "    ddddddddddd    ",
            "  ddddddddddddddd  ",
            " ddddddddddddddddd ",
            "ddddddddddddddddddd",
            " ddddddddddddddddd ",
            "  ddddddddddddddd  ",
            "    ddddddddddd    ",
            "       ddddd       ");
        paintLayer(center, -7, p,
            "     ddddddddd     ",
            "   ddddttttddddd   ",
            " ddddttttttttddddd ",
            "ddddttttttttttddddd",
            "ddddttttttttttddddd",
            " ddddttttttttdddd  ",
            "  dddddttttdddddd  ",
            "    ddddddddddd    ");
        paintLayer(center, -6, p,
            "    ddddttttdddd    ",
            "  dddtttttttttdddd  ",
            " dddtttttttttttdddd ",
            "dddtttrrrrrrttttdddd",
            "dddtttrrrrrrttttdddd",
            " dddtttttttttttdddd ",
            "  ddddttttttttdddd  ",
            "    dddddddddddd    ");
        paintLayer(center, -5, p,
            "     ttttttttt      ",
            "   ttttttttttttt    ",
            " tttttrrrrrrrttttt  ",
            "ttttrrrrrrrrrrrtttt ",
            "ttttrrrrwwrrrrrtttt ",
            " ttttrrrwwrrrrtttt  ",
            "  ttttttrrrrrtttt   ",
            "    ttttttttttt     ");

        // Broken cave sidewalls form a nave-like space rather than a closed blob.
        int[][] leftWall = {
            {-9,-4,-5},{-9,-3,-5},{-9,-2,-5},{-9,-1,-5},{-9,0,-5},
            {-8,1,-5},{-8,2,-5},{-7,3,-5},{-7,4,-5},
            {-9,-4,-2},{-9,-3,-2},{-9,-2,-2},{-8,-1,-2},{-8,0,-2},{-7,1,-2},{-7,2,-2}
        };
        for (int[] q:leftWall) set(center,q[0],q[1],q[2],(q[1]&1)==0?Material.DEEPSLATE:Material.TUFF);
        for (int[] q:leftWall) set(center,-q[0],q[1],q[2],(q[1]&1)==0?Material.TUFF:Material.DEEPSLATE);

        // Ceiling ribs.
        for (int z=-7;z<=2;z++) {
            set(center,-5,5,z,Material.DEEPSLATE);
            set(center,5,5,z,Material.DEEPSLATE);
            if ((z&1)==0) {
                set(center,-4,6,z,Material.TUFF);
                set(center,4,6,z,Material.TUFF);
            }
        }
        for (int z=-6;z<=0;z+=2) {
            set(center,0,7,z,Material.DRIPSTONE_BLOCK);
            setPointedDripstone(center,0,6,z,false);
        }

        // Stalagmite clusters.
        int[][] bases={{-5,-4,-1},{-2,-4,-5},{3,-4,-4},{6,-4,0},{1,-4,2}};
        int[] heights={4,3,5,3,2};
        for(int i=0;i<bases.length;i++){
            int[] q=bases[i];
            for(int y=0;y<heights[i];y++) set(center,q[0],q[1]+y,q[2],Material.DRIPSTONE_BLOCK);
            setPointedDripstone(center,q[0],q[1]+heights[i],q[2],true);
        }

        // A water drip pool and dangerous lava pocket.
        set(center,-6,-4,3,Material.WATER);
        set(center,-5,-4,3,Material.WATER);
        set(center,-6,-4,4,Material.WATER);
        set(center,6,-4,3,Material.LAVA);
        set(center,5,-4,3,Material.LAVA);
        set(center,-7,-2,-5,Material.COPPER_ORE);
        set(center,7,-2,-4,Material.RAW_COPPER_BLOCK);
        set(center,6,-1,-5,Material.COPPER_ORE);
    }

    private void buildMoor(Location center) {
        Map<Character, Material> p=Map.of(
            'm',Material.MUD,
            'r',Material.MUDDY_MANGROVE_ROOTS,
            'd',Material.DIRT,
            'w',Material.WATER,
            'c',Material.CLAY
        );

        paintLayer(center,-5,p,
            "      rrrrrrr      ",
            "   rrrrrrrrrrrrr   ",
            " rrrrrrrrrrrrrrrrr ",
            "rrrrrrrrrrrrrrrrrrr",
            "rrrrrrrrrrrrrrrrrrr",
            " rrrrrrrrrrrrrrrrr ",
            "   rrrrrrrrrrrrr   ",
            "      rrrrrrr      ");
        paintLayer(center,-4,p,
            "    mmmmmmmmmmm    ",
            "  mmmmmmmmmmmmmmm  ",
            " mmmmmmmmmmmmmmmmm ",
            "mmmmmmmmmmmmmmmmmmm",
            "mmmmmmmmmmmmmmmmmmm",
            " mmmmmmmmmmmmmmmmm ",
            "   mmmmmmmmmmmmm   ");
        paintLayer(center,-3,p,
            "   mmmmmmmmmmmmm   ",
            " mmmmmmmrrmmmmmmmm ",
            "mmmmmmrrrrrrmmmmmmm",
            "mmmmmrrrwwrrrmmmmmm",
            "mmmmrrwwwwwwrrmmmmm",
            "mmmmmrrwwwwrrmmmmmm",
            " mmmmmmrrrrmmmmmmm ",
            "   mmmmmmmmmmmmm   ");

        // Marsh pools, roots and mushrooms.
        for(int[] q:new int[][]{{-4,-2,0},{-3,-2,0},{-4,-2,1},{3,-2,-2},{4,-2,-2},{3,-2,-1}}) {
            set(center,q[0],q[1],q[2],Material.WATER);
        }
        for(int[] q:new int[][]{{-7,-2,3},{-6,-2,4},{6,-2,3},{7,-2,-3},{0,-2,6}}) {
            set(center,q[0],q[1],q[2],Material.MANGROVE_ROOTS);
        }
        set(center,-3,-1,0,Material.LILY_PAD);
        set(center,4,-1,-2,Material.LILY_PAD);
        set(center,-7,-1,-2,Material.RED_MUSHROOM);
        set(center,7,-1,2,Material.BROWN_MUSHROOM);

        buildTwistedMangrove(center.clone().add(-7,-2,-5));
        buildBetterWitchHut(center.clone().add(5,-2,4));

        spawnIfFewer(center,Frog.class,2,15,10,14,center.clone().add(-2.5,-1,1.5));
        spawnIfFewer(center,Witch.class,1,15,10,14,center.clone().add(5.5,2,4.5));
    }

    private void buildPortal(Location center) {
        Map<Character,Material> p=Map.of(
            'n',Material.NETHERRACK,
            'b',Material.BLACKSTONE,
            'm',Material.MAGMA_BLOCK,
            's',Material.SOUL_SOIL,
            'o',Material.OBSIDIAN
        );

        paintLayer(center,-5,p,
            "      bbbbbbb      ",
            "   bbbbbbbbbbbbb   ",
            " bbbbbbbbbbbbbbbbb ",
            "bbbbbbbbbbbbbbbbbbb",
            "bbbbbbbbbbbbbbbbbbb",
            " bbbbbbbbbbbbbbbbb ",
            "   bbbbbbbbbbbbb   ",
            "      bbbbbbb      ");
        paintLayer(center,-4,p,
            "    bbbbnnnbbbb    ",
            "  bbbnnnnnnnnnbbb  ",
            " bbnnnnnnnnnnnnnbb ",
            "bbnnnnnnnnnnnnnnnbb",
            "bbnnnnnnnnnnnnnnnbb",
            " bbnnnnnnnnnnnnnbb ",
            "   bbbnnnnnnnbbb   ");
        paintLayer(center,-3,p,
            "    nnnnnnnnnnn    ",
            "  nnnnnnnnnnnnnnn  ",
            " nnnnnnmmnnnnnnnnn ",
            "nnnnnnnnnnnnnnnnnnn",
            "nnnnnnnnnnnnnnnnnnn",
            " nnnnnnnnnmmnnnnnn ",
            "   nnnnnnnnnnnnn   ");

        // Borrow a real vanilla ruined-portal template for the broken masonry.
        boolean placed = placeVanillaStructure(
            center,-6,-2,-5,"ruined_portal/portal_7",StructureRotation.CLOCKWISE_90
        );
        if (!placed) {
            buildFallbackRuinedPortal(center.clone().add(-3,-2,-2));
        }

        // A smaller repaired portal gives the island a reliable progression function.
        for(int x=4;x<=7;x++){
            set(center,x,-2,3,Material.OBSIDIAN);
            set(center,x,3,3,Material.OBSIDIAN);
        }
        for(int y=-1;y<=2;y++){
            set(center,4,y,3,Material.OBSIDIAN);
            set(center,7,y,3,Material.OBSIDIAN);
            set(center,5,y,3,Material.NETHER_PORTAL);
            set(center,6,y,3,Material.NETHER_PORTAL);
        }

        // Scattered corruption.
        for(int[] q:new int[][]{{-9,-2,4},{-8,-2,5},{8,-2,-4},{9,-2,-3},{0,-2,7}}) {
            set(center,q[0],q[1],q[2],Material.MAGMA_BLOCK);
        }
        set(center,-8,-1,-4,Material.CRYING_OBSIDIAN);
        set(center,8,-1,-5,Material.GILDED_BLACKSTONE);
        set(center,0,-2,-7,Material.SOUL_SAND);

        cleanTemplateMarkers(center,20,14,18);
        removeNonVillageContainers(center,20,14,18);
    }

    private void buildMonument(Location center) {
        Map<Character,Material> p=Map.of(
            'p',Material.PRISMARINE,
            'b',Material.PRISMARINE_BRICKS,
            'd',Material.DARK_PRISMARINE,
            'w',Material.WATER
        );

        paintLayer(center,-6,p,
            "      ppppppp      ",
            "   ppppppppppppp   ",
            " ppppppppppppppppp ",
            "ppppppppppppppppppp",
            "ppppppppppppppppppp",
            " ppppppppppppppppp ",
            "   ppppppppppppp   ",
            "      ppppppp      ");
        paintLayer(center,-5,p,
            "    pppbbbbbbppp    ",
            "  ppbbbbbbbbbbbbpp  ",
            " pbbbbbbbbbbbbbbbbp ",
            "pbbbbbbbbbbbbbbbbbbp",
            "pbbbbbbbbbbbbbbbbbbp",
            " pbbbbbbbbbbbbbbbbp ",
            "   ppbbbbbbbbbbpp   ");
        paintLayer(center,-4,p,
            "    bbbbbbbbbbbb    ",
            "  bbbbwwwwwwbbbbb  ",
            " bbbwwwwwwwwwwbbbb ",
            "bbbwwwwwwwwwwwwbbbb",
            "bbbwwwwwwwwwwwwbbbb",
            " bbbbwwwwwwwwbbbb  ",
            "   bbbbbbbbbbbbbb  ");

        // Broken gate: two unequal towers with a snapped lintel.
        for(int y=-3;y<=5;y++){
            set(center,-7,y,-2,(y%3==0)?Material.DARK_PRISMARINE:Material.PRISMARINE_BRICKS);
            set(center,-6,y,-2,Material.PRISMARINE);
        }
        for(int y=-3;y<=3;y++){
            set(center,7,y,-2,(y%2==0)?Material.DARK_PRISMARINE:Material.PRISMARINE_BRICKS);
            set(center,6,y,-2,Material.PRISMARINE);
        }
        for(int x=-5;x<=2;x++) set(center,x,5,-2,Material.PRISMARINE_BRICKS);
        set(center,-4,4,-2,Material.SEA_LANTERN);
        set(center,4,2,-2,Material.SEA_LANTERN);

        // Water court and ruined stepping stones.
        for(int x=-4;x<=4;x++){
            for(int z=0;z<=5;z++){
                if(Math.abs(x)+Math.abs(z-2)<=6) set(center,x,-3,z,Material.WATER);
            }
        }
        for(int[] q:new int[][]{{-3,-2,1},{0,-2,1},{3,-2,2},{-2,-2,4},{2,-2,5}}) {
            set(center,q[0],q[1],q[2],Material.DARK_PRISMARINE);
        }
        set(center,-5,-2,5,Material.WET_SPONGE);
        set(center,5,-2,4,Material.SPONGE);
        set(center,0,-2,6,Material.SEA_LANTERN);

        // Side buttresses / broken columns.
        int[][] cols={{-10,-3,2,4},{10,-3,1,3},{-9,-3,-5,3},{9,-3,-5,5}};
        for(int[] q:cols){
            for(int y=q[1];y<=q[3];y++) set(center,q[0],y,q[2],Material.PRISMARINE);
            set(center,q[0],q[3]+1,q[2],Material.PRISMARINE_BRICK_STAIRS);
        }

        spawnIfFewer(center,Guardian.class,2,17,12,16,center.clone().add(-2.5,-2,2.5));
        spawnIfFewer(center,Guardian.class,2,17,12,16,center.clone().add(2.5,-2,3.5));
    }

    private void buildDesert(Location center) {
        Map<Character, Material> p = Map.of(
            's', Material.SAND,
            'S', Material.SANDSTONE,
            'r', Material.RED_SAND,
            'c', Material.CLAY,
            'w', Material.WATER
        );

        // Explicit stepped underside and dune plan; no ellipsoid/cap generation.
        paintLayer(center, -5, p,
            "       SSSSS       ",
            "    SSSSSSSSSS     ",
            "  SSSSSSSSSSSSSS   ",
            " SSSSSSSSSSSSSSSS  ",
            "SSSSSSSSSSSSSSSSSS ",
            " SSSSSSSSSSSSSSSS  ",
            "  SSSSSSSSSSSSSS   ",
            "    SSSSSSSSSS     ",
            "       SSSSS       ");
        paintLayer(center, -4, p,
            "    SSSSSSSSSSS    ",
            "  SSSSSSSSSSSSSSS  ",
            " SSSSSSSSSSSSSSSSS ",
            "SSSSSSSSSSSSSSSSSSS",
            "SSSSSSSSSSSSSSSSSSS",
            " SSSSSSSSSSSSSSSSS ",
            "  SSSSSSSSSSSSSSS  ",
            "    SSSSSSSSSSS    ");
        paintLayer(center, -3, p,
            "   SSSSSSSSSSSSS   ",
            " SSSSSSSSSSSSSSSSS ",
            "SSSSSSSSSSSSSSSSSSS",
            "SSSSSSSSSSSSSSSSSSS",
            "SSSSSSSSSSSSSSSSSSS",
            "SSSSSSSSSSSSSSSSSSS",
            " SSSSSSSSSSSSSSSSS ",
            "   SSSSSSSSSSSSS   ");
        paintLayer(center, -2, p,
            "   sssssssssssssss  ",
            " sssssssssssssssssss",
            "sssssssssssssssssssss",
            "sssssssssssssssssssss",
            "sssssssssssssssssssss",
            "sssssssssssssssssssss",
            " sssssssssssssssssss ",
            "   sssssssssssssss   ");
        paintLayer(center, -1, p,
            "    sssssssssssss    ",
            "  sssssssssssssssss  ",
            " ssssssscccccsssssss ",
            "ssssssccwwwwwccssssss",
            "sssssscwwwwwwwcssssss",
            "ssssssccwwwwwccssssss",
            " ssssssscccccsssssss ",
            "   sssssssssssssss   ");

        // Uneven dunes/ledges.
        for (int[] q : new int[][]{
            {-11,0,-3},{-10,0,-3},{-9,0,-3},{-10,1,-3},
            {9,0,4},{10,0,4},{11,0,4},{10,1,4},
            {-7,0,7},{-6,0,7},{-5,0,7},{-6,1,7},
            {5,0,-7},{6,0,-7},{7,0,-7}
        }) set(center,q[0],q[1],q[2],Material.SAND);

        buildBetterPalm(center.clone().add(-7, 0, -4));
        buildBetterPalm(center.clone().add(7, 0, 4));

        // Reeds around the oasis and small desert details.
        for (int[] q : new int[][]{{-4,0,0},{-4,1,0},{-3,0,1},{4,0,-1},{4,1,-1},{3,0,-2}}) {
            set(center,q[0],q[1],q[2],Material.SUGAR_CANE);
        }
        for (int y = 0; y <= 2; y++) set(center, -11, y, 3, Material.CACTUS);
        for (int y = 0; y <= 1; y++) set(center, 11, y, -3, Material.CACTUS);
        set(center, -8, 0, 6, Material.DEAD_BUSH);
        set(center, 8, 0, -6, Material.DEAD_BUSH);
        set(center, 3, 0, 7, Material.RED_SAND);
        set(center, 4, 0, 7, Material.RED_SAND);

        buildBrokenDesertArch(center.clone().add(0, 0, 8));
        spawnIfFewer(center, Camel.class, 1, 18, 10, 16, center.clone().add(8.5, 1, 0.5));
    }

    private void buildFrozen(Location center) {
        Map<Character,Material> p=Map.of(
            's',Material.STONE,
            'i',Material.PACKED_ICE,
            'n',Material.SNOW_BLOCK,
            'b',Material.BLUE_ICE,
            'c',Material.CALCITE
        );

        paintLayer(center,-7,p,
            "       sssss       ",
            "    sssssssssss    ",
            "  sssssssssssssss  ",
            " sssssssssssssssss ",
            "sssssssssssssssssss",
            " sssssssssssssssss ",
            "   sssssssssssss   ",
            "      sssssss      ");
        paintLayer(center,-6,p,
            "     sssiiisss     ",
            "   ssiiiiiiiiiss   ",
            " ssiiiiiiiiiiiiiss ",
            "ssiiiiiiiiiiiiiiiss",
            "ssiiiiiiiiiiiiiiiss",
            " ssiiiiiiiiiiiiiss ",
            "   ssiiiiiiiiiss   ");
        paintLayer(center,-5,p,
            "    iiiiiiiiiii    ",
            "  iiiiiiiiiiiiiii  ",
            " iiiiiiiiiiiiiiiii ",
            "iiiiiiiiiiiiiiiiiii",
            "iiiiiiiiiiiiiiiiiii",
            " iiiiiiiiiiiiiiiii ",
            "   iiiiiiiiiiiii   ");
        paintLayer(center,-4,p,
            "    nnnnnnnnnnn    ",
            "  nnnnnnnnnnnnnnn  ",
            " nnnnnnnnnnnnnnnnn ",
            "nnnnnnnnnnnnnnnnnnn",
            "nnnnnnnnnnnnnnnnnnn",
            " nnnnnnnnnnnnnnnnn ",
            "   nnnnnnnnnnnnn   ");

        // Cliff shards and blue-ice seams.
        for(int[] q:new int[][]{{-10,-3,2},{-10,-2,2},{10,-3,-1},{10,-2,-1},{-7,-3,-6},{7,-3,6}}) {
            set(center,q[0],q[1],q[2],Material.BLUE_ICE);
        }

        // Hand-built observatory: an asymmetrical stone/calacite tower with a copper telescope.
        int[][] base={
            {-3,-3,-3},{-2,-3,-3},{-1,-3,-3},{0,-3,-3},{1,-3,-3},{2,-3,-3},{3,-3,-3},
            {-4,-3,-2},{4,-3,-2},{-4,-3,-1},{4,-3,-1},{-4,-3,0},{4,-3,0},{-4,-3,1},{4,-3,1},
            {-3,-3,2},{-2,-3,2},{-1,-3,2},{0,-3,2},{1,-3,2},{2,-3,2},{3,-3,2}
        };
        for(int[] q:base) set(center,q[0],q[1],q[2],Material.STONE_BRICKS);
        for(int y=-2;y<=2;y++){
            for(int[] q:new int[][]{{-4,y,-2},{-4,y,1},{4,y,-2},{4,y,1},{-3,y,-3},{3,y,-3},{-3,y,2},{3,y,2}}) {
                set(center,q[0],q[1],q[2],Material.CALCITE);
            }
        }
        // Curved-ish roof rim with slabs.
        for(int[] q:new int[][]{
            {-4,3,-2},{-4,3,-1},{-4,3,0},{-4,3,1},
            {4,3,-2},{4,3,-1},{4,3,0},{4,3,1},
            {-3,3,-3},{-2,3,-3},{-1,3,-3},{0,3,-3},{1,3,-3},{2,3,-3},{3,3,-3},
            {-3,3,2},{-2,3,2},{-1,3,2},{0,3,2},{1,3,2},{2,3,2},{3,3,2}
        }) set(center,q[0],q[1],q[2],Material.STONE_BRICK_SLAB);

        // Telescope points out over the void.
        set(center,0,1,-1,Material.COPPER_BLOCK);
        set(center,0,2,-1,Material.COPPER_BLOCK);
        set(center,0,3,-2,Material.EXPOSED_COPPER);
        set(center,0,4,-3,Material.WEATHERED_COPPER);
        set(center,0,5,-4,Material.LIGHTNING_ROD);
        set(center,1,0,-1,Material.LECTERN);
        set(center,-1,0,0,Material.CANDLE);

        buildBetterSpruce(center.clone().add(-8,-3,4));
        set(center,7,-3,5,Material.POWDER_SNOW);
        set(center,8,-3,4,Material.BLUE_ICE);

        spawnIfFewer(center,Goat.class,2,16,12,15,center.clone().add(6.5,-2,-1.5));
        spawnIfFewer(center,Goat.class,2,16,12,15,center.clone().add(-6.5,-2,1.5));
    }

    private void buildMushroom(Location center) {
        Map<Character,Material> p=Map.of(
            'd',Material.DIRT,
            'm',Material.MYCELIUM,
            's',Material.STONE,
            'p',Material.PODZOL
        );

        paintLayer(center,-6,p,
            "       sssss       ",
            "    sssssssssss    ",
            "  sssssssssssssss  ",
            " sssssssssssssssss ",
            "sssssssssssssssssss",
            " sssssssssssssssss ",
            "   sssssssssssss   ",
            "      sssssss      ");
        paintLayer(center,-5,p,
            "     ddddddddd     ",
            "   ddddddddddddd   ",
            " ddddddddddddddddd ",
            "ddddddddddddddddddd",
            "ddddddddddddddddddd",
            " ddddddddddddddddd ",
            "   ddddddddddddd   ");
        paintLayer(center,-4,p,
            "    mmmmmmmmmmm    ",
            "  mmmmmmmmmmmmmmm  ",
            " mmmmmmmmmmmmmmmmm ",
            "mmmmmmmmmmmmmmmmmmm",
            "mmmmmmmmmmmmmmmmmmm",
            " mmmmmmmmmmmmmmmmm ",
            "   mmmmmmmmmmmmm   ");

        for(int[] q:new int[][]{
            {-11,-4,-1},{-10,-4,-1},{-10,-3,-1},{10,-4,2},{11,-4,2},
            {-7,-5,7},{-6,-5,7},{7,-5,-7},{8,-5,-7}
        }) set(center,q[0],q[1],q[2], q[1]==-3 ? Material.MYCELIUM : Material.DIRT);

        buildOrganicMushroom(center.clone().add(-6,-3,-2),true,7);
        buildOrganicMushroom(center.clone().add(5,-3,3),false,6);
        buildOrganicMushroom(center.clone().add(1,-3,-6),true,5);

        for(int[] q:new int[][]{{-2,-3,5},{3,-3,-4},{7,-3,-2},{-8,-3,3},{0,-3,6}}) {
            set(center,q[0],q[1],q[2], (q[0]&1)==0 ? Material.RED_MUSHROOM : Material.BROWN_MUSHROOM);
        }
        set(center,-3,-3,-5,Material.PODZOL);
        set(center,-4,-3,-5,Material.PODZOL);
        set(center,3,-3,6,Material.PODZOL);

        spawnIfFewer(center,MushroomCow.class,2,17,10,16,center.clone().add(1.5,-2,0.5));
        spawnIfFewer(center,MushroomCow.class,2,17,10,16,center.clone().add(-2.5,-2,2.5));
    }

    private void buildGeode(Location center) {
        Map<Character,Material> p=Map.of(
            'b',Material.SMOOTH_BASALT,
            'c',Material.CALCITE,
            'a',Material.AMETHYST_BLOCK,
            'u',Material.BUDDING_AMETHYST
        );

        paintLayer(center,-8,p,
            "       bbbbb       ",
            "    bbbbbbbbbbb    ",
            "  bbbbbbbbbbbbbbb  ",
            " bbbbbbbbbbbbbbbbb ",
            "bbbbbbbbbbbbbbbbbbb",
            " bbbbbbbbbbbbbbbbb ",
            "   bbbbbbbbbbbbb   ");
        paintLayer(center,-7,p,
            "     bbbbbbbbb     ",
            "   bbbbccccbbbbb   ",
            " bbbbccccccccbbbbb ",
            "bbbccccccccccccbbbb",
            "bbbccccccccccccbbbb",
            " bbbbccccccccbbbb  ",
            "   bbbbccccbbbb    ");
        paintLayer(center,-6,p,
            "    bbbccccccbbb    ",
            "  bbbcccaaaacccbbb  ",
            " bbcccaaaaaaaaacccbb",
            "bbcccaaaaaaaaaacccbb",
            " bbcccaaaaaaaaacccbb",
            "   bbbcccaaaacccbbb ");
        paintLayer(center,-5,p,
            "     ccaaaaacc      ",
            "   ccaaaaaaaaacc    ",
            "  caaaaaaaaaaaaac   ",
            " caaaaaaaaaaaaaaac  ",
            "  caaaaaaaaaaaaac   ",
            "    ccaaaaaaaacc    ");

        int[][] shell={
            {-8,-4,-4},{-8,-3,-4},{-8,-2,-4},{-7,-1,-4},{-7,0,-4},{-6,1,-4},{-5,2,-4},
            {8,-4,-4},{8,-3,-4},{8,-2,-4},{7,-1,-4},{7,0,-4},{6,1,-4},{5,2,-4},
            {-5,3,-4},{-3,4,-4},{0,5,-4},{3,4,-4},{5,3,-4},
            {-7,-3,0},{-7,-2,0},{-6,-1,0},{-6,0,0},{6,-3,0},{6,-2,0},{6,-1,0},{6,0,0}
        };
        for(int[] q:shell) set(center,q[0],q[1],q[2], ((q[0]+q[1])&1)==0 ? Material.CALCITE : Material.SMOOTH_BASALT);

        int[][] amethyst={
            {-5,-4,-2},{-4,-4,-3},{-3,-4,-4},{3,-4,-4},{4,-4,-3},{5,-4,-2},
            {-5,-1,-5},{5,0,-5},{-3,2,-5},{3,2,-5},{0,4,-5},
            {-4,-2,1},{4,-2,1},{-2,1,0},{2,1,0}
        };
        for(int[] q:amethyst) set(center,q[0],q[1],q[2],Material.AMETHYST_BLOCK);

        int[][] budding={{-4,-3,-2},{4,-3,-2},{-3,0,-5},{3,1,-5},{0,3,-5},{-2,-2,1}};
        for(int[] q:budding) set(center,q[0],q[1],q[2],Material.BUDDING_AMETHYST);

        setData(center,-4,-2,-2,"minecraft:amethyst_cluster[facing=up,waterlogged=false]");
        setData(center,4,-2,-2,"minecraft:large_amethyst_bud[facing=up,waterlogged=false]");
        setData(center,-3,1,-5,"minecraft:amethyst_cluster[facing=south,waterlogged=false]");
        setData(center,3,2,-5,"minecraft:medium_amethyst_bud[facing=south,waterlogged=false]");
        setData(center,0,4,-5,"minecraft:amethyst_cluster[facing=down,waterlogged=false]");

        for(int[] q:new int[][]{{-2,-4,4},{-1,-4,5},{0,-4,5},{1,-4,5},{2,-4,4}}) {
            set(center,q[0],q[1],q[2],Material.CALCITE);
        }
        for(int[] q:new int[][]{{-7,-5,3},{7,-5,2},{-5,-6,5},{5,-6,4}}) {
            set(center,q[0],q[1],q[2],Material.SMOOTH_BASALT);
        }
    }

    private void buildApiary(Location center) {
        Map<Character,Material> p=Map.of(
            's',Material.STONE,
            'd',Material.DIRT,
            'g',Material.GRASS_BLOCK,
            'm',Material.MOSS_BLOCK
        );

        paintLayer(center,-6,p,
            "      sssssss      ",
            "   sssssssssssss   ",
            " sssssssssssssssss ",
            "sssssssssssssssssss",
            "sssssssssssssssssss",
            " sssssssssssssssss ",
            "   sssssssssssss   ");
        paintLayer(center,-5,p,
            "    ddddddddddd    ",
            "  ddddddddddddddd  ",
            " ddddddddddddddddd ",
            "ddddddddddddddddddd",
            "ddddddddddddddddddd",
            " ddddddddddddddddd ",
            "   ddddddddddddd   ");
        paintLayer(center,-4,p,
            "    ggggggggggg    ",
            "  ggggggggggggggg  ",
            " ggggggggggggggggg ",
            "ggggggggggggggggggg",
            "ggggggggggggggggggg",
            " ggggggggggggggggg ",
            "   ggggggggggggg   ");

        buildApiaryTree(center.clone().add(-5,-3,-1));

        for(int[] q:new int[][]{{3,-3,-3},{7,-3,-3},{3,-3,3},{7,-3,3}}) {
            for(int y=0;y<=4;y++) set(center,q[0],q[1]+y,q[2],Material.OAK_LOG);
        }
        for(int x=2;x<=8;x++){
            set(center,x,2,-4,Material.OAK_STAIRS);
            set(center,x,2,4,Material.OAK_STAIRS);
            if(x>=3 && x<=7) {
                set(center,x,3,-3,Material.OAK_SLAB);
                set(center,x,3,3,Material.OAK_SLAB);
            }
        }
        for(int z=-2;z<=2;z+=2){
            set(center,4,-1,z,Material.BEEHIVE);
            set(center,6,-1,z,Material.BEE_NEST);
        }
        set(center,5,-2,0,Material.HONEY_BLOCK);
        set(center,5,-2,1,Material.HONEYCOMB_BLOCK);
        set(center,4,-2,1,Material.HONEYCOMB_BLOCK);

        Material[] flowers={
            Material.DANDELION,Material.POPPY,Material.OXEYE_DAISY,
            Material.CORNFLOWER,Material.ALLIUM,Material.AZURE_BLUET
        };
        int[][] flowerSpots={
            {-9,-3,-5},{-8,-3,-2},{-9,-3,3},{-7,-3,6},{-4,-3,6},
            {-2,-3,5},{0,-3,6},{1,-3,3},{0,-3,-5},{2,-3,-6},
            {9,-3,-5},{10,-3,-1},{9,-3,5},{6,-3,6},{3,-3,5},
            {-2,-3,-6},{-6,-3,-6},{8,-3,2}
        };
        for(int i=0;i<flowerSpots.length;i++){
            int[] q=flowerSpots[i];
            set(center,q[0],q[1],q[2],flowers[i%flowers.length]);
        }

        set(center,-1,-3,0,Material.WATER);
        set(center,-1,-3,1,Material.WATER);
        set(center,-2,-3,0,Material.MOSS_BLOCK);
        set(center,-2,-3,1,Material.MOSS_BLOCK);

        spawnIfFewer(center,Bee.class,4,17,12,16,center.clone().add(1.5,0,0.5));
        spawnIfFewer(center,Bee.class,4,17,12,16,center.clone().add(5.5,0,1.5));
    }

    private void buildEndShrine(Location center) {
        Map<Character,Material> p=Map.of(
            'e',Material.END_STONE,
            'b',Material.END_STONE_BRICKS,
            'p',Material.PURPUR_BLOCK,
            'o',Material.OBSIDIAN
        );

        paintLayer(center,-7,p,
            "       eeeee       ",
            "    eeeeeeeeeee    ",
            "  eeeeeeeeeeeeeee  ",
            " eeeeeeeeeeeeeeeee ",
            "eeeeeeeeeeeeeeeeeee",
            " eeeeeeeeeeeeeeeee ",
            "   eeeeeeeeeeeee   ",
            "      eeeeeee      ");
        paintLayer(center,-6,p,
            "     eeeeeeeee     ",
            "   eeeeeeeeeeeee   ",
            " eeeeeeeeeeeeeeeee ",
            "eeeeeeeeeeeeeeeeeee",
            "eeeeeeeeeeeeeeeeeee",
            " eeeeeeeeeeeeeeeee ",
            "   eeeeeeeeeeeee   ");
        paintLayer(center,-5,p,
            "    bbbbbbbbbbb    ",
            "  bbbbbbbbbbbbbbb  ",
            " bbbbbbbbbbbbbbbbb ",
            "bbbbbbbbbbbbbbbbbbb",
            "bbbbbbbbbbbbbbbbbbb",
            " bbbbbbbbbbbbbbbbb ",
            "   bbbbbbbbbbbbb   ");

        for(int z=7;z<=13;z++){
            set(center,0,-4,z,Material.END_STONE_BRICKS);
            if(z%2==0) set(center,1,-4,z,Material.END_STONE);
            if(z%3==0) set(center,-1,-4,z,Material.PURPUR_SLAB);
        }

        buildEndPylon(center,-7,-4,-6,8);
        buildEndPylon(center,7,-4,-6,6);
        buildEndPylon(center,-7,-4,5,5);
        buildEndPylon(center,7,-4,5,9);

        for(int[] q:new int[][]{
            {-6,4,-6},{-5,5,-6},{-4,6,-6},{-3,6,-6},
            {6,2,-6},{5,3,-6},{4,4,-6},
            {-6,1,5},{-5,2,5},{-4,3,5},
            {6,5,5},{5,6,5},{4,7,5},{3,7,5}
        }) set(center,q[0],q[1],q[2],Material.PURPUR_PILLAR);

        for(int x=-3;x<=3;x++){
            for(int z=-3;z<=3;z++){
                if(Math.abs(x)==3 || Math.abs(z)==3) set(center,x,-4,z,Material.OBSIDIAN);
                else if(Math.abs(x)==2 || Math.abs(z)==2) set(center,x,-4,z,Material.END_STONE_BRICKS);
            }
        }
        for(int x=-1;x<=1;x++){
            for(int z=-1;z<=1;z++) set(center,x,-3,z,Material.END_PORTAL);
        }

        set(center,-9,-4,0,Material.CHORUS_FLOWER);
        set(center,9,-4,1,Material.CHORUS_FLOWER);
        set(center,-7,5,-6,Material.END_ROD);
        set(center,7,3,-6,Material.END_ROD);
        set(center,-7,2,5,Material.END_ROD);
        set(center,7,6,5,Material.END_ROD);
        for(int[] q:new int[][]{{-10,-5,4},{10,-5,-3},{-5,-6,8},{6,-6,7}}) {
            set(center,q[0],q[1],q[2],Material.END_STONE);
        }
    }

    private void buildVillage(Location center) {
        Map<Character, Material> p = Map.of(
            'g', Material.GRASS_BLOCK,
            'd', Material.DIRT,
            's', Material.STONE,
            'c', Material.COBBLESTONE,
            'p', Material.DIRT_PATH,
            'w', Material.WATER,
            'f', Material.FARMLAND
        );

        // Hand-authored floating terrain with a lopsided rock underside.
        paintLayer(center, -6, p,
            "        sssss        ",
            "     sssssssssss     ",
            "   sssssssssssssss   ",
            "  sssssssssssssssss  ",
            "   sssssssssssssss   ",
            "     sssssssssss     ",
            "        sssss        ");
        paintLayer(center, -5, p,
            "      sssssssss      ",
            "   sssssssssssssss   ",
            " sssssssssssssssssss ",
            "sssssssssssssssssssss",
            "sssssssssssssssssssss",
            " sssssssssssssssssss ",
            "   sssssssssssssss   ",
            "      sssssssss      ");
        paintLayer(center, -4, p,
            "    ddddddddddddd    ",
            "  ddddddddddddddddd  ",
            " ddddddddddddddddddd ",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            " ddddddddddddddddddd ",
            "  ddddddddddddddddd  ",
            "    ddddddddddddd    ");
        paintLayer(center, -3, p,
            "   ddddddddddddddd   ",
            " ddddddddddddddddddd ",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            "ddddddddddddddddddddd",
            " ddddddddddddddddddd ",
            "   ddddddddddddddd   ");
        paintLayer(center, -2, p,
            "    ggggggggggggg    ",
            "  ggggggggggggggggg  ",
            " ggggggggggggggggggg ",
            "ggggggggggggggggggggg",
            "ggggggggggggggggggggg",
            "ggggggggggggggggggggg",
            " ggggggggggggggggggg ",
            "  ggggggggggggggggg  ",
            "    ggggggggggggg    ");

        // Real vanilla house templates. These are server resources, not bundled copies.
        boolean houseA = placeVanillaStructure(center, -13, -1, -8,
            "village/plains/houses/plains_small_house_4", StructureRotation.NONE);
        boolean houseB = placeVanillaStructure(center, 5, -1, -8,
            "village/plains/houses/plains_library_1", StructureRotation.CLOCKWISE_180);

        if (!houseA) buildFallbackCottage(center.clone().add(-9, -1, -4), false);
        if (!houseB) buildFallbackCottage(center.clone().add(8, -1, -4), true);

        // Winding path and village green.
        int[][] path = {
            {0,-1,8},{0,-1,7},{-1,-1,6},{-1,-1,5},{0,-1,4},{0,-1,3},{0,-1,2},
            {-1,-1,1},{-2,-1,0},{-3,-1,-1},{-4,-1,-2},{-5,-1,-3},
            {1,-1,1},{2,-1,0},{3,-1,-1},{4,-1,-2},{5,-1,-3}
        };
        for (int[] q : path) {
            set(center,q[0],q[1],q[2],Material.DIRT_PATH);
            if ((q[0]+q[2]) % 3 == 0) {
                set(center,q[0]+1,q[1],q[2],Material.COARSE_DIRT);
            }
        }

        // Bell square under a crooked oak.
        set(center, 0, 0, 1, Material.COBBLESTONE_WALL);
        set(center, 0, 1, 1, Material.BELL);
        buildVillageOak(center.clone().add(-3, 0, 3));

        // Small irrigated farm, intentionally irregular.
        for (int x=-7;x<=-2;x++) {
            for (int z=4;z<=7;z++) {
                set(center,x,-1,z,Material.FARMLAND);
                Material crop = ((x+z)&1)==0 ? Material.WHEAT : Material.CARROTS;
                set(center,x,0,z,crop);
            }
        }
        set(center,-5,-1,5,Material.WATER);
        set(center,-5,-1,6,Material.WATER);
        set(center,-8,-1,5,Material.COMPOSTER);
        set(center,-8,-1,7,Material.HAY_BLOCK);
        set(center,-8,0,7,Material.HAY_BLOCK);

        // Pond and flowers break up the open grass.
        for (int[] q : new int[][]{{7,-1,5},{8,-1,5},{7,-1,6},{8,-1,6},{9,-1,6}}) {
            set(center,q[0],q[1],q[2],Material.WATER);
        }
        set(center,6,0,6,Material.DANDELION);
        set(center,9,0,5,Material.POPPY);
        set(center,4,0,6,Material.OXEYE_DAISY);
        set(center,-1,0,6,Material.CORNFLOWER);

        // Only the village is allowed a structure loot chest; tuck it inside the green.
        villageChest(center.clone().add(2, 0, 4),
            new ItemStack(Material.BREAD, 6),
            new ItemStack(Material.EMERALD, 4),
            new ItemStack(Material.POTATO, 4),
            new ItemStack(Material.CARROT, 4),
            new ItemStack(Material.BEETROOT_SEEDS, 4)
        );

        // Remove raw template connector/debug blocks if a vanilla structure exposes them.
        cleanTemplateMarkers(center, 22, 14, 18);

        long villagers = center.getWorld().getNearbyEntities(center, 22, 14, 20, entity -> entity instanceof Villager).size();
        if (villagers < 2) {
            Villager farmer = center.getWorld().spawn(center.clone().add(-2.5, 0, 2.5), Villager.class);
            farmer.setProfession(Villager.Profession.FARMER);
            farmer.setPersistent(true);
            villagers++;
        }
        if (villagers < 2) {
            Villager librarian = center.getWorld().spawn(center.clone().add(2.5, 0, 2.5), Villager.class);
            librarian.setProfession(Villager.Profession.LIBRARIAN);
            librarian.setPersistent(true);
        }
    }

    private void setData(Location center, int dx, int dy, int dz, String blockData) {
        block(center,dx,dy,dz).setBlockData(Bukkit.createBlockData(blockData),false);
    }

    private void buildOrganicMushroom(Location base, boolean red, int height) {
        for(int y=0;y<height;y++) {
            int x = y >= height-2 ? 1 : 0;
            base.clone().add(x,y,0).getBlock().setType(Material.MUSHROOM_STEM,false);
        }
        Material cap=red?Material.RED_MUSHROOM_BLOCK:Material.BROWN_MUSHROOM_BLOCK;
        int top=height;
        int[][] capOffsets = red
            ? new int[][]{
                {-3,0,0},{-2,0,-2},{-2,0,-1},{-2,0,0},{-2,0,1},{-2,0,2},
                {-1,0,-3},{-1,0,-2},{-1,0,-1},{-1,0,0},{-1,0,1},{-1,0,2},{-1,0,3},
                {0,0,-3},{0,0,-2},{0,0,-1},{0,0,0},{0,0,1},{0,0,2},{0,0,3},
                {1,0,-3},{1,0,-2},{1,0,-1},{1,0,0},{1,0,1},{1,0,2},{1,0,3},
                {2,0,-2},{2,0,-1},{2,0,0},{2,0,1},{2,0,2},{3,0,0},
                {-2,1,-1},{-1,1,-2},{-1,1,-1},{-1,1,0},{-1,1,1},{0,1,-2},{0,1,-1},
                {0,1,0},{0,1,1},{0,1,2},{1,1,-1},{1,1,0},{1,1,1},{2,1,0}
            }
            : new int[][]{
                {-2,0,-1},{-2,0,0},{-2,0,1},
                {-1,0,-2},{-1,0,-1},{-1,0,0},{-1,0,1},{-1,0,2},
                {0,0,-2},{0,0,-1},{0,0,0},{0,0,1},{0,0,2},
                {1,0,-2},{1,0,-1},{1,0,0},{1,0,1},{1,0,2},
                {2,0,-1},{2,0,0},{2,0,1},
                {-1,1,0},{0,1,-1},{0,1,0},{0,1,1},{1,1,0}
            };
        for(int[] q:capOffsets) base.clone().add(q[0]+1,top+q[1],q[2]).getBlock().setType(cap,false);
    }

    private void buildApiaryTree(Location base) {
        int[][] trunk={{0,0,0},{0,1,0},{0,2,0},{0,3,0},{1,4,0},{1,5,0},{1,6,-1}};
        for(int[] q:trunk) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.OAK_LOG,false);
        int[][] leaves={
            {-2,4,0},{-1,4,-2},{-1,4,-1},{-1,4,0},{-1,4,1},{0,4,-2},{0,4,-1},{0,4,0},{0,4,1},{0,4,2},
            {1,4,-2},{1,4,-1},{1,4,0},{1,4,1},{1,4,2},{2,4,-1},{2,4,0},{2,4,1},{3,4,0},
            {-1,5,-1},{-1,5,0},{0,5,-2},{0,5,-1},{0,5,0},{0,5,1},{1,5,-2},{1,5,-1},{1,5,0},{1,5,1},
            {2,5,-1},{2,5,0},{2,5,1},{3,5,0},
            {0,6,-1},{0,6,0},{1,6,-2},{1,6,-1},{1,6,0},{1,6,1},{2,6,-1},{2,6,0}
        };
        for(int[] q:leaves) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.OAK_LEAVES,false);
        base.clone().add(2,3,0).getBlock().setType(Material.BEE_NEST,false);
        base.clone().add(-1,3,1).getBlock().setType(Material.BEEHIVE,false);
    }

    private void buildEndPylon(Location center, int x, int y, int z, int height) {
        for(int i=0;i<height;i++) {
            Material material = (i%3==0) ? Material.PURPUR_PILLAR : Material.OBSIDIAN;
            set(center,x,y+i,z,material);
            if(i==1 || i==height-2) {
                set(center,x+(x<0?1:-1),y+i,z,Material.END_STONE_BRICKS);
            }
        }
    }

    private void setPointedDripstone(Location center, int dx, int dy, int dz, boolean up) {
        String direction = up ? "up" : "down";
        block(center,dx,dy,dz).setBlockData(
            Bukkit.createBlockData("minecraft:pointed_dripstone[vertical_direction=" + direction + ",thickness=tip]"),
            false
        );
    }

    private void buildTwistedMangrove(Location base) {
        int[][] trunk={{0,0,0},{0,1,0},{1,2,0},{1,3,0},{1,4,-1},{2,5,-1}};
        for(int[] q:trunk) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.MANGROVE_LOG,false);
        for(int[] q:new int[][]{{-1,0,0},{1,0,1},{0,0,-1},{2,1,0},{2,0,-2}}) {
            base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.MANGROVE_ROOTS,false);
        }
        int[][] leaves={
            {0,5,-1},{1,5,-2},{2,5,-2},{3,5,-1},{2,5,0},
            {0,6,-2},{1,6,-2},{2,6,-2},{3,6,-2},{1,6,-1},{2,6,-1},
            {-1,5,-2},{4,5,-2},{1,5,-3},{2,5,1}
        };
        for(int[] q:leaves) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.MANGROVE_LEAVES,false);
        base.clone().add(1,4,-2).getBlock().setType(Material.MANGROVE_PROPAGULE,false);
    }

    private void buildBetterWitchHut(Location base) {
        // Four crooked stilts.
        for(int[] p:new int[][]{{-3,0,-2},{3,0,-2},{-3,0,2},{3,0,2}}) {
            for(int y=0;y<=3;y++) base.clone().add(p[0],y,p[2]).getBlock().setType(Material.SPRUCE_LOG,false);
        }
        // Irregular floor.
        for(int[] q:new int[][]{
            {-3,3,-2},{-2,3,-2},{-1,3,-2},{0,3,-2},{1,3,-2},{2,3,-2},{3,3,-2},
            {-3,3,-1},{-2,3,-1},{-1,3,-1},{0,3,-1},{1,3,-1},{2,3,-1},{3,3,-1},
            {-3,3,0},{-2,3,0},{-1,3,0},{0,3,0},{1,3,0},{2,3,0},{3,3,0},
            {-3,3,1},{-2,3,1},{-1,3,1},{0,3,1},{1,3,1},{2,3,1},{3,3,1},
            {-2,3,2},{-1,3,2},{0,3,2},{1,3,2},{2,3,2}
        }) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.SPRUCE_PLANKS,false);

        // Walls with deliberate gaps/windows.
        for(int[] q:new int[][]{
            {-3,4,-2},{-2,4,-2},{-1,4,-2},{1,4,-2},{2,4,-2},{3,4,-2},
            {-3,5,-2},{-2,5,-2},{2,5,-2},{3,5,-2},
            {-3,4,-1},{-3,4,0},{-3,4,1},{-3,5,-1},{-3,5,1},
            {3,4,-1},{3,4,0},{3,4,1},{3,5,-1},{3,5,1},
            {-2,4,2},{-1,4,2},{0,4,2},{1,4,2},{2,4,2}
        }) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.SPRUCE_PLANKS,false);

        // Sloped, overhanging roof using stairs and slabs.
        for(int x=-4;x<=4;x++){
            base.clone().add(x,6,-3).getBlock().setType(Material.DARK_OAK_STAIRS,false);
            base.clone().add(x,6,3).getBlock().setType(Material.DARK_OAK_STAIRS,false);
        }
        for(int x=-3;x<=3;x++){
            base.clone().add(x,7,-2).getBlock().setType(Material.DARK_OAK_STAIRS,false);
            base.clone().add(x,7,2).getBlock().setType(Material.DARK_OAK_STAIRS,false);
        }
        for(int x=-2;x<=2;x++) base.clone().add(x,8,0).getBlock().setType(Material.DARK_OAK_SLAB,false);

        base.clone().add(0,4,0).getBlock().setType(Material.CAULDRON,false);
        base.clone().add(2,4,0).getBlock().setType(Material.BROWN_MUSHROOM,false);
        base.clone().add(-1,4,1).getBlock().setType(Material.CRAFTING_TABLE,false);
        base.clone().add(0,4,2).getBlock().setType(Material.AIR,false);
    }

    private void buildFallbackRuinedPortal(Location base) {
        for(int[] q:new int[][]{
            {0,0,0},{1,0,0},{2,0,0},{3,0,0},
            {0,1,0},{0,2,0},{0,3,0},{0,4,0},
            {3,1,0},{3,2,0},
            {1,4,0},{2,4,0}
        }) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.OBSIDIAN,false);
        base.clone().add(3,3,0).getBlock().setType(Material.CRYING_OBSIDIAN,false);
        base.clone().add(-1,0,1).getBlock().setType(Material.MAGMA_BLOCK,false);
        base.clone().add(4,0,-1).getBlock().setType(Material.GILDED_BLACKSTONE,false);
    }

    private void removeNonVillageContainers(Location center, int rx, int ry, int rz) {
        for(int x=-rx;x<=rx;x++) {
            for(int y=-6;y<=ry;y++) {
                for(int z=-rz;z<=rz;z++) {
                    Block b=block(center,x,y,z);
                    if (b.getType()==Material.CHEST || b.getType()==Material.TRAPPED_CHEST
                        || b.getType()==Material.BARREL) {
                        b.setType(Material.AIR,false);
                    }
                }
            }
        }
    }

    private void buildBetterSpruce(Location base) {
        int[][] trunk={{0,0,0},{0,1,0},{0,2,0},{0,3,0},{0,4,0},{0,5,0},{0,6,0}};
        for(int[] q:trunk) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.SPRUCE_LOG,false);
        int[][] leaves={
            {-1,3,0},{1,3,0},{0,3,-1},{0,3,1},
            {-2,4,0},{-1,4,-1},{-1,4,0},{-1,4,1},{0,4,-2},{0,4,-1},{0,4,1},{0,4,2},{1,4,-1},{1,4,0},{1,4,1},{2,4,0},
            {-1,5,0},{0,5,-1},{0,5,0},{0,5,1},{1,5,0},
            {0,6,0},{0,7,0}
        };
        for(int[] q:leaves) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.SPRUCE_LEAVES,false);
    }

    private void paintLayer(Location center, int dy, Map<Character, Material> palette, String... rows) {
        int z0 = -(rows.length / 2);
        for (int rz = 0; rz < rows.length; rz++) {
            String row = rows[rz];
            int x0 = -(row.length() / 2);
            for (int rx = 0; rx < row.length(); rx++) {
                char symbol = row.charAt(rx);
                Material material = palette.get(symbol);
                if (material != null) {
                    set(center, x0 + rx, dy, z0 + rz, material);
                }
            }
        }
    }

    private boolean placeVanillaStructure(
        Location center, int dx, int dy, int dz, String path, StructureRotation rotation
    ) {
        try {
            Structure structure = plugin.getServer().getStructureManager()
                .loadStructure(NamespacedKey.minecraft(path));
            if (structure == null) {
                plugin.getLogger().warning("Vanilla structure not found: minecraft:" + path);
                return false;
            }
            structure.place(
                center.clone().add(dx, dy, dz),
                false,
                rotation,
                Mirror.NONE,
                0,
                1.0f,
                new Random(0x5A17B10CL)
            );
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not place vanilla structure minecraft:" + path + ": " + ex.getMessage());
            return false;
        }
    }

    private void cleanTemplateMarkers(Location center, int rx, int ry, int rz) {
        for (int x=-rx;x<=rx;x++) {
            for (int y=-4;y<=ry;y++) {
                for (int z=-rz;z<=rz;z++) {
                    Block b=block(center,x,y,z);
                    if (b.getType()==Material.JIGSAW || b.getType()==Material.STRUCTURE_BLOCK
                        || b.getType()==Material.STRUCTURE_VOID) {
                        b.setType(Material.AIR,false);
                    }
                }
            }
        }
    }

    private void buildBetterPalm(Location base) {
        // Slightly bent trunk.
        int[][] trunk={{0,0,0},{0,1,0},{0,2,0},{1,3,0},{1,4,0},{1,5,0}};
        for (int[] q:trunk) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.JUNGLE_LOG,false);
        int[][] leaves={
            {1,6,0},{0,6,0},{2,6,0},{1,6,1},{1,6,-1},
            {-1,6,0},{3,6,0},{1,6,2},{1,6,-2},
            {-2,5,0},{4,5,0},{1,5,3},{1,5,-3},
            {0,7,0},{2,7,0},{1,7,1},{1,7,-1}
        };
        for(int[] q:leaves) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.JUNGLE_LEAVES,false);
    }

    private void buildBrokenDesertArch(Location base) {
        int[][] sandstone={
            {-4,0,0},{-4,1,0},{-4,2,0},{-4,3,0},{-4,4,0},
            {-3,4,0},{-2,5,0},{-1,5,0},{0,5,0},{1,5,0},
            {2,4,0},{3,4,0},{3,3,0},{3,2,0},
            {-3,0,1},{-2,0,1},{2,0,-1},{3,0,-1}
        };
        for(int[] q:sandstone) base.clone().add(q[0],q[1],q[2]).getBlock().setType(
            (q[1]>=4 ? Material.CUT_SANDSTONE : Material.SANDSTONE),false);
        base.clone().add(-4,5,0).getBlock().setType(Material.CHISELED_SANDSTONE,false);
        base.clone().add(3,5,0).getBlock().setType(Material.CHISELED_SANDSTONE,false);
    }

    private void buildVillageOak(Location base) {
        int[][] trunk={{0,0,0},{0,1,0},{0,2,0},{0,3,0},{1,4,0},{1,5,0}};
        for(int[] q:trunk) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.OAK_LOG,false);
        int[][] leaves={
            {-1,4,0},{0,4,-1},{0,4,1},{1,4,-1},{1,4,1},{2,4,0},
            {-2,5,0},{-1,5,-1},{-1,5,1},{0,5,-2},{0,5,2},{1,5,-2},{1,5,2},{2,5,-1},{2,5,1},{3,5,0},
            {-1,6,0},{0,6,-1},{0,6,0},{0,6,1},{1,6,-1},{1,6,0},{1,6,1},{2,6,0}
        };
        for(int[] q:leaves) base.clone().add(q[0],q[1],q[2]).getBlock().setType(Material.OAK_LEAVES,false);
    }

    private void buildFallbackCottage(Location base, boolean stone) {
        Material wall=stone ? Material.STONE_BRICKS : Material.OAK_PLANKS;
        Material frame=stone ? Material.COBBLESTONE : Material.STRIPPED_OAK_LOG;
        int[][] floor={
            {-3,0,-2},{-2,0,-2},{-1,0,-2},{0,0,-2},{1,0,-2},{2,0,-2},{3,0,-2},
            {-3,0,-1},{-2,0,-1},{-1,0,-1},{0,0,-1},{1,0,-1},{2,0,-1},{3,0,-1},
            {-3,0,0},{-2,0,0},{-1,0,0},{0,0,0},{1,0,0},{2,0,0},{3,0,0},
            {-3,0,1},{-2,0,1},{-1,0,1},{0,0,1},{1,0,1},{2,0,1},{3,0,1},
            {-3,0,2},{-2,0,2},{-1,0,2},{0,0,2},{1,0,2},{2,0,2},{3,0,2}
        };
        for(int[] q:floor) base.clone().add(q[0],q[1],q[2]).getBlock().setType(wall,false);
        for(int y=1;y<=3;y++){
            for(int x=-3;x<=3;x++){ base.clone().add(x,y,-2).getBlock().setType(wall,false); base.clone().add(x,y,2).getBlock().setType(wall,false); }
            for(int z=-1;z<=1;z++){ base.clone().add(-3,y,z).getBlock().setType(wall,false); base.clone().add(3,y,z).getBlock().setType(wall,false); }
        }
        for(int[] q:new int[][]{{-3,1,-2},{3,1,-2},{-3,1,2},{3,1,2}}){
            for(int y=0;y<=4;y++) base.clone().add(q[0],y,q[2]).getBlock().setType(frame,false);
        }
        base.clone().add(0,1,2).getBlock().setType(Material.AIR,false);
        base.clone().add(0,2,2).getBlock().setType(Material.AIR,false);
        for(int x=-4;x<=4;x++){
            int h=4+Math.max(0,3-Math.abs(x));
            base.clone().add(x,h,-3).getBlock().setType(Material.OAK_STAIRS,false);
            base.clone().add(x,h,3).getBlock().setType(Material.OAK_STAIRS,false);
        }
        base.clone().add(-1,1,0).getBlock().setType(Material.RED_BED,false);
        base.clone().add(1,1,0).getBlock().setType(Material.BARREL,false);
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
