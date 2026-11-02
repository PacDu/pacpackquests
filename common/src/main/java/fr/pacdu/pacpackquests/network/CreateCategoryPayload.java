package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CreateCategoryPayload(String category) implements CustomPacketPayload {
    public static final Type<CreateCategoryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "create_category"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateCategoryPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CreateCategoryPayload::category,
            CreateCategoryPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}