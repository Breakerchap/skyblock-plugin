package io.github.breakerchap.skyblock.command;

import io.github.breakerchap.skyblock.SkyblockPlugin;
import io.github.breakerchap.skyblock.island.IslandDefinition;
import io.github.breakerchap.skyblock.island.IslandManager;
import io.github.breakerchap.skyblock.progress.CommunityGoal;
import io.github.breakerchap.skyblock.progress.ProgressionService;
import io.github.breakerchap.skyblock.trader.TraderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkyblockCommand implements CommandExecutor, TabCompleter {
    private final SkyblockPlugin plugin;
    private final ProgressionService progression;
    private final IslandManager islands;
    private final TraderManager traders;

    public SkyblockCommand(
        SkyblockPlugin plugin,
        ProgressionService progression,
        IslandManager islands,
        TraderManager traders
    ) {
        this.plugin = plugin;
        this.progression = progression;
        this.islands = islands;
        this.traders = traders;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("progress")) {
            showProgress(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("islands")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("generate")) {
                if (!requireAdmin(sender)) {
                    return true;
                }
                boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
                int generated = islands.generateAll(force);
                sender.sendMessage("Generated " + generated + " exploration islands.");
                return true;
            }
            listIslands(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("trader")) {
            if (!requireAdmin(sender)) {
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Run this command in-game.");
                return true;
            }
            traders.summonFor(player, true);
            return true;
        }

        if (args[0].equalsIgnoreCase("grant")) {
            if (!requireAdmin(sender)) {
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("Usage: /skyblock grant <player> <advancement-id>");
                return true;
            }

            Player player = Bukkit.getPlayerExact(args[1]);
            if (player == null) {
                sender.sendMessage("That player is not online.");
                return true;
            }

            String id = args[2].toLowerCase(Locale.ROOT);
            if (!progression.advancements().exists(id)) {
                sender.sendMessage("Unknown advancement id: " + id);
                return true;
            }

            boolean granted = progression.grant(player, id);
            sender.sendMessage(granted ? "Granted skyblock:" + id : "That advancement was already complete.");
            return true;
        }

        sender.sendMessage("Usage: /skyblock [progress|islands|trader|grant]");
        return true;
    }

    private void showProgress(CommandSender sender) {
        sender.sendMessage(Component.text("Skyblock community progress", NamedTextColor.GOLD));

        for (CommunityGoal goal : CommunityGoal.values()) {
            long current = progression.counter(goal);
            long target = progression.threshold(goal);
            NamedTextColor colour = progression.isCommunityComplete(goal)
                ? NamedTextColor.GREEN
                : NamedTextColor.GRAY;

            sender.sendMessage(Component.text(
                " • " + goal.displayName() + ": " + Math.min(current, target) + " / " + target,
                colour
            ));
        }

        if (sender instanceof Player player) {
            sender.sendMessage(Component.text(
                "Your advancements: " + progression.advancements().completedCount(player)
                    + " / " + progression.advancements().totalCount(),
                NamedTextColor.AQUA
            ));
        }
    }

    private void listIslands(CommandSender sender) {
        if (!sender.hasPermission("skyblock.admin")) {
            sender.sendMessage(Component.text(
                "There are " + islands.definitions().size() + " exploration islands somewhere in the void. Go find them.",
                NamedTextColor.AQUA
            ));
            return;
        }

        sender.sendMessage(Component.text("Exploration islands", NamedTextColor.AQUA));
        for (IslandDefinition definition : islands.definitions()) {
            Location location = islands.location(definition);
            sender.sendMessage(Component.text(
                " • " + definition.displayName() + " — "
                    + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(),
                NamedTextColor.GRAY
            ));
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("skyblock.admin")) {
            return true;
        }
        sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(List.of("progress", "islands", "trader", "grant"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("islands")) {
            return prefix(List.of("generate"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("islands") && args[1].equalsIgnoreCase("generate")) {
            return prefix(List.of("force"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("grant")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grant")) {
            return prefix(progression.advancements().ids(), args[2]);
        }
        return List.of();
    }

    private List<String> prefix(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
