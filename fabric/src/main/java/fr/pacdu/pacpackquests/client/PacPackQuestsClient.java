package fr.pacdu.pacpackquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.client.config.ClientConfig;
import fr.pacdu.pacpackquests.client.screen.QuestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class PacPackQuestsClient implements ClientModInitializer {

	public static final String QUEST_CATEGORY_KEY = "key.categories." + Constants.MOD_ID;

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientConfig.load());

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientConfig.save();
			ClientQuestData.CLIENT_DEFINITIONS.clear();
			ClientQuestData.CLIENT_PROGRESS.clear();
			ClientQuestData.CLIENT_FINISHED.clear();
			ClientQuestData.CLIENT_CLAIMED.clear();
			ClientQuestData.CLIENT_CATEGORIES.clear();
			ClientQuestData.SERVER_STRUCTURES.clear();
			ClientQuestData.SERVER_LOOT_TABLES.clear();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientConfig.save());

		ClientQuestData.openQuestMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.pacpackquests.open-quest-menu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				QUEST_CATEGORY_KEY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ClientQuestData.openQuestMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new QuestScreen());
				}
			}
		});

		ClientNetworkHandler.register();
	}
}
