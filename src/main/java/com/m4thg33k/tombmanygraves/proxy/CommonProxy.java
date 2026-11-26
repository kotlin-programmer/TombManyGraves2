package com.m4thg33k.tombmanygraves.proxy;

import org.lwjgl.util.vector.Vector3f;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.TombManyGraves;
import com.m4thg33k.tombmanygraves.blocks.ModBlocks;
import com.m4thg33k.tombmanygraves.events.CommonEvents;
import com.m4thg33k.tombmanygraves.gui.ModGuiHandler;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.network.TMGNetwork;
import com.m4thg33k.tombmanygraves.tiles.ModTiles;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {

    public void preinit(FMLPreInitializationEvent e)
    {
        ModConfigs.preInit(e);
        TMGNetwork.setup();
        ModItems.createItems();
        ModBlocks.preInit();
    }

    public void init(FMLInitializationEvent e)
    {
        NetworkRegistry.INSTANCE.registerGuiHandler(TombManyGraves.INSTANCE, new ModGuiHandler());
        MinecraftForge.EVENT_BUS.register(new CommonEvents());

        ModTiles.init();
    }

    public void postinit(FMLPostInitializationEvent e)
    {

    }

    public void particleStream(Vector3f start, Vector3f end)
    {

    }

    public void pathFX(double x, double y, double z, float r, float g, float b, float size,
                       float motionX, float motionY, float motionZ, float maxAge)
    {

    }

    public void toggleGraveRendering()
    {
    }

    public void toggleGravePositionRendering()
    {

    }
}
