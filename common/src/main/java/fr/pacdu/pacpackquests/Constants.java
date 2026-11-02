package fr.pacdu.pacpackquests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global constant definitions and logger for the PacPack Quests mod.
 */
public final class Constants {

    private Constants() {}

    /** The unique mod identifier. */
    public static final String MOD_ID = "pacpackquests";

    /** The user-facing mod name. */
    public static final String MOD_NAME = "PacPack Quests";

    /** Global SLF4J logger instance for the mod. */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /** The default category name used when no custom categories are specified. */
    public static final String DEFAULT_CATEGORY_NAME = "Main";
}
