package io.github.breakerchap.skyblock.recipe;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.progress.CommunityGoal;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeManager implements Listener {
    private final SkyblockPlugin plugin;
    private final ProgressionService progression;
    private final NamespacedKey wayfarerBellTag;
    private final Map<NamespacedKey, Unlock> unlocks = new LinkedHashMap<>();

    public RecipeManager(SkyblockPlugin plugin, ProgressionService progression) {
        this.plugin = plugin;
        this.progression = progression;
        this.wayfarerBellTag = new NamespacedKey(plugin, "wayfarer_bell");
    }

    public void registerAll() {
        registerGravel();
        registerSand();
        registerDirt();
        registerCalcite();
        registerWayfarerBell();
        Bukkit.updateRecipes();
    }

    private void registerGravel() {
        NamespacedKey key = new NamespacedKey(plugin, "gravel_from_cobblestone");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.GRAVEL));
        recipe.addIngredient(4, Material.COBBLESTONE);
        register(key, recipe, Unlock.personal("getting_started/cobblestone", "Gravel from Cobblestone"));
    }

    private void registerSand() {
        NamespacedKey key = new NamespacedKey(plugin, "sand_from_gravel");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.SAND));
        recipe.addIngredient(2, Material.GRAVEL);
        register(key, recipe, Unlock.community(CommunityGoal.COBBLE, "Sand from Gravel"));
    }

    private void registerDirt() {
        NamespacedKey key = new NamespacedKey(plugin, "dirt_cultivation");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.DIRT, 2));
        recipe.shape("FFF", "FDF", "FFF");
        recipe.setIngredient('F', Material.ROTTEN_FLESH);
        recipe.setIngredient('D', Material.DIRT);
        register(key, recipe, Unlock.community(CommunityGoal.LIFE, "Dirt Cultivation"));
    }

    private void registerCalcite() {
        NamespacedKey key = new NamespacedKey(plugin, "calcite_from_quartz");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.CALCITE, 2));
        recipe.addIngredient(2, Material.QUARTZ);
        recipe.addIngredient(2, Material.BONE_MEAL);
        register(key, recipe, Unlock.personal("nether/root", "Calcite"));
    }

    private void registerWayfarerBell() {
        NamespacedKey key = new NamespacedKey(plugin, "wayfarer_bell");
        ShapedRecipe recipe = new ShapedRecipe(key, createWayfarerBell());
        recipe.shape("EGE", "GBG", "EGE");
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('B', Material.BELL);
        register(key, recipe, Unlock.community(CommunityGoal.HUNTER, "Wayfarer's Bell"));
    }

    private void register(NamespacedKey key, Recipe recipe, Unlock unlock) {
        Bukkit.removeRecipe(key);
        if (!Bukkit.addRecipe(recipe)) {
            plugin.getLogger().warning("Could not register recipe " + key);
        }
        unlocks.put(key, unlock);
    }

    public ItemStack createWayfarerBell() {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Wayfarer's Bell", NamedTextColor.GOLD));
        meta.lore(List.of(
            Component.text("Ring under the open sky to call a wandering trader.", NamedTextColor.GRAY),
            Component.text("Reusable, but has a cooldown.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(wayfarerBellTag, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWayfarerBell(ItemStack item) {
        if (item == null || item.getType() != Material.BELL || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer()
            .get(wayfarerBellTag, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public void syncAll(boolean announce) {
        Bukkit.getOnlinePlayers().forEach(player -> syncPlayer(player, announce));
    }

    public void syncPlayer(Player player, boolean announce) {
        for (Map.Entry<NamespacedKey, Unlock> entry : unlocks.entrySet()) {
            boolean unlocked = entry.getValue().isUnlocked(player, progression);
            if (unlocked) {
                boolean discovered = player.discoverRecipe(entry.getKey());
                if (announce && discovered) {
                    player.sendMessage(
                        Component.text("Recipe unlocked: ", NamedTextColor.GREEN)
                            .append(Component.text(entry.getValue().label(), NamedTextColor.WHITE))
                    );
                }
            } else {
                player.undiscoverRecipe(entry.getKey());
            }
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        NamespacedKey key = recipeKey(event.getRecipe());
        if (key != null && !isUnlocked(player, key)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        NamespacedKey key = recipeKey(event.getRecipe());
        if (key != null && !isUnlocked(player, key)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("That recipe has not been unlocked yet.", NamedTextColor.RED));
        }
    }

    private NamespacedKey recipeKey(Recipe recipe) {
        if (recipe instanceof Keyed keyed && unlocks.containsKey(keyed.getKey())) {
            return keyed.getKey();
        }
        return null;
    }

    private boolean isUnlocked(Player player, NamespacedKey key) {
        Unlock unlock = unlocks.get(key);
        return unlock == null || unlock.isUnlocked(player, progression);
    }

    private record Unlock(String personalAdvancement, CommunityGoal communityGoal, String label) {
        static Unlock personal(String advancement, String label) {
            return new Unlock(advancement, null, label);
        }

        static Unlock community(CommunityGoal goal, String label) {
            return new Unlock(null, goal, label);
        }

        boolean isUnlocked(Player player, ProgressionService progression) {
            if (personalAdvancement != null) {
                return progression.has(player, personalAdvancement);
            }
            return communityGoal != null && progression.isCommunityComplete(communityGoal);
        }
    }
}
