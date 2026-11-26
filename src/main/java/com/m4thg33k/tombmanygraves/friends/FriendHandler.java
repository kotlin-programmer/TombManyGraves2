package com.m4thg33k.tombmanygraves.friends;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.util.LogHelper;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class FriendHandler {

    private static FriendData getData()
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null ? null : FriendData.get(server);
    }

    public static boolean hasAsFriend(UUID owner, UUID friend)
    {
        FriendData data = getData();
        return data != null && data.getFriends(owner).contains(friend);
    }

    public static boolean addFriendToList(UUID owner, UUID friend)
    {
        FriendData data = getData();
        if (data == null || !data.getFriends(owner).add(friend))
        {
            return false;
        }
        data.markDirty();
        return true;
    }

    public static boolean removeFriend(UUID owner, UUID friend)
    {
        FriendData data = getData();
        if (data == null || !data.getFriends(owner).remove(friend))
        {
            return false;
        }
        data.markDirty();
        return true;
    }

    public static List<String> getFriendStringListFor(MinecraftServer server, UUID playerID)
    {
        List<String> names = new ArrayList<>();
        for (UUID id : FriendData.get(server).getFriends(playerID))
        {
            GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(id);
            if (profile == null)
            {
                LogHelper.error("Error getting name for id: " + id.toString());
            }
            else
            {
                names.add(profile.getName());
            }
        }
        return names;
    }

    public static void printFriendList(MinecraftServer server, EntityPlayer player)
    {
        List<String> friends = getFriendStringListFor(server, player.getUniqueID());

        if (friends.isEmpty())
        {
            player.sendMessage(new TextComponentString("You have not added any friends yet."));
            return;
        }

        for (String name : friends)
        {
            player.sendMessage(new TextComponentString(name));
        }
    }
}
