package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record ClaimQuestPayload(String questId) implements CustomPacketPayload {
    public static final Type<ClaimQuestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "claim_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimQuestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClaimQuestPayload::questId,
            ClaimQuestPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
