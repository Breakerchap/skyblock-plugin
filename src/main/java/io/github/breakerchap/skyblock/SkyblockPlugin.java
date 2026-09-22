package io.github.breakerchap.skyblock;

import io.github.breakerchap.skyblock.advancement.AdvancementManager;
import io.github.breakerchap.skyblock.command.SkyblockCommand;
import io.github.breakerchap.skyblock.island.IslandManager;
import io.github.breakerchap.skyblock.progress.ProgressListener;
import io.github.breakerchap.skyblock.progress.ProgressStore;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import io.github.breakerchap.skyblock.recipe.RecipeManager;
import io.github.breakerchap.skyblock.trader.TraderManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyblockPlugin extends JavaPlugin {
    private ProgressStore store;
    private AdvancementManager advancements;
    private ProgressionService progression;
    private RecipeManager recipes;
    private IslandManager islands;
    private TraderManager traders;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.store = new ProgressStore(this);
        this.advancements = new AdvancementManager(this);
        advancements.registerAll();

        this.progression = new ProgressionService(this, store, advancements);
        this.recipes = new RecipeManager(this, progression);
        progression.setRecipeManager(recipes);
        recipes.registerAll();

        this.islands = new IslandManager(this, store, progression);
        this.traders = new TraderManager(this, store, recipes);

        Bukkit.getPluginManager().registerEvents(new ProgressListener(this, progression), this);
        Bukkit.getPluginManager().registerEvents(recipes, this);
        Bukkit.getPluginManager().registerEvents(islands, this);
        Bukkit.getPluginManager().registerEvents(traders, this);

        SkyblockCommand commandHandler = new SkyblockCommand(this, progression, islands, traders);
        PluginCommand command = getCommand("skyblock");
        if (command == null) {
            throw new IllegalStateException("skyblock command is missing from plugin.yml");
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        Bukkit.getScheduler().runTaskTimer(this, store::saveIfDirty, 1200L, 1200L);

        if (getConfig().getBoolean("islands.auto-generate", true)) {
            Bukkit.getScheduler().runTaskLater(this, () -> islands.generateAll(false), 20L);
        }

        Bukkit.getScheduler().runTask(this, () ->
            Bukkit.getOnlinePlayers().forEach(progression::syncPlayer)
        );

        getLogger().info("Skyblock Progression enabled: custom advancements, recipes, traders and exploration islands are active.");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }
}
