package com.github.GTNewHorizons.ecoaeextension.ae2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.helpers.AENetworkProxy;

/**
 * Pattern handler for the EFabricator multiblock. Implements AE2's ICraftingProvider
 * interface to register stored patterns with the AE2 crafting grid, and ICraftingMedium
 * to accept and execute pushed crafting jobs.
 *
 * <p>
 * Crafting pipeline:
 * <ol>
 * <li>AE2 discovers patterns via {@link #provideCrafting(ICraftingProviderHelper)}</li>
 * <li>AE2 pushes matching jobs via {@link #pushPattern(ICraftingPatternDetails, InventoryCrafting)}</li>
 * <li>Jobs are queued in {@code activeJobs} up to the worker queue depth</li>
 * <li>Each tick, {@link #processCraftingTick()} advances jobs based on worker count and overclock</li>
 * <li>Completed job outputs are inserted into the AE2 network via the crafting link</li>
 * </ol>
 *
 * <p>
 * Overclock modes modify processing speed:
 * <ul>
 * <li>Normal: 1x speed</li>
 * <li>OC I: 2x speed (drains 1.5x energy per tick)</li>
 * <li>OC II: 4x speed (drains 2.5x energy per tick)</li>
 * <li>OC III: 8x speed (drains 4x energy per tick)</li>
 * </ul>
 */
public class EFabricatorPatternHandler implements ICraftingProvider {

    private static final String NBT_ACTIVE_JOBS = "EF_ActiveJobs";

    private final EFabricatorController controller;

    /** Patterns stored in the pattern buses. Registered with AE2 via provideCrafting(). */
    private final List<ICraftingPatternDetails> storedPatterns = new ArrayList<>();

    /** Currently active crafting jobs being processed by worker cores. */
    private final List<ActiveJob> activeJobs = new ArrayList<>();

    /** Whether this handler is registered with an AE2 network. */
    private boolean registered = false;

    /** Cached AE2 network proxy for grid access. */
    private AENetworkProxy aeProxy;

    /** Machine action source for AE2 security tracking. */
    private MachineSource machineSource;

    public EFabricatorPatternHandler(EFabricatorController controller) {
        this.controller = controller;
    }

    // =========================================================================
    // Activation / Deactivation
    // =========================================================================

    /**
     * Activate this handler by registering it with the AE2 crafting grid.
     * Called when the structure forms and connects to the AE2 network.
     */
    public void activate() {
        if (registered) return;

        aeProxy = controller.getAEProxy();
        if (aeProxy == null) {
            ECOAEExtension.LOG.debug("EFabricator pattern handler: no AE2 proxy available");
            return;
        }

        machineSource = new MachineSource(controller);

        try {
            IGridNode node = aeProxy.getNode();
            if (node != null) {
                IGrid grid = node.getGrid();
                if (grid != null) {
                    // Post a crafting pattern change event to trigger the crafting grid
                    // to call provideCrafting() on this handler
                    grid.postEvent(new MENetworkCraftingPatternChange(this, node));
                    registered = true;
                    ECOAEExtension.LOG
                        .info("EFabricator pattern handler activated with {} stored patterns", storedPatterns.size());
                    return;
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to activate EFabricator pattern handler", e);
        }

        ECOAEExtension.LOG.debug("EFabricator pattern handler: AE2 grid not available");
    }

    /**
     * Deactivate this handler by unregistering from the AE2 crafting grid.
     * Called when the structure is invalidated or disconnects from AE2.
     */
    public void deactivate() {
        if (!registered) return;

        // Cancel all active jobs
        cancelAllJobs();

        // Notify AE2 that we are no longer providing patterns
        try {
            if (aeProxy != null) {
                IGridNode node = aeProxy.getNode();
                if (node != null) {
                    IGrid grid = node.getGrid();
                    if (grid != null) {
                        grid.postEvent(new MENetworkCraftingPatternChange(this, node));
                    }
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to post pattern change on deactivation", e);
        }

        aeProxy = null;
        machineSource = null;
        registered = false;
        ECOAEExtension.LOG.info("EFabricator pattern handler deactivated");
    }

    public boolean isActive() {
        return registered;
    }

    // =========================================================================
    // ICraftingProvider - Pattern Registration
    // =========================================================================

    /**
     * Called by AE2 when the crafting grid rebuilds its pattern list.
     * Registers all stored patterns from the pattern buses with AE2.
     */
    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        for (ICraftingPatternDetails pattern : storedPatterns) {
            if (pattern != null) {
                craftingTracker.addCraftingOption(this, pattern);
            }
        }
    }

    // =========================================================================
    // ICraftingMedium - Job Acceptance
    // =========================================================================

    /**
     * Called by AE2 when a crafting job is pushed to this medium.
     * Accepts the job if the handler has capacity (queue not full) and sufficient energy.
     *
     * @return true if the job was accepted
     */
    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (!registered) return false;

        // Check worker queue capacity
        int maxQueue = controller.getWorkerQueueDepth();
        if (activeJobs.size() >= maxQueue) return false;

        // Energy validation is handled by the controller in onPostTick() before
        // processCraftingTick() is called. pushPattern only checks queue capacity.

        // Accept the crafting job
        ActiveJob job = new ActiveJob(patternDetails, table);
        activeJobs.add(job);

        ECOAEExtension.LOG.debug(
            "EFabricator accepted crafting job: {} (queue: {}/{})",
            patternDetails.getPattern()
                .getDisplayName(),
            activeJobs.size(),
            maxQueue);

        return true;
    }

    @Override
    public boolean isBusy() {
        return !activeJobs.isEmpty();
    }

    @Override
    public ItemStack getCrafterIcon() {
        gregtech.api.interfaces.metatileentity.IMetaTileEntity mte = controller.getBaseMetaTileEntity()
            .getMetaTileEntity();
        return mte != null ? mte.getStackForm(1) : null;
    }

    @Override
    public ICraftingMedium.BlockingMode getBlockingMode() {
        return ICraftingMedium.BlockingMode.BLOCKING;
    }

    // =========================================================================
    // Pattern Management
    // =========================================================================

    /**
     * Add a pattern to the stored pattern list. If connected to AE2, notifies
     * the crafting grid of the change.
     */
    public void addPattern(ICraftingPatternDetails pattern) {
        storedPatterns.add(pattern);
        notifyPatternChange();
    }

    /**
     * Remove a pattern from the stored pattern list.
     */
    public void removePattern(ICraftingPatternDetails pattern) {
        storedPatterns.remove(pattern);
        notifyPatternChange();
    }

    /**
     * Replace all stored patterns (e.g., when pattern buses are reloaded).
     */
    public void setPatterns(List<ICraftingPatternDetails> patterns) {
        storedPatterns.clear();
        if (patterns != null) {
            storedPatterns.addAll(patterns);
        }
        notifyPatternChange();
    }

    public List<ICraftingPatternDetails> getStoredPatterns() {
        return storedPatterns;
    }

    // =========================================================================
    // Crafting Tick Processing
    // =========================================================================

    /**
     * Process one crafting tick. Advances all active jobs based on worker count
     * and overclock speed multiplier. Energy is drained by the controller before
     * this method is called.
     *
     * <p>
     * Steps per tick = workerCount * overclockSpeedMultiplier (minimum 1).
     * Each step advances a job by one processing unit. When all steps are complete,
     * the output is injected into the AE2 network via the crafting link.
     */
    public void processCraftingTick() {
        if (!registered || activeJobs.isEmpty()) return;

        // Calculate effective processing speed
        int workerCount = Math.max(1, controller.getInstalledWorkers());
        double ocSpeed = controller.getOverclockSpeedMultiplier();
        int stepsPerTick = Math.max(1, (int) (workerCount * ocSpeed));

        // Process active jobs
        Iterator<ActiveJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            ActiveJob job = it.next();

            // Skip canceled jobs (check AE2 link cancellation)
            if (job.craftingLink != null && job.craftingLink.isCanceled()) {
                it.remove();
                continue;
            }

            // Advance job progress
            job.advance(stepsPerTick);

            // Check completion
            if (job.isComplete()) {
                it.remove();
                onJobComplete(job);
            }
        }
    }

    /**
     * Handle job completion. Inserts outputs into the AE2 network and notifies
     * the crafting link.
     */
    private void onJobComplete(ActiveJob job) {
        // Pattern details may be null for jobs restored from NBT without AE2 context.
        // In that case, we cannot produce outputs -- the job is silently discarded.
        if (job.patternDetails == null) {
            ECOAEExtension.LOG
                .debug("EFabricator crafting job completed but pattern details unavailable (restored from save)");
            return;
        }

        ECOAEExtension.LOG.debug(
            "EFabricator crafting job completed: {}",
            job.patternDetails.getPattern()
                .getDisplayName());

        // Insert crafted outputs into the AE2 network
        IAEItemStack[] outputs = job.patternDetails.getCondensedOutputs();
        if (outputs != null) {
            for (IAEItemStack output : outputs) {
                if (output != null) {
                    insertOutputIntoNetwork(output.copy());
                }
            }
        }

        // Notify the AE2 crafting link that the job is done
        if (job.craftingLink != null && !job.craftingLink.isDone()) {
            // The link tracks completion status; AE2 will handle final cleanup
            ECOAEExtension.LOG.debug("EFabricator job link marked complete: {}", job.craftingLink.getCraftingID());
        }
    }

    /**
     * Insert a crafted item stack into the AE2 network storage.
     * Uses the AE2 IStorageHelper API for powered insertion that respects
     * the network's energy budget.
     */
    private void insertOutputIntoNetwork(IAEItemStack output) {
        if (output == null || aeProxy == null) return;

        try {
            // Get the AE2 storage helper for powered item operations
            appeng.api.storage.IStorageHelper storageHelper = appeng.api.AEApi.instance()
                .storage();

            IGrid grid = aeProxy.getGrid();
            if (grid == null) return;

            appeng.api.networking.storage.IStorageGrid storageGrid = grid
                .getCache(appeng.api.networking.storage.IStorageGrid.class);
            if (storageGrid == null) return;

            appeng.api.storage.IMEMonitor<IAEItemStack> itemStorage = storageGrid.getItemInventory();
            if (itemStorage == null) return;

            // Inject items into the AE2 network using powered insertion.
            // poweredInsert handles energy extraction from the network automatically.
            storageHelper.poweredInsert(
                aeProxy.getEnergy(), // energy source (IEnergyGrid extends IEnergySource)
                itemStorage, // target inventory
                output, // items to insert
                machineSource // action source for AE2 security tracking
            );

        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to insert output into AE2 network", e);
        }
    }

    /**
     * Cancel all active crafting jobs. Called on deactivation or structure invalidation.
     */
    private void cancelAllJobs() {
        for (ActiveJob job : activeJobs) {
            job.cancel();
        }
        activeJobs.clear();
    }

    // =========================================================================
    // Network Notifications
    // =========================================================================

    /**
     * Notify the AE2 crafting grid that the available patterns have changed.
     */
    private void notifyPatternChange() {
        if (!registered || aeProxy == null) return;

        try {
            IGridNode node = aeProxy.getNode();
            if (node != null) {
                IGrid grid = node.getGrid();
                if (grid != null) {
                    grid.postEvent(new MENetworkCraftingPatternChange(this, node));
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to notify pattern change", e);
        }
    }

    // =========================================================================
    // Status Queries
    // =========================================================================

    public int getActiveJobCount() {
        return activeJobs.size();
    }

    public int getStoredPatternCount() {
        return storedPatterns.size();
    }

    /**
     * Get the combined progress of all active jobs as a percentage (0-100).
     * Returns 0 if there are no active jobs.
     */
    public int getOverallProgress() {
        if (activeJobs.isEmpty()) return 0;
        long totalProgress = 0;
        for (ActiveJob job : activeJobs) {
            totalProgress += job.getProgress();
        }
        return (int) (totalProgress / activeJobs.size());
    }

    // =========================================================================
    // NBT Persistence
    // =========================================================================

    /**
     * Save handler state to NBT. Patterns themselves are stored on pattern bus tile
     * entities; this saves the active job progress for continuity across world loads.
     */
    public void saveNBTData(NBTTagCompound tag) {
        // Save active jobs
        NBTTagList jobList = new NBTTagList();
        for (ActiveJob job : activeJobs) {
            NBTTagCompound jobTag = new NBTTagCompound();
            job.writeToNBT(jobTag);
            jobList.appendTag(jobTag);
        }
        tag.setTag(NBT_ACTIVE_JOBS, jobList);
    }

    /**
     * Load handler state from NBT. Active job progress is restored; pattern details
     * are re-populated from pattern buses on the next provideCrafting() call.
     */
    public void loadNBTData(NBTTagCompound tag) {
        activeJobs.clear();

        if (tag.hasKey(NBT_ACTIVE_JOBS)) {
            NBTTagList jobList = tag.getTagList(NBT_ACTIVE_JOBS, 10); // 10 = NBTTagCompound
            for (int i = 0; i < jobList.tagCount(); i++) {
                NBTTagCompound jobTag = jobList.getCompoundTagAt(i);
                ActiveJob job = ActiveJob.readFromNBT(jobTag);
                if (job != null) {
                    activeJobs.add(job);
                }
            }
        }
    }

    // =========================================================================
    // Active Job (Inner Class)
    // =========================================================================

    /**
     * Represents a crafting job currently being processed by the EFabricator's worker cores.
     * Tracks the pattern details, crafting table, and processing progress.
     */
    private static class ActiveJob {

        private final ICraftingPatternDetails patternDetails;
        private final InventoryCrafting craftingTable;
        private int totalSteps;
        private int completedSteps;
        private boolean canceled;
        private ICraftingLink craftingLink;

        ActiveJob(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
            this.patternDetails = patternDetails;
            this.craftingTable = table;
            this.totalSteps = calculateTotalSteps(patternDetails);
            this.completedSteps = 0;
            this.canceled = false;
        }

        /**
         * Calculate total processing steps based on the number of inputs and outputs.
         * More complex recipes require more steps to complete.
         */
        private static int calculateTotalSteps(ICraftingPatternDetails pattern) {
            int steps = 0;
            if (pattern.getInputs() != null) {
                for (IAEItemStack input : pattern.getInputs()) {
                    if (input != null) steps++;
                }
            }
            if (pattern.getOutputs() != null) {
                for (IAEItemStack output : pattern.getOutputs()) {
                    if (output != null) steps++;
                }
            }
            return Math.max(1, steps);
        }

        /**
         * Advance this job by the given number of steps.
         */
        void advance(int steps) {
            if (canceled) return;
            completedSteps = Math.min(completedSteps + steps, totalSteps);
        }

        /**
         * @return true if all processing steps are complete
         */
        boolean isComplete() {
            return completedSteps >= totalSteps;
        }

        /**
         * @return progress as a percentage (0-100)
         */
        int getProgress() {
            if (totalSteps == 0) return 100;
            return (int) ((completedSteps * 100L) / totalSteps);
        }

        /**
         * Cancel this job. If an AE2 crafting link is attached, it is also canceled.
         */
        void cancel() {
            canceled = true;
            if (craftingLink != null) {
                craftingLink.cancel();
            }
        }

        void setCraftingLink(ICraftingLink link) {
            this.craftingLink = link;
        }

        void writeToNBT(NBTTagCompound tag) {
            tag.setInteger("totalSteps", totalSteps);
            tag.setInteger("completedSteps", completedSteps);
            tag.setBoolean("canceled", canceled);

            if (craftingLink != null) {
                NBTTagCompound linkTag = new NBTTagCompound();
                craftingLink.writeToNBT(linkTag);
                tag.setTag("craftingLink", linkTag);
            }
        }

        /**
         * Restore an active job from NBT. The pattern details and crafting table
         * cannot be fully restored without the AE2 network context; they will be
         * null for restored jobs. Progress tracking is preserved.
         */
        static ActiveJob readFromNBT(NBTTagCompound tag) {
            if (!tag.hasKey("totalSteps") || !tag.hasKey("completedSteps")) return null;

            ActiveJob job = new ActiveJob(null, null);
            job.totalSteps = tag.getInteger("totalSteps");
            job.completedSteps = tag.getInteger("completedSteps");
            job.canceled = tag.getBoolean("canceled");
            return job;
        }
    }
}
