package fr.pacdu.pacpackquests;

import fr.pacdu.pacpackquests.reward.*;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the immutable static configuration and metadata of a quest.
 * A quest is an aggregate holding display metadata, an extensible list of tasks (objectives),
 * an extensible list of rewards, and arbitrary properties/descriptions.
 *
 * @param id The unique identifier of the quest (used in files, network packets, and save states).
 * @param title The human-readable title displayed in screens and tooltips.
 * @param category The name of the tab/category this quest belongs to.
 * @param icon The ItemStack rendered as the visual icon for the quest node.
 * @param displayX The X coordinate of the quest node on the visual canvas.
 * @param displayY The Y coordinate of the quest node on the visual canvas.
 * @param parents The list of prerequisite quest IDs that must be completed before unlocking this quest.
 * @param properties A key-value map containing arbitrary metadata (such as custom descriptions).
 * @param tasks The list of objectives that must be satisfied to complete this quest.
 * @param rewards The list of rewards granted to the player upon claiming this quest.
 */
public record QuestDefinition(
        String id,
        String title,
        String category,
        ItemStack icon,
        int displayX,
        int displayY,
        List<String> parents,
        Map<String, String> properties,
        List<QuestTask> tasks,
        List<QuestReward> rewards
) {

    public QuestDefinition {
        if (parents == null) parents = new ArrayList<>();
        if (properties == null) properties = Map.of();
        if (tasks == null) tasks = List.of(new QuestTask(TaskType.MINE_BLOCK, "minecraft:stone", 1));
        if (rewards == null) rewards = List.of(new ItemReward("minecraft:diamond", 1));
    }

    /**
     * Creates a copy of this quest with updated canvas coordinates.
     *
     * @param newX The new X coordinate on the visual canvas.
     * @param newY The new Y coordinate on the visual canvas.
     * @return A new {@link QuestDefinition} with updated position.
     */
    public QuestDefinition withPosition(int newX, int newY) {
        return new QuestDefinition(id, title, category, icon, newX, newY, parents, properties, tasks, rewards);
    }

    /**
     * Creates a copy of this quest with a new ID and title.
     *
     * @param newId The new unique identifier.
     * @param newTitle The new display title.
     * @return A new {@link QuestDefinition} with updated ID and title.
     */
    public QuestDefinition withIdAndTitle(String newId, String newTitle) {
        return new QuestDefinition(newId, newTitle, category, icon, displayX, displayY, parents, properties, tasks, rewards);
    }

    /**
     * Creates a copy of this quest moved into a different category.
     *
     * @param newCategory The target category name.
     * @return A new {@link QuestDefinition} placed in the target category.
     */
    public QuestDefinition withCategory(String newCategory) {
        return new QuestDefinition(id, title, newCategory, icon, displayX, displayY, parents, properties, tasks, rewards);
    }

    /**
     * Factory helper to create a concrete {@link QuestReward} instance based on a type enum.
     *
     * @param type The {@link RewardType} category.
     * @param target The target identifier (item ID or command string).
     * @param amount The reward quantity or count.
     * @param description A custom description for command rewards.
     * @return A concrete {@link QuestReward} instance.
     */
    public static QuestReward createReward(RewardType type, String target, int amount, String description) {
        if (type == null) return new ItemReward("minecraft:diamond", 1);
        return switch (type) {
            case ITEM -> new ItemReward(target != null ? target : "", amount);
            case XP -> new XpReward(amount);
            case LEVEL -> new LevelReward(amount);
            case COMMAND -> new CommandReward(target != null ? target : "", description != null ? description : "", amount);
        };
    }

    // --- Convenience Delegating Accessors ---

    /**
     * Returns the first task of the quest, or a default fallback task if none exist.
     *
     * @return The primary {@link QuestTask}.
     */
    public QuestTask primaryTask() {
        return (tasks != null && !tasks.isEmpty()) ? tasks.getFirst() : new QuestTask(TaskType.MINE_BLOCK, "minecraft:stone", 1);
    }

    /**
     * Returns the first reward of the quest, or a default fallback reward if none exist.
     *
     * @return The primary {@link QuestReward}.
     */
    public QuestReward primaryReward() {
        return (rewards != null && !rewards.isEmpty()) ? rewards.getFirst() : new ItemReward("minecraft:diamond", 1);
    }

    /**
     * Returns the task type of the primary task.
     *
     * @return The {@link TaskType}.
     */
    public TaskType taskType() {
        return primaryTask().type();
    }

    /**
     * Returns the target string (e.g. block ID, mob ID) of the primary task.
     *
     * @return The target string.
     */
    public String target() {
        return primaryTask().target();
    }

    /**
     * Returns the required amount to fulfill the primary task.
     *
     * @return The required amount integer.
     */
    public int requiredAmount() {
        return primaryTask().requiredAmount();
    }

    /**
     * Returns the reward type of the primary reward.
     *
     * @return The {@link RewardType}.
     */
    public RewardType rewardType() {
        return primaryReward().type();
    }

    /**
     * Returns the target identifier of the primary reward (item ID or command string).
     *
     * @return The target identifier string.
     */
    public String reward() {
        if (primaryReward() instanceof ItemReward ir) {
            return ir.target();
        } else if (primaryReward() instanceof CommandReward cr) {
            return cr.command();
        }
        return "";
    }

    /**
     * Returns the numerical amount of the primary reward.
     *
     * @return The reward amount.
     */
    public int rewardAmount() {
        return primaryReward().amount();
    }

    /**
     * Returns the human-readable description of the primary reward if applicable.
     *
     * @return The reward description string.
     */
    public String rewardDescription() {
        if (primaryReward() instanceof CommandReward cr) {
            return cr.description();
        }
        return "";
    }

    /**
     * Returns the general description of this quest stored in its properties map.
     *
     * @return The quest description string.
     */
    public String description() {
        return properties != null ? properties.getOrDefault("description", "") : "";
    }
}
