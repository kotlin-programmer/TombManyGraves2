package com.m4thg33k.tombmanygraves.commands;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.invman.DeathInventoryHandler;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandGetDeathList extends CommandBase {

    public CommandGetDeathList()
    {
        super("tmg_getDeathList", 2, false);

        aliases.add("tmg_deathlist");
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return COMMAND_NAME + " <player> <timestamp or latest> [receiving player]";
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0 || index == 2;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1 || args.length == 3)
        {
            return getListOfStringMatchingLastWord(args, server.getOnlinePlayerNames());
        }

        if (args.length == 2)
        {
            return getListOfStringMatchingLastWord(args, DeathInventoryHandler.getFilenames(args[0]));
        }

        return new ArrayList<>();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender.getEntityWorld().isRemote)
        {
            return;
        }

        if (! ModConfigs.ALLOW_INVENTORY_SAVES)
        {
            sender.sendMessage(new TextComponentString("This command has been disabled."));
            return;
        }

        if (args.length < 2)
        {
            sender.sendMessage(new TextComponentString(getUsage(sender)));
            return;
        }

        EntityPlayer receiver;
        if (args.length > 2)
        {
            receiver = server.getPlayerList().getPlayerByUsername(args[2]);
        }
        else if (sender.getCommandSenderEntity() instanceof EntityPlayer)
        {
            receiver = (EntityPlayer) sender.getCommandSenderEntity();
        }
        else
        {
            receiver = server.getPlayerList().getPlayerByUsername(args[0]);
        }

        if (receiver == null)
        {
            sender.sendMessage(new TextComponentString("The receiving player is offline."));
            return;
        }

        ItemStack deathList = DeathInventoryHandler.getDeathListFromFile(args[0], args[1]);
        if (deathList == null)
        {
            sender.sendMessage(new TextComponentString("Failed to retrieve list."));
            sender.sendMessage(new TextComponentString("Check spelling and timestamp."));
            return;
        }

        ItemHandlerHelper.giveItemToPlayer(receiver, deathList);
    }
}
