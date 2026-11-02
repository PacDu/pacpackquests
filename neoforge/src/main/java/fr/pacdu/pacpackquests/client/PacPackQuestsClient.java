package fr.pacdu.pacpackquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.client.config.ClientConfig;
import fr.pacdu.pacpackquests.client.screen.QuestScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class PacPackQuestsClient {

    public static final KeyMapping.Category QUEST_CATEGORY_KEY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "keys"));

    private PacPackQuestsClient() {}

    public static void init(IEventBus modEventBus) {
        ClientQuestData.openQuestMenuKey = new KeyMapping(
                "key.pacpackquests.open-quest-menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                QUEST_CATEGORY_KEY
        );

        modEventBus.addListener(PacPackQuestsClient::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(PacPackQuestsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(PacPackQuestsClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(PacPackQuestsClient::onLoggingOut);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientQuestData.openQuestMenuKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (ClientQuestData.openQuestMenuKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new QuestScreen());
            }
        }
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientConfig.load();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientConfig.save();
        ClientQuestData.CLIENT_DEFINITIONS.clear();
        ClientQuestData.CLIENT_PROGRESS.clear();
        ClientQuestData.CLIENT_FINISHED.clear();
        ClientQuestData.CLIENT_CLAIMED.clear();
        ClientQuestData.CLIENT_CATEGORIES.clear();
        ClientQuestData.SERVER_STRUCTURES.clear();
        ClientQuestData.SERVER_LOOT_TABLES.clear();
    }
}
