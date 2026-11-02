package fr.pacdu.pacpackquests.client.config;

import com.google.gson.*;
import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.util.PathSecurity;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("pacpackquests/client.json");

    public static Map<String, CameraState> cameraPositions = new HashMap<>();
    public record CameraState(double panX, double panY, float zoom) {}

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        if (json.has("cameraPositions") && json.get("cameraPositions").isJsonObject()) {
                            cameraPositions.clear();
                            JsonObject camObj = json.getAsJsonObject("cameraPositions");
                            for (Map.Entry<String, JsonElement> entry : camObj.entrySet()) {
                                String cat = entry.getKey();
                                if (PathSecurity.isValidIdentifier(cat) && entry.getValue().isJsonObject()) {
                                    JsonObject c = entry.getValue().getAsJsonObject();
                                    double pX = c.has("panX") ? c.get("panX").getAsDouble() : 0.0;
                                    double pY = c.has("panY") ? c.get("panY").getAsDouble() : 0.0;
                                    float z = c.has("zoom") ? c.get("zoom").getAsFloat() : 1.0f;
                                    cameraPositions.put(cat, new CameraState(pX, pY, z));
                                }
                            }
                        }
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load client config.", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            JsonObject json = new JsonObject();

            if (!cameraPositions.isEmpty()) {
                JsonObject camObj = new JsonObject();
                for (Map.Entry<String, CameraState> entry : cameraPositions.entrySet()) {
                    JsonObject c = new JsonObject();
                    c.addProperty("panX", entry.getValue().panX());
                    c.addProperty("panY", entry.getValue().panY());
                    c.addProperty("zoom", entry.getValue().zoom());
                    camObj.add(entry.getKey(), c);
                }
                json.add("cameraPositions", camObj);
            }

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save client config.", e);
        }
    }
}
