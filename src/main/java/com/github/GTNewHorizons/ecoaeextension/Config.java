package com.github.GTNewHorizons.ecoaeextension;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    // General
    public static boolean debugMode = false;

    // EStorage
    public static long eStorageBaseCapacityL4 = 1_000_000;
    public static long eStorageBaseCapacityL6 = 16_000_000;
    public static long eStorageBaseCapacityL9 = 256_000_000;
    public static int eStorageMaxCellDrivesL4 = 4;
    public static int eStorageMaxCellDrivesL6 = 8;
    public static int eStorageMaxCellDrivesL9 = 16;

    // ECalculator
    public static int eCalculatorBaseThreadCoresL4 = 4;
    public static int eCalculatorBaseThreadCoresL6 = 8;
    public static int eCalculatorBaseThreadCoresL9 = 16;
    public static double eCalculatorHyperThreadCostMultiplier = 1.1;

    // EFabricator
    public static int eFabricatorPatternBusSlotsL4 = 18;
    public static int eFabricatorPatternBusSlotsL6 = 36;
    public static int eFabricatorPatternBusSlotsL9 = 72;
    public static int eFabricatorWorkerQueueDepth = 32;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        // General
        debugMode = configuration.getBoolean(
            "debugMode",
            Configuration.CATEGORY_GENERAL,
            debugMode,
            "Enable debug logging");

        // EStorage
        eStorageBaseCapacityL4 = configuration.get(
            "EStorage", "eStorageBaseCapacityL4", (int) eStorageBaseCapacityL4,
            "Base storage capacity for L4 tier (bytes)").getInt();
        eStorageBaseCapacityL6 = configuration.get(
            "EStorage", "eStorageBaseCapacityL6", (int) eStorageBaseCapacityL6,
            "Base storage capacity for L6 tier (bytes)").getInt();
        eStorageBaseCapacityL9 = configuration.get(
            "EStorage", "eStorageBaseCapacityL9", (int) eStorageBaseCapacityL9,
            "Base storage capacity for L9 tier (bytes)").getInt();
        eStorageMaxCellDrivesL4 = configuration.getInt(
            "eStorageMaxCellDrivesL4", "EStorage", eStorageMaxCellDrivesL4, 1, 64,
            "Max cell drives for L4 tier");
        eStorageMaxCellDrivesL6 = configuration.getInt(
            "eStorageMaxCellDrivesL6", "EStorage", eStorageMaxCellDrivesL6, 1, 64,
            "Max cell drives for L6 tier");
        eStorageMaxCellDrivesL9 = configuration.getInt(
            "eStorageMaxCellDrivesL9", "EStorage", eStorageMaxCellDrivesL9, 1, 64,
            "Max cell drives for L9 tier");

        // ECalculator
        eCalculatorBaseThreadCoresL4 = configuration.getInt(
            "eCalculatorBaseThreadCoresL4", "ECalculator", eCalculatorBaseThreadCoresL4, 1, 256,
            "Base thread cores for L4 tier");
        eCalculatorBaseThreadCoresL6 = configuration.getInt(
            "eCalculatorBaseThreadCoresL6", "ECalculator", eCalculatorBaseThreadCoresL6, 1, 256,
            "Base thread cores for L6 tier");
        eCalculatorBaseThreadCoresL9 = configuration.getInt(
            "eCalculatorBaseThreadCoresL9", "ECalculator", eCalculatorBaseThreadCoresL9, 1, 256,
            "Base thread cores for L9 tier");
        eCalculatorHyperThreadCostMultiplier = configuration.getFloat(
            "eCalculatorHyperThreadCostMultiplier", "ECalculator",
            (float) eCalculatorHyperThreadCostMultiplier, 1.0f, 10.0f,
            "Cost multiplier for hyper-thread cores (1.1 = 10% more)");

        // EFabricator
        eFabricatorPatternBusSlotsL4 = configuration.getInt(
            "eFabricatorPatternBusSlotsL4", "EFabricator", eFabricatorPatternBusSlotsL4, 1, 256,
            "Pattern bus slots for L4 tier");
        eFabricatorPatternBusSlotsL6 = configuration.getInt(
            "eFabricatorPatternBusSlotsL6", "EFabricator", eFabricatorPatternBusSlotsL6, 1, 256,
            "Pattern bus slots for L6 tier");
        eFabricatorPatternBusSlotsL9 = configuration.getInt(
            "eFabricatorPatternBusSlotsL9", "EFabricator", eFabricatorPatternBusSlotsL9, 1, 256,
            "Pattern bus slots for L9 tier");
        eFabricatorWorkerQueueDepth = configuration.getInt(
            "eFabricatorWorkerQueueDepth", "EFabricator", eFabricatorWorkerQueueDepth, 1, 1024,
            "Worker queue depth");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
