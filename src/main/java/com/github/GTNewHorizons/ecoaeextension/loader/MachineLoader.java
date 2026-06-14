package com.github.GTNewHorizons.ecoaeextension.loader;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

/**
 * Machine registration for ECOAE Extension. Each multiblock controller gets a unique machine ID.
 * In GTNH, machine IDs must be unique across all mods. We use a dedicated range starting at 19500
 * to avoid conflicts with other popular GTNH mods.
 *
 * Machine registration happens automatically when the MetaTileEntity constructor is called with an
 * ID - the constructor internally registers with GregTech's MetaTileEntity registry.
 */
public class MachineLoader {

    // Machine ID range: 19500-19599 (reserved for ECOAE Extension)
    // EStorage controllers: 19500-19509
    public static final int ID_ESTORAGE_CONTROLLER_L4 = 19500;
    public static final int ID_ESTORAGE_CONTROLLER_L6 = 19501;
    public static final int ID_ESTORAGE_CONTROLLER_L9 = 19502;

    // ECalculator controllers: 19510-19519
    public static final int ID_ECALCULATOR_CONTROLLER_L4 = 19510;
    public static final int ID_ECALCULATOR_CONTROLLER_L6 = 19511;
    public static final int ID_ECALCULATOR_CONTROLLER_L9 = 19512;

    // EFabricator controllers: 19520-19529
    public static final int ID_EFABRICATOR_CONTROLLER_L4 = 19520;
    public static final int ID_EFABRICATOR_CONTROLLER_L6 = 19521;
    public static final int ID_EFABRICATOR_CONTROLLER_L9 = 19522;

    // Custom hatches: 19530-19549
    public static final int ID_HATCH_AE_STORAGE_BUS = 19530;
    public static final int ID_HATCH_AE_PATTERN_PROVIDER = 19531;

    /**
     * Register all machines. Called during init phase.
     * Each new constructor call automatically registers the MetaTileEntity with GT.
     */
    public static void registerMachines() {
        ECOAEExtension.LOG.info("Registering ECOAE Extension machines...");

        // EStorage controllers - L4/L6/L9 tiers
        new EStorageController(ID_ESTORAGE_CONTROLLER_L4, "ECOAE_EStorage_L4", "EStorage Controller L4");
        new EStorageController(ID_ESTORAGE_CONTROLLER_L6, "ECOAE_EStorage_L6", "EStorage Controller L6");
        new EStorageController(ID_ESTORAGE_CONTROLLER_L9, "ECOAE_EStorage_L9", "EStorage Controller L9");

        // ECalculator controllers - L4/L6/L9 tiers
        new ECalculatorController(ID_ECALCULATOR_CONTROLLER_L4, "ECOAE_ECalculator_L4", "ECalculator Controller L4");
        new ECalculatorController(ID_ECALCULATOR_CONTROLLER_L6, "ECOAE_ECalculator_L6", "ECalculator Controller L6");
        new ECalculatorController(ID_ECALCULATOR_CONTROLLER_L9, "ECOAE_ECalculator_L9", "ECalculator Controller L9");

        // EFabricator controllers - L4/L6/L9 tiers
        new EFabricatorController(ID_EFABRICATOR_CONTROLLER_L4, "ECOAE_EFabricator_L4", "EFabricator Controller L4");
        new EFabricatorController(ID_EFABRICATOR_CONTROLLER_L6, "ECOAE_EFabricator_L6", "EFabricator Controller L6");
        new EFabricatorController(ID_EFABRICATOR_CONTROLLER_L9, "ECOAE_EFabricator_L9", "EFabricator Controller L9");

        // Custom AE2 hatches - TODO: implement after base multiblocks are working
        // new HatchAEStorageBus(ID_HATCH_AE_STORAGE_BUS, "ECOAE_AE_Storage_Bus", "AE Storage Bus Hatch");
        // new HatchAEPatternProvider(ID_HATCH_AE_PATTERN_PROVIDER, "ECOAE_AE_Pattern_Provider", "AE Pattern Provider Hatch");

        ECOAEExtension.LOG.info("ECOAE Extension machines registered.");
    }
}
