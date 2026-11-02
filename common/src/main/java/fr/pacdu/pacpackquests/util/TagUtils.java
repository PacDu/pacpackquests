package fr.pacdu.pacpackquests.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class TagUtils {

    /**
     * Gets the first item associated with a tag Identifier.
     * Perfect for getting a display icon for a UI.
     * @param tagId the Identifier of the tag.
     * @return the first Item in the tag list.
     */
    public static Item getFirstItemInTag(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        var entryList = BuiltInRegistries.ITEM.get(tagKey);

        if (entryList.isPresent()) {
            for (var entry : entryList.get()) {
                return entry.value();
            }
        }

        return Items.BARRIER;
    }

    /**
     * Gets the first block associated with a tag Identifier.
     * Perfect for getting a display icon for a UI.
     * @param tagId the Identifier of the tag.
     * @return the first Block in the tag list.
     */
    public static Block getFirstBlockInTag(Identifier tagId) {
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
        var entryList = BuiltInRegistries.BLOCK.get(tagKey);

        if (entryList.isPresent()) {
            for (var entry : entryList.get()) {
                return entry.value();
            }
        }

        return Blocks.BARRIER;
    }

    /**
     * Gets the first entity associated with a tag Identifier.
     * Perfect for getting a display icon for a UI.
     * @param tagId the Identifier of the tag.
     * @return the first Entity in the tag list.
     */
    public static EntityType<?> getFirstEntityTypeInTag(Identifier tagId) {
        TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);
        var entryList = BuiltInRegistries.ENTITY_TYPE.get(tagKey);

        if (entryList.isPresent()) {
            for (var entry : entryList.get()) {
                return entry.value();
            }
        }

        return EntityType.PLAYER;
    }

    /**
     * Gets ALL items associated with a tag Identifier.
     * Useful if you want to make the icon cycle through all possibilities.
     * @param tagId the Identifier of the tag.
     * @return a List of all the items in the tag.
     */
    public static List<Item> getAllItemsInTag(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        List<Item> items = new ArrayList<>();

        BuiltInRegistries.ITEM.get(tagKey).ifPresent(entries -> {
            entries.forEach(entry -> items.add(entry.value()));
        });

        return items;
    }
}
