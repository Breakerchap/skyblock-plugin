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
    public static final List<String> REGISTERED_RECIPE_IDS = List.of(
        "gravel_from_cobblestone",
        "sand_from_gravel",
        "dirt_cultivation",
        "calcite_from_quartz",
        "bell",
        "wayfarer_bell",
        "void_trowel"
    );

    private final SkyblockPlugin plugin;
    private final ProgressionService progression;
    private final NamespacedKey wayfarerBellTag;
    private final NamespacedKey voidTrowelTag;
    private final Map<NamespacedKey, Unlock> unlocks = new LinkedHashMap<>();

    public RecipeManager(SkyblockPlugin plugin, ProgressionService progression) {
        this.plugin = plugin;
        this.progression = progression;
        this.wayfarerBellTag = new NamespacedKey(plugin, "wayfarer_bell");
        this.voidTrowelTag = new NamespacedKey(plugin, "void_trowel");
    }

    public void registerAll() {
        registerGravel();
        registerSand();
        registerDirt();
        registerCalcite();
        registerBell();
        registerWayfarerBell();
        registerVoidTrowel();
        Bukkit.updateRecipes();
        verifyRegisteredRecipes();
    }

    private void registerGravel() {
        NamespacedKey key = key("gravel_from_cobblestone");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.GRAVEL));
        recipe.setGroup("skyblock");
        recipe.addIngredient(4, Material.COBBLESTONE);
        register(key, recipe, Unlock.personal("getting_started/cobblestone", "Gravel from Cobblestone"));
    }

    private void registerSand() {
        NamespacedKey key = key("sand_from_gravel");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.SAND));
        recipe.setGroup("skyblock");
        recipe.addIngredient(2, Material.GRAVEL);
        register(key, recipe, Unlock.community(CommunityGoal.COBBLE, "Sand from Gravel"));
    }

    private void registerDirt() {
        NamespacedKey key = key("dirt_cultivation");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.DIRT, 2));
        recipe.setGroup("skyblock");
        recipe.shape("FFF", "FDF", "FFF");
        recipe.setIngredient('F', Material.ROTTEN_FLESH);
        recipe.setIngredient('D', Material.DIRT);
        register(key, recipe, Unlock.community(CommunityGoal.LIFE, "Dirt Cultivation"));
    }

    private void registerCalcite() {
        NamespacedKey key = key("calcite_from_quartz");
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.CALCITE, 2));
        recipe.setGroup("skyblock");
        recipe.addIngredient(2, Material.QUARTZ);
        recipe.addIngredient(2, Material.BONE_MEAL);
        register(key, recipe, Unlock.personal("nether/root", "Calcite"));
    }

    private void registerBell() {
        NamespacedKey key = key("bell");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.BELL));
        recipe.setGroup("skyblock");
        recipe.shape("GIG", " S ");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        register(key, recipe, Unlock.always("Bell"));
    }

    private void registerWayfarerBell() {
        NamespacedKey key = key("wayfarer_bell");
        ShapedRecipe recipe = new ShapedRecipe(key, createWayfarerBell());
        recipe.setGroup("skyblock");
        recipe.shape("EGE", "GBG", "EGE");
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('B', Material.BELL);
        register(key, recipe, Unlock.always("Wayfarer's Bell"));
    }

    private void registerVoidTrowel() {
        NamespacedKey key = key("void_trowel");
        ShapedRecipe recipe = new ShapedRecipe(key, createVoidTrowel());
        recipe.setGroup("skyblock");
        recipe.shape("I", "S");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        register(key, recipe, Unlock.always("Void Trowel"));
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    private void register(NamespacedKey key, Recipe recipe, Unlock unlock) {
        Bukkit.removeRecipe(key);
        if (!Bukkit.addRecipe(recipe)) {
            throw new IllegalStateException("Paper rejected custom recipe " + key);
        }
        unlocks.put(key, unlock);
    }

    private void verifyRegisteredRecipes() {
        for (String id : REGISTERED_RECIPE_IDS) {
            NamespacedKey key = key(id);
            if (Bukkit.getRecipe(key) == null) {
                throw new IllegalStateException("Custom recipe was not present after registration: " + key);
            }
        }
        plugin.getLogger().info("Verified " + REGISTERED_RECIPE_IDS.size() + " custom crafting recipes.");
    }

    public ItemStack createWayfarerBell() {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Wayfarer's Bell", NamedTextColor.GOLD));
        meta.lore(List.of(
            Component.text("Ring under the open sky to call a wandering trader.", NamedTextColor.GRAY),
            Component.text("Reusable. Ring it as often as you like.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(wayfarerBellTag, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createVoidTrowel() {
        ItemStack item = new ItemStack(Material.BRUSH);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Void Trowel", NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("Offhand this; hold blocks in your main hand.", NamedTextColor.GRAY),
            Component.text("Right-click to extend the bridge in front of your feet.", NamedTextColor.GRAY),
            Component.text("Java bridging, minus the neck pain.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(voidTrowelTag, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWayfarerBell(ItemStack item) {
        return hasMarker(item, Material.BELL, wayfarerBellTag);
    }

    public boolean isVoidTrowel(ItemStack item) {
        return hasMarker(item, Material.BRUSH, voidTrowelTag);
    }

    private boolean hasMarker(ItemStack item, Material material, NamespacedKey key) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
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

    private record Unlock(boolean always, String personalAdvancement, CommunityGoal communityGoal, String label) {
        static Unlock always(String label) {
            return new Unlock(true, null, null, label);
        }

        static Unlock personal(String advancement, String label) {
            return new Unlock(false, advancement, null, label);
        }

        static Unlock community(CommunityGoal goal, String label) {
            return new Unlock(false, null, goal, label);
        }

        boolean isUnlocked(Player player, ProgressionService progression) {
            if (always) {
                return true;
            }
            if (personalAdvancement != null) {
                return progression.has(player, personalAdvancement);
            }
            return communityGoal != null && progression.isCommunityComplete(communityGoal);
        }
    }
}
