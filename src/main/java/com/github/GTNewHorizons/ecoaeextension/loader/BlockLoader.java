package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.item.ItemStack;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.block.BlockECOAEItemBlock;
import com.github.GTNewHorizons.ecoaeextension.block.BlockECOAEMeta;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Block registration for ECOAE Extension.
 * Registers three meta blocks (one per subsystem) with metadata variants for each block type.
 */
public class BlockLoader {

    // EStorage blocks (meta: 0=casing, 1=cell_drive, 2=energy_cell, 3=me_channel, 4=vent)
    public static BlockECOAEMeta estorageBlocks;
    public static final int ESTORAGE_META_CASING = 0;
    public static final int ESTORAGE_META_CELL_DRIVE = 1;
    public static final int ESTORAGE_META_ENERGY_CELL = 2;
    public static final int ESTORAGE_META_ME_CHANNEL = 3;
    public static final int ESTORAGE_META_VENT = 4;

    // ECalculator blocks (meta: 0=casing, 1=thread_core, 2=hyper_thread, 3=parallel_proc, 4=cell_drive, 5=me_channel,
    // 6=transmitter_bus, 7=tail)
    public static BlockECOAEMeta ecalculatorBlocks;
    public static final int ECALC_META_CASING = 0;
    public static final int ECALC_META_THREAD_CORE = 1;
    public static final int ECALC_META_HYPER_THREAD = 2;
    public static final int ECALC_META_PARALLEL_PROC = 3;
    public static final int ECALC_META_CELL_DRIVE = 4;
    public static final int ECALC_META_ME_CHANNEL = 5;
    public static final int ECALC_META_TRANSMITTER_BUS = 6;
    public static final int ECALC_META_TAIL = 7;

    // EFabricator blocks (meta: 0=casing, 1=worker, 2=pattern_bus, 3=parallel_proc, 4=me_channel, 5=vent)
    public static BlockECOAEMeta efabricatorBlocks;
    public static final int EFAB_META_CASING = 0;
    public static final int EFAB_META_WORKER = 1;
    public static final int EFAB_META_PATTERN_BUS = 2;
    public static final int EFAB_META_PARALLEL_PROC = 3;
    public static final int EFAB_META_ME_CHANNEL = 4;
    public static final int EFAB_META_VENT = 5;

    public static void registerBlocks() {
        ECOAEExtension.LOG.info("Registering ECOAE Extension blocks...");

        // EStorage blocks
        estorageBlocks = new BlockECOAEMeta(
            "estorage_blocks",
            new String[] { "casing", "cell_drive", "energy_cell", "me_channel", "vent" },
            new String[] { "blocks/estorage/casing", "blocks/estorage/cell_drive", "blocks/estorage/energy_cell",
                "blocks/estorage/me_channel", "blocks/estorage/vent" });
        GameRegistry.registerBlock(estorageBlocks, BlockECOAEItemBlock.class, "estorage_blocks");

        // ECalculator blocks
        ecalculatorBlocks = new BlockECOAEMeta(
            "ecalculator_blocks",
            new String[] { "casing", "thread_core", "hyper_thread", "parallel_proc", "cell_drive", "me_channel",
                "transmitter_bus", "tail" },
            new String[] { "blocks/ecalculator/casing", "blocks/ecalculator/thread_core",
                "blocks/ecalculator/hyper_thread", "blocks/ecalculator/parallel_proc", "blocks/ecalculator/cell_drive",
                "blocks/ecalculator/me_channel", "blocks/ecalculator/transmitter_bus", "blocks/ecalculator/tail" });
        GameRegistry.registerBlock(ecalculatorBlocks, BlockECOAEItemBlock.class, "ecalculator_blocks");

        // EFabricator blocks
        efabricatorBlocks = new BlockECOAEMeta(
            "efabricator_blocks",
            new String[] { "casing", "worker", "pattern_bus", "parallel_proc", "me_channel", "vent" },
            new String[] { "blocks/efabricator/casing", "blocks/efabricator/worker", "blocks/efabricator/pattern_bus",
                "blocks/efabricator/parallel_proc", "blocks/efabricator/me_channel", "blocks/efabricator/vent" });
        GameRegistry.registerBlock(efabricatorBlocks, BlockECOAEItemBlock.class, "efabricator_blocks");

        ECOAEExtension.LOG.info("ECOAE Extension blocks registered.");
    }

    public static void registerBlockRenderers() {
        // Block renderers are handled automatically by Forge for standard blocks
    }

    // Helper methods to get specific block stacks
    public static ItemStack getEStorageBlock(int meta, int amount) {
        return new ItemStack(estorageBlocks, amount, meta);
    }

    public static ItemStack getECalculatorBlock(int meta, int amount) {
        return new ItemStack(ecalculatorBlocks, amount, meta);
    }

    public static ItemStack getEFabricatorBlock(int meta, int amount) {
        return new ItemStack(efabricatorBlocks, amount, meta);
    }
}
