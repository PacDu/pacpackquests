package fr.pacdu.pacpackquests.platform;

import fr.pacdu.pacpackquests.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getModName(String modId) {
        if (modId == null || modId.isEmpty() || modId.equals("minecraft")) return "Minecraft";
        if (modId.equals("c")) return "Common";
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getName())
                .orElse(StringUtils.capitalize(modId));
    }
}
