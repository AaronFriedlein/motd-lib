package io.github.personal.motdlib.commands;

import io.github.personal.motdlib.MotdLib;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Root executor for the {@code /motd} command. Dispatches to subcommand
 * handlers based on {@code args[0]}:
 * <ul>
 *   <li>{@code /motd reload} – reloads config and all MOTD files from disk.</li>
 *   <li>{@code /motd set <random|id|date> [id]} – updates the active fixed MOTD at runtime.</li>
 * </ul>
 */
public final class ReloadCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final MotdLib plugin;
    private final SetMotdCommand setMotdCommand;

    public ReloadCommand(MotdLib plugin) {
        this.plugin = plugin;
        this.setMotdCommand = new SetMotdCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " <reload|set>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("motdlib.reload")) {
                    sender.sendMessage(MINI.deserialize(
                            "<red>You do not have permission to use this command."));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getMotdManager().reload();
                sender.sendMessage(MINI.deserialize(
                        "<green>MotdLib reloaded successfully."));
            }
            case "set" -> setMotdCommand.handle(sender, label, args);
            default -> sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " <reload|set>"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "set");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return List.of("random", "id", "date");
        }
        return List.of();
    }
}
