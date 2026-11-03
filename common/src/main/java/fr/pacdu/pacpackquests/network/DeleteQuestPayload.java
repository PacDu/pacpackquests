package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record DeleteQuestPayload(String questId) implements CustomPacketPayload {
    public static final Type<DeleteQuestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "delete_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteQuestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DeleteQuestPayload::questId,
            DeleteQuestPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
