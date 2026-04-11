package io.github.personal.motdlib;

import io.github.personal.motdlib.commands.ReloadCommand;
import io.github.personal.motdlib.events.PingListener;
import io.github.personal.motdlib.manager.MotdManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

/**
 * Main entry point for MotdPlugin.
 *
 * <p>On enable the plugin:
 * <ol>
 *   <li>Saves the default {@code config.yml} if it does not yet exist.</li>
 *   <li>Creates the {@code motds/} and {@code images/} sub-directories and
 *       copies the bundled example files if they do not yet exist.</li>
 *   <li>Loads all MOTD files and icon images via {@link MotdManager}.</li>
 *   <li>Registers the {@link PingListener} and the {@code /motd} command.</li>
 * </ol>
 */
public final class MotdLib extends JavaPlugin {

    private MotdManager motdManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDefaultFiles();

        motdManager = new MotdManager(this);
        motdManager.reload();

        getServer().getPluginManager().registerEvents(new PingListener(this), this);

        ReloadCommand reloadCommand = new ReloadCommand(this);
        Objects.requireNonNull(getCommand("motd")).setExecutor(reloadCommand);
        Objects.requireNonNull(getCommand("motd")).setTabCompleter(reloadCommand);

        getLogger().info("MotdLib v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MotdLib disabled.");
    }

    public MotdManager getMotdManager() {
        return motdManager;
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private void createDefaultFiles() {
        File motdsDir  = new File(getDataFolder(), "motds");
        File imagesDir = new File(getDataFolder(), "images");

        if (!motdsDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            motdsDir.mkdirs();
            // Copy the bundled example file so the server has something to work
            // with out of the box.
            saveResource("motds/default.yml", false);
            getLogger().info("Created motds/ directory with default.yml example.");
        }

        if (!imagesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            imagesDir.mkdirs();
            getLogger().info("Created images/ directory. Add your 64x64 PNG/JPG icons here.");
        }
    }
}
