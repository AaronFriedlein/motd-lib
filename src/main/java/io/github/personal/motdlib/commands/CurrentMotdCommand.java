package io.github.personal.motdlib.commands;

import io.github.personal.motdlib.MotdLib;
import io.github.personal.motdlib.model.MotdEntry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public final class CurrentMotdCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final MotdLib plugin;

    public CurrentMotdCommand(@NotNull MotdLib plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the current MOTD command, which shows the currently active
     * MOTD entry (message + image) in chat.
     *
     * @param sender the command sender.
     * @param label  the command label (e.g. {@code motd}).
     * @param args   the full argument array from the root command, where
     *               {@code args[0]} is {@code "current"}.
     * @return {@code true} (always handled).
     */
    public boolean handle(@NotNull CommandSender sender,
                          @NotNull String label,
                          @NotNull String[] args) {

        String type = plugin.getConfig().getString("type", "random");

        MotdEntry entry;
        if ("fixed".equalsIgnoreCase(type)) {
            entry = plugin.getMotdManager().getFixedEntry();
            sender.sendMessage(MINI.deserialize(
                    "<blue>Current MOTD:\n" + entry.getMessage() + 
                    "\n<blue>With server icon: " + entry.getImage()));

            sender.sendMessage(MINI.deserialize(
                    "<gray>(with server icon: " + entry.getImage() + ")"));
        } else if ("random".equalsIgnoreCase(type)) {
            sender.sendMessage(MINI.deserialize(
                    "<blue>Currently using random MOTDs!"));
        } else {
            // Catch unrecognized types, shouldnt happen.
            sender.sendMessage(MINI.deserialize(
                    "<red>Unrecognized MOTD type configured: " + type));
        }

        return true;
    }
}