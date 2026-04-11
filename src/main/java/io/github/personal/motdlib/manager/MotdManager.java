package io.github.personal.motdlib.manager;

import io.github.personal.motdlib.MotdLib;
import io.github.personal.motdlib.model.MotdEntry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Manages loading, caching, and random selection of MOTD entries and server
 * icon images.
 *
 * <p>Directory layout inside the plugin's data folder:
 * <pre>
 * motds/         - YAML files, each containing a "motds:" list of entries
 * images/        - PNG/JPG server icon files (must be 64x64 px)
 * </pre>
 */
public final class MotdManager {

    private final MotdLib plugin;
    private final Logger log;

    /** filename → ordered list of entries (never null, never empty after load). */
    private volatile Map<String, List<MotdEntry>> motdFiles = Collections.emptyMap();

    /** image filename → pre-loaded BufferedImage. */
    private volatile Map<String, BufferedImage> imageCache = Collections.emptyMap();

    private volatile @Nullable BufferedImage defaultImage;

    public MotdManager(@NotNull MotdLib plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Reloads all MOTD files and image caches from disk. */
    public void reload() {
        Map<String, List<MotdEntry>> newMotdFiles = new HashMap<>();
        Map<String, BufferedImage>  newImageCache = new HashMap<>();

        loadMotdFiles(newMotdFiles);
        loadImages(newMotdFiles, newImageCache);
        BufferedImage newDefault = loadDefaultImage(newImageCache);

        // Swap atomically so the event thread always sees a consistent snapshot.
        motdFiles    = Collections.unmodifiableMap(newMotdFiles);
        imageCache   = Collections.unmodifiableMap(newImageCache);
        defaultImage = newDefault;

        log.info("Loaded " + motdFiles.size() + " MOTD file(s) with "
                + motdFiles.values().stream().mapToInt(List::size).sum() + " total entries.");
    }

    /**
     * Returns a randomly selected {@link MotdEntry} by first choosing a random
     * MOTD file and then a random entry within that file.
     *
     * @return a random entry, or {@code null} if no entries are loaded.
     */
    @Nullable
    public MotdEntry getRandomEntry() {
        Map<String, List<MotdEntry>> snapshot = motdFiles;
        if (snapshot.isEmpty()) return null;

        List<String> keys = new ArrayList<>(snapshot.keySet());
        String chosenFile = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        List<MotdEntry> entries = snapshot.get(chosenFile);
        return entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
    }

    /**
     * Returns the {@link MotdEntry} at the fixed position specified in config
     * ({@code fixed-file} / {@code fixed-index}).
     *
     * @return the fixed entry, or {@code null} if the configured file/index is invalid.
     */
    @Nullable
    public MotdEntry getFixedEntry() {
        String fixedFile  = plugin.getConfig().getString("fixed-file", "default.yml");
        int    fixedIndex = plugin.getConfig().getInt("fixed-index", 0);

        Map<String, List<MotdEntry>> snapshot = motdFiles;
        List<MotdEntry> entries = snapshot.get(fixedFile);
        if (entries == null) {
            log.warning("fixed-file '" + fixedFile + "' not found in motds/ directory.");
            return null;
        }
        if (fixedIndex < 0 || fixedIndex >= entries.size()) {
            log.warning("fixed-index " + fixedIndex + " is out of range for '" + fixedFile
                    + "' (has " + entries.size() + " entries).");
            return null;
        }
        return entries.get(fixedIndex);
    }

    /**
     * Returns the {@link BufferedImage} for the given {@link MotdEntry}.
     * Falls back to the default image when the entry has no image or the file
     * cannot be found.
     *
     * @param entry the MOTD entry whose icon should be resolved.
     * @return the resolved image, or {@code null} if no image is available.
     */
    @Nullable
    public BufferedImage getImageForEntry(@NotNull MotdEntry entry) {
        String imageName = entry.getImage();
        if (imageName != null && !imageName.isBlank()) {
            BufferedImage img = imageCache.get(imageName);
            if (img != null) return img;
        }
        return defaultImage;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void loadMotdFiles(Map<String, List<MotdEntry>> target) {
        File motdsDir = new File(plugin.getDataFolder(), "motds");
        if (!motdsDir.isDirectory()) {
            log.warning("motds/ directory does not exist inside the plugin folder.");
            return;
        }

        File[] files = motdsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("No .yml files found in motds/ directory.");
            return;
        }

        for (File file : files) {
            List<MotdEntry> entries = parseMotdFile(file);
            if (!entries.isEmpty()) {
                target.put(file.getName(), Collections.unmodifiableList(entries));
            }
        }
    }

    private List<MotdEntry> parseMotdFile(File file) {
        List<MotdEntry> entries = new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawList = yaml.getMapList("motds");

        if (rawList.isEmpty()) {
            log.warning("No 'motds' list found in " + file.getName() + " – skipping.");
            return entries;
        }

        for (int i = 0; i < rawList.size(); i++) {
            Map<?, ?> raw = rawList.get(i);
            Object msgObj = raw.get("message");
            if (!(msgObj instanceof String message) || message.isBlank()) {
                log.warning("Entry " + i + " in " + file.getName()
                        + " has no valid 'message' field – skipping.");
                continue;
            }
            Object imgObj = raw.get("image");
            String image  = (imgObj instanceof String s && !s.isBlank()) ? s : null;
            entries.add(new MotdEntry(message, image));
        }
        return entries;
    }

    /**
     * Pre-loads every image that is referenced by at least one entry plus the
     * default image, so the event thread never has to touch the filesystem.
     */
    private void loadImages(Map<String, List<MotdEntry>> motds,
                            Map<String, BufferedImage> imageTarget) {
        // Collect all referenced image names.
        for (List<MotdEntry> entries : motds.values()) {
            for (MotdEntry e : entries) {
                String name = e.getImage();
                if (name != null && !imageTarget.containsKey(name)) {
                    BufferedImage img = readImageFile(name);
                    if (img != null) imageTarget.put(name, img);
                }
            }
        }
    }

    private @Nullable BufferedImage loadDefaultImage(Map<String, BufferedImage> imageTarget) {
        String defaultName = plugin.getConfig().getString("default-image", "");
        if (defaultName == null || defaultName.isBlank()) return null;

        // May already be cached from the entry scan.
        if (imageTarget.containsKey(defaultName)) return imageTarget.get(defaultName);

        BufferedImage img = readImageFile(defaultName);
        if (img != null) {
            imageTarget.put(defaultName, img);
            log.info("Default image loaded: " + defaultName);
        }
        return img;
    }

    /**
     * Reads and validates a single image file from the images/ directory.
     * Returns {@code null} and logs a warning on any problem.
     */
    private @Nullable BufferedImage readImageFile(String filename) {
        if (!isValidImageExtension(filename)) {
            log.warning("Unsupported image extension for '" + filename
                    + "'. Only PNG and JPG are supported.");
            return null;
        }

        File imageFile = new File(plugin.getDataFolder(),
                "images" + File.separator + filename);
        if (!imageFile.exists()) {
            log.warning("Image file not found: " + imageFile.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img == null) {
                log.warning("Could not decode image: " + filename);
                return null;
            }
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                log.warning("Image '" + filename + "' is " + img.getWidth() + "x"
                        + img.getHeight() + " – server icons must be exactly 64x64 pixels.");
                return null;
            }
            return img;
        } catch (IOException e) {
            log.severe("Error reading image '" + filename + "': " + e.getMessage());
            return null;
        }
    }

    private boolean isValidImageExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot + 1);
        return ext.equalsIgnoreCase("png")
                || ext.equalsIgnoreCase("jpg")
                || ext.equalsIgnoreCase("jpeg");
    }
}
