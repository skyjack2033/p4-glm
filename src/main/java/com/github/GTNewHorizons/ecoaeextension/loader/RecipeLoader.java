package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.item.ItemStack;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.GT_Recipe;
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
        // EStorage Casing: Steel plates (6) + HV Circuit + Emitter (HV) -> 1 Casing
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.Steel, 6),
                ItemList.Circuit_HV.get(1),
                ItemList.Emitter_HV.get(1))
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1))
            .eut(480)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EStorage Cell Drive: Casing + HV Circuit + AE2 storage component -> 1 Cell Drive
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1),
                ItemList.Circuit_HV.get(1),
                getAE2StorageComponent())
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CELL_DRIVE, 1))
            .eut(480)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EStorage Energy Cell: Casing + Capacitor + Lapotron Crystal -> 1 Energy Cell
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1),
                ItemList.Cover_Capacitor.get(1),
                ItemList.Energy_LapotronCrystal.get(1))
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_ENERGY_CELL, 1))
            .eut(480)
            .duration(250)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EStorage ME Channel: Casing + HV Circuit + AE2 ME Glass Cable -> 1 ME Channel
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1),
                ItemList.Circuit_HV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_ME_CHANNEL, 1))
            .eut(480)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EStorage Vent: Casing + Iron Bars + Motor (HV) -> 1 Vent
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_CASING, 1),
                new ItemStack(net.minecraft.init.Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(BlockLoader.getEStorageBlock(BlockLoader.ESTORAGE_META_VENT, 1))
            .eut(120)
            .duration(100)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);
    }

    // =========================================================================
    // ECalculator Recipes (IV tier - 1920 EU/t)
    // =========================================================================

    private static void registerECalculatorRecipes() {
        // ECalculator Casing: Aluminium plates (6) + IV Circuit + Processor (IV) -> 1 Casing
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.Aluminium, 6),
                ItemList.Circuit_IV.get(1),
                ItemList.Processor_IV.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Thread Core: Casing + IV Circuit + RAM Chip -> 1 Thread Core
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.Circuit_IV.get(1),
                ItemList.Circuit_Chip_Ram.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_THREAD_CORE, 1))
            .eut(1920)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Hyper-Thread Core: Thread Core + IV Circuit + Naquadah Alloy -> 1 Hyper-Thread
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_THREAD_CORE, 1),
                ItemList.Circuit_IV.get(1),
                OrePrefixes.ingot.get(Materials.NaquadahAlloy))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_HYPER_THREAD, 1))
            .eut(1920)
            .duration(500)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Parallel Processor: Casing + IV Circuit + CPU Chip -> 1 Processor
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.Circuit_IV.get(1),
                ItemList.Circuit_Chip_Cpu.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_PARALLEL_PROC, 1))
            .eut(1920)
            .duration(350)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Cell Drive: Casing + IV Circuit + AE2 storage -> 1 Cell Drive
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.Circuit_IV.get(1),
                getAE2StorageComponent())
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CELL_DRIVE, 1))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator ME Channel: Casing + IV Circuit + AE2 cable -> 1 ME Channel
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.Circuit_IV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_ME_CHANNEL, 1))
            .eut(1920)
            .duration(250)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Transmitter Bus: Casing + IV Circuit + Emitter (IV) -> 1 Transmitter Bus
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.Circuit_IV.get(1),
                ItemList.Emitter_IV.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_TRANSMITTER_BUS, 1))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // ECalculator Tail: Casing + Coolant Cell -> 1 Tail
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_CASING, 1),
                ItemList.CoolantCell_60k.get(1))
            .itemOutputs(BlockLoader.getECalculatorBlock(BlockLoader.ECALC_META_TAIL, 1))
            .eut(1920)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);
    }

    // =========================================================================
    // EFabricator Recipes (LuV tier - 7680 EU/t)
    // =========================================================================

    private static void registerEFabricatorRecipes() {
        // EFabricator Casing: TungstenSteel plates (6) + LuV Circuit + Robot Arm (LuV) -> 1 Casing
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.TungstenSteel, 6),
                ItemList.Circuit_LuV.get(1),
                ItemList.Robot_Arm_LuV.get(1))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1))
            .eut(7680)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EFabricator Worker Core: Casing + LuV Circuit + Crafting Table -> 1 Worker
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1),
                ItemList.Circuit_LuV.get(1),
                new ItemStack(net.minecraft.init.Blocks.crafting_table))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_WORKER, 1))
            .eut(7680)
            .duration(500)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EFabricator Pattern Bus: Casing + LuV Circuit + AE2 Blank Pattern -> 1 Pattern Bus
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1),
                ItemList.Circuit_LuV.get(1),
                getAE2BlankPattern())
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_PATTERN_BUS, 1))
            .eut(7680)
            .duration(350)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EFabricator Parallel Processor: Casing + LuV Circuit + CPU Chip -> 1 Processor
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1),
                ItemList.Circuit_LuV.get(1),
                ItemList.Circuit_Chip_Cpu.get(1))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_PARALLEL_PROC, 1))
            .eut(7680)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EFabricator ME Channel: Casing + LuV Circuit + AE2 cable -> 1 ME Channel
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1),
                ItemList.Circuit_LuV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_ME_CHANNEL, 1))
            .eut(7680)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // EFabricator Vent: Casing + Iron Bars + Motor (HV) -> 1 Vent
        GTValues.RA.stdBuilder()
            .itemInputs(
                BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_CASING, 1),
                new ItemStack(net.minecraft.init.Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(BlockLoader.getEFabricatorBlock(BlockLoader.EFAB_META_VENT, 1))
            .eut(1920)
            .duration(150)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private static ItemStack getPlateStack(Materials material, int amount) {
        ItemStack stack = OrePrefixes.plate.get(material);
        stack.stackSize = amount;
        return stack;
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
