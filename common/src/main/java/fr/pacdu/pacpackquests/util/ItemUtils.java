package fr.pacdu.pacpackquests.util;

import com.mojang.brigadier.StringReader;
import fr.pacdu.pacpackquests.Constants;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for converting between ItemStacks and vanilla 1.21 item command syntax strings (with Data Components),
 * as well as matching player ItemStacks against quest targets.
 */
public final class ItemUtils {

    private static HolderLookup.Provider STATIC_LOOKUP;
    private static final Map<String, ItemStack> STACK_CACHE = new HashMap<>();

    public static HolderLookup.Provider getStaticLookup() {
        if (STATIC_LOOKUP == null) {
            STATIC_LOOKUP = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        }
        return STATIC_LOOKUP;
    }

    private ItemUtils() {}

    public static String toCommandString(ItemStack stack) {
        return toCommandString(stack, null);
    }

    /**
     * Converts an ItemStack to standard vanilla 1.21 item command syntax.
     * If the item has modified components (e.g. enchanted books, potions), serializes them inside brackets:
     * "minecraft:enchanted_book[stored_enchantments={levels:{'minecraft:feather_falling':4}}]"
     * If no components are modified, returns simple "namespace:path".
     */
    public static String toCommandString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }

        // If no components were changed from default, return standard identifier
        if (stack.getComponentsPatch().isEmpty()) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }

        HolderLookup.Provider lookup = registries != null ? registries : getStaticLookup();
        try {
            return new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(lookup);
        } catch (Exception e) {
            Constants.LOG.warn("Failed to serialize item to command string: {}", stack, e);
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
    }

    /**
     * Reconstructs an ItemStack from a target string.
     * Supports:
     * - Modern component syntax: "namespace:path[components]"
     * - Tags: "#namespace:tag_name" (returns representative first item)
     * - Standard items: "namespace:path"
     */
    public static ItemStack fromCommandString(String itemString, HolderLookup.Provider registries) {
        if (itemString == null || itemString.isEmpty() || itemString.equals("minecraft:air")) {
            return ItemStack.EMPTY;
        }

        // 1. Tag (#)
        if (itemString.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(itemString.substring(1));
            return tagId != null ? new ItemStack(TagUtils.getFirstItemInTag(tagId)) : ItemStack.EMPTY;
        }

        // 2. Modern syntax with Data Components [...]
        if (itemString.contains("[")) {
            HolderLookup.Provider lookup = registries != null ? registries : getStaticLookup();
            try {
                ItemParser reader = new ItemParser(lookup);
                ItemParser.ItemResult result = reader.parse(new StringReader(itemString));
                ItemStack stack = new ItemStack(result.item());
                stack.applyComponentsAndValidate(result.components());
                return stack;
            } catch (Exception e) {
                Constants.LOG.warn("Failed to parse item with components: {}", itemString, e);
            }
            // Fallback to base item if components failed to parse
            int bracketIndex = itemString.indexOf('[');
            ResourceLocation baseId = ResourceLocation.tryParse(itemString.substring(0, bracketIndex));
            if (baseId != null && BuiltInRegistries.ITEM.containsKey(baseId)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(baseId));
            }
            return ItemStack.EMPTY;
        }

        // 3. Standard item ID
        ResourceLocation id = ResourceLocation.tryParse(itemString);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(id));
        }

        return ItemStack.EMPTY;
    }

    /**
     * Checks if a player's ItemStack satisfies a quest target requirement.
     * Handles tags, component matching (e.g. enchanted book level, potion contents), and standard items.
     */
    public static boolean matches(ItemStack playerStack, String target, HolderLookup.Provider registries) {
        if (playerStack == null || playerStack.isEmpty() || target == null || target.isEmpty()) {
            return false;
        }

        // 1. Check item tag (#)
        if (target.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(target.substring(1));
            return tagId != null && playerStack.is(TagKey.create(Registries.ITEM, tagId));
        }

        // 2. Target with components [...]
        if (target.contains("[")) {
            ItemStack targetStack;
            if (STACK_CACHE.containsKey(target)) {
                targetStack = STACK_CACHE.get(target);
            } else {
                targetStack = fromCommandString(target, registries);
                STACK_CACHE.put(target, targetStack);
            }
            if (targetStack.isEmpty()) return false;
            return matchesComponents(playerStack, targetStack);
        }

        // 3. Standard item comparison
        ResourceLocation targetId = ResourceLocation.tryParse(target);
        return targetId != null && BuiltInRegistries.ITEM.getKey(playerStack.getItem()).equals(targetId);
    }

    /**
     * Compares an item and its components against a target template stack.
     */
    private static boolean matchesComponents(ItemStack playerStack, ItemStack targetStack) {
        if (!playerStack.is(targetStack.getItem())) {
            return false;
        }

        DataComponentPatch targetChanges = targetStack.getComponentsPatch();
        for (var entry : targetChanges.entrySet()) {
            DataComponentType<?> type = entry.getKey();
            Optional<?> expectedOptional = entry.getValue();

            if (expectedOptional.isPresent()) {
                Object expected = expectedOptional.get();
                Object actual = playerStack.get(type);
                if (actual == null) {
                    return false;
                }

                // If comparing stored enchantments, allow equal or higher level
                if (type == DataComponents.STORED_ENCHANTMENTS && expected instanceof ItemEnchantments expEnch && actual instanceof ItemEnchantments actEnch) {
                    for (var enchEntry : expEnch.entrySet()) {
                        if (actEnch.getLevel(enchEntry.getKey()) < enchEntry.getIntValue()) {
                            return false;
                        }
                    }
                } else if (!Objects.equals(actual, expected)) {
                    return false;
                }
            } else {
                // Expected component to be explicitly absent
                if (playerStack.get(type) != null) {
                    return false;
                }
            }
        }
        return true;
    }
}
