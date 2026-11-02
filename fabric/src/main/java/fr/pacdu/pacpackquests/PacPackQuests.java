package fr.pacdu.pacpackquests;

import fr.pacdu.pacpackquests.config.ModConfig;
import fr.pacdu.pacpackquests.data.QuestManager;
import fr.pacdu.pacpackquests.event.FabricQuestEventHandler;
import fr.pacdu.pacpackquests.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacPackQuests implements ModInitializer {

	@Override
	public void onInitialize() {
		Constants.LOG.info("Initializing PacPack Quests...");

		ModConfig.load();
		ServerLifecycleEvents.SERVER_STARTING.register(QuestManager::loadQuests);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ModConfig.save());

		// Register S2C payloads
		PayloadTypeRegistry.playS2C().register(QuestSyncPayload.TYPE, QuestSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(QuestProgressPayload.TYPE, QuestProgressPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeleteQuestPayload.TYPE, DeleteQuestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CreateCategoryPayload.TYPE, CreateCategoryPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeleteCategoryPayload.TYPE, DeleteCategoryPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ReorderCategoryPayload.TYPE, ReorderCategoryPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EditCategoryPayload.TYPE, EditCategoryPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(QuestCompletedPayload.TYPE, QuestCompletedPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncRegistryPayload.TYPE, SyncRegistryPayload.CODEC);

		// Register C2S payloads
		PayloadTypeRegistry.playC2S().register(ClaimQuestPayload.TYPE, ClaimQuestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MoveQuestPayload.TYPE, MoveQuestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SaveQuestPayload.TYPE, SaveQuestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DeleteQuestPayload.TYPE, DeleteQuestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CreateCategoryPayload.TYPE, CreateCategoryPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DeleteCategoryPayload.TYPE, DeleteCategoryPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ReorderCategoryPayload.TYPE, ReorderCategoryPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EditCategoryPayload.TYPE, EditCategoryPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestRegistryPayload.TYPE, RequestRegistryPayload.CODEC);

		// Register C2S Server Packet Receivers
		ServerPlayNetworking.registerGlobalReceiver(ClaimQuestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleClaimQuest(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(MoveQuestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleMoveQuest(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(SaveQuestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleSaveQuest(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(DeleteQuestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleDeleteQuest(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(CreateCategoryPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleCreateCategory(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(DeleteCategoryPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleDeleteCategory(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(ReorderCategoryPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleReorderCategory(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(EditCategoryPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleEditCategory(context.player(), payload)));

		ServerPlayNetworking.registerGlobalReceiver(RequestRegistryPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerNetworkHandler.handleRequestRegistry(context.player(), payload)));

		// Connection Event: Sync all quests and category order when a player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerNetworkHandler.syncToPlayer(handler.player);
		});

		// Register gameplay task listeners
		FabricQuestEventHandler.register();
	}
}
