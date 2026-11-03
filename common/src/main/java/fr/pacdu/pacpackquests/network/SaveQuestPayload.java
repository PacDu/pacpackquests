package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.QuestTask;
import fr.pacdu.pacpackquests.reward.QuestReward;
import fr.pacdu.pacpackquests.util.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SaveQuestPayload(
        String questId,
        String title,
        String category,
        String iconId,
        int displayX,
        int displayY,
        List<String> parents,
        Map<String, String> properties,
        List<QuestTask> tasks,
        List<QuestReward> rewards
) implements CustomPacketPayload {

    public static final Type<SaveQuestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "save_quest"));

    public SaveQuestPayload(QuestDefinition quest, HolderLookup.Provider registries) {
        this(
                quest.id(),
                quest.title(),
                quest.category(),
                ItemUtils.toCommandString(quest.icon(), registries),
                quest.displayX(),
                quest.displayY(),
                quest.parents(),
                quest.properties(),
                quest.tasks(),
                quest.rewards()
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveQuestPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.questId());
                buf.writeUtf(value.title());
                buf.writeUtf(value.category());
                buf.writeUtf(value.iconId());
                buf.writeInt(value.displayX());
                buf.writeInt(value.displayY());
                buf.writeCollection(value.parents(), FriendlyByteBuf::writeUtf);
                buf.writeMap(value.properties(), (b, k) -> b.writeUtf(k), (b, v) -> b.writeUtf(v));
                buf.writeCollection(value.tasks(), (b, task) -> task.write(b));
                buf.writeCollection(value.rewards(), (b, reward) -> reward.write(b));
            },
            buf -> new SaveQuestPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                    buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf),
                    buf.readCollection(ArrayList::new, QuestTask::read),
                    buf.readCollection(ArrayList::new, QuestReward::read)
            )
    );

    public QuestDefinition toDefinition(HolderLookup.Provider registries) {
        ItemStack icon = ItemUtils.fromCommandString(iconId, registries);
        return new QuestDefinition(questId, title, category, icon, displayX, displayY, parents, properties, tasks, rewards);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
