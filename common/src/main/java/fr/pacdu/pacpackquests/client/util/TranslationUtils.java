package fr.pacdu.pacpackquests.client.util;

import fr.pacdu.pacpackquests.client.ClientQuestData;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.reward.*;
import fr.pacdu.pacpackquests.util.ItemUtils;
import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class TranslationUtils {

    private TranslationUtils() {}

    /**
     * Formats the visual display text of a reward with appropriate styling and color.
     */
    public static Component getRewardText(QuestReward reward, boolean isClaimed) {
        if (reward == null) return Component.empty();

        MutableComponent text = switch (reward) {
            case XpReward xr -> Component.literal(xr.amount() + " XP");
            case LevelReward lr -> Component.literal(lr.amount() + " ").append(Component.translatable("gui.pacpackquests.levels"));
            case ItemReward ir -> Component.literal(ir.amount() + "x ").append(getTranslatedRewardName(ir.target(), RegistryType.ITEM));
            case CommandReward cr -> {
                if (cr.description() != null && !cr.description().isBlank()) {
                    Component descText = I18n.exists(cr.description())
                            ? Component.translatable(cr.description())
                            : Component.literal(cr.description());
                    yield Component.literal(cr.amount() + "x ").append(descText);
                } else {
                    String cmd = cr.command();
                    yield Component.literal(cr.amount() + "x ").append(Component.literal((cmd.startsWith("/") ? "" : "/") + (cmd.length() > 64 ? cmd.substring(0, 64) + "..." : cmd)));
                }
            }
        };

        if (isClaimed) {
            return text.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
        }

        return switch (reward.type()) {
            case XP -> text.withStyle(ChatFormatting.GREEN);
            case LEVEL -> text.withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
            case ITEM -> text.withStyle(ChatFormatting.WHITE);
            case COMMAND -> text.withStyle(ChatFormatting.YELLOW);
        };
    }

    /**
     * Translate an id (e.g. "minecraft:stone", "#minecraft:logs", or item with components) into a readable name.
     * @param entryId a valid ID or tag.
     * @param type the taskType of the ID corresponding to a Minecraft Registry.
     */
    public static String getTranslatedName(String entryId, RegistryType type) {
        return getTranslatedName(entryId, type, false);
    }

    /**
     * Translate an id specifically for a reward into a readable name (e.g. "Random Logs" instead of "Any Logs").
     */
    public static String getTranslatedRewardName(String entryId, RegistryType type) {
        return getTranslatedName(entryId, type, true);
    }

    public static String getTranslatedName(String entryId, RegistryType type, boolean isReward) {
        if (entryId == null || entryId.isEmpty() || type == null) return "...";

        boolean isTag = entryId.startsWith("#");
        boolean isComplexItem = entryId.contains("[");
        String rawId = isTag ? entryId.substring(1) : entryId;
        rawId = isComplexItem ? rawId.substring(0, entryId.indexOf('[')) : rawId;
        Identifier id = Identifier.tryParse(rawId);

        if (id == null) return entryId;

        if (isTag) {
            String path = id.getPath();
            String prefix = isReward ? "Random " : "Any ";
            return prefix + path.substring(0, 1).toUpperCase() + path.substring(1).replace("_", " ");
        }

        return switch (type) {
            case ITEM -> {
                if (isComplexItem) {
                    var world = Minecraft.getInstance().level;
                    var reg = world != null ? world.registryAccess() : null;
                    ItemStack stack = ItemUtils.fromCommandString(entryId, reg);
                    if (!stack.isEmpty()) {
                        if (stack.is(Items.ENCHANTED_BOOK)) {
                            var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
                            if (stored != null && !stored.isEmpty()) {
                                var firstEntry = stored.entrySet().iterator().next();
                                Component enchText = Enchantment.getFullname(firstEntry.getKey(), firstEntry.getIntValue());
                                yield stack.getHoverName().getString() + " (" + enchText.getString() + ")";
                            }
                        }
                        yield stack.getHoverName().getString();
                    }
                }
                yield BuiltInRegistries.ITEM.getValue(id).getName().getString();
            }
            case BLOCK -> BuiltInRegistries.BLOCK.getValue(id).getName().getString();
            case MOB -> BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription().getString();
            case COMMAND -> {
                String command = (entryId.startsWith("/") ? "" : "/") + entryId;
                if (command.length() > 24) {
                    command = command.substring(0, 24) + "...";
                }
                yield command;
            }
            default -> {
                String path = id.getPath();
                yield path.substring(0, 1).toUpperCase() + path.substring(1).replace("_", " ");
            }
        };
    }

    // Helper to format the long IDs into short readable button texts (e.g., "minecraft:stone" -> "Stone")
    public static String formatDisplayString(String raw) {
        if (raw == null || raw.isEmpty()) return "...";
        boolean isTag = raw.startsWith("#");
        String clean = isTag ? raw.substring(1) : raw;
        if (clean.contains("[")) clean = clean.substring(0, clean.indexOf('['));
        Identifier id = Identifier.tryParse(clean);
        if (id == null) return raw;

        String path = id.getPath();
        String formatted = path.substring(0, 1).toUpperCase() + path.substring(1).replace("_", " ");
        if (formatted.length() > 18) formatted = formatted.substring(0, 15) + "..."; // Truncate long names
        return isTag ? "# " + formatted : formatted;
    }

    /**
     * Resolves human-readable mod name for a given namespace.
     */
    public static String getModName(String namespace) {
        return Services.PLATFORM.getModName(namespace);
    }

    /**
     * Returns the number of elements contained in a tag for any given RegistryType.
     */
    public static int getTagSize(Identifier tagId, RegistryType type) {
        if (tagId == null || type == null) return -1;
        Minecraft client = Minecraft.getInstance();

        return switch (type) {
            case ITEM -> BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tagId))
                    .map(HolderSet.Named::size).orElse(-1);
            case BLOCK -> BuiltInRegistries.BLOCK.get(TagKey.create(Registries.BLOCK, tagId))
                    .map(HolderSet.Named::size).orElse(-1);
            case MOB -> BuiltInRegistries.ENTITY_TYPE.get(TagKey.create(Registries.ENTITY_TYPE, tagId))
                    .map(HolderSet.Named::size).orElse(-1);
            case BIOME -> {
                if (client != null && client.level != null) {
                    yield client.level.registryAccess().lookup(Registries.BIOME)
                            .flatMap(reg -> reg.get(TagKey.create(Registries.BIOME, tagId)))
                            .map(HolderSet.Named::size).orElse(-1);
                }
                yield -1;
            }
            case STRUCTURE -> {
                if (client != null && client.getSingleplayerServer() != null) {
                    var opt = client.getSingleplayerServer().registryAccess().lookup(Registries.STRUCTURE);
                    if (opt.isPresent()) {
                        yield opt.get().get(TagKey.create(Registries.STRUCTURE, tagId))
                                .map(HolderSet.Named::size).orElse(-1);
                    }
                }
                yield -1;
            }
            case LOOT_TABLE -> {
                if (tagId.equals(Identifier.fromNamespaceAndPath("pacpackquests", "loot_chest"))) {
                    if (client != null && client.getSingleplayerServer() != null) {
                        var opt = client.getSingleplayerServer().reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
                        if (opt.isPresent()) {
                            yield (int) opt.get().listElementIds()
                                    .filter(key -> key.identifier().getPath().startsWith("chests/"))
                                    .count();
                        }
                    }
                    yield ClientQuestData.SERVER_LOOT_TABLES.size();
                }
                yield -1;
            }
            default -> -1;
        };
    }
}
