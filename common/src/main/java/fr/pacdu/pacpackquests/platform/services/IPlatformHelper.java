package fr.pacdu.pacpackquests.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
    Path getConfigDirectory();

    default String getModName(String modId) {
        if (modId == null || modId.isEmpty() || modId.equals("minecraft")) return "Minecraft";
        if (modId.equals("c")) return "Common";
        return modId;
    }
}
