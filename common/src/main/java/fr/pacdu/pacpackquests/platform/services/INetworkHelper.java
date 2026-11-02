package fr.pacdu.pacpackquests.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
    void sendToServer(CustomPacketPayload payload);
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    default void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        if (server != null && server.getPlayerList() != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendToPlayer(player, payload);
            }
        }
    }
}
