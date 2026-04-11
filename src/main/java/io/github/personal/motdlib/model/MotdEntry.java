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

    public MotdEntry(@NotNull String message, @Nullable String image) {
        this.message = message;
        this.image = image;
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
}
