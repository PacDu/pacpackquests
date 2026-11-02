package fr.pacdu.pacpackquests;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Represents an individual task or objective within a quest (e.g. mine a block, craft an item, visit a biome).
 * A quest can contain one or multiple tasks.
 *
 * @param type The {@link TaskType} defining the nature of the objective.
 * @param target The target registry ID, tag, or dimension/biome identifier to match.
 * @param requiredAmount The amount of times the objective must be completed.
 */
public record QuestTask(TaskType type, String target, int requiredAmount) {

    /**
     * Creates a new {@link QuestTask} instance.
     *
     * @param type The type of task.
     * @param target The target identifier.
     * @param requiredAmount The target count required.
     * @return A new {@link QuestTask} instance.
     */
    public static QuestTask of(TaskType type, String target, int requiredAmount) {
        return new QuestTask(type, target, requiredAmount);
    }

    /**
     * Serializes this task into a {@link FriendlyByteBuf}.
     *
     * @param buf The buffer to write into.
     */
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        buf.writeUtf(this.target != null ? this.target : "");
        buf.writeInt(this.requiredAmount);
    }

    /**
     * Deserializes a {@link QuestTask} from a {@link FriendlyByteBuf}.
     *
     * @param buf The buffer to read from.
     * @return The deserialized {@link QuestTask}.
     */
    public static QuestTask read(FriendlyByteBuf buf) {
        TaskType type = buf.readEnum(TaskType.class);
        String target = buf.readUtf();
        int requiredAmount = buf.readInt();
        return new QuestTask(type, target, requiredAmount);
    }
}
