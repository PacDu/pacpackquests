package fr.pacdu.pacpackquests.data;

import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.TaskType;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootTable;

import java.lang.reflect.Method;
import java.util.UUID;

public class QuestEventHandler {

    private QuestEventHandler() {}

    public static void onBlockBreak(ServerPlayer player, BlockState state) {
        for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
            if (quest.taskType() == TaskType.MINE_BLOCK) {
                boolean isTarget;
                String target = quest.target();

                if (target.startsWith("#")) {
                    Identifier id = Identifier.tryParse(target.substring(1));
                    if (id == null) continue;
                    TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
                    isTarget = state.is(tag);
                } else {
                    isTarget = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(target);
                }

                if (isTarget) {
                    QuestProgressHandler.incrementProgress(player, quest, 1);
                }
            }
        }
    }

    public static void onEntityKill(ServerPlayer player, Entity killedEntity) {
        for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
            if (quest.taskType() == TaskType.KILL_MOB) {
                boolean isTarget;
                String target = quest.target();

                if (target.startsWith("#")) {
                    Identifier id = Identifier.tryParse(target.substring(1));
                    if (id == null) continue;
                    TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, id);
                    isTarget = killedEntity.getType().is(tag);
                } else {
                    isTarget = BuiltInRegistries.ENTITY_TYPE.getKey(killedEntity.getType()).toString().equals(target);
                }

                if (isTarget) {
                    QuestProgressHandler.incrementProgress(player, quest, 1);
                }
            }
        }
    }

    public static void onUseBlock(ServerPlayer player, Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity lootable) {
            ResourceKey<LootTable> lootKey = lootable.getLootTable();
            computeOpenedChest(lootKey, player, be);
        }
    }

    public static void onUseEntity(ServerPlayer player, Level world, Entity entity) {
        if (entity instanceof ContainerEntity vehicle) {
            ResourceKey<LootTable> lootKey = vehicle.getContainerLootTable();
            computeOpenedChest(lootKey, player, entity);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlockPos pos = player.blockPosition();
            ServerLevel world = player.level();
            HolderLookup.Provider registryManager = world.registryAccess();

            for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
                boolean requirementMet = false;
                String target = quest.target();

                if (quest.taskType() == TaskType.OBTAIN_ITEM) {
                    int targetItemCount = 0;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            if (ItemUtils.matches(stack, target, registryManager)) {
                                targetItemCount += stack.getCount();
                            }
                        }
                    }
                    if (targetItemCount > 0) {
                        QuestProgressHandler.setProgress(player, quest, targetItemCount);
                    }
                    continue;
                }

                boolean isTag = target.startsWith("#");
                Identifier targetId = Identifier.tryParse(isTag ? target.substring(1) : target);

                if (targetId == null) continue;

                switch (quest.taskType()) {
                    case EXPLORE_DIMENSION -> {
                        if (world.dimension().identifier().equals(targetId)) {
                            requirementMet = true;
                        }
                    }
                    case EXPLORE_BIOME -> {
                        Holder<Biome> biomeEntry = world.getBiome(pos);
                        if (isTag) {
                            if (biomeEntry.is(TagKey.create(Registries.BIOME, targetId))) requirementMet = true;
                        } else {
                            if (biomeEntry.is(targetId)) requirementMet = true;
                        }
                    }
                    case EXPLORE_STRUCTURE -> {
                        var structureRegistry = registryManager.lookup(Registries.STRUCTURE);
                        StructureStart structureStart = null;

                        if (isTag) {
                            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, targetId);
                            structureStart = world.structureManager().getStructureWithPieceAt(pos, tag);
                        } else if (structureRegistry.isPresent()) {
                            var structureHolder = structureRegistry.get().get(ResourceKey.create(Registries.STRUCTURE, targetId));
                            if (structureHolder.isPresent()) {
                                structureStart = world.structureManager().getStructureWithPieceAt(pos, structureHolder.get().value());
                            }
                        }

                        if (structureStart != null && structureStart.isValid()) {
                            long structureUniqueId = structureStart.getChunkPos().toLong();
                            QuestState state = QuestState.getServerState(server);
                            UUID playerUuid = player.getUUID();

                            if (!state.hasDiscoveredStructure(playerUuid, quest.id(), structureUniqueId)) {
                                state.addDiscoveredStructure(playerUuid, quest.id(), structureUniqueId);
                                requirementMet = true;
                            }
                        }
                    }
                    default -> {}
                }

                if (requirementMet) {
                    QuestProgressHandler.incrementProgress(player, quest, 1);
                }
            }
        }
    }

    private static void computeOpenedChest(ResourceKey<LootTable> lootKey, ServerPlayer player, Object entity) {
        if (lootKey != null) {
            Identifier lootTableId = lootKey.identifier();
            for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
                if (quest.taskType() == TaskType.OPEN_LOOT_CHEST) {
                    String target = quest.target();
                    if (checkLootrStatus(player, entity) && (target.equals("#pacpackquests:loot_chest") || target.equals(lootTableId.toString()))) {
                        QuestProgressHandler.incrementProgress(player, quest, 1);
                    }
                }
            }
        }
    }

    private static boolean checkLootrStatus(ServerPlayer player, Object target) {
        if (!Services.PLATFORM.isModLoaded("lootr")) {
            return true;
        }
        try {
            Class<?> infoClass = Class.forName("noobanidus.mods.lootr.common.api.data.ILootrInfoProvider");
            if (infoClass.isInstance(target)) {
                Method hasOpened = infoClass.getMethod("hasOpened", UUID.class);
                boolean opened = (boolean) hasOpened.invoke(target, player.getUUID());
                return !opened;
            }
        } catch (Exception e) {
            Constants.LOG.warn("You are using an outdated or incompatible version of Lootr! Loot chest quests may be exploitable.");
        }
        return true;
    }
}
