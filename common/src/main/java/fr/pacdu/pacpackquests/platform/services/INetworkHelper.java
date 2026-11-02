package fr.pacdu.pacpackquests.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Service Provider Interface (SPI) abstracting network packet dispatching across mod loaders.
 */
public interface INetworkHelper {

    /**
     * Sends a custom packet payload from the client to the server.
     *
     * @param payload The {@link CustomPacketPayload} to send.
     */
    void sendToServer(CustomPacketPayload payload);

    /**
     * Sends a custom packet payload from the server to a specific connected player.
     *
     * @param player The target {@link ServerPlayer}.
     * @param payload The {@link CustomPacketPayload} to send.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * Broadcasts a custom packet payload from the server to all connected players.
     *
     * @param server The running {@link MinecraftServer}.
     * @param payload The {@link CustomPacketPayload} to broadcast.
     */
    default void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        if (server != null && server.getPlayerList() != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendToPlayer(player, payload);
            }
        }
    }
}
