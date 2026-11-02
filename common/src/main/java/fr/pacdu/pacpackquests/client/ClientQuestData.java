package fr.pacdu.pacpackquests.client;

import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.client.config.ClientConfig;
import fr.pacdu.pacpackquests.client.screen.QuestScreen;
import fr.pacdu.pacpackquests.client.screen.SelectionScreen;
import fr.pacdu.pacpackquests.network.*;
import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.*;

public class ClientQuestData {

    public static KeyMapping openQuestMenuKey;
    public static final Map<String, QuestDefinition> CLIENT_DEFINITIONS = new HashMap<>();
    public static final List<String> CLIENT_CATEGORIES = new ArrayList<>();
    public static final Map<String, Integer> CLIENT_PROGRESS = new HashMap<>();
    public static final Map<String, Boolean> CLIENT_FINISHED = new HashMap<>();
    public static final Map<String, Boolean> CLIENT_CLAIMED = new HashMap<>();

    public static final List<String> SERVER_STRUCTURES = new ArrayList<>();
    public static final List<String> SERVER_LOOT_TABLES = new ArrayList<>();

    private ClientQuestData() {}

    public static void handleQuestSync(QuestSyncPayload payload) {
        Minecraft client = Minecraft.getInstance();
        var registries = client.level != null ? client.level.registryAccess() : (client.getSingleplayerServer() != null ? client.getSingleplayerServer().registryAccess() : null);
        QuestDefinition def = payload.toDefinition(registries);
        CLIENT_DEFINITIONS.put(payload.questId(), def);
        if (!CLIENT_CATEGORIES.contains(def.category())) {
            CLIENT_CATEGORIES.add(def.category());
        }

        if (client.screen instanceof QuestScreen qs) {
            qs.refreshUI();
        }
    }

    public static void handleQuestProgress(QuestProgressPayload payload) {
        Minecraft client = Minecraft.getInstance();
        CLIENT_PROGRESS.put(payload.questId(), payload.progress());
        CLIENT_FINISHED.put(payload.questId(), payload.isFinished());
        CLIENT_CLAIMED.put(payload.questId(), payload.isClaimed());

        if (client.screen instanceof QuestScreen questScreen) {
            questScreen.updateClaimButtonState();
        }
    }

    public static void handleDeleteQuest(DeleteQuestPayload payload) {
        Minecraft client = Minecraft.getInstance();
        CLIENT_DEFINITIONS.remove(payload.questId());
        CLIENT_PROGRESS.remove(payload.questId());
        CLIENT_FINISHED.remove(payload.questId());
        CLIENT_CLAIMED.remove(payload.questId());

        if (client.screen instanceof QuestScreen questScreen) {
            questScreen.refreshUI();
        }
    }

    public static void handleCreateCategory(CreateCategoryPayload payload) {
        if (!CLIENT_CATEGORIES.contains(payload.category())) {
            CLIENT_CATEGORIES.add(payload.category());
        }
    }

    public static void handleDeleteCategory(DeleteCategoryPayload payload) {
        Minecraft client = Minecraft.getInstance();
        CLIENT_DEFINITIONS.entrySet().removeIf(entry -> entry.getValue().category().equals(payload.category()));
        CLIENT_CATEGORIES.remove(payload.category());
        ClientConfig.cameraPositions.remove(payload.category());

        if (client.screen instanceof QuestScreen questScreen) {
            questScreen.removeCategory(payload.category());
        }
    }

    public static void handleReorderCategory(ReorderCategoryPayload payload) {
        Minecraft client = Minecraft.getInstance();
        CLIENT_CATEGORIES.clear();
        CLIENT_CATEGORIES.addAll(payload.categories());
        if (client.screen instanceof QuestScreen questScreen) {
            questScreen.refreshUI();
        }
    }

    public static void handleEditCategory(EditCategoryPayload payload) {
        Minecraft client = Minecraft.getInstance();
        for (QuestDefinition quest : CLIENT_DEFINITIONS.values()) {
            if (quest.category().equals(payload.categoryOld())) {
                QuestDefinition updatedQuest = quest.withCategory(payload.categoryNew());
                CLIENT_DEFINITIONS.put(quest.id(), updatedQuest);
            }
        }

        ClientConfig.CameraState state = ClientConfig.cameraPositions.remove(payload.categoryOld());
        if (state != null) {
            ClientConfig.cameraPositions.put(payload.categoryNew(), state);
        }

        if (client.screen instanceof QuestScreen questScreen) {
            questScreen.renameCategory(payload.categoryOld(), payload.categoryNew());
        }
    }

    public static void handleSyncRegistry(SyncRegistryPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (payload.registryType() == RegistryType.STRUCTURE) {
            SERVER_STRUCTURES.clear();
            SERVER_STRUCTURES.addAll(payload.entries());
            if (client.screen instanceof SelectionScreen screen && screen.getRegistryType() == RegistryType.STRUCTURE) {
                screen.refreshEntries(SERVER_STRUCTURES);
            }
        } else if (payload.registryType() == RegistryType.LOOT_TABLE) {
            SERVER_LOOT_TABLES.clear();
            SERVER_LOOT_TABLES.addAll(payload.entries());
            if (client.screen instanceof SelectionScreen screen && screen.getRegistryType() == RegistryType.LOOT_TABLE) {
                screen.refreshEntries(SERVER_LOOT_TABLES);
            }
        }
    }

    public static void handleQuestCompleted(QuestCompletedPayload payload) {
        Minecraft client = Minecraft.getInstance();
        QuestDefinition quest = CLIENT_DEFINITIONS.get(payload.questId());
        if (quest != null) {
            client.getToastManager().addToast(new QuestCompletedToast(quest));
        }
    }
}
