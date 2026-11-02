package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestRegistryPayload(RegistryType registryType) implements CustomPacketPayload {

    public static final Type<RequestRegistryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "req_registry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRegistryPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeEnum(value.registryType());
            },
            buf -> new RequestRegistryPayload(
                    buf.readEnum(RegistryType.class)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
