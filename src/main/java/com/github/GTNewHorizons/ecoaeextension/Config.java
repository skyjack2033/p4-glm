package com.github.GTNewHorizons.ecoaeextension;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    // General
    public static boolean debugMode = false;

    // EStorage - energy cell capacity per segment (EU)
    public static long eStorageEnergyCellCapacityL4 = 2_500_000;
    public static long eStorageEnergyCellCapacityL6 = 25_000_000;
    public static long eStorageEnergyCellCapacityL9 = 250_000_000;
    // EStorage - cell drive capacity in bytes (storage per cell)
    public static long eStorageCellDriveCapacityL4 = 65_536_000L;
    public static long eStorageCellDriveCapacityL6 = 1_048_576_000L;
    public static long eStorageCellDriveCapacityL9 = 16_777_216_000L;
    // EStorage - max cell drive blocks allowed in structure
    public static int eStorageMaxCellDrivesL4 = 4;
    public static int eStorageMaxCellDrivesL6 = 8;
    public static int eStorageMaxCellDrivesL9 = 16;

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
    // ECalculator - component counts for fixed structure
    public static int eCalculatorBaseCellDrives = 4;
    public static int eCalculatorBaseParallelProcs = 2;
    public static int eCalculatorBaseTransmitterBuses = 1;

    // EFabricator - parallel proc values per block
    public static int eFabricatorParallelProcL4 = 24;
    public static int eFabricatorParallelProcL6 = 72;
    public static int eFabricatorParallelProcL9 = 256;
    public static int eFabricatorWorkerQueueDepth = 32;
    public static int eFabricatorWorkDelay = 20; // ticks
    // EFabricator - component counts for fixed structure
    public static int eFabricatorBasePatternBuses = 2;
    public static int eFabricatorBaseWorkers = 1;
    public static int eFabricatorBaseProcessors = 2;

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
        eCalculatorBaseCellDrives = configuration.getInt(
            "eCalculatorBaseCellDrives",
            "ECalculator",
            eCalculatorBaseCellDrives,
            1,
            64,
            "Base cell drives in fixed structure");
        eCalculatorBaseParallelProcs = configuration.getInt(
            "eCalculatorBaseParallelProcs",
            "ECalculator",
            eCalculatorBaseParallelProcs,
            1,
            64,
            "Base parallel processors in fixed structure");
        eCalculatorBaseTransmitterBuses = configuration.getInt(
            "eCalculatorBaseTransmitterBuses",
            "ECalculator",
            eCalculatorBaseTransmitterBuses,
            0,
            16,
            "Base transmitter buses in fixed structure");

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
        eFabricatorBasePatternBuses = configuration.getInt(
            "eFabricatorBasePatternBuses",
            "EFabricator",
            eFabricatorBasePatternBuses,
            1,
            64,
            "Base pattern buses in fixed structure");
        eFabricatorBaseWorkers = configuration.getInt(
            "eFabricatorBaseWorkers",
            "EFabricator",
            eFabricatorBaseWorkers,
            1,
            16,
            "Base worker cores in fixed structure");
        eFabricatorBaseProcessors = configuration.getInt(
            "eFabricatorBaseProcessors",
            "EFabricator",
            eFabricatorBaseProcessors,
            1,
            64,
            "Base parallel processors in fixed structure");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
