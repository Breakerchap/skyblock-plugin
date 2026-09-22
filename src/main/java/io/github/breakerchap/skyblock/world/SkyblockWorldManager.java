package io.github.breakerchap.skyblock.world;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SkyblockWorldManager implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressStore store;
    private final NamespacedKey netherReturnX;
    private final NamespacedKey netherReturnY;
    private final NamespacedKey netherReturnZ;
    private World world;

    public SkyblockWorldManager(SkyblockPlugin plugin, ProgressStore store) {
        this.plugin = plugin;
        this.store = store;
        this.netherReturnX = new NamespacedKey(plugin, "nether_return_x");
        this.netherReturnY = new NamespacedKey(plugin, "nether_return_y");
        this.netherReturnZ = new NamespacedKey(plugin, "nether_return_z");
    }

    public World ensureWorld() {
        String name = plugin.getConfig().getString("world", "skyblock").trim();
        if (name.isEmpty()) {
            name = "skyblock";
        }

        world = plugin.getServer().getWorld(name);
        if (world == null) {
            world = WorldCreator.name(name)
                .environment(World.Environment.NORMAL)
                .generator(new VoidChunkGenerator())
                .generateStructures(false)
                .createWorld();
        }

        if (world == null) {
            throw new IllegalStateException("Could not create Skyblock world '" + name + "'.");
        }

        int y = plugin.getConfig().getInt("world-generation.spawn-y", 64);
        world.setSpawnLocation(0, y + 2, 0);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        if (!store.isStarterIslandGenerated() || plugin.getConfig().getBoolean("world-generation.rebuild-starter-on-start", false)) {
            buildStarterIsland(new Location(world, 0, y, 0));
            store.markStarterIslandGenerated();
            store.save();
        }

        plugin.getLogger().info("Skyblock void world ready: " + world.getName());
        return world;
    }

    public World world() {
        return world != null ? world : ensureWorld();
    }

    public Location spawnLocation() {
        return world().getSpawnLocation().clone().add(0.5, 0.0, 0.5);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("world-generation.teleport-new-players", true)) {
            return;
        }
        if (!event.getPlayer().hasPlayedBefore()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getPlayer().teleport(spawnLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            if (event.getFrom().getWorld().equals(world())) {
                rememberNetherReturn(event.getPlayer(), event.getFrom());
            } else if (event.getFrom().getWorld().getEnvironment() == World.Environment.NETHER
                && event.getTo() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.NORMAL) {
                event.setTo(readNetherReturn(event.getPlayer()));
            }
        }

        if (event.getCause() == TeleportCause.END_PORTAL
            && event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setTo(spawnLocation());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        World respawnWorld = event.getRespawnLocation().getWorld();
        if (event.getPlayer().getWorld().equals(world())
            || (respawnWorld.getEnvironment() == World.Environment.NORMAL && !respawnWorld.equals(world()))) {
            event.setRespawnLocation(spawnLocation());
        }
    }

    private void rememberNetherReturn(org.bukkit.entity.Player player, Location location) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(netherReturnX, PersistentDataType.DOUBLE, location.getX());
        data.set(netherReturnY, PersistentDataType.DOUBLE, location.getY());
        data.set(netherReturnZ, PersistentDataType.DOUBLE, location.getZ());
    }

    private Location readNetherReturn(org.bukkit.entity.Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        Double x = data.get(netherReturnX, PersistentDataType.DOUBLE);
        Double y = data.get(netherReturnY, PersistentDataType.DOUBLE);
        Double z = data.get(netherReturnZ, PersistentDataType.DOUBLE);
        if (x == null || y == null || z == null) {
            return spawnLocation();
        }
        return new Location(world(), x, y, z);
    }

    private void buildStarterIsland(Location center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > 4.4) {
                    continue;
                }
                set(center, x, 0, z, Material.GRASS_BLOCK);
                if (distance < 3.8) {
                    set(center, x, -1, z, Material.DIRT);
                }
                if (distance < 2.8) {
                    set(center, x, -2, z, Material.DIRT);
                }
            }
        }

        set(center, 0, -3, 0, Material.STONE);
        buildOak(center.clone().add(-2, 1, -1));

        Block chestBlock = center.clone().add(2, 1, 1).getBlock();
        chestBlock.setType(Material.CHEST, false);
        if (chestBlock.getState() instanceof Chest chest) {
            chest.getBlockInventory().clear();
            chest.getBlockInventory().addItem(
                new ItemStack(Material.LAVA_BUCKET, 1),
                new ItemStack(Material.ICE, 2),
                new ItemStack(Material.OAK_SAPLING, 1),
                new ItemStack(Material.PUMPKIN_SEEDS, 1),
                new ItemStack(Material.SUGAR_CANE, 1)
            );
            chest.update(true, false);
        }
    }

    private void buildOak(Location base) {
        for (int y = 0; y < 5; y++) {
            base.clone().add(0, y, 0).getBlock().setType(Material.OAK_LOG, false);
        }
        for (int y = 3; y <= 5; y++) {
            int radius = y == 5 ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius && Math.abs(z) == radius && y == 5) {
                        continue;
                    }
                    Block block = base.clone().add(x, y, z).getBlock();
                    if (block.isEmpty()) {
                        block.setType(Material.OAK_LEAVES, false);
                    }
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
}
