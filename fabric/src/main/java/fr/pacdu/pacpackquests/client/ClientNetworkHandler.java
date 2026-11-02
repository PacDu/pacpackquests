package fr.pacdu.pacpackquests.client;

import fr.pacdu.pacpackquests.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkHandler {

    private ClientNetworkHandler() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(QuestSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleQuestSync(payload)));

        ClientPlayNetworking.registerGlobalReceiver(QuestProgressPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleQuestProgress(payload)));

        ClientPlayNetworking.registerGlobalReceiver(DeleteQuestPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleDeleteQuest(payload)));

        ClientPlayNetworking.registerGlobalReceiver(CreateCategoryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleCreateCategory(payload)));

        ClientPlayNetworking.registerGlobalReceiver(DeleteCategoryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleDeleteCategory(payload)));

        ClientPlayNetworking.registerGlobalReceiver(ReorderCategoryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleReorderCategory(payload)));

        ClientPlayNetworking.registerGlobalReceiver(EditCategoryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleEditCategory(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncRegistryPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleSyncRegistry(payload)));

        ClientPlayNetworking.registerGlobalReceiver(QuestCompletedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientQuestData.handleQuestCompleted(payload)));
    }
}
