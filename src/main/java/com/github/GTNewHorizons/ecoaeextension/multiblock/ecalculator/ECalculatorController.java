package com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.GTNewHorizons.ecoaeextension.Config;
import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.gui.mui2.ECalculatorGui;
import com.github.GTNewHorizons.ecoaeextension.loader.BlockLoader;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ECOAEExtendedPowerMultiBlockBase;
import com.github.GTNewHorizons.ecoaeextension.util.ECOAETier;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingCpuChange;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * ECalculator Controller - An extendable AE2 crafting CPU multiblock.
 *
 * <p>
 * Provides virtual CPUs to the AE2 crafting network, enabling massively parallel
 * autocrafting through thread cores, hyper-thread cores, parallel processors, and
 * flash memory cells.
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 * <li>Virtual CPUs (vCPUs) exposed to the AE2 network as crafting storage</li>
 * <li>Thread cores for parallel crafting execution</li>
 * <li>Hyper-thread cores for additional parallelism (with 10% byte cost increase)</li>
 * <li>Parallel processors for scaling throughput</li>
 * <li>Cell drives for flash memory cells (crafting storage bytes)</li>
 * <li>Transmitter bus for connecting cell drives to the crafting cluster</li>
 * <li>Three tiers: L4 (HV), L6 (IV), L9 (LuV) affecting thread count and parallelism</li>
 * </ul>
 *
 * <h3>Structure</h3>
 * <p>
 * Linear multiblock extending from the controller block. Consists of:
 * </p>
 * <ul>
 * <li>Fixed 3x3x3 section with controller and ME channel</li>
 * <li>Repeating segments with thread cores, parallel processors, cell drives, transmitter buses</li>
 * <li>End cap with tail block</li>
 * </ul>
 *
 * <h3>AE2 Integration</h3>
 * <p>
 * Implements {@link ICraftingProvider} and {@link ICraftingMedium} to participate in AE2's
 * crafting system. Also implements {@link ICraftingCPU} to expose virtual CPU status to the
 * AE2 crafting grid.
 * </p>
 */
public class ECalculatorController extends ECOAEExtendedPowerMultiBlockBase<ECalculatorController>
    implements ICraftingProvider, ICraftingCPU {

    // =========================================================================
    // NBT Constants
    // =========================================================================

    private static final String NBT_THREAD_CORES = "ECalcThreadCores";
    private static final String NBT_HYPER_THREADS = "ECalcHyperThreads";
    private static final String NBT_CELL_DRIVES = "ECalcCellDrives";
    private static final String NBT_PARALLEL_PROCESSORS = "ECalcParallelProcs";
    private static final String NBT_TRANSMITTER_BUSES = "ECalcTransmitterBuses";
    private static final String NBT_TOTAL_STORAGE = "ECalcTotalStorage";
    private static final String NBT_VCPU_ACTIVE = "ECalcVCPUActive";
    private static final String NBT_CRAFTING_JOBS = "ECalcCraftingJobs";

    // =========================================================================
    // Structure Element Keys
    // =========================================================================

    /** Casing block - standard ECalculator casing */
    private static final char CASING = 'C';
    /** Controller block */
    private static final char CONTROLLER = 'E';
    /** ME channel block - connects to AE2 network */
    private static final char ME_CHANNEL = 'M';
    /** Thread core - provides parallel crafting threads */
    private static final char THREAD_CORE = 'T';
    /** Hyper-thread core - additional parallelism with byte cost increase */
    private static final char HYPER_THREAD = 'H';
    /** Parallel processor - scales crafting throughput */
    private static final char PARALLEL_PROC = 'P';
    /** Cell drive - holds flash memory cells for crafting storage */
    private static final char CELL_DRIVE = 'D';
    /** Transmitter bus - connects cell drives to the crafting cluster */
    private static final char TRANSMITTER_BUS = 'B';
    /** Tail block - end cap marker */
    private static final char TAIL = 'L';

    // =========================================================================
    // Limits
    // =========================================================================

    /** Maximum number of repeating segments allowed to prevent unbounded scanning */
    private static final int MAX_SEGMENTS = 16;

    // =========================================================================
    // Structure Shape Names
    // =========================================================================

    private static final String SHAPE_FIXED = "fixed";
    private static final String SHAPE_SEGMENT_THREAD = "seg_thread";
    private static final String SHAPE_SEGMENT_HYPER = "seg_hyper";
    private static final String SHAPE_ENDCAP = "endcap";

    // =========================================================================
    // Configuration Fields
    // =========================================================================

    /** Base number of thread cores allowed by tier */
    private int baseThreadCores;

    // =========================================================================
    // Runtime State
    // =========================================================================

    /** Number of thread cores found in the structure */
    private int installedThreadCores = 0;

    /** Number of hyper-thread cores found in the structure */
    private int installedHyperThreads = 0;

    /** Number of cell drives found in the structure */
    private int installedCellDrives = 0;

    /** Number of parallel processors found in the structure */
    private int installedParallelProcessors = 0;

    /** Number of transmitter buses found in the structure */
    private int installedTransmitterBuses = 0;

    /** Total crafting storage bytes from installed calculator cells */
    private long totalStorageBytes = 0;

    /** Whether the virtual CPU cluster is active and connected to AE2 */
    private boolean vCPUActive = false;

    /** Queue of active crafting jobs being processed */
    private final List<ActiveCraftingJob> activeJobs = new ArrayList<>();

    /** Pattern items stored in the controller. Decoded into ICraftingPatternDetails on demand. */
    private final List<ItemStack> patternItems = new ArrayList<>();

    /** Decoded patterns registered with AE2 via provideCrafting(). */
    private final List<ICraftingPatternDetails> storedPatterns = new ArrayList<>();

    /** Machine action source for AE2 security */
    private MachineSource machineSource;

    /** Listeners for crafting monitor updates */
    @SuppressWarnings("rawtypes")
    private final List<IMEMonitorHandlerReceiver> listeners = new ArrayList<>();

    // =========================================================================
    // Constructors
    // =========================================================================

    public ECalculatorController(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public ECalculatorController(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new ECalculatorController(this.mName);
    }

    // =========================================================================
    // Tier Configuration
    // =========================================================================

    @Override
    protected void onTierChanged(ECOAETier newTier) {
        super.onTierChanged(newTier);
        updateTierConfig();
    }

    private void updateTierConfig() {
        switch (currentTier) {
            case L9:
                baseThreadCores = Config.eCalculatorThreadsPerCoreL9;
                break;
            case L6:
                baseThreadCores = Config.eCalculatorThreadsPerCoreL6;
                break;
            case L4:
            default:
                baseThreadCores = Config.eCalculatorThreadsPerCoreL4;
                break;
        }
    }

    // =========================================================================
    // Structure Definition
    // =========================================================================

    @Override
    public IStructureDefinition<ECalculatorController> getStructureDefinition() {
        // TODO: Register actual MTE blocks for casings, thread cores, etc. in MachineLoader
        // then define proper .addElement() calls here with ofBlock() matching.
        //
        // Example (once blocks are registered):
        // .addElement(CASING, ofBlock(ECalculatorBlocks.CASING, 0))
        // .addElement(CONTROLLER, ofController())
        // .addElement(ME_CHANNEL, ofBlock(ECalculatorBlocks.ME_CHANNEL, 0))
        // .addElement(THREAD_CORE, ofBlock(ECalculatorBlocks.THREAD_CORE, 0))
        // .addElement(HYPER_THREAD, ofBlock(ECalculatorBlocks.HYPER_THREAD, 0))
        // .addElement(PARALLEL_PROC, ofBlock(ECalculatorBlocks.PARALLEL_PROC, 0))
        // .addElement(CELL_DRIVE, ofBlock(ECalculatorBlocks.CELL_DRIVE, 0))
        // .addElement(TRANSMITTER_BUS, ofBlock(ECalculatorBlocks.TRANSMITTER_BUS, 0))
        // .addElement(TAIL, ofBlock(ECalculatorBlocks.TAIL, 0))
        return StructureDefinition.<ECalculatorController>builder()
            .addShape(SHAPE_FIXED, getFixedSectionPattern())
            .addShape(SHAPE_SEGMENT_THREAD, getThreadSegmentPattern())
            .addShape(SHAPE_SEGMENT_HYPER, getHyperSegmentPattern())
            .addShape(SHAPE_ENDCAP, getEndCapPattern())
            .build();
    }

    /**
     * Fixed 3x3x3 section containing the controller block and ME channel.
     *
     * <pre>
     * y=0 (bottom):  CCC / CCC / CCC
     * y=1 (middle):  CEC / CCC / CCC   (E = controller at x=1,z=0)
     * y=2 (top):     CCC / CMC / CCC   (M = ME channel at x=1,z=1)
     * </pre>
     *
     * Each String[y] contains 3 rows separated by '/' for z=0..2.
     * Each row has 3 characters for x=0..2.
     */
    @Override
    public String[][] getStructurePattern() {
        // Return the fixed section as the default pattern
        return getFixedSectionPattern();
    }

    /**
     * Fixed 3x3x3 section.
     * Layer format: "row_z0 / row_z1 / row_z2" where each row has 3 x-positions.
     */
    private String[][] getFixedSectionPattern() {
        return new String[][] {
            // y=0: full casing layer
            { "CCC", "CCC", "CCC" },
            // y=1: controller at center-front (x=1, z=0)
            { "CEC", "CCC", "CCC" },
            // y=2: ME channel at center-middle (x=1, z=1)
            { "CCC", "CMC", "CCC" } };
    }

    /**
     * Repeating thread core segment (3 wide x 3 high x 2 deep).
     * Thread core at center of far-depth layer, with parallel processors,
     * cell drives, and transmitter buses in near-depth layer.
     *
     * <pre>
     * y=0: z=0 "CDC", z=1 "CPC"   (D=cell drive, P=parallel proc)
     * y=1: z=0 "CBC", z=1 "CTC"   (B=transmitter bus, T=thread core)
     * y=2: z=0 "CDC", z=1 "CPC"
     * </pre>
     */
    private String[][] getThreadSegmentPattern() {
        return new String[][] { { "CDC", "CPC" }, { "CBC", "CTC" }, { "CDC", "CPC" } };
    }

    /**
     * Repeating hyper-thread segment (3 wide x 3 high x 2 deep).
     * Same layout as thread segment but with hyper-thread core (H) instead of thread core (T).
     *
     * <pre>
     * y=0: z=0 "CDC", z=1 "CPC"
     * y=1: z=0 "CBC", z=1 "CHC"   (H=hyper-thread core)
     * y=2: z=0 "CDC", z=1 "CPC"
     * </pre>
     */
    private String[][] getHyperSegmentPattern() {
        return new String[][] { { "CDC", "CPC" }, { "CBC", "CHC" }, { "CDC", "CPC" } };
    }

    /**
     * End cap segment (3x3x3).
     * Contains the tail block (L) at center as a structure terminator.
     *
     * <pre>
     * y=0: CCC / CCC / CCC
     * y=1: CCC / CLC / CCC   (L=tail block)
     * y=2: CCC / CCC / CCC
     * </pre>
     */
    private String[][] getEndCapPattern() {
        return new String[][] { { "CCC", "CCC", "CCC" }, { "CCC", "CLC", "CCC" }, { "CCC", "CCC", "CCC" } };
    }

    // =========================================================================
    // Structure Validation
    // =========================================================================

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        // Reset component counts before structure scan
        installedThreadCores = 0;
        installedHyperThreads = 0;
        installedCellDrives = 0;
        installedParallelProcessors = 0;
        installedTransmitterBuses = 0;
        totalStorageBytes = 0;

        // Validate the fixed section (3x3x3 around the controller)
        if (!checkFixedSection(aBaseMetaTileEntity)) {
            return false;
        }

        // Scan outward from the fixed section for repeating segments and end cap
        if (!checkRepeatingSegments(aBaseMetaTileEntity)) {
            return false;
        }

        // Validate that we found at least one thread core or hyper-thread
        if (installedThreadCores == 0 && installedHyperThreads == 0) {
            return false;
        }

        // Validate that we found at least one cell drive
        if (installedCellDrives == 0) {
            return false;
        }

        // Validate thread core count against tier limit
        if (installedThreadCores + installedHyperThreads > baseThreadCores * 2) {
            return false;
        }

        // Structure is valid - notify AE2 and update tier
        onStructureFormed();
        return true;
    }

    /**
     * Validate the fixed 3x3x3 section containing the controller and ME channel.
     *
     * @return true if the fixed section is valid
     */
    private boolean checkFixedSection(IGregTechTileEntity base) {
        int cx = base.getXCoord();
        int cy = base.getYCoord();
        int cz = base.getZCoord();

        // The controller is at the center of the fixed section.
        // Scan the 3x3x3 volume centered on the controller position.
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int wx = cx + dx;
                    int wy = cy + dy;
                    int wz = cz + dz;

                    // Skip the controller block itself
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    net.minecraft.block.Block block = base.getWorld()
                        .getBlock(wx, wy, wz);
                    int meta = base.getWorld()
                        .getBlockMetadata(wx, wy, wz);

                    // ME channel at position (0, +1, 0) relative to controller
                    if (dx == 0 && dy == 1 && dz == 0) {
                        if (block != BlockLoader.ecalculatorBlocks || meta != BlockLoader.ECALC_META_ME_CHANNEL) {
                            return false;
                        }
                        continue;
                    }

                    // All other positions: casings
                    if (block != BlockLoader.ecalculatorBlocks || meta != BlockLoader.ECALC_META_CASING) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Scan for repeating segments extending from the fixed section along the structure axis.
     * Each segment is 3 high x 2 deep x 3 wide (with side casings).
     * Segments are followed by a 3x3x3 end cap with a tail block at center.
     *
     * @return true if the structure terminates correctly with an end cap
     */
    private boolean checkRepeatingSegments(IGregTechTileEntity base) {
        // Determine the scan direction from the controller's facing.
        // The structure extends opposite to the front face.
        ForgeDirection facing = base.getFrontFacing();
        int fwdX, fwdZ;
        switch (facing) {
            case NORTH:
                fwdX = 0;
                fwdZ = 1;
                break; // front faces north -> extends south (+Z)
            case SOUTH:
                fwdX = 0;
                fwdZ = -1;
                break; // front faces south -> extends north (-Z)
            case WEST:
                fwdX = 1;
                fwdZ = 0;
                break; // front faces west -> extends east (+X)
            case EAST:
                fwdX = -1;
                fwdZ = 0;
                break; // front faces east -> extends west (-X)
            default:
                return false;
        }

        int cx = base.getXCoord();
        int cy = base.getYCoord();
        int cz = base.getZCoord();

        // The fixed section is 3x3x3 centered on the controller, spanning
        // from (cx-1,cy-1,cz-1) to (cx+1,cy+1,cz+1). The first segment
        // starts 2 blocks forward from the controller (one past the front face).
        int segmentStart = 2;
        int segmentCount = 0;
        boolean foundEndCap = false;

        while (!foundEndCap) {
            if (segmentCount >= MAX_SEGMENTS) {
                return false;
            }

            // Try to validate as a 2-deep segment starting at this position
            int segX = cx + fwdX * segmentStart;
            int segY = cy;
            int segZ = cz + fwdZ * segmentStart;

            if (checkSegment(base.getWorld(), segX, segY, segZ)) {
                // Valid segment - advance by 2 (segment depth)
                segmentStart += 2;
                segmentCount++;
                continue;
            }

            // Not a valid segment - check if this is an end cap.
            // The end cap is a 3x3x3 volume centered 1 block forward from segmentStart.
            int endCapX = cx + fwdX * (segmentStart + 1);
            int endCapZ = cz + fwdZ * (segmentStart + 1);
            if (isEndCap(base.getWorld(), endCapX, segY, endCapZ)) {
                foundEndCap = true;
                break;
            }

            // Neither a valid segment nor an end cap - structure is invalid
            return false;
        }

        return foundEndCap && segmentCount > 0;
    }

    /**
     * Check if a 3x3x3 volume at the given position is an end cap.
     */
    private boolean isEndCap(net.minecraft.world.World world, int cx, int cy, int cz) {
        // Check for the tail block at the center of the 3x3x3 volume
        net.minecraft.block.Block centerBlock = world.getBlock(cx, cy, cz);
        int centerMeta = world.getBlockMetadata(cx, cy, cz);

        if (centerBlock != BlockLoader.ecalculatorBlocks || centerMeta != BlockLoader.ECALC_META_TAIL) {
            return false;
        }

        // Verify the 3x3x3 volume has casings around the tail block
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // Skip center (tail block)
                    net.minecraft.block.Block block = world.getBlock(cx + dx, cy + dy, cz + dz);
                    int meta = world.getBlockMetadata(cx + dx, cy + dy, cz + dz);
                    if (block != BlockLoader.ecalculatorBlocks || meta != BlockLoader.ECALC_META_CASING) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check and count components in a segment at the given position.
     * Each segment is 3 high x 2 deep x 3 wide (with side casings).
     *
     * <p>
     * Original JSON segment layout (center column, 3 high x 2 deep x 1 wide):
     * 
     * <pre>
     * z=0: cell drive (y=0), transmitter bus (y=1), cell drive (y=2)
     * z=1: parallel proc (y=0), thread/hyper (y=1), parallel proc (y=2)
     * </pre>
     *
     * <p>
     * The position (cx, cy, cz) is the near-depth, bottom-left corner of the segment.
     * Side columns (dx=0 and dx=2) are casings; center column (dx=1) has components.
     *
     * @return true if the segment is valid
     */
    private boolean checkSegment(net.minecraft.world.World world, int cx, int cy, int cz) {
        // Iterate over 3 wide x 3 high x 2 deep
        for (int dy = 0; dy < 3; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    int wx = cx + dx;
                    int wy = cy + dy;
                    int wz = cz + dz;

                    net.minecraft.block.Block block = world.getBlock(wx, wy, wz);
                    int meta = world.getBlockMetadata(wx, wy, wz);

                    if (block != BlockLoader.ecalculatorBlocks) return false;

                    // Side casings (dx=0 and dx=2)
                    if (dx != 1) {
                        if (meta != BlockLoader.ECALC_META_CASING) return false;
                        continue;
                    }

                    // Center column (dx=1)
                    if (dz == 0) {
                        // Near depth (z=0): cell drive, transmitter bus, cell drive
                        if (dy == 1) {
                            // Transmitter bus at y=1
                            if (meta != BlockLoader.ECALC_META_TRANSMITTER_BUS) return false;
                            installedTransmitterBuses++;
                        } else {
                            // Cell drives at y=0 and y=2
                            if (meta != BlockLoader.ECALC_META_CELL_DRIVE) return false;
                            installedCellDrives++;
                        }
                    } else {
                        // Far depth (z=1): parallel proc, thread/hyper, parallel proc
                        if (dy == 1) {
                            // Thread core or hyper-thread at y=1
                            if (meta == BlockLoader.ECALC_META_THREAD_CORE) {
                                installedThreadCores++;
                            } else if (meta == BlockLoader.ECALC_META_HYPER_THREAD) {
                                installedHyperThreads++;
                            } else {
                                return false;
                            }
                        } else {
                            // Parallel processors at y=0 and y=2
                            if (meta != BlockLoader.ECALC_META_PARALLEL_PROC) return false;
                            installedParallelProcessors++;
                        }
                    }
                }
            }
        }

        return true;
    }

    // =========================================================================
    // AE2 Integration - Crafting Provider
    // =========================================================================

    /**
     * Called by AE2 when the crafting grid rebuilds its pattern list.
     * Registers all stored patterns from the controller's pattern inventory.
     */
    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        for (ICraftingPatternDetails pattern : storedPatterns) {
            if (pattern != null) {
                craftingTracker.addCraftingOption(this, pattern);
            }
        }
    }

    /**
     * Called by AE2 when a crafting pattern is pushed to this medium for execution.
     * Accepts the pattern if the ECalculator has available capacity.
     *
     * @return true if the pattern was accepted for processing
     */
    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        // This is called when AE2 pushes a crafting pattern to us as a medium.
        // Accept the pattern if we have capacity.
        if (!vCPUActive) return false;
        if (isBusy()) return false;

        // Check if we have enough storage for this job
        long requiredBytes = estimateStorageRequirement(patternDetails);
        if (requiredBytes > getAvailableStorage()) return false;

        // Accept the crafting job
        ActiveCraftingJob job = new ActiveCraftingJob(patternDetails, table);
        activeJobs.add(job);

        ECOAEExtension.LOG.debug(
            "ECalculator accepted crafting job: {} ({} bytes)",
            patternDetails.getPattern()
                .getDisplayName(),
            requiredBytes);

        return true;
    }

    @Override
    public boolean isBusy() {
        // Allow multiple concurrent jobs based on thread core count
        int maxConcurrent = Math.max(1, installedThreadCores + installedHyperThreads);
        return activeJobs.size() >= maxConcurrent;
    }

    // =========================================================================
    // Pattern Management
    // =========================================================================

    /**
     * Add a pattern item to the controller. The item is decoded into ICraftingPatternDetails
     * and registered with AE2 if connected.
     *
     * @param patternStack The pattern item stack to add
     * @return true if the pattern was added successfully
     */
    public boolean addPatternItem(ItemStack patternStack) {
        if (patternStack == null) return false;
        if (!(patternStack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem)) return false;

        try {
            net.minecraft.world.World world = getBaseMetaTileEntity() != null ? getBaseMetaTileEntity().getWorld()
                : null;
            if (world == null) return false;

            ICraftingPatternDetails details = ((appeng.api.implementations.ICraftingPatternItem) patternStack.getItem())
                .getPatternForItem(patternStack, world);
            if (details == null) return false;

            patternItems.add(patternStack.copy());
            storedPatterns.add(details);
            notifyPatternChange();
            return true;
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to decode pattern item", e);
            return false;
        }
    }

    /**
     * Remove a pattern item at the given index.
     *
     * @param index The index of the pattern to remove
     * @return The removed pattern ItemStack, or null if index is invalid
     */
    public ItemStack removePatternItem(int index) {
        if (index < 0 || index >= patternItems.size()) return null;

        ItemStack removed = patternItems.remove(index);
        if (index < storedPatterns.size()) {
            storedPatterns.remove(index);
        }
        notifyPatternChange();
        return removed;
    }

    /**
     * Get the list of pattern items stored in this controller.
     */
    public List<ItemStack> getPatternItems() {
        return patternItems;
    }

    /**
     * Refresh stored patterns by re-decoding all pattern items.
     */
    public void refreshPatterns() {
        storedPatterns.clear();
        net.minecraft.world.World world = getBaseMetaTileEntity() != null ? getBaseMetaTileEntity().getWorld() : null;
        if (world == null) return;

        for (ItemStack stack : patternItems) {
            if (stack != null && stack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem) {
                try {
                    ICraftingPatternDetails details = ((appeng.api.implementations.ICraftingPatternItem) stack
                        .getItem()).getPatternForItem(stack, world);
                    if (details != null) {
                        storedPatterns.add(details);
                    }
                } catch (Exception e) {
                    ECOAEExtension.LOG.debug("Failed to refresh pattern", e);
                }
            }
        }
        notifyPatternChange();
    }

    /**
     * Notify AE2 that patterns have changed.
     */
    private void notifyPatternChange() {
        if (aeProxy == null || !ae2Connected) return;
        try {
            appeng.api.networking.IGridNode node = aeProxy.getNode();
            if (node != null) {
                appeng.api.networking.IGrid grid = node.getGrid();
                if (grid != null) {
                    grid.postEvent(new MENetworkCraftingPatternChange(this, node));
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to notify pattern change", e);
        }
    }

    @Override
    public ItemStack getCrafterIcon() {
        // Return the controller's item stack form as the crafter icon
        IMetaTileEntity mte = getBaseMetaTileEntity().getMetaTileEntity();
        if (mte != null) {
            return mte.getStackForm(1);
        }
        return null;
    }

    // =========================================================================
    // AE2 Integration - Crafting CPU (Virtual CPU)
    // =========================================================================

    @Override
    public BaseActionSource getActionSource() {
        if (machineSource == null) {
            machineSource = new MachineSource(this);
        }
        return machineSource;
    }

    @Override
    public long getAvailableStorage() {
        return totalStorageBytes;
    }

    @Override
    public long getUsedStorage() {
        long used = 0;
        for (ActiveCraftingJob job : activeJobs) {
            used += job.getStorageUsed();
        }
        return used;
    }

    @Override
    public int getCoProcessors() {
        // Co-processors = parallel processors (each adds one co-processor slot)
        return installedParallelProcessors;
    }

    @Override
    public String getName() {
        return "ECalculator " + currentTier.name;
    }

    @SuppressWarnings("rawtypes")
    public void addListener(IMEMonitorHandlerReceiver l, Object verificationToken) {
        listeners.add(l);
    }

    @SuppressWarnings("rawtypes")
    public void removeListener(IMEMonitorHandlerReceiver l) {
        listeners.remove(l);
    }

    // =========================================================================
    // AE2 Network Lifecycle
    // =========================================================================

    @Override
    protected void connectToAE2Network() {
        super.connectToAE2Network();

        if (!ae2Connected) return;

        try {
            // Refresh patterns from stored items before notifying AE2
            refreshPatterns();

            // Register as a crafting provider with the AE2 crafting grid
            IGrid grid = getGrid();
            if (grid != null && aeProxy != null && aeProxy.getNode() != null) {
                // Notify AE2 that patterns have changed
                grid.postEvent(new MENetworkCraftingPatternChange(this, aeProxy.getNode()));
            }

            // Calculate total storage from installed calculator cells
            recalculateStorage();

            // Initialize machine source for AE2 security
            machineSource = new MachineSource(this);

            vCPUActive = true;

            ECOAEExtension.LOG.info(
                "ECalculator connected to AE2 network: threads={}, hyper={}, storage={} bytes, parallel={}, patterns={}",
                installedThreadCores,
                installedHyperThreads,
                totalStorageBytes,
                getParallelCount(),
                storedPatterns.size());

        } catch (Exception e) {
            ECOAEExtension.LOG.error("Failed to connect ECalculator to AE2 network", e);
            vCPUActive = false;
        }
    }

    @Override
    protected void disconnectFromAE2Network() {
        // Cancel all active crafting jobs
        cancelAllJobs();

        vCPUActive = false;
        machineSource = null;

        // Notify the AE2 grid that we're leaving
        try {
            IGrid grid = getGrid();
            if (grid != null && aeProxy != null && aeProxy.getNode() != null) {
                grid.postEvent(new MENetworkCraftingCpuChange(aeProxy.getNode()));
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to post CPU change on disconnect", e);
        }

        super.disconnectFromAE2Network();
    }

    // =========================================================================
    // Storage Management
    // =========================================================================

    /**
     * Recalculate total crafting storage from installed calculator cells.
     * Called when the structure forms or cells are changed.
     */
    private void recalculateStorage() {
        totalStorageBytes = 0;

        // Sum storage from all installed calculator cells in cell drives.
        // TODO: Iterate over cell drive inventories and sum ItemCalculatorCell bytes.
        // For now, estimate based on installed cell drives with default cell size.
        //
        // Each cell drive can hold one calculator cell.
        // Default cell size depends on tier:
        // L4: 64M bytes per cell
        // L6: 1024M bytes per cell
        // L9: 16384M bytes per cell
        long bytesPerCell;
        switch (currentTier) {
            case L9:
                bytesPerCell = 16_384_000_000L;
                break;
            case L6:
                bytesPerCell = 1_024_000_000L;
                break;
            case L4:
            default:
                bytesPerCell = 64_000_000L;
                break;
        }

        totalStorageBytes = (long) installedCellDrives * bytesPerCell;

        // Apply hyper-thread cost multiplier to effective storage
        // Hyper-threads consume 10% more storage per operation
        if (installedHyperThreads > 0) {
            double hyperCost = 1.0 + (installedHyperThreads * (1.1 - 1.0));
            totalStorageBytes = (long) (totalStorageBytes / hyperCost);
        }
    }

    /**
     * Estimate the storage requirement for a crafting pattern.
     */
    private long estimateStorageRequirement(ICraftingPatternDetails pattern) {
        // Estimate based on the number of inputs and outputs
        int inputCount = 0;
        int outputCount = 0;

        if (pattern.getInputs() != null) {
            for (Object input : pattern.getInputs()) {
                if (input != null) inputCount++;
            }
        }
        if (pattern.getOutputs() != null) {
            for (Object output : pattern.getOutputs()) {
                if (output != null) outputCount++;
            }
        }

        // Rough estimate: 256 bytes per input/output slot used
        return (long) (inputCount + outputCount) * 256L;
    }

    // =========================================================================
    // Crafting Job Processing
    // =========================================================================

    /**
     * Process active crafting jobs. Called every tick from onPostTick.
     * Processes multiple steps per tick based on thread count and parallelism.
     */
    private void processCraftingJobs() {
        if (!vCPUActive || activeJobs.isEmpty()) return;

        int stepsPerTick = getTotalThreads() * currentTier.parallelMultiplier;

        Iterator<ActiveCraftingJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            ActiveCraftingJob job = it.next();

            // Process multiple steps for this job
            for (int i = 0; i < stepsPerTick; i++) {
                if (job.processStep()) {
                    // Job completed
                    it.remove();
                    notifyJobComplete(job);
                    break;
                }
            }
        }
    }

    /**
     * Notify listeners that a crafting job has completed.
     */
    private void notifyJobComplete(ActiveCraftingJob job) {
        ECOAEExtension.LOG.debug(
            "ECalculator crafting job completed: {}",
            job.getPattern()
                .getPattern()
                .getDisplayName());

        // Notify AE2 crafting monitors
        @SuppressWarnings("rawtypes")
        List<IMEMonitorHandlerReceiver> snapshot = listeners;
        for (IMEMonitorHandlerReceiver listener : snapshot) {
            try {
                // The listener interface uses postChange for updates
                // TODO: Call the appropriate notification method on the listener
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to notify crafting listener", e);
            }
        }
    }

    /**
     * Cancel all active crafting jobs. Called when the structure is invalidated.
     */
    private void cancelAllJobs() {
        for (ActiveCraftingJob job : activeJobs) {
            job.cancel();
        }
        activeJobs.clear();
    }

    // =========================================================================
    // Tick Processing
    // =========================================================================

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);

        if (aBaseMetaTileEntity.isServerSide() && mMachine) {
            // Process crafting acceleration every tick
            processCraftingJobs();

            // Periodic storage recalculation (in case cells are swapped)
            if (aTick % 200 == 0) {
                recalculateStorage();
            }
        }
    }

    // =========================================================================
    // Structure Lifecycle
    // =========================================================================

    @Override
    protected void onStructureFormed() {
        super.onStructureFormed();
        updateTierConfig();
    }

    @Override
    protected void onStructureInvalidated() {
        cancelAllJobs();
        installedThreadCores = 0;
        installedHyperThreads = 0;
        installedCellDrives = 0;
        installedParallelProcessors = 0;
        installedTransmitterBuses = 0;
        totalStorageBytes = 0;
        super.onStructureInvalidated();
    }

    // =========================================================================
    // NBT Save/Load
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);

        aNBT.setInteger(NBT_THREAD_CORES, installedThreadCores);
        aNBT.setInteger(NBT_HYPER_THREADS, installedHyperThreads);
        aNBT.setInteger(NBT_CELL_DRIVES, installedCellDrives);
        aNBT.setInteger(NBT_PARALLEL_PROCESSORS, installedParallelProcessors);
        aNBT.setInteger(NBT_TRANSMITTER_BUSES, installedTransmitterBuses);
        aNBT.setLong(NBT_TOTAL_STORAGE, totalStorageBytes);
        aNBT.setBoolean(NBT_VCPU_ACTIVE, vCPUActive);

        // Save active crafting jobs
        NBTTagList jobList = new NBTTagList();
        for (ActiveCraftingJob job : activeJobs) {
            NBTTagCompound jobTag = new NBTTagCompound();
            job.writeToNBT(jobTag);
            jobList.appendTag(jobTag);
        }
        aNBT.setTag(NBT_CRAFTING_JOBS, jobList);

        // Save pattern items
        NBTTagList patternList = new NBTTagList();
        for (ItemStack stack : patternItems) {
            if (stack != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                stack.writeToNBT(stackTag);
                patternList.appendTag(stackTag);
            }
        }
        aNBT.setTag("ECalc_PatternItems", patternList);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        installedThreadCores = aNBT.getInteger(NBT_THREAD_CORES);
        installedHyperThreads = aNBT.getInteger(NBT_HYPER_THREADS);
        installedCellDrives = aNBT.getInteger(NBT_CELL_DRIVES);
        installedParallelProcessors = aNBT.getInteger(NBT_PARALLEL_PROCESSORS);
        installedTransmitterBuses = aNBT.getInteger(NBT_TRANSMITTER_BUSES);
        totalStorageBytes = aNBT.getLong(NBT_TOTAL_STORAGE);
        vCPUActive = aNBT.getBoolean(NBT_VCPU_ACTIVE);

        // Load active crafting jobs
        activeJobs.clear();
        if (aNBT.hasKey(NBT_CRAFTING_JOBS)) {
            NBTTagList jobList = aNBT.getTagList(NBT_CRAFTING_JOBS, 10); // 10 = NBTTagCompound
            for (int i = 0; i < jobList.tagCount(); i++) {
                NBTTagCompound jobTag = jobList.getCompoundTagAt(i);
                ActiveCraftingJob job = ActiveCraftingJob.readFromNBT(jobTag);
                if (job != null) {
                    activeJobs.add(job);
                }
            }
        }

        // Load pattern items
        patternItems.clear();
        storedPatterns.clear();
        if (aNBT.hasKey("ECalc_PatternItems")) {
            NBTTagList patternList = aNBT.getTagList("ECalc_PatternItems", 10);
            for (int i = 0; i < patternList.tagCount(); i++) {
                NBTTagCompound stackTag = patternList.getCompoundTagAt(i);
                ItemStack stack = ItemStack.loadItemStackFromNBT(stackTag);
                if (stack != null) {
                    patternItems.add(stack);
                }
            }
        }
    }

    // =========================================================================
    // Public Accessors
    // =========================================================================

    /**
     * Get the total number of crafting threads available.
     * Includes both regular and hyper threads.
     */
    public int getTotalThreads() {
        return installedThreadCores + installedHyperThreads;
    }

    /**
     * Get the effective parallel count, accounting for thread cores and tier.
     */
    @Override
    public int getParallelCount() {
        int baseParallel = super.getParallelCount();
        int threadBonus = Math.max(1, getTotalThreads());
        return baseParallel * threadBonus;
    }

    /**
     * Get the byte cost multiplier for hyper-thread cores.
     */
    public double getHyperThreadCostMultiplier() {
        return 1.1;
    }

    /**
     * Get the total crafting storage bytes available.
     */
    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    /**
     * Check if the virtual CPU cluster is active.
     */
    public boolean isVCPUActive() {
        return vCPUActive;
    }

    /**
     * Get the number of installed thread cores.
     */
    public int getInstalledThreadCores() {
        return installedThreadCores;
    }

    /**
     * Get the number of installed hyper-thread cores.
     */
    public int getInstalledHyperThreads() {
        return installedHyperThreads;
    }

    /**
     * Get the number of installed cell drives.
     */
    public int getInstalledCellDrives() {
        return installedCellDrives;
    }

    /**
     * Get the number of installed parallel processors.
     */
    public int getInstalledParallelProcessors() {
        return installedParallelProcessors;
    }

    /**
     * Get the number of installed transmitter buses.
     */
    public int getInstalledTransmitterBuses() {
        return installedTransmitterBuses;
    }

    /**
     * Get the number of active crafting jobs.
     */
    public int getActiveJobCount() {
        return activeJobs.size();
    }

    // =========================================================================
    // GUI
    // =========================================================================

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            openGui(aPlayer);
            return true;
        }

        // Check if player is holding a pattern - insert it
        ItemStack heldItem = aPlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof appeng.api.implementations.ICraftingPatternItem) {
            if (addPatternItem(heldItem)) {
                aPlayer.inventory.decrStackSize(aPlayer.inventory.currentItem, 1);
                aPlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GREEN + String.format(
                            StatCollector.translateToLocal("ecoaeext.chat.pattern_added"),
                            patternItems.size())));
                return true;
            } else {
                aPlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.RED
                            + StatCollector.translateToLocal("ecoaeext.chat.pattern_failed")));
                return true;
            }
        }

        // Check if player is sneaking with empty hand - remove last pattern
        if (aPlayer.isSneaking() && (heldItem == null)) {
            if (!patternItems.isEmpty()) {
                ItemStack removed = removePatternItem(patternItems.size() - 1);
                if (removed != null) {
                    if (!aPlayer.inventory.addItemStackToInventory(removed)) {
                        aPlayer.dropPlayerItemWithRandomChoice(removed, false);
                    }
                    aPlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            net.minecraft.util.EnumChatFormatting.YELLOW + String.format(
                                StatCollector.translateToLocal("ecoaeext.chat.pattern_removed"),
                                patternItems.size())));
                    return true;
                }
            }
            aPlayer.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    net.minecraft.util.EnumChatFormatting.RED
                        + StatCollector.translateToLocal("ecoaeext.chat.no_patterns")));
            return true;
        }

        // Normal right-click opens GUI via GT5 MUI2 system
        openGui(aPlayer);
        return true;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new ECalculatorGui(this);
    }

    // =========================================================================
    // Display and UI
    // =========================================================================

    // getDescription() is final in MTETooltipMultiBlockBase and cannot be overridden.
    // Static tooltip information is provided via createTooltip() in the base class,
    // and dynamic runtime information is provided via addAdditionalTooltipInformation().

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip
            .add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_controller"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_desc"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_vcpu"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_threads"));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_hyper"),
                (int) ((1.1 - 1.0) * 100)));
        tooltip.add("");
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_tier_l4"),
                Config.eCalculatorThreadsPerCoreL4,
                "1x"));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_tier_l6"),
                Config.eCalculatorThreadsPerCoreL6,
                "4x"));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.ecalculator_tier_l9"),
                Config.eCalculatorThreadsPerCoreL9,
                "16x"));
    }

    /**
     * Format byte count to human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + "B";
    }

    // =========================================================================
    // Active Crafting Job (Inner Class)
    // =========================================================================

    /**
     * Represents a crafting job currently being processed by the ECalculator.
     * Tracks the pattern, progress, and storage usage.
     */
    private static class ActiveCraftingJob {

        private final ICraftingPatternDetails pattern;
        private final InventoryCrafting craftingTable;
        private int totalSteps;
        private int completedSteps;
        private boolean canceled;
        private ICraftingLink craftingLink;

        ActiveCraftingJob(ICraftingPatternDetails pattern, InventoryCrafting table) {
            this.pattern = pattern;
            this.craftingTable = table;
            this.totalSteps = calculateTotalSteps(pattern);
            this.completedSteps = 0;
            this.canceled = false;
        }

        /**
         * Calculate the total number of processing steps for a pattern.
         * Based on the number of inputs and outputs.
         */
        private int calculateTotalSteps(ICraftingPatternDetails pattern) {
            if (pattern == null) return 1;
            int steps = 0;
            if (pattern.getInputs() != null) {
                for (Object input : pattern.getInputs()) {
                    if (input != null) steps++;
                }
            }
            if (pattern.getOutputs() != null) {
                for (Object output : pattern.getOutputs()) {
                    if (output != null) steps++;
                }
            }
            return Math.max(1, steps);
        }

        /**
         * Process one step of this crafting job.
         *
         * @return true if the job is now complete
         */
        boolean processStep() {
            if (canceled) return true;

            completedSteps++;
            return completedSteps >= totalSteps;
        }

        /**
         * Cancel this crafting job.
         */
        void cancel() {
            canceled = true;
            if (craftingLink != null) {
                craftingLink.cancel();
            }
        }

        /**
         * Get the estimated storage used by this job in bytes.
         */
        long getStorageUsed() {
            return (long) totalSteps * 256L;
        }

        /**
         * Get the progress as a percentage (0-100).
         */
        int getProgress() {
            if (totalSteps == 0) return 100;
            return (int) ((completedSteps * 100L) / totalSteps);
        }

        /**
         * Check if this job is complete.
         */
        boolean isComplete() {
            return completedSteps >= totalSteps;
        }

        /**
         * Check if this job was canceled.
         */
        boolean isCanceled() {
            return canceled;
        }

        /**
         * Get the pattern for this job.
         */
        ICraftingPatternDetails getPattern() {
            return pattern;
        }

        /**
         * Set the crafting link for this job.
         */
        void setCraftingLink(ICraftingLink link) {
            this.craftingLink = link;
        }

        /**
         * Write this job to NBT for persistence.
         */
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
         * Read a job from NBT. Returns null if the data is invalid.
         * Note: The pattern and crafting table cannot be fully restored from NBT
         * alone - they require the AE2 network context. This is a best-effort restore.
         */
        static ActiveCraftingJob readFromNBT(NBTTagCompound tag) {
            if (!tag.hasKey("totalSteps") || !tag.hasKey("completedSteps")) return null;

            // We cannot fully restore the pattern and crafting table from NBT
            // without the AE2 network context. Return a placeholder that preserves progress.
            ActiveCraftingJob job = new ActiveCraftingJob(null, null);
            job.totalSteps = tag.getInteger("totalSteps");
            job.completedSteps = tag.getInteger("completedSteps");
            job.canceled = tag.getBoolean("canceled");
            return job;
        }
    }
}
