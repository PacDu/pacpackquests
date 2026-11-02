package fr.pacdu.pacpackquests.network;

import fr.pacdu.pacpackquests.Constants;

import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncRegistryPayload(RegistryType registryType, List<String> entries) implements CustomPacketPayload {

    public static final Type<SyncRegistryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sync_registry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRegistryPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeEnum(value.registryType());
                buf.writeCollection(value.entries(), FriendlyByteBuf::writeUtf);
            },
            buf -> new SyncRegistryPayload(
                    buf.readEnum(RegistryType.class),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
