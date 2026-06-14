package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.block.Block;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

/**
 * Block registration for ECOAE Extension. In GTNH, most multiblock parts are registered through
 * GregTech's MetaTileEntity system rather than as standalone blocks. This loader handles any
 * custom blocks that aren't part of the GT machine system.
 */
public class BlockLoader {

    // Custom blocks (if needed beyond GT machine blocks)
    // public static Block ecoaeCasing;

    /**
     * Register all custom blocks. Called during preInit.
     */
    public static void registerBlocks() {
        ECOAEExtension.LOG.info("Registering ECOAE Extension blocks...");

        // In GTNH, multiblock casings and parts are typically registered as MetaTileEntities
        // through MachineLoader. Custom blocks (if any) go here.

        // TODO: Register any custom blocks that aren't GT MetaTileEntities
        // ecoaeCasing = new BlockECOAECasing();
        // GameRegistry.registerBlock(ecoaeCasing, ItemBlockECOAECasing.class, "ecoae_casing");
    }

    /**
     * Register block renderers. Called during client init.
     */
    public static void registerBlockRenderers() {
        // TODO: Register block renderers when custom blocks are implemented
    }
}
