package fr.pacdu.pacpackquests.client;

import fr.pacdu.pacpackquests.network.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    private ClientPayloadHandler() {}

    public static void handleQuestSync(QuestSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleQuestSync(payload));
    }

    public static void handleQuestProgress(QuestProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleQuestProgress(payload));
    }

    public static void handleDeleteQuest(DeleteQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleDeleteQuest(payload));
    }

    public static void handleCreateCategory(CreateCategoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleCreateCategory(payload));
    }

    public static void handleDeleteCategory(DeleteCategoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleDeleteCategory(payload));
    }

    public static void handleReorderCategory(ReorderCategoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleReorderCategory(payload));
    }

    public static void handleEditCategory(EditCategoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleEditCategory(payload));
    }

    public static void handleSyncRegistry(SyncRegistryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleSyncRegistry(payload));
    }

    public static void handleQuestCompleted(QuestCompletedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestData.handleQuestCompleted(payload));
    }
}
