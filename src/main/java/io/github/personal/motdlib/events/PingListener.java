package io.github.personal.motdlib.events;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.github.personal.motdlib.MotdLib;
import io.github.personal.motdlib.model.MotdEntry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.CachedServerIcon;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Listens for server-list ping events and applies the selected MOTD entry
 * (message + icon).
 */
public final class PingListener implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final MotdLib plugin;
    private final Logger log;

    public PingListener(MotdLib plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        String type = plugin.getConfig().getString("type", "random");

        MotdEntry entry;
        if ("fixed".equalsIgnoreCase(type)) {
            entry = plugin.getMotdManager().getFixedEntry();
        } else {
            // Default to random for any unrecognised value.
            entry = plugin.getMotdManager().getRandomEntry();
        }

        if (entry == null) return;

        // Apply MiniMessage-formatted MOTD text.
        event.motd(MINI_MESSAGE.deserialize(entry.getMessage()));

        // Resolve and apply the server icon.
        BufferedImage image = plugin.getMotdManager().getImageForEntry(entry);
        if (image != null) {
            try {
                CachedServerIcon icon = Bukkit.loadServerIcon(image);
                event.setServerIcon(icon);
            } catch (Exception e) {
                log.severe("Failed to set server icon: " + e.getMessage());
            }
        }
    }
}
