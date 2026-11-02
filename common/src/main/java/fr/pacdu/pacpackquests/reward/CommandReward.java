package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record CommandReward(String command, String description, int amount) implements QuestReward {

    @Override
    public RewardType type() {
        return RewardType.COMMAND;
    }

    @Override
    public void grant(ServerPlayer player, RegistryAccess registryManager) {
        if (player == null || command == null || command.isEmpty() || amount <= 0) return;

        MinecraftServer server = player.level().getServer();
        String cleanCommand = command.startsWith("/") ? command.substring(1) : command;

        CommandSourceStack source = server.createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withRotation(player.getRotationVector())
                .withLevel(player.level())
                .withPermission(PermissionSet.ALL_PERMISSIONS)
                .withSuppressedOutput();

        try {
            for (int i = 0; i < Math.min(amount, 256); i++) {
                server.getCommands().getDispatcher().execute(cleanCommand, source);
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            Constants.LOG.error("Failed to execute quest reward command: {}", cleanCommand, e);
        }
    }

    @Override
    public ItemStack createIconStack(HolderLookup.Provider registries) {
        return new ItemStack(Items.WRITABLE_BOOK);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.COMMAND);
        buf.writeUtf(command != null ? command : "");
        buf.writeUtf(description != null ? description : "");
        buf.writeInt(amount);
    }

    public static CommandReward read(FriendlyByteBuf buf) {
        String command = buf.readUtf();
        String description = buf.readUtf();
        int amount = buf.readInt();
        return new CommandReward(command, description, amount);
    }
}
