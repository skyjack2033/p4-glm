package com.github.GTNewHorizons.ecoaeextension;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = ECOAEExtension.MODID,
    version = Tags.VERSION,
    name = "ECOAE Extension",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech; required-after:appliedenergistics2; after:structurelib")
public class ECOAEExtension {

    public static final String MODID = "ecoaeext";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(ECOAEExtension.MODID)
    public static ECOAEExtension instance;

    @SidedProxy(
        clientSide = "com.github.GTNewHorizons.ecoaeextension.ClientProxy",
        serverSide = "com.github.GTNewHorizons.ecoaeextension.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
