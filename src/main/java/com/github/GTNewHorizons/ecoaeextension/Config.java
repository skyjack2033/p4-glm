package com.github.GTNewHorizons.ecoaeextension;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    // General
    public static boolean debugMode = false;

    // EStorage - energy cell capacity per segment (RF -> EU, 1 EU = 4 RF)
    public static long eStorageEnergyCellCapacityL4 = 2_500_000; // 10M RF / 4
    public static long eStorageEnergyCellCapacityL6 = 25_000_000; // 100M RF / 4
    public static long eStorageEnergyCellCapacityL9 = 250_000_000; // 1B RF / 4
    public static long eStorageCellDriveCapacityL4 = 65_536_000L; // 64M * 1024 bytes
    public static long eStorageCellDriveCapacityL6 = 1_048_576_000L; // 1024M * 1024 bytes
    public static long eStorageCellDriveCapacityL9 = 16_777_216_000L; // 16384M * 1024 bytes

    // ECalculator - parallel proc values per block
    public static int eCalculatorParallelProcL4 = 256;
    public static int eCalculatorParallelProcL6 = 2048;
    public static int eCalculatorParallelProcL9 = 16384;
    public static int eCalculatorThreadsPerCoreL4 = 1;
    public static int eCalculatorThreadsPerCoreL6 = 2;
    public static int eCalculatorThreadsPerCoreL9 = 4;
    public static int eCalculatorHyperThreadsL4 = 2;
    public static int eCalculatorHyperThreadsL6 = 4;
    public static int eCalculatorHyperThreadsL9 = 8;

    // EFabricator - parallel proc values per block
    public static int eFabricatorParallelProcL4 = 24;
    public static int eFabricatorParallelProcL6 = 72;
    public static int eFabricatorParallelProcL9 = 256;
    public static int eFabricatorWorkerQueueDepth = 32;
    public static int eFabricatorWorkDelay = 20; // ticks

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        // General
        debugMode = configuration
            .getBoolean("debugMode", Configuration.CATEGORY_GENERAL, debugMode, "Enable debug logging");

        // EStorage - energy cell capacity per segment (EU). Values fit in int range.
        eStorageEnergyCellCapacityL4 = configuration
            .get(
                "EStorage",
                "eStorageEnergyCellCapacityL4",
                (int) eStorageEnergyCellCapacityL4,
                "Energy cell capacity for L4 tier (EU)")
            .getInt();
        eStorageEnergyCellCapacityL6 = configuration
            .get(
                "EStorage",
                "eStorageEnergyCellCapacityL6",
                (int) eStorageEnergyCellCapacityL6,
                "Energy cell capacity for L6 tier (EU)")
            .getInt();
        eStorageEnergyCellCapacityL9 = configuration
            .get(
                "EStorage",
                "eStorageEnergyCellCapacityL9",
                (int) eStorageEnergyCellCapacityL9,
                "Energy cell capacity for L9 tier (EU)")
            .getInt();

        // EStorage - cell drive capacity in bytes. These are long values stored as string.
        eStorageCellDriveCapacityL4 = Long.parseLong(
            configuration
                .get(
                    "EStorage",
                    "eStorageCellDriveCapacityL4",
                    String.valueOf(eStorageCellDriveCapacityL4),
                    "Cell drive capacity for L4 tier (bytes)")
                .getString());
        eStorageCellDriveCapacityL6 = Long.parseLong(
            configuration
                .get(
                    "EStorage",
                    "eStorageCellDriveCapacityL6",
                    String.valueOf(eStorageCellDriveCapacityL6),
                    "Cell drive capacity for L6 tier (bytes)")
                .getString());
        eStorageCellDriveCapacityL9 = Long.parseLong(
            configuration
                .get(
                    "EStorage",
                    "eStorageCellDriveCapacityL9",
                    String.valueOf(eStorageCellDriveCapacityL9),
                    "Cell drive capacity for L9 tier (bytes)")
                .getString());

        // ECalculator
        eCalculatorParallelProcL4 = configuration.getInt(
            "eCalculatorParallelProcL4",
            "ECalculator",
            eCalculatorParallelProcL4,
            1,
            65536,
            "Parallel proc value per block L4");
        eCalculatorParallelProcL6 = configuration.getInt(
            "eCalculatorParallelProcL6",
            "ECalculator",
            eCalculatorParallelProcL6,
            1,
            65536,
            "Parallel proc value per block L6");
        eCalculatorParallelProcL9 = configuration.getInt(
            "eCalculatorParallelProcL9",
            "ECalculator",
            eCalculatorParallelProcL9,
            1,
            65536,
            "Parallel proc value per block L9");

        // EFabricator
        eFabricatorParallelProcL4 = configuration.getInt(
            "eFabricatorParallelProcL4",
            "EFabricator",
            eFabricatorParallelProcL4,
            1,
            65536,
            "Parallel proc value per block L4");
        eFabricatorParallelProcL6 = configuration.getInt(
            "eFabricatorParallelProcL6",
            "EFabricator",
            eFabricatorParallelProcL6,
            1,
            65536,
            "Parallel proc value per block L6");
        eFabricatorParallelProcL9 = configuration.getInt(
            "eFabricatorParallelProcL9",
            "EFabricator",
            eFabricatorParallelProcL9,
            1,
            65536,
            "Parallel proc value per block L9");
        eFabricatorWorkerQueueDepth = configuration.getInt(
            "eFabricatorWorkerQueueDepth",
            "EFabricator",
            eFabricatorWorkerQueueDepth,
            1,
            1024,
            "Worker queue depth");
        eFabricatorWorkDelay = configuration
            .getInt("eFabricatorWorkDelay", "EFabricator", eFabricatorWorkDelay, 1, 100, "Work delay in ticks");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
