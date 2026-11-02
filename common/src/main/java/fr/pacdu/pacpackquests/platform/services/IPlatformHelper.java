package fr.pacdu.pacpackquests.platform.services;

import java.nio.file.Path;

/**
 * Service Provider Interface (SPI) abstracting environment and mod-loader specific operations.
 */
public interface IPlatformHelper {

    /**
     * Returns the human-readable name of the running mod loader (e.g. "Fabric" or "NeoForge").
     *
     * @return The loader platform name string.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given ID is present in the current environment.
     *
     * @param modId The mod identifier to check.
     * @return {@code true} if the mod is loaded; {@code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Checks if the game is running within a development environment.
     *
     * @return {@code true} in dev environments; {@code false} in production builds.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Returns the global configuration directory for the game instance.
     *
     * @return The {@link Path} to the config directory.
     */
    Path getConfigDirectory();

    /**
     * Retrieves the display name of a mod from its identifier.
     *
     * @param modId The mod ID.
     * @return The formatted mod display name.
     */
    default String getModName(String modId) {
        if (modId == null || modId.isEmpty() || modId.equals("minecraft")) return "Minecraft";
        if (modId.equals("c")) return "Common";
        return modId;
    }
}
