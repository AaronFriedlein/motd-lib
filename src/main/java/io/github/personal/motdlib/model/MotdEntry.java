package io.github.personal.motdlib.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single MOTD entry parsed from a motds/ YAML file.
 * Each entry holds a MiniMessage-formatted MOTD string and an optional
 * image filename that refers to a file inside the images/ directory.
 */
public final class MotdEntry {

    private final @NotNull String message;
    private final @Nullable String image;
    private final @Nullable String id;

    public MotdEntry(@NotNull String message, @Nullable String image, @Nullable String id) {
        this.message = message;
        this.image = image;
        this.id = id;
    }

    /** Returns the MiniMessage-formatted MOTD string. */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * Returns the image filename relative to the images/ directory,
     * or {@code null} when no image was specified for this entry.
     */
    @Nullable
    public String getImage() {
        return image;
    }

    /**
     * Returns the optional unique ID for this entry, or {@code null} if none was specified.
     */
    @Nullable
    public String getId() {
        return id;
    }
}
