package fr.pacdu.pacpackquests;

import fr.pacdu.pacpackquests.client.ClientPayloadHandler;
import fr.pacdu.pacpackquests.client.PacPackQuestsClient;
import fr.pacdu.pacpackquests.config.ModConfig;
import fr.pacdu.pacpackquests.data.QuestManager;
import fr.pacdu.pacpackquests.event.NeoForgeQuestEventHandler;
import fr.pacdu.pacpackquests.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class PacPackQuests {

    public PacPackQuests(IEventBus modEventBus) {
        Constants.LOG.info("Initializing PacPack Quests...");

        ModConfig.load();

        // Mod bus events
        modEventBus.addListener(this::registerPayloads);

        // Game bus events
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);

        // Register gameplay task listeners
        NeoForgeQuestEventHandler.register();

        // Initialize client if on client physical side
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            PacPackQuestsClient.init(modEventBus);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).versioned("1.0.0").optional();

        // S2C only payloads
        registrar.playToClient(QuestSyncPayload.TYPE, QuestSyncPayload.CODEC, ClientPayloadHandler::handleQuestSync);
        registrar.playToClient(QuestProgressPayload.TYPE, QuestProgressPayload.CODEC, ClientPayloadHandler::handleQuestProgress);
        registrar.playToClient(QuestCompletedPayload.TYPE, QuestCompletedPayload.CODEC, ClientPayloadHandler::handleQuestCompleted);
        registrar.playToClient(SyncRegistryPayload.TYPE, SyncRegistryPayload.CODEC, ClientPayloadHandler::handleSyncRegistry);

        // C2S only payloads
        registrar.playToServer(ClaimQuestPayload.TYPE, ClaimQuestPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleClaimQuest(sp, payload);
                    }
                }));

        registrar.playToServer(MoveQuestPayload.TYPE, MoveQuestPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleMoveQuest(sp, payload);
                    }
                }));

        registrar.playToServer(SaveQuestPayload.TYPE, SaveQuestPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleSaveQuest(sp, payload);
                    }
                }));

        registrar.playToServer(RequestRegistryPayload.TYPE, RequestRegistryPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleRequestRegistry(sp, payload);
                    }
                }));

        // Bidirectional payloads
        registrar.playBidirectional(
                DeleteQuestPayload.TYPE,
                DeleteQuestPayload.CODEC,
                ClientPayloadHandler::handleDeleteQuest,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleDeleteQuest(sp, payload);
                    }
                })
        );

        registrar.playBidirectional(
                CreateCategoryPayload.TYPE,
                CreateCategoryPayload.CODEC,
                ClientPayloadHandler::handleCreateCategory,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleCreateCategory(sp, payload);
                    }
                })
        );

        registrar.playBidirectional(
                DeleteCategoryPayload.TYPE,
                DeleteCategoryPayload.CODEC,
                ClientPayloadHandler::handleDeleteCategory,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleDeleteCategory(sp, payload);
                    }
                })
        );

        registrar.playBidirectional(
                ReorderCategoryPayload.TYPE,
                ReorderCategoryPayload.CODEC,
                ClientPayloadHandler::handleReorderCategory,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleReorderCategory(sp, payload);
                    }
                })
        );

        registrar.playBidirectional(
                EditCategoryPayload.TYPE,
                EditCategoryPayload.CODEC,
                ClientPayloadHandler::handleEditCategory,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        ServerNetworkHandler.handleEditCategory(sp, payload);
                    }
                })
        );
    }

    private void onServerStarting(ServerStartingEvent event) {
        QuestManager.loadQuests(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        ModConfig.save();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ServerNetworkHandler.syncToPlayer(sp);
        }
    }
}
