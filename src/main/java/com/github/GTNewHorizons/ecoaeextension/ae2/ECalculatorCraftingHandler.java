package com.github.GTNewHorizons.ecoaeextension.ae2;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;

public class ECalculatorCraftingHandler {

    private final ECalculatorController controller;
    private boolean active = false;

    public ECalculatorCraftingHandler(ECalculatorController controller) {
        this.controller = controller;
    }

    public void activate() {
        if (active) return;
        ECOAEExtension.LOG.info("ECalculator crafting handler activated");
        // The actual AE2 crafting provider registration is handled by the controller itself
        // This handler manages the crafting acceleration logic
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
     * The ECalculator accelerates by processing multiple crafting steps per tick.
     */
    public void processCraftingTick() {
        if (!active) return;
        // The actual crafting acceleration is handled by the controller's
        // ICraftingCPU implementation which provides parallel processing
    }

    public int getParallelCraftingCount() {
        return controller.getParallelCount();
    }

    public double getHyperThreadCostMultiplier() {
        return controller.getHyperThreadCostMultiplier();
    }
}
