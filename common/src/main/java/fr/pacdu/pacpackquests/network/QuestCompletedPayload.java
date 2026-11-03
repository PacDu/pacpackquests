package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

// A lightweight payload to tell the client "Hey, show the toast for this quest TYPE!"
public record QuestCompletedPayload(String questId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestCompletedPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "quest_completed"));

    // 1.21 Packet Codec structure
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestCompletedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestCompletedPayload::questId,
            QuestCompletedPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
