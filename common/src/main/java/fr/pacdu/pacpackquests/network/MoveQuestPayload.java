package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record MoveQuestPayload(String questId, int newX, int newY) implements CustomPacketPayload {
    public static final Type<MoveQuestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "move_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoveQuestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MoveQuestPayload::questId,
            ByteBufCodecs.VAR_INT, MoveQuestPayload::newX,
            ByteBufCodecs.VAR_INT, MoveQuestPayload::newY,
            MoveQuestPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
