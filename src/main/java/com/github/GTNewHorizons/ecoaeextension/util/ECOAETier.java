package com.github.GTNewHorizons.ecoaeextension.util;

import com.github.GTNewHorizons.ecoaeextension.Config;

/**
 * Tier system for ECOAE Extension multiblocks. Maps the original L4/L6/L9 tiers to GregTech
 * voltage tiers and provides tier-specific configuration values.
 */
public enum ECOAETier {

    L4("L4", 3, 512, 1),
    L6("L6", 5, 2048, 4),
    L9("L9", 6, 8192, 16);

    public final String name;
    public final int voltageTier;
    public final long voltage;
    public final int parallelMultiplier;

    ECOAETier(String name, int voltageTier, long voltage, int parallelMultiplier) {
        this.name = name;
        this.voltageTier = voltageTier;
        this.voltage = voltage;
        this.parallelMultiplier = parallelMultiplier;
    }

    /**
     * Determine the tier from the maximum voltage tier of attached energy hatches.
     *
     * @param maxVoltageTier The highest voltage tier found in the multiblock's energy hatches
     * @return The corresponding ECOAETier, defaults to L4 if below HV
     */
    public static ECOAETier fromVoltageTier(int maxVoltageTier) {
        if (maxVoltageTier >= 6) return L9; // LuV = tier 6
        if (maxVoltageTier >= 5) return L6; // IV = tier 5
        return L4;
    }

    /**
     * Get the tier from actual voltage value.
     *
     * @param voltage The voltage in EU/t
     * @return The corresponding ECOAETier
     */
    public static ECOAETier fromVoltage(long voltage) {
        if (voltage >= 8192) return L9;
        if (voltage >= 2048) return L6;
        return L4;
    }

    /**
     * Get the maximum number of cell drives allowed for this tier from Config.
     *
     * @return The max cell drives for this tier
     */
    public long getEnergyCellCapacity() {
        switch (this) {
            case L9:
                return Config.eStorageEnergyCellCapacityL9;
            case L6:
                return Config.eStorageEnergyCellCapacityL6;
            default:
                return Config.eStorageEnergyCellCapacityL4;
        }
    }

    /**
     * Get the parallel proc value per block for ECalculator at this tier from Config.
     *
     * @return The parallel proc value per block
     */
    public int getCalculatorParallelProc() {
        switch (this) {
            case L9:
                return Config.eCalculatorParallelProcL9;
            case L6:
                return Config.eCalculatorParallelProcL6;
            default:
                return Config.eCalculatorParallelProcL4;
        }
    }

    /**
     * Get the parallel proc value per block for EFabricator at this tier from Config.
     *
     * @return The parallel proc value per block
     */
    public int getFabricatorParallelProc() {
        switch (this) {
            case L9:
                return Config.eFabricatorParallelProcL9;
            case L6:
                return Config.eFabricatorParallelProcL6;
            default:
                return Config.eFabricatorParallelProcL4;
        }
    }

    /**
     * Get a human-readable voltage string for display purposes.
     *
     * @return A formatted string like "512 EU/t"
     */
    public String getVoltageForDisplay() {
        return voltage + " EU/t";
    }
}
