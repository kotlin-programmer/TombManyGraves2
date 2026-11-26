package com.m4thg33k.tombmanygraves.invman;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.Names;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.DimensionManager;

public class DeathInventoryHandler {

    private static final String LEGACY_DIR = "./TombManyGraves/savedInventories";

    private static File getSaveDir()
    {
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir == null)
        {
            return null;
        }
        File dir = new File(worldDir, Names.MODID + "/savedInventories");
        if (!dir.exists())
        {
            if (!dir.mkdirs())
            {
                return null;
            }
            importLegacy(dir);
        }
        return dir;
    }

    // Saves used to be stored in the instance dir
    private static void importLegacy(File dir)
    {
        File[] legacy = new File(LEGACY_DIR).listFiles((d, name) -> name.endsWith(".json"));
        if (legacy == null || legacy.length == 0)
        {
            return;
        }
        int imported = 0;
        for (File file : legacy)
        {
            try
            {
                Files.copy(file.toPath(), new File(dir, file.getName()).toPath());
                imported++;
            }
            catch (Exception e)
            {
                LogHelper.warn("Could not import " + file + ": " + e);
            }
        }
        LogHelper.info("Imported " + imported + " inventory saves from " + LEGACY_DIR);
        if (imported < legacy.length)
        {
            LogHelper.warn("Not all inventory saves could be imported, leaving " + LEGACY_DIR + " in place");
        }
        else if (!new File(LEGACY_DIR).renameTo(new File(LEGACY_DIR + "_imported")))
        {
            LogHelper.warn("Could not rename " + LEGACY_DIR + " after importing it");
        }
    }

    private static File getFile(String playerName, String timestamp)
    {
        File dir = getSaveDir();
        return dir == null ? null : new File(dir, playerName + "#" + timestamp + ".json");
    }

    public static boolean writeInventoryFile(InventoryHolder inventoryHolder)
    {
        if (!ModConfigs.ALLOW_INVENTORY_SAVES)
        {
            return false;
        }

        File file = getFile(inventoryHolder.getPlayerName(), inventoryHolder.getTimestamp());
        File latest = getFile(inventoryHolder.getPlayerName(), "latest");
        if (file == null || latest == null)
        {
            return false;
        }

        try
        {
            NBTTagCompound tagCompound = inventoryHolder.writeToNBT(new NBTTagCompound());
            Files.write(file.toPath(), tagCompound.toString().getBytes(StandardCharsets.UTF_8));
            Files.copy(file.toPath(), latest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (Exception e)
        {
            LogHelper.error("Failed to write " + file + ": " + e);
            return false;
        }
    }

    public static List<String> getFilenames(String playerName)
    {
        List<String> saved = new ArrayList<>();
        File dir = getSaveDir();
        String[] files = dir == null ? null : dir.list();
        if (files == null)
        {
            return saved;
        }
        String prefix = playerName + "#";
        for (String file : files)
        {
            if (file.startsWith(prefix) && file.endsWith(".json"))
            {
                saved.add(file.substring(prefix.length(), file.length() - 5));
            }
        }

        return saved;
    }

    public static ItemStack getDeathListFromFile(String playerName, String timestamp)
    {
        NBTTagCompound allNBT = getSavedInventoryAsNBT(playerName, timestamp);
        if (allNBT == null || allNBT.isEmpty())
        {
            return null;
        }
        ItemStack theList = new ItemStack(ModItems.itemDeathList, 1);
        theList.setTagCompound(allNBT);
        return theList;
    }

    public static NBTTagCompound getSavedInventoryAsNBT(String playerName, String timestamp)
    {
        File file = getFile(playerName, timestamp);
        if (file == null || !file.isFile())
        {
            return null;
        }
        try
        {
            return JsonToNBT.getTagFromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
