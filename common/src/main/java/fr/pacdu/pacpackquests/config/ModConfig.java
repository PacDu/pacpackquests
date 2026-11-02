package fr.pacdu.pacpackquests.config;

import com.google.gson.*;
import fr.pacdu.pacpackquests.PacPackQuests;
import fr.pacdu.pacpackquests.util.PathSecurity;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static fr.pacdu.pacpackquests.PacPackQuests.DEFAULT_CATEGORY_NAME;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pacpackquests/categories.json");

    public static List<String> categoryOrder = new ArrayList<>();
    public static int maxGridColumns = 20;
    public static int maxGridRows = 20;

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        if (json.has("categoryOrder") && json.get("categoryOrder").isJsonArray()) {
                            categoryOrder.clear();
                            JsonArray categoryOrderArray = json.getAsJsonArray("categoryOrder");
                            for (JsonElement element : categoryOrderArray) {
                                String cat = element.getAsString();
                                if (PathSecurity.isValidIdentifier(cat)) {
                                    categoryOrder.add(cat);
                                }
                            }
                        }
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            PacPackQuests.LOGGER.error("Failed to load categories config.", e);
        }
    }

    public static void save() {
        try {
            if (categoryOrder.isEmpty()) categoryOrder.add(DEFAULT_CATEGORY_NAME);
            Files.createDirectories(CONFIG_FILE.getParent());
            JsonObject json = new JsonObject();
            json.add("categoryOrder", GSON.toJsonTree(categoryOrder));

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            PacPackQuests.LOGGER.error("Failed to save categories config.", e);
        }
    }
}