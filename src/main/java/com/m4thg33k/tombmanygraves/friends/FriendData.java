package com.m4thg33k.tombmanygraves.friends;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.Names;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

public class FriendData extends WorldSavedData {

    private static final String NAME = Names.MODID + "_friends";
    private static final String LEGACY_FILE = "./TombManyGraves/friends.fdat";
    private static final String OWNER = "Owner";
    private static final String FRIENDS = "Friends";

    private final Map<UUID, Set<UUID>> friendList = new HashMap<>();

    public FriendData(String name)
    {
        super(name);
    }

    public static FriendData get(MinecraftServer server)
    {
        MapStorage storage = server.getEntityWorld().getMapStorage();
        FriendData data = (FriendData) storage.getOrLoadData(FriendData.class, NAME);
        if (data == null)
        {
            data = new FriendData(NAME);
            data.importLegacy();
            storage.setData(NAME, data);
        }
        return data;
    }

    // Friends used to be stored as a java-serialized HashMap<UUID, HashSet<UUID>> in the instance dir
    @SuppressWarnings("unchecked")
    private void importLegacy()
    {
        File file = new File(LEGACY_FILE);
        if (!file.isFile())
        {
            return;
        }
        try (ObjectInputStream in = new LegacyInputStream(new FileInputStream(file)))
        {
            Map<UUID, Set<UUID>> legacy = (Map<UUID, Set<UUID>>) in.readObject();
            for (Map.Entry<UUID, Set<UUID>> entry : legacy.entrySet())
            {
                getFriends(entry.getKey()).addAll(entry.getValue());
            }
            markDirty();
            LogHelper.info("Imported friend lists of " + legacy.size() + " players from " + LEGACY_FILE);
        }
        catch (Exception e)
        {
            LogHelper.warn("Could not import friend lists from " + LEGACY_FILE + ": " + e);
            return;
        }
        if (!file.renameTo(new File(LEGACY_FILE + ".imported")))
        {
            LogHelper.warn("Could not rename " + LEGACY_FILE + " after importing it");
        }
    }

    private static class LegacyInputStream extends ObjectInputStream
    {
        private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
                HashMap.class.getName(), HashSet.class.getName(), UUID.class.getName()));

        LegacyInputStream(InputStream in) throws IOException
        {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
        {
            if (!ALLOWED.contains(desc.getName()))
            {
                throw new InvalidClassException(desc.getName(), "Unexpected class in legacy friends file");
            }
            return super.resolveClass(desc);
        }
    }

    public Set<UUID> getFriends(UUID owner)
    {
        return friendList.computeIfAbsent(owner, k -> new HashSet<>());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        friendList.clear();
        NBTTagList owners = nbt.getTagList(FRIENDS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < owners.tagCount(); i++)
        {
            NBTTagCompound entry = owners.getCompoundTagAt(i);
            Set<UUID> friends = getFriends(NBTUtil.getUUIDFromTag(entry.getCompoundTag(OWNER)));
            NBTTagList friendTags = entry.getTagList(FRIENDS, Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < friendTags.tagCount(); j++)
            {
                friends.add(NBTUtil.getUUIDFromTag(friendTags.getCompoundTagAt(j)));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        NBTTagList owners = new NBTTagList();
        for (Map.Entry<UUID, Set<UUID>> entry : friendList.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            NBTTagCompound ownerTag = new NBTTagCompound();
            ownerTag.setTag(OWNER, NBTUtil.createUUIDTag(entry.getKey()));
            NBTTagList friendTags = new NBTTagList();
            for (UUID friend : entry.getValue())
            {
                friendTags.appendTag(NBTUtil.createUUIDTag(friend));
            }
            ownerTag.setTag(FRIENDS, friendTags);
            owners.appendTag(ownerTag);
        }
        nbt.setTag(FRIENDS, owners);
        return nbt;
    }
}
