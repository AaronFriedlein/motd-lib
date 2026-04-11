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
 * Handles the {@code /motd reload} command, which reloads {@code config.yml}
 * and all MOTD files and images from disk.
 */
public final class ReloadCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final MotdLib plugin;

    public ReloadCommand(MotdLib plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("motdlib.reload")) {
            sender.sendMessage(MINI.deserialize(
                    "<red>You do not have permission to use this command."));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " reload"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getMotdManager().reload();
        sender.sendMessage(MINI.deserialize(
                "<green>MotdLib reloaded successfully."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
