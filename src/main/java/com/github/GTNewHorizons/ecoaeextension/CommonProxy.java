package com.github.GTNewHorizons.ecoaeextension;

import com.github.GTNewHorizons.ecoaeextension.loader.BlockLoader;
import com.github.GTNewHorizons.ecoaeextension.loader.ItemLoader;
import com.github.GTNewHorizons.ecoaeextension.loader.MachineLoader;
import com.github.GTNewHorizons.ecoaeextension.loader.RecipeLoader;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ECOAEExtension.LOG.info("ECOAE Extension v" + Tags.VERSION + " pre-initializing...");

        // Register blocks and items
        BlockLoader.registerBlocks();
        ItemLoader.registerItems();
    }

    public void init(FMLInitializationEvent event) {
        // Register machines (requires GT to be loaded)
        MachineLoader.registerMachines();

        // Register GUI handler
        cpw.mods.fml.common.network.NetworkRegistry.INSTANCE
            .registerGuiHandler(ECOAEExtension.instance, new com.github.GTNewHorizons.ecoaeextension.gui.ECOAEGuiHandler());
    }

    public void postInit(FMLPostInitializationEvent event) {
        // Register recipes after all mods are loaded
        RecipeLoader.registerRecipes();
        ECOAEExtension.LOG.info("ECOAE Extension post-initialization complete.");
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
