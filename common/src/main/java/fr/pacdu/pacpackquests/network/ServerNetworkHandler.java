package fr.pacdu.pacpackquests.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.config.ModConfig;
import fr.pacdu.pacpackquests.data.QuestManager;
import fr.pacdu.pacpackquests.data.QuestState;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.reward.QuestReward;
import fr.pacdu.pacpackquests.util.PathSecurity;
import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static fr.pacdu.pacpackquests.Constants.DEFAULT_CATEGORY_NAME;

public class ServerNetworkHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ServerNetworkHandler() {}

    private static boolean isOp(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server != null && server.getPlayerList().isOp(player.getGameProfile());
    }

    public static void handleClaimQuest(ServerPlayer player, ClaimQuestPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        QuestState questState = QuestState.getServerState(server);
        UUID playerId = player.getUUID();

        QuestDefinition quest = QuestManager.LOADED_QUESTS.get(payload.questId());
        if (quest != null) {
            int progress = questState.getProgress(playerId, quest.id());
            boolean claimed = questState.isClaimed(playerId, quest.id());

            if (progress >= quest.requiredAmount() && !claimed) {
                if (quest.rewards() != null) {
                    for (QuestReward reward : quest.rewards()) {
                        reward.grant(player, server.registryAccess());
                    }
                }
                questState.setClaimed(playerId, quest.id(), true);
                Services.NETWORK.sendToPlayer(player, new QuestProgressPayload(quest.id(), progress, true, true));
            }
        }
    }

    public static void handleMoveQuest(ServerPlayer player, MoveQuestPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;
        if (payload.newX() < 0 || payload.newY() < 0 || payload.newX() >= ModConfig.maxGridColumns || payload.newY() >= ModConfig.maxGridRows) return;

        QuestDefinition quest = QuestManager.LOADED_QUESTS.get(payload.questId());
        if (quest == null) return;

        Optional<Path> questFileOpt = PathSecurity.getSafeQuestFile(quest.category(), payload.questId());
        if (questFileOpt.isEmpty()) {
            Constants.LOG.warn("Blocked unsafe quest move attempt: category={}, id={}", quest.category(), payload.questId());
            return;
        }

        Path questFile = questFileOpt.get();
        if (Files.exists(questFile)) {
            try {
                JsonObject json = GSON.fromJson(Files.readString(questFile), JsonObject.class);
                json.addProperty("displayX", payload.newX());
                json.addProperty("displayY", payload.newY());
                Files.writeString(questFile, GSON.toJson(json));

                QuestDefinition newDef = quest.withPosition(payload.newX(), payload.newY());
                QuestManager.LOADED_QUESTS.put(payload.questId(), newDef);

                QuestSyncPayload syncPacket = new QuestSyncPayload(newDef, server.registryAccess());
                Services.NETWORK.sendToAll(server, syncPacket);
            } catch (Exception e) {
                Constants.LOG.error("Failed to update quest file!", e);
            }
        }
    }

    public static void handleSaveQuest(ServerPlayer player, SaveQuestPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        QuestDefinition quest = payload.toDefinition(server.registryAccess());

        if (QuestManager.saveQuest(quest, server.registryAccess())) {
            QuestSyncPayload syncPacket = new QuestSyncPayload(quest, server.registryAccess());
            Services.NETWORK.sendToAll(server, syncPacket);
        }
    }

    public static void handleDeleteQuest(ServerPlayer player, DeleteQuestPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        QuestDefinition quest = QuestManager.LOADED_QUESTS.get(payload.questId());
        if (quest == null) return;

        Optional<Path> questFileOpt = PathSecurity.getSafeQuestFile(quest.category(), payload.questId());
        if (questFileOpt.isEmpty()) {
            Constants.LOG.warn("Blocked unsafe quest delete attempt: category={}, id={}", quest.category(), payload.questId());
            return;
        }

        Path questFile = questFileOpt.get();
        try {
            if (Files.deleteIfExists(questFile)) {
                QuestManager.LOADED_QUESTS.remove(payload.questId());
                QuestState.getServerState(server).deleteQuest(payload.questId());
                Services.NETWORK.sendToAll(server, new DeleteQuestPayload(payload.questId()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to delete quest file!", e);
        }
    }

    public static void handleCreateCategory(ServerPlayer player, CreateCategoryPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        Optional<Path> categoryDirOpt = PathSecurity.getSafeCategoryDir(payload.category());
        if (categoryDirOpt.isEmpty()) {
            Constants.LOG.warn("Blocked unsafe create category attempt: category={}", payload.category());
            return;
        }

        Path categoryDir = categoryDirOpt.get();
        try {
            if (!Files.exists(categoryDir)) {
                Files.createDirectories(categoryDir);
                ModConfig.categoryOrder.add(payload.category());
                ModConfig.save();
                Services.NETWORK.sendToAll(server, new CreateCategoryPayload(payload.category()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to create category folder!", e);
        }
    }

    public static void handleEditCategory(ServerPlayer player, EditCategoryPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        Optional<Path> oldDirOpt = PathSecurity.getSafeCategoryDir(payload.categoryOld());
        Optional<Path> newDirOpt = PathSecurity.getSafeCategoryDir(payload.categoryNew());
        if (oldDirOpt.isEmpty() || newDirOpt.isEmpty()) {
            Constants.LOG.warn("Blocked unsafe edit category attempt: old={}, new={}", payload.categoryOld(), payload.categoryNew());
            return;
        }

        Path oldDir = oldDirOpt.get();
        Path newDir = newDirOpt.get();
        if (Files.exists(oldDir) && !Files.exists(newDir)) {
            try {
                Files.move(oldDir, newDir);

                for (QuestDefinition quest : new ArrayList<>(QuestManager.LOADED_QUESTS.values())) {
                    if (quest.category().equals(payload.categoryOld())) {
                        QuestDefinition updatedQuest = quest.withCategory(payload.categoryNew());
                        QuestManager.LOADED_QUESTS.put(quest.id(), updatedQuest);
                    }
                }

                int index = ModConfig.categoryOrder.indexOf(payload.categoryOld());
                if (index != -1) {
                    ModConfig.categoryOrder.set(index, payload.categoryNew());
                } else {
                    ModConfig.categoryOrder.add(payload.categoryNew());
                }
                ModConfig.save();

                Services.NETWORK.sendToAll(server, new EditCategoryPayload(payload.categoryOld(), payload.categoryNew()));
            } catch (IOException e) {
                Constants.LOG.error("Failed to rename category folder from {} to {}", payload.categoryOld(), payload.categoryNew(), e);
            }
        }
    }

    public static void handleDeleteCategory(ServerPlayer player, DeleteCategoryPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        Optional<Path> categoryDirOpt = PathSecurity.getSafeCategoryDir(payload.category());
        if (categoryDirOpt.isEmpty()) {
            Constants.LOG.warn("Blocked unsafe delete category attempt: category={}", payload.category());
            return;
        }

        Path categoryDir = categoryDirOpt.get();
        if (Files.exists(categoryDir)) {
            File dirFile = categoryDir.toFile();
            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        String questId = file.getName().substring(0, file.getName().length() - 5);
                        if (!file.delete()) {
                            Constants.LOG.warn("Failed to delete quest file \"{}\" while deleting category \"{}\".", file.getName(), payload.category());
                        } else {
                            QuestManager.LOADED_QUESTS.remove(questId);
                        }
                    }
                }
            }

            if (!dirFile.delete()) {
                Constants.LOG.warn("Failed to delete category folder: {}", categoryDir);
            }

            ModConfig.categoryOrder.remove(payload.category());
            ModConfig.save();

            Services.NETWORK.sendToAll(server, new DeleteCategoryPayload(payload.category()));
        }
    }

    public static void handleReorderCategory(ServerPlayer player, ReorderCategoryPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !isOp(player)) return;

        ModConfig.categoryOrder.clear();
        ModConfig.categoryOrder.addAll(payload.categories());
        ModConfig.save();

        Services.NETWORK.sendToAll(server, new ReorderCategoryPayload(payload.categories()));
    }

    public static void handleRequestRegistry(ServerPlayer player, RequestRegistryPayload payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        List<String> entries = new ArrayList<>();

        if (payload.registryType() == RegistryType.STRUCTURE) {
            var structureRegistryOpt = server.registryAccess().lookup(Registries.STRUCTURE);
            if (structureRegistryOpt.isPresent()) {
                entries = structureRegistryOpt.get().listElementIds()
                        .map(ResourceKey::location)
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList();
            }
        } else if (payload.registryType() == RegistryType.LOOT_TABLE) {
            entries = server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream()
                    .filter(id -> id.getPath().startsWith("chests/"))
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
        }

        Services.NETWORK.sendToPlayer(player, new SyncRegistryPayload(payload.registryType(), entries));
    }

    public static void syncToPlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        QuestState questState = QuestState.getServerState(server);
        UUID playerId = player.getUUID();

        // 1. Send Quest Definitions
        for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
            Services.NETWORK.sendToPlayer(player, new QuestSyncPayload(quest, server.registryAccess()));
        }

        // 2. Send Category Order from server config
        List<String> orderedCategories = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String cat : ModConfig.categoryOrder) {
            if (seen.add(cat)) {
                orderedCategories.add(cat);
            }
        }

        for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
            if (seen.add(quest.category())) {
                orderedCategories.add(quest.category());
            }
        }

        if (orderedCategories.isEmpty()) {
            orderedCategories.add(DEFAULT_CATEGORY_NAME);
        }

        Services.NETWORK.sendToPlayer(player, new ReorderCategoryPayload(orderedCategories));

        // 3. Send Player Progress
        for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
            int progress = questState.getProgress(playerId, quest.id());
            boolean claimed = questState.isClaimed(playerId, quest.id());
            boolean isFinished = progress >= quest.requiredAmount();

            Services.NETWORK.sendToPlayer(player, new QuestProgressPayload(quest.id(), progress, isFinished, claimed));
        }
    }
}
