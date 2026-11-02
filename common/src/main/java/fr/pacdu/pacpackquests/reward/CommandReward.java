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

/**
 * A quest reward that executes a server command on behalf of or targeting the player upon claiming.
 *
 * @param command The command string to execute (e.g. "/effect give @s speed 60 1").
 * @param description An optional friendly description explaining what the command does.
 * @param amount The number of times the command should be run (or repeat count).
 */
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
        buf.writeUtf(this.command != null ? this.command : "");
        buf.writeUtf(this.description != null ? this.description : "");
        buf.writeInt(this.amount);
    }

    /**
     * Reads a {@link CommandReward} from the network buffer.
     *
     * @param buf The buffer to read from.
     * @return The read {@link CommandReward}.
     */
    public static CommandReward read(FriendlyByteBuf buf) {
        return new CommandReward(buf.readUtf(), buf.readUtf(), buf.readInt());
    }
}
