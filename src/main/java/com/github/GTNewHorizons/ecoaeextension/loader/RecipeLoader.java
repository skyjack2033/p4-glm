package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.init.Blocks;
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
 * Registers assembler recipes for all multiblock structure blocks (casings, components, etc.)
 * using the standard GT5U assembler recipe system. Recipes are organized by multiblock type:
 * <ul>
 *   <li>EStorage (HV tier, 480 EU/t)</li>
 *   <li>ECalculator (IV tier, 1920 EU/t)</li>
 *   <li>EFabricator (LuV tier, 7680 EU/t)</li>
 * </ul>
 *
 * Note: Recipe registration requires all mods to be loaded, so this runs during postInit.
 */
public class RecipeLoader {

    /**
     * Register all recipes. Called during postInit after all mods are loaded.
     */
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

    /**
     * Register EStorage block build recipes.
     * EStorage components use HV-tier circuits and steel construction.
     */
    private static void registerEStorageRecipes() {
        ECOAEExtension.LOG.debug("Registering EStorage recipes...");

        // Recipe 1: EStorage Casing
        // Steel plates (6) + HV Circuit + Emitter (HV) -> EStorage Casing
        // 480 EU/t, 200 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.Steel, 6),
                ItemList.Circuit_HV.get(1),
                ItemList.Emitter_HV.get(1))
            .itemOutputs(getBlockOutput("EStorage Casing"))
            .eut(480)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 2: EStorage Cell Drive
        // EStorage Casing + HV Circuit + AE2 storage component -> Cell Drive
        // 480 EU/t, 300 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EStorage Casing"),
                ItemList.Circuit_HV.get(1),
                getAE2StorageComponent())
            .itemOutputs(getBlockOutput("EStorage Cell Drive"))
            .eut(480)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 3: EStorage Energy Cell
        // EStorage Casing + HV Capacitor + Lapotron Crystal -> Energy Cell
        // 480 EU/t, 250 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EStorage Casing"),
                ItemList.Cover_Capacitor.get(1),
                ItemList.Energy_LapotronCrystal.get(1))
            .itemOutputs(getBlockOutput("EStorage Energy Cell"))
            .eut(480)
            .duration(250)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 4: EStorage ME Channel
        // EStorage Casing + HV Circuit + AE2 ME Glass Cable -> ME Channel
        // 480 EU/t, 200 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EStorage Casing"),
                ItemList.Circuit_HV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(getBlockOutput("EStorage ME Channel"))
            .eut(480)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 5: EStorage Vent
        // EStorage Casing + Iron Bars + Fan (HV) -> Vent
        // 120 EU/t, 100 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EStorage Casing"),
                new ItemStack(Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(getBlockOutput("EStorage Vent"))
            .eut(120)
            .duration(100)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        ECOAEExtension.LOG.debug("EStorage recipes registered");
    }

    // =========================================================================
    // ECalculator Recipes (IV tier - 1920 EU/t)
    // =========================================================================

    /**
     * Register ECalculator block build recipes.
     * ECalculator components use IV-tier circuits, aluminium construction, and computing elements.
     */
    private static void registerECalculatorRecipes() {
        ECOAEExtension.LOG.debug("Registering ECalculator recipes...");

        // Recipe 6: ECalculator Casing
        // Aluminium plates (6) + IV Circuit + Processor (IV) -> ECalculator Casing
        // 1920 EU/t, 300 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.Aluminium, 6),
                ItemList.Circuit_IV.get(1),
                ItemList.Processor_IV.get(1))
            .itemOutputs(getBlockOutput("ECalculator Casing"))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 7: ECalculator Thread Core
        // ECalculator Casing + IV Circuit + RAM Chip -> Thread Core
        // 1920 EU/t, 400 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.Circuit_IV.get(1),
                ItemList.Circuit_Chip_Ram.get(1))
            .itemOutputs(getBlockOutput("ECalculator Thread Core"))
            .eut(1920)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 8: ECalculator Hyper-Thread Core
        // Thread Core + IV Circuit + Naquadah Alloy Ingot -> Hyper-Thread Core
        // 1920 EU/t, 500 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Thread Core"),
                ItemList.Circuit_IV.get(1),
                OrePrefixes.ingot.get(Materials.NaquadahAlloy))
            .itemOutputs(getBlockOutput("ECalculator Hyper-Thread Core"))
            .eut(1920)
            .duration(500)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 9: ECalculator Parallel Processor
        // ECalculator Casing + IV Circuit + CPU Chip -> Parallel Processor
        // 1920 EU/t, 350 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.Circuit_IV.get(1),
                ItemList.Circuit_Chip_Cpu.get(1))
            .itemOutputs(getBlockOutput("ECalculator Parallel Processor"))
            .eut(1920)
            .duration(350)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 10: ECalculator Cell Drive
        // ECalculator Casing + IV Circuit + AE2 storage component -> Cell Drive
        // 1920 EU/t, 300 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.Circuit_IV.get(1),
                getAE2StorageComponent())
            .itemOutputs(getBlockOutput("ECalculator Cell Drive"))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 11: ECalculator ME Channel
        // ECalculator Casing + IV Circuit + AE2 ME Glass Cable -> ME Channel
        // 1920 EU/t, 250 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.Circuit_IV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(getBlockOutput("ECalculator ME Channel"))
            .eut(1920)
            .duration(250)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 12: ECalculator Transmitter Bus
        // ECalculator Casing + IV Circuit + Emitter (IV) -> Transmitter Bus
        // 1920 EU/t, 300 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.Circuit_IV.get(1),
                ItemList.Emitter_IV.get(1))
            .itemOutputs(getBlockOutput("ECalculator Transmitter Bus"))
            .eut(1920)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 13: ECalculator Tail
        // ECalculator Casing + Cooling Component -> Tail
        // 1920 EU/t, 200 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("ECalculator Casing"),
                ItemList.CoolantCell_60k.get(1))
            .itemOutputs(getBlockOutput("ECalculator Tail"))
            .eut(1920)
            .duration(200)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        ECOAEExtension.LOG.debug("ECalculator recipes registered");
    }

    // =========================================================================
    // EFabricator Recipes (LuV tier - 7680 EU/t)
    // =========================================================================

    /**
     * Register EFabricator block build recipes.
     * EFabricator components use LuV-tier circuits, TungstenSteel construction, and AE2 pattern elements.
     */
    private static void registerEFabricatorRecipes() {
        ECOAEExtension.LOG.debug("Registering EFabricator recipes...");

        // Recipe 14: EFabricator Casing
        // TungstenSteel plates (6) + LuV Circuit + Robot Arm (LuV) -> EFabricator Casing
        // 7680 EU/t, 400 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getPlateStack(Materials.TungstenSteel, 6),
                ItemList.Circuit_LuV.get(1),
                ItemList.Robot_Arm_LuV.get(1))
            .itemOutputs(getBlockOutput("EFabricator Casing"))
            .eut(7680)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 15: EFabricator Worker Core
        // EFabricator Casing + LuV Circuit + Crafting Table -> Worker Core
        // 7680 EU/t, 500 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EFabricator Casing"),
                ItemList.Circuit_LuV.get(1),
                new ItemStack(Blocks.crafting_table))
            .itemOutputs(getBlockOutput("EFabricator Worker Core"))
            .eut(7680)
            .duration(500)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 16: EFabricator Pattern Bus
        // EFabricator Casing + LuV Circuit + AE2 Pattern -> Pattern Bus
        // 7680 EU/t, 350 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EFabricator Casing"),
                ItemList.Circuit_LuV.get(1),
                getAE2BlankPattern())
            .itemOutputs(getBlockOutput("EFabricator Pattern Bus"))
            .eut(7680)
            .duration(350)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 17: EFabricator Parallel Processor
        // EFabricator Casing + LuV Circuit + CPU Chip -> Parallel Processor
        // 7680 EU/t, 400 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EFabricator Casing"),
                ItemList.Circuit_LuV.get(1),
                ItemList.Circuit_Chip_Cpu.get(1))
            .itemOutputs(getBlockOutput("EFabricator Parallel Processor"))
            .eut(7680)
            .duration(400)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 18: EFabricator ME Channel
        // EFabricator Casing + LuV Circuit + AE2 ME Glass Cable -> ME Channel
        // 7680 EU/t, 300 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EFabricator Casing"),
                ItemList.Circuit_LuV.get(1),
                getAE2MEGlassCable())
            .itemOutputs(getBlockOutput("EFabricator ME Channel"))
            .eut(7680)
            .duration(300)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        // Recipe 19: EFabricator Vent
        // EFabricator Casing + Iron Bars + Fan (HV) -> Vent
        // 1920 EU/t, 150 ticks
        GTValues.RA.stdBuilder()
            .itemInputs(
                getBlockOutput("EFabricator Casing"),
                new ItemStack(Blocks.iron_bars),
                ItemList.Electric_Motor_HV.get(1))
            .itemOutputs(getBlockOutput("EFabricator Vent"))
            .eut(1920)
            .duration(150)
            .addTo(GT_Recipe.GT_Recipe_Map.sAssemblerRecipes);

        ECOAEExtension.LOG.debug("EFabricator recipes registered");
    }

    // =========================================================================
    // Helper methods for recipe inputs
    // =========================================================================

    /**
     * Create an ItemStack of plates with the specified amount.
     *
     * @param material the material type (e.g. Materials.Steel, Materials.TungstenSteel)
     * @param amount   the number of plates
     * @return an ItemStack of the specified plates
     */
    private static ItemStack getPlateStack(Materials material, int amount) {
        ItemStack stack = OrePrefixes.plate.get(material);
        stack.stackSize = amount;
        return stack;
    }

    /**
     * Create a placeholder ItemStack for a custom multiblock block.
     *
     * TODO: Replace with actual block items from BlockLoader when the custom casing/part blocks
     *       are registered. The actual implementation will use GT's MetaTileEntity item system,
     *       e.g. {@code MachineLoader.getMetaTileEntityItem(ID_EFOO_CASING)}.
     *
     * @param blockName descriptive name for logging (unused in placeholder implementation)
     * @return a placeholder ItemStack
     */
    private static ItemStack getBlockOutput(String blockName) {
        // Placeholder - actual implementation uses GT's MTE item system
        return new ItemStack(Blocks.air, 1, 0);
    }

    // =========================================================================
    // Helper methods for AE2 item references
    // =========================================================================

    /**
     * Get an AE2 storage component item for Cell Drive recipes.
     * Uses the 4k storage component as a mid-tier reference.
     *
     * TODO: Verify AE2 item name and metadata value for your GTNH AE2 version.
     *       The metadata value 36 corresponds to the 4k storage component in standard AE2.
     *       Adjust the tier (and metadata) to match the intended progression.
     *
     * @return an AE2 storage component ItemStack
     */
    private static ItemStack getAE2StorageComponent() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 36);
    }

    /**
     * Get an AE2 ME Glass Cable item for ME Channel recipes.
     * Uses the Fluix-colored glass cable.
     *
     * TODO: Verify AE2 item name and metadata value for your GTNH AE2 version.
     *       The metadata value 76 corresponds to Fluix glass cable in standard AE2.
     *
     * @return an AE2 ME Glass Cable ItemStack
     */
    private static ItemStack getAE2MEGlassCable() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiPart", 1, 76);
    }

    /**
     * Get an AE2 Blank Pattern item for Pattern Bus recipes.
     *
     * TODO: Verify AE2 item name and metadata value for your GTNH AE2 version.
     *       The metadata value 520 corresponds to the blank pattern in standard AE2.
     *
     * @return an AE2 Blank Pattern ItemStack
     */
    private static ItemStack getAE2BlankPattern() {
        return GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiPart", 1, 520);
    }
}
