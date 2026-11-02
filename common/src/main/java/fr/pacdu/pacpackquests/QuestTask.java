package fr.pacdu.pacpackquests;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Represents an individual task or objective within a quest (e.g. mine a block, craft an item, visit a biome).
 * A quest can contain one or multiple tasks.
 */
public record QuestTask(TaskType type, String target, int requiredAmount) {

    public static QuestTask of(TaskType type, String target, int requiredAmount) {
        return new QuestTask(type, target, requiredAmount);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        buf.writeUtf(this.target != null ? this.target : "");
        buf.writeInt(this.requiredAmount);
    }

    public static QuestTask read(FriendlyByteBuf buf) {
        TaskType type = buf.readEnum(TaskType.class);
        String target = buf.readUtf();
        int requiredAmount = buf.readInt();
        return new QuestTask(type, target, requiredAmount);
    }
}
