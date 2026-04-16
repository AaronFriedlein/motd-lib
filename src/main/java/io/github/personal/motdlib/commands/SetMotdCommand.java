package io.github.personal.motdlib.commands;

import io.github.personal.motdlib.MotdLib;
import io.github.personal.motdlib.model.MotdEntry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Handles the {@code /motd set <random|id|date> [id]} subcommand.
 *
 * <p>Requires the server to be configured with {@code type: fixed} in
 * {@code config.yml}. Overrides the fixed MOTD entry at runtime without
 * touching the config file. The override is cleared on the next
 * {@code /motd reload}.
 */
public final class SetMotdCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final MotdLib plugin;

    public SetMotdCommand(@NotNull MotdLib plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the set subcommand.
     *
     * @param sender the command sender.
     * @param label  the command label (e.g. {@code motd}).
     * @param args   the full argument array from the root command, where
     *               {@code args[0]} is {@code "set"}.
     * @return {@code true} (always handled).
     */
    public boolean handle(@NotNull CommandSender sender,
                          @NotNull String label,
                          @NotNull String[] args) {

        if (!sender.hasPermission("motdlib.set")) {
            sender.sendMessage(MINI.deserialize(
                    "<red>You do not have permission to use this command."));
            return true;
        }

        String type = plugin.getConfig().getString("type", "random");
        if (!"fixed".equalsIgnoreCase(type)) {
            sender.sendMessage(MINI.deserialize(
                    "<red>The server is not using a fixed MOTD. "
                    + "Set <white>type: fixed</white> in config.yml first."));
            return true;
        }

        // args[0] == "set"; args[1] is the mode
        if (args.length < 2) {
            sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " set <random|id|date> [id]"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "random" -> handleRandom(sender);
            case "id"     -> handleId(sender, label, args);
            case "date"   -> handleDate(sender);
            default       -> sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " set <random|id|date> [id]"));
        }
        return true;
    }

    private void handleRandom(@NotNull CommandSender sender) {
        MotdEntry entry = plugin.getMotdManager().getRandomEntry();
        if (entry == null) {
            sender.sendMessage(MINI.deserialize("<red>No MOTD entries are currently loaded."));
            return;
        }
        plugin.getMotdManager().setFixedEntry(entry);
        sender.sendMessage(MINI.deserialize("<green>Fixed MOTD updated to a random entry."));
    }

    private void handleId(@NotNull CommandSender sender,
                          @NotNull String label,
                          @NotNull String[] args) {
        if (args.length < 3 || args[2].isBlank()) {
            sender.sendMessage(MINI.deserialize(
                    "<yellow>Usage: <white>/" + label + " set id <entry-id>"));
            return;
        }
        String id = args[2];
        MotdEntry entry = plugin.getMotdManager().getEntryById(id);
        if (entry == null) {
            sender.sendMessage(MINI.deserialize(
                    "<red>No MOTD entry found with ID '<white>" + id + "<red>'."));
            return;
        }
        plugin.getMotdManager().setFixedEntry(entry);
        sender.sendMessage(MINI.deserialize(
                "<green>Fixed MOTD updated to entry with ID '<white>" + id + "<green>'."));
    }

    private void handleDate(@NotNull CommandSender sender) {
        String today = LocalDate.now().format(DATE_FMT);
        MotdEntry entry = plugin.getMotdManager().getEntryById(today);
        if (entry == null) {
            sender.sendMessage(MINI.deserialize(
                    "<red>No MOTD entry found for today's date ID '<white>" + today + "<red>'."));
            return;
        }
        plugin.getMotdManager().setFixedEntry(entry);
        sender.sendMessage(MINI.deserialize(
                "<green>Fixed MOTD updated to entry for date '<white>" + today + "<green>'."));
    }
}
