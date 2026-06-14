package com.github.GTNewHorizons.ecoaeextension.ae2;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;

public class EFabricatorPatternHandler {

    private final EFabricatorController controller;
    private boolean active = false;

    public EFabricatorPatternHandler(EFabricatorController controller) {
        this.controller = controller;
    }

    public void activate() {
        if (active) return;
        ECOAEExtension.LOG.info("EFabricator pattern handler activated");
        // Register as crafting provider with AE2 network
        // This is handled by the controller's connectToAE2Network()
        active = true;
    }

    public void deactivate() {
        if (!active) return;
        ECOAEExtension.LOG.info("EFabricator pattern handler deactivated");
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Process a crafting tick. Called every server tick to process pending crafting jobs.
     */
    public void processCraftingTick() {
        if (!active) return;
        // TODO: Implement pattern-based crafting logic
        // 1. Check for pending crafting requests from AE2
        // 2. Read patterns from pattern buses
        // 3. Execute patterns using worker cores
        // 4. Apply overclock modifiers
        // 5. Insert results into output buffer
    }

    public int getPatternSlots() {
        return controller.getTotalPatternSlots();
    }

    public double getOverclockSpeedMultiplier() {
        return controller.getOverclockSpeedMultiplier();
    }

    public double getOverclockEnergyMultiplier() {
        return controller.getOverclockEnergyMultiplier();
    }

    public boolean isCoolingEnabled() {
        return controller.isCoolingEnabled();
    }
}
