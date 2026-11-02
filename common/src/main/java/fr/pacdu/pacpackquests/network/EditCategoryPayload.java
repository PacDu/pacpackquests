package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EditCategoryPayload(String categoryOld, String categoryNew) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EditCategoryPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "edit_category"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditCategoryPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EditCategoryPayload::categoryOld,
            ByteBufCodecs.STRING_UTF8, EditCategoryPayload::categoryNew,
            EditCategoryPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
