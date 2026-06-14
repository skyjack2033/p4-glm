package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.item.ItemStack;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;

/**
 * Recipe registration for ECOAE Extension.
 *
 * Registers assembler recipes for all multiblock structure blocks using GT5U's assembler system.
 * EStorage = HV tier (480 EU/t), ECalculator = IV tier (1920 EU/t), EFabricator = LuV tier (7680 EU/t).
 */
public class RecipeLoader {

    public static void registerRecipes() {
        ECOAEExtension.LOG.info("Registering ECOAE Extension recipes...");

        registerEStorageRecipes();
        registerECalculatorRecipes();
        registerEFabricatorRecipes();

        ECOAEExtension.LOG.info("ECOAE Extension recipes registered.");
    }

    // =========================================================================
    // EStorage Recipes (HV tier - 480 EU/t)
    // =========================================================================

    private static void registerEStorageRecipes() {
        ItemStack casing = BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1);

        // EStorage Casing: Steel plates (6) + HV Circuit + Emitter (HV)
        GTValues.RA.stdBuilder()
            .itemInputs(getPlateStack(Materials.Steel, 6), getHVChip(), ItemList.Emitter_HV.get(1))
            .itemOutputs(casing)
            .eut(480)
            .duration(200)
            .addTo(RecipeMaps.assemblerRecipes);

        // EStorage Cell Drive: Casing + HV Circuit + AE2 storage component
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getHVChip(), getAE2StorageComponent())
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CELL_DRIVE, 1))
            .eut(480)
            .duration(300)
            .addTo(RecipeMaps.assemblerRecipes);

        // EStorage Energy Cell: Casing + Solar Panel (HV) + Lapotronic Orb
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), ItemList.Cover_SolarPanel_HV.get(1), ItemList.Energy_LapotronicOrb.get(1))
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_ENERGY_CELL, 1))
            .eut(480)
            .duration(250)
            .addTo(RecipeMaps.assemblerRecipes);

        // EStorage ME Channel: Casing + HV Circuit + AE2 ME Glass Cable
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getHVChip(), getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_ME_CHANNEL, 1))
            .eut(480)
            .duration(200)
            .addTo(RecipeMaps.assemblerRecipes);

        // EStorage Vent: Casing + Iron Bars + Motor (HV)
        GTValues.RA.stdBuilder()
            .itemInputs(
                casing.copy(),
                new ItemStack(net.minecraft.init.Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_VENT, 1))
            .eut(120)
            .duration(100)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    // =========================================================================
    // ECalculator Recipes (IV tier - 1920 EU/t)
    // =========================================================================

    private static void registerECalculatorRecipes() {
        ItemStack casing = BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1);

        // ECalculator Casing: Aluminium plates (6) + IV Circuit + CPU Chip
        GTValues.RA.stdBuilder()
            .itemInputs(getPlateStack(Materials.Aluminium, 6), getIVChip(), ItemList.Circuit_Chip_CPU.get(1))
            .itemOutputs(casing)
            .eut(1920)
            .duration(300)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Thread Core: Casing + IV Circuit + RAM Chip
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getIVChip(), ItemList.Circuit_Chip_Ram.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_THREAD_CORE, 1))
            .eut(1920)
            .duration(400)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Hyper-Thread Core: Thread Core + IV Circuit + Naquadah Alloy
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_THREAD_CORE, 1),
                getIVChip(),
                gregtech.api.util.GTOreDictUnificator.get(OrePrefixes.ingot, Materials.NaquadahAlloy, 1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_HYPER_THREAD, 1))
            .eut(1920)
            .duration(500)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Parallel Processor: Casing + IV Circuit + CPU Chip
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getIVChip(), ItemList.Circuit_Chip_CPU.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_PARALLEL_PROC, 1))
            .eut(1920)
            .duration(350)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Cell Drive: Casing + IV Circuit + AE2 storage
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getIVChip(), getAE2StorageComponent())
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CELL_DRIVE, 1))
            .eut(1920)
            .duration(300)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator ME Channel: Casing + IV Circuit + AE2 cable
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getIVChip(), getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_ME_CHANNEL, 1))
            .eut(1920)
            .duration(250)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Transmitter Bus: Casing + IV Circuit + Emitter (IV)
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getIVChip(), ItemList.Emitter_IV.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_TRANSMITTER_BUS, 1))
            .eut(1920)
            .duration(300)
            .addTo(RecipeMaps.assemblerRecipes);

        // ECalculator Tail: Casing + Heat Vent
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), ItemList.Reactor_Coolant_Sp_6.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_TAIL, 1))
            .eut(1920)
            .duration(200)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    // =========================================================================
    // EFabricator Recipes (LuV tier - 7680 EU/t)
    // =========================================================================

    private static void registerEFabricatorRecipes() {
        ItemStack casing = BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1);

        // EFabricator Casing: TungstenSteel plates (6) + LuV Circuit + Robot Arm (LuV)
        GTValues.RA.stdBuilder()
            .itemInputs(getPlateStack(Materials.TungstenSteel, 6), getLuVChip(), ItemList.Robot_Arm_LuV.get(1))
            .itemOutputs(casing)
            .eut(7680)
            .duration(400)
            .addTo(RecipeMaps.assemblerRecipes);

        // EFabricator Worker Core: Casing + LuV Circuit + Crafting Table
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getLuVChip(), new ItemStack(net.minecraft.init.Blocks.crafting_table))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_WORKER, 1))
            .eut(7680)
            .duration(500)
            .addTo(RecipeMaps.assemblerRecipes);

        // EFabricator Pattern Bus: Casing + LuV Circuit + AE2 Blank Pattern
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getLuVChip(), getAE2BlankPattern())
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_PATTERN_BUS, 1))
            .eut(7680)
            .duration(350)
            .addTo(RecipeMaps.assemblerRecipes);

        // EFabricator Parallel Processor: Casing + LuV Circuit + CPU Chip
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getLuVChip(), ItemList.Circuit_Chip_CPU.get(1))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_PARALLEL_PROC, 1))
            .eut(7680)
            .duration(400)
            .addTo(RecipeMaps.assemblerRecipes);

        // EFabricator ME Channel: Casing + LuV Circuit + AE2 cable
        GTValues.RA.stdBuilder()
            .itemInputs(casing.copy(), getLuVChip(), getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_ME_CHANNEL, 1))
            .eut(7680)
            .duration(300)
            .addTo(RecipeMaps.assemblerRecipes);

        // EFabricator Vent: Casing + Iron Bars + Motor (HV)
        GTValues.RA.stdBuilder()
            .itemInputs(
                casing.copy(),
                new ItemStack(net.minecraft.init.Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_VENT, 1))
            .eut(1920)
            .duration(150)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private static ItemStack getPlateStack(Materials material, int amount) {
        return gregtech.api.util.GTOreDictUnificator.get(OrePrefixes.plate, material, amount);
    }

    /** HV circuit */
    private static ItemStack getHVChip() {
        return ItemList.Circuit_Advanced.get(1);
    }

    /** IV circuit */
    private static ItemStack getIVChip() {
        return ItemList.Circuit_Elite.get(1);
    }

    /** LuV circuit */
    private static ItemStack getLuVChip() {
        return ItemList.Circuit_Master.get(1);
    }

    private static ItemStack getAE2StorageComponent() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 36);
    }

    private static ItemStack getAE2MEGlassCable() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiPart", 1, 76);
    }

    private static ItemStack getAE2BlankPattern() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiPart", 1, 520);
    }
}
