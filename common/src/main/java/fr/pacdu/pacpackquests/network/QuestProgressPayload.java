package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QuestProgressPayload(String questId, int progress, boolean isFinished, boolean isClaimed) implements CustomPacketPayload {
    public static final Type<QuestProgressPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "quest_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestProgressPayload::questId,
            ByteBufCodecs.VAR_INT, QuestProgressPayload::progress,
            ByteBufCodecs.BOOL, QuestProgressPayload::isFinished,
            ByteBufCodecs.BOOL, QuestProgressPayload::isClaimed,
            QuestProgressPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
