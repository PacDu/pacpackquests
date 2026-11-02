package fr.pacdu.pacpackquests;

import fr.pacdu.pacpackquests.reward.*;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the static configuration of a quest.
 * A quest is an aggregate holding display metadata, an extensible list of tasks (objectives),
 * an extensible list of rewards, and arbitrary properties/descriptions.
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

    public QuestDefinition withPosition(int newX, int newY) {
        return new QuestDefinition(id, title, category, icon, newX, newY, parents, properties, tasks, rewards);
    }

    public QuestDefinition withIdAndTitle(String newId, String newTitle) {
        return new QuestDefinition(newId, newTitle, category, icon, displayX, displayY, parents, properties, tasks, rewards);
    }

    public QuestDefinition withCategory(String newCategory) {
        return new QuestDefinition(id, title, newCategory, icon, displayX, displayY, parents, properties, tasks, rewards);
    }

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

    public QuestTask primaryTask() {
        return (tasks != null && !tasks.isEmpty()) ? tasks.getFirst() : new QuestTask(TaskType.MINE_BLOCK, "minecraft:stone", 1);
    }

    public QuestReward primaryReward() {
        return (rewards != null && !rewards.isEmpty()) ? rewards.getFirst() : new ItemReward("minecraft:diamond", 1);
    }

    public TaskType taskType() {
        return primaryTask().type();
    }

    public String target() {
        return primaryTask().target();
    }

    public int requiredAmount() {
        return primaryTask().requiredAmount();
    }

    public RewardType rewardType() {
        return primaryReward().type();
    }

    public String reward() {
        if (primaryReward() instanceof ItemReward ir) {
            return ir.target();
        } else if (primaryReward() instanceof CommandReward cr) {
            return cr.command();
        }
        return "";
    }

    public int rewardAmount() {
        return primaryReward().amount();
    }

    public String rewardDescription() {
        if (primaryReward() instanceof CommandReward cr) {
            return cr.description();
        }
        return "";
    }

    public String description() {
        return properties != null ? properties.getOrDefault("description", "") : "";
    }
}
