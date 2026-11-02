package fr.pacdu.pacpackquests.util;

import fr.pacdu.pacpackquests.platform.Services;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class providing path validation, canonicalization, and security checks
 * against path traversal attacks and reserved filesystem names.
 */
public final class PathSecurity {

    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9_]{1,64}$");

    private static final Set<String> RESERVED_WINDOWS_NAMES = Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    );

    private PathSecurity() {}

    /**
     * Checks if the given identifier is safe to be used in file and directory names.
     * Must be 1-64 characters matching [a-z0-9_], and not a reserved Windows device name.
     *
     * @param id The identifier string to check.
     * @return {@code true} if valid and safe; {@code false} otherwise.
     */
    public static boolean isValidIdentifier(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (!VALID_IDENTIFIER_PATTERN.matcher(id).matches()) {
            return false;
        }
        return !RESERVED_WINDOWS_NAMES.contains(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the absolute, normalized root directory for quests.
     *
     * @return The canonical {@link Path} to the quests root directory.
     */
    public static Path getQuestsRootDir() {
        return Services.PLATFORM.getConfigDirectory().resolve("pacpackquests/quests").toAbsolutePath().normalize();
    }

    /**
     * Checks if child path is strictly inside parent path (not equal, no traversal escaping).
     *
     * @param parent The trusted base directory path.
     * @param child The candidate target path to verify.
     * @return {@code true} if child is strictly inside parent; {@code false} otherwise.
     */
    public static boolean isPathContained(Path parent, Path child) {
        if (parent == null || child == null) {
            return false;
        }
        Path normParent = parent.toAbsolutePath().normalize();
        Path normChild = child.toAbsolutePath().normalize();
        return normChild.startsWith(normParent) && !normChild.equals(normParent);
    }

    /**
     * Returns the safe category directory Path if valid, or empty Optional if unsafe or invalid.
     *
     * @param category The category identifier to resolve.
     * @return An {@link Optional} holding the validated directory {@link Path}, or empty if invalid.
     */
    public static Optional<Path> getSafeCategoryDir(String category) {
        if (!isValidIdentifier(category)) {
            return Optional.empty();
        }
        Path root = getQuestsRootDir();
        Path resolved = root.resolve(category).toAbsolutePath().normalize();
        if (!isPathContained(root, resolved)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    /**
     * Returns the safe quest JSON file Path if both category and questId are valid,
     * or empty Optional if unsafe or invalid.
     *
     * @param category The category identifier.
     * @param questId The quest identifier.
     * @return An {@link Optional} holding the validated JSON {@link Path}, or empty if invalid.
     */
    public static Optional<Path> getSafeQuestFile(String category, String questId) {
        if (!isValidIdentifier(questId)) {
            return Optional.empty();
        }
        Optional<Path> categoryDir = getSafeCategoryDir(category);
        if (categoryDir.isEmpty()) {
            return Optional.empty();
        }
        Path catDir = categoryDir.get();
        Path questFile = catDir.resolve(questId + ".json").toAbsolutePath().normalize();
        if (!isPathContained(catDir, questFile)) {
            return Optional.empty();
        }
        return Optional.of(questFile);
    }

    /**
     * Formats an arbitrary string to be safe for filenames and filesystem paths:
     * converts to lowercase, decomposes accents/diacritics, and removes invalid characters.
     *
     * @param str The raw input string.
     * @return The sanitized, path-safe string.
     */
    public static String formatStringToValidPath(String str) {
        String formatted = str.toLowerCase(Locale.ROOT)
                .replace("œ", "oe")
                .replace("æ", "ae");
        formatted = Normalizer.normalize(formatted, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_]", "");
        return formatted;
    }
}
