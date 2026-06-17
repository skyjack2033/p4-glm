package com.github.GTNewHorizons.ecoaeextension.ae2;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;

/**
 * Manages crafting acceleration for the ECalculator multiblock.
 *
 * <p>
 * The ECalculator provides virtual CPUs to the AE2 crafting network. This handler
 * coordinates the crafting acceleration logic, including parallel processing based on
 * thread cores and hyper-thread cores.
 * </p>
 *
 * <p>
 * The actual AE2 crafting provider registration (ICraftingProvider, ICraftingCPU)
 * is handled directly by {@link ECalculatorController}. This handler manages the
 * acceleration pipeline and parallel execution.
 * </p>
 */
public class ECalculatorCraftingHandler {

    private final ECalculatorController controller;
    private boolean active = false;

    public ECalculatorCraftingHandler(ECalculatorController controller) {
        this.controller = controller;
    }

    public void activate() {
        if (active) return;
        ECOAEExtension.LOG.info("ECalculator crafting handler activated");
        active = true;
    }

    public void deactivate() {
        if (!active) return;
        ECOAEExtension.LOG.info("ECalculator crafting handler deactivated");
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Process a crafting tick. Called every server tick to accelerate crafting jobs.
     * The ECalculator accelerates by processing multiple crafting steps per tick,
     * based on the total thread count (thread cores + hyper-thread cores) and
     * the tier's parallel multiplier.
     *
     * <p>
     * Crafting acceleration is handled by the controller's ICraftingCPU implementation
     * which provides parallel processing via processCraftingJobs().
     * </p>
     */
    public void processCraftingTick() {
        if (!active) return;
        if (controller == null) return;

        // The crafting acceleration is handled by the controller's
        // ICraftingCPU implementation (processCraftingJobs in onPostTick).
        // This handler provides the interface for future expansion
        // (e.g., custom acceleration modes, coolant-based boosts).
    }

    /**
     * Get the effective parallel crafting count, combining the tier's base parallelism
     * with the installed thread cores and hyper-thread cores.
     */
    public int getParallelCraftingCount() {
        return controller.getParallelCount();
    }

    /**
     * Get the hyper-thread cost multiplier. Hyper-threads increase parallelism
     * but consume additional storage bytes per operation.
     */
    public double getHyperThreadCostMultiplier() {
        return controller.getHyperThreadCostMultiplier();
    }
}
