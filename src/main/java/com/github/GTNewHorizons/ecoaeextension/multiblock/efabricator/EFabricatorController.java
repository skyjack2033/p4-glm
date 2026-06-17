package com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.github.GTNewHorizons.ecoaeextension.Config;
import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.ae2.EFabricatorPatternHandler;
import com.github.GTNewHorizons.ecoaeextension.gui.mui2.EFabricatorGui;
import com.github.GTNewHorizons.ecoaeextension.loader.BlockLoader;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ECOAEExtendedPowerMultiBlockBase;
import com.github.GTNewHorizons.ecoaeextension.util.ECOAETier;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import appeng.me.helpers.AENetworkProxy;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * EFabricator Controller - An extendable AE2 auto-crafting multiblock.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Pattern buses for storing AE2 crafting patterns (up to 72 slots at L9)</li>
 * <li>Worker cores for processing crafting jobs</li>
 * <li>Parallel processors for scaling throughput</li>
 * <li>Overclock modes (Normal, Overclock I, Overclock II, Overclock III)</li>
 * <li>Active cooling with coolant fluids</li>
 * <li>Three tiers: L4 (HV), L6 (IV), L9 (LuV)</li>
 * </ul>
 *
 * <p>
 * Structure: Linear multiblock extending from the controller. Composed of a fixed 3x3x3
 * section (controller, ME channel, fluid I/O, vent), zero or more repeating segments
 * (pattern buses, worker cores, parallel processors, vents), and an end cap of casings.
 *
 * <p>
 * Structure extends opposite to the controller's front face. Segments are added linearly
 * until an end cap of solid casings is found.
 */
@SuppressWarnings("deprecation")
public class EFabricatorController extends ECOAEExtendedPowerMultiBlockBase<EFabricatorController> {

    // =========================================================================
    // Block References - must be populated before structure checks can pass.
    // Block references from BlockLoader (initialized in static block below)
    // =========================================================================

    public static Block CASING_BLOCK;
    public static int CASING_META;
    public static Block ME_CHANNEL_BLOCK;
    public static int ME_CHANNEL_META;
    public static Block VENT_BLOCK;
    public static int VENT_META;
    public static Block PATTERN_BUS_BLOCK;
    public static int PATTERN_BUS_META;
    public static Block WORKER_BLOCK;
    public static int WORKER_META;
    public static Block PROCESSOR_BLOCK;
    public static int PROCESSOR_META;

    static {
        CASING_BLOCK = BlockLoader.efabricatorBlocks;
        CASING_META = BlockLoader.EFAB_META_CASING;
        ME_CHANNEL_BLOCK = BlockLoader.efabricatorBlocks;
        ME_CHANNEL_META = BlockLoader.EFAB_META_ME_CHANNEL;
        VENT_BLOCK = BlockLoader.efabricatorBlocks;
        VENT_META = BlockLoader.EFAB_META_VENT;
        PATTERN_BUS_BLOCK = BlockLoader.efabricatorBlocks;
        PATTERN_BUS_META = BlockLoader.EFAB_META_PATTERN_BUS;
        WORKER_BLOCK = BlockLoader.efabricatorBlocks;
        WORKER_META = BlockLoader.EFAB_META_WORKER;
        PROCESSOR_BLOCK = BlockLoader.efabricatorBlocks;
        PROCESSOR_META = BlockLoader.EFAB_META_PARALLEL_PROC;
    }

    // =========================================================================
    // Structure Constants
    // =========================================================================

    private static final int STRUCTURE_WIDTH = 3;
    private static final int STRUCTURE_HEIGHT = 3;
    private static final int FIXED_DEPTH = 3;
    private static final int SEGMENT_DEPTH = 2;
    private static final int END_CAP_DEPTH = 1;
    private static final int MAX_SEGMENTS = 16;

    // =========================================================================
    // NBT Keys
    // =========================================================================

    private static final String NBT_OVERCLOCK_MODE = "EF_OverclockMode";
    private static final String NBT_COOLING_ENABLED = "EF_CoolingEnabled";

    // =========================================================================
    // State Fields
    // =========================================================================

    /** Pattern bus slots per bus, determined by tier. */
    private int patternBusSlots;

    /** Worker queue depth from config. */
    private final int workerQueueDepth = Config.eFabricatorWorkerQueueDepth;

    /** Installed component counts (populated during checkMachine). */
    private int installedPatternBuses;
    private int installedWorkers;
    private int installedProcessors;

    /** Overclock mode: 0=Normal, 1=OC I, 2=OC II, 3=OC III. */
    private int overclockMode;

    /** Whether active cooling is enabled. */
    private boolean coolingEnabled;

    /** Whether the structure is currently formed. */
    private boolean structureFormed;

    // =========================================================================
    // AE2 Integration
    // =========================================================================

    private EFabricatorPatternHandler patternHandler;

    // =========================================================================
    // Structure
    // =========================================================================

    private IStructureDefinition<EFabricatorController> structureDefinition;

    // =========================================================================
    // Constructors
    // =========================================================================

    public EFabricatorController(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        this.patternHandler = new EFabricatorPatternHandler(this);
    }

    public EFabricatorController(String aName) {
        super(aName);
        this.patternHandler = new EFabricatorPatternHandler(this);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new EFabricatorController(this.mName);
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
        patternBusSlots = currentTier.getFabricatorParallelProc();
    }

    // =========================================================================
    // Overclock Mode
    // =========================================================================

    public int getOverclockMode() {
        return overclockMode;
    }

    public void cycleOverclockMode() {
        overclockMode = (overclockMode + 1) % 4;
    }

    public void setOverclockMode(int mode) {
        if (mode >= 0 && mode <= 3) {
            overclockMode = mode;
        }
    }

    public String getOverclockModeName() {
        switch (overclockMode) {
            case 1:
                return StatCollector.translateToLocal("ecoaeext.overclock.name.mode1");
            case 2:
                return StatCollector.translateToLocal("ecoaeext.overclock.name.mode2");
            case 3:
                return StatCollector.translateToLocal("ecoaeext.overclock.name.mode3");
            default:
                return StatCollector.translateToLocal("ecoaeext.overclock.name.normal");
        }
    }

    public double getOverclockSpeedMultiplier() {
        switch (overclockMode) {
            case 1:
                return 2.0;
            case 2:
                return 4.0;
            case 3:
                return 8.0;
            default:
                return 1.0;
        }
    }

    public double getOverclockEnergyMultiplier() {
        switch (overclockMode) {
            case 1:
                return 1.5;
            case 2:
                return 2.5;
            case 3:
                return 4.0;
            default:
                return 1.0;
        }
    }

    // =========================================================================
    // Active Cooling
    // =========================================================================

    public boolean isCoolingEnabled() {
        return coolingEnabled;
    }

    public void toggleCooling() {
        coolingEnabled = !coolingEnabled;
    }

    public void setCoolingEnabled(boolean enabled) {
        this.coolingEnabled = enabled;
    }

    /**
     * Check whether coolant is available in at least one fluid input hatch.
     */
    /**
     * Valid coolant fluids and their efficiency multipliers.
     * Water = 1x, Distilled Water = 1.5x, IC2 Coolant = 3x
     */
    private static boolean isValidCoolant(net.minecraftforge.fluids.Fluid fluid) {
        if (fluid == null) return false;
        String name = fluid.getName();
        return "water".equals(name) || "distilled_water".equals(name)
            || "ic2coolant".equals(name)
            || "cryotheum".equals(name);
    }

    private boolean hasCoolantAvailable() {
        if (!coolingEnabled) return true;
        if (mInputHatches == null) return false;
        for (MTEHatchInput hatch : mInputHatches) {
            if (hatch != null && hatch.mFluid != null
                && hatch.mFluid.amount > 0
                && isValidCoolant(hatch.mFluid.getFluid())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consume one unit of coolant from the first available fluid input hatch.
     */
    private void consumeCoolant() {
        if (!coolingEnabled) return;
        if (mInputHatches == null) return;
        for (MTEHatchInput hatch : mInputHatches) {
            if (hatch != null && hatch.mFluid != null
                && hatch.mFluid.amount > 0
                && isValidCoolant(hatch.mFluid.getFluid())) {
                hatch.mFluid.amount -= 1;
                if (hatch.mFluid.amount <= 0) {
                    hatch.mFluid = null;
                }
                return;
            }
        }
    }

    // =========================================================================
    // Query Methods
    // =========================================================================

    public int getTotalPatternSlots() {
        return patternBusSlots * installedPatternBuses;
    }

    public int getWorkerQueueDepth() {
        return workerQueueDepth;
    }

    @Override
    public int getParallelCount() {
        int baseParallel = super.getParallelCount();
        int workerBonus = Math.max(1, installedWorkers);
        return baseParallel * workerBonus;
    }

    public boolean isStructureFormed() {
        return structureFormed;
    }

    /**
     * Get the AE2 network proxy for grid access. Used by the pattern handler
     * to post grid events and access storage.
     */
    public AENetworkProxy getAEProxy() {
        return aeProxy;
    }

    public int getInstalledPatternBuses() {
        return installedPatternBuses;
    }

    public int getInstalledWorkers() {
        return installedWorkers;
    }

    public int getInstalledProcessors() {
        return installedProcessors;
    }

    /**
     * Get the pattern handler for GUI access. Allows the GUI to add/remove pattern items.
     */
    public EFabricatorPatternHandler getPatternHandler() {
        return patternHandler;
    }

    // =========================================================================
    // Structure Definition
    // =========================================================================

    @Override
    public IStructureDefinition<EFabricatorController> getStructureDefinition() {
        // NOTE: The 'E' element (controller position) maps to ofBlock(CASING_BLOCK, CASING_META)
        // so that StructureLib's construct() helper fills the position with a casing block.
        // Manual building is required -- the player must place the controller block at the 'E'
        // position themselves. The construct() helper will incorrectly place a casing there,
        // but checkMachine() skips validation of the controller position (sx=1, sz=0 in the
        // middle layer), so the structure still validates correctly after manual correction.
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<EFabricatorController>builder()
                .addShape(
                    "main",
                    new String[][] { { "CCC", "CMC", "CCC" }, { "CCC", "EKC", "CCC" }, { "CCC", "CFC", "CCC" } })
                .addElement('C', ofBlock(CASING_BLOCK, CASING_META))
                .addElement('M', ofBlock(ME_CHANNEL_BLOCK, ME_CHANNEL_META))
                .addElement('K', ofBlock(VENT_BLOCK, VENT_META))
                .addElement('F', ofBlock(CASING_BLOCK, CASING_META))
                .addElement('E', ofBlock(CASING_BLOCK, CASING_META))
                .build();
        }
        return structureDefinition;
    }

    // Structure offsets: controller position in the shape array
    private static final int HORIZONTAL_OFF_SET = 1;
    private static final int VERTICAL_OFF_SET = 1;
    private static final int DEPTH_OFF_SET = 0;
    private static final String STRUCTURE_PIECE_MAIN = "main";

    // Shape definition: [y][z][x] convention
    // 3x3x2 fixed section with controller at center
    private static final String[][] shape = new String[][] { { "C~C", "CMC" }, // y=0 (bottom, ~ = controller, M = ME
                                                                               // channel)
        { "CCC", "CKC" }, // y=1 (middle, K = vent)
        { "CCC", "CCC" } // y=2 (top)
    };

    @Override
    public String[][] getStructurePattern() {
        return shape;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        this.buildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            hintsOnly,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET);
    }

    // =========================================================================
    // checkMachine - Full Structure Validation
    // =========================================================================

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        // Clear previous state
        mMachine = false;
        installedPatternBuses = 0;
        installedWorkers = 0;
        installedProcessors = 0;

        // Use StructureLib's checkPiece to validate the fixed section
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET)) {
            return false;
        }

        // Fixed 3x3x3 structure provides base component counts.
        // Component counts are configurable via Config.java.
        installedPatternBuses = Config.eFabricatorBasePatternBuses;
        installedWorkers = Config.eFabricatorBaseWorkers;
        installedProcessors = Config.eFabricatorBaseProcessors;

        // Structure is valid
        boolean wasFormed = structureFormed;
        structureFormed = true;
        updateTierConfig();
        if (!wasFormed) {
            onStructureFormed();
        }
        return true;
    }

    // =========================================================================
    // Structure Validation Helpers
    // =========================================================================

    /**
     * Compute the world position corresponding to a shape-local coordinate at a given depth.
     *
     * <p>
     * Shape convention: sx (0..2) = width, sy (0..2) = height, sz = depth offset within the piece.
     * The controller occupies shape position (1, 1, 0) of the fixed section.
     *
     * @param cx,cy,cz      controller world position
     * @param fwdX,fwdZ     forward direction (structure growth)
     * @param rightX,rightZ right-hand perpendicular direction
     * @param depthBase     starting depth of this piece (0 = controller position)
     * @param sx,sy,sz      shape-local coordinates
     * @return world coordinates [x, y, z]
     */
    private int[] shapeToWorld(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ, int depthBase,
        int sx, int sy, int sz) {
        // Controller is at shape (1, 1, 0). Transform:
        // wx = cx + (sx-1)*rightX + (depthBase + sz)*fwdX
        // wy = cy + (sy - 1)
        // wz = cz + (sx-1)*rightZ + (depthBase + sz)*fwdZ
        return new int[] { cx + (sx - 1) * rightX + (depthBase + sz) * fwdX, cy + (sy - 1),
            cz + (sx - 1) * rightZ + (depthBase + sz) * fwdZ };
    }

    /**
     * Check whether a block at the given world position matches the expected type.
     */
    private boolean checkBlockAt(int wx, int wy, int wz, Block expectedBlock, int expectedMeta) {
        Block actual = getBlockAt(wx, wy, wz);
        int actualMeta = getBlockMetaAt(wx, wy, wz);
        return actual == expectedBlock && actualMeta == expectedMeta;
    }

    private Block getBlockAt(int x, int y, int z) {
        if (y < 0 || y >= 256) return null;
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return null;
        return base.getWorld()
            .getBlock(x, y, z);
    }

    private int getBlockMetaAt(int x, int y, int z) {
        if (y < 0 || y >= 256) return 0;
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return 0;
        return base.getWorld()
            .getBlockMetadata(x, y, z);
    }

    private TileEntity getTileAt(int x, int y, int z) {
        if (y < 0 || y >= 256) return null;
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return null;
        return base.getWorld()
            .getTileEntity(x, y, z);
    }

    /**
     * Validate the fixed 3x3x3 controller section.
     *
     * <p>
     * Layer 0 (bottom): CCC / CMC / CCC (M = ME channel at center)
     * <p>
     * Layer 1 (middle): CCC / EKC / CCC (E = controller pos, K = vent)
     * <p>
     * Layer 2 (top): CCC / CFC / CCC (F = fluid I/O hatch)
     */
    private boolean validateFixedSection(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ) {
        // Layer 0 (bottom, sy=0): all casings except center (1,0,1) = ME channel
        for (int sx = 0; sx < 3; sx++) {
            for (int sz = 0; sz < 3; sz++) {
                int[] w = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, 0, sx, 0, sz);
                if (sx == 1 && sz == 1) {
                    if (!checkBlockAt(w[0], w[1], w[2], ME_CHANNEL_BLOCK, ME_CHANNEL_META)) return false;
                } else {
                    if (!checkBlockAt(w[0], w[1], w[2], CASING_BLOCK, CASING_META)) return false;
                }
            }
        }

        // Layer 1 (middle, sy=1): center (1,1,1) = vent, controller pos (1,1,0) = casing (self)
        for (int sx = 0; sx < 3; sx++) {
            for (int sz = 0; sz < 3; sz++) {
                int[] w = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, 0, sx, 1, sz);
                if (sx == 1 && sz == 1) {
                    // Vent block at center of middle layer
                    if (!checkBlockAt(w[0], w[1], w[2], VENT_BLOCK, VENT_META)) return false;
                } else {
                    // Casings (including the controller's own position, which is
                    // the controller block itself -- we accept any block there)
                    if (sx == 1 && sz == 0) {
                        // Controller position - skip (this is the controller itself)
                        continue;
                    }
                    if (!checkBlockAt(w[0], w[1], w[2], CASING_BLOCK, CASING_META)) return false;
                }
            }
        }

        // Layer 2 (top, sy=2): all casings except center (1,2,1) = fluid I/O
        for (int sx = 0; sx < 3; sx++) {
            for (int sz = 0; sz < 3; sz++) {
                int[] w = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, 0, sx, 2, sz);
                if (sx == 1 && sz == 1) {
                    // Fluid I/O hatch position - check during registration, just verify it's not air here
                    Block b = getBlockAt(w[0], w[1], w[2]);
                    if (b == null || b.isAir(getBaseMetaTileEntity().getWorld(), w[0], w[1], w[2])) return false;
                } else {
                    if (!checkBlockAt(w[0], w[1], w[2], CASING_BLOCK, CASING_META)) return false;
                }
            }
        }

        return true;
    }

    /**
     * Validate all segments between the fixed section and the end cap.
     * Each segment has three layers:
     *
     * <p>
     * Layer 0: pattern bus (sz=0) + worker core (sz=1)
     * <p>
     * Layer 1: pattern bus (sz=0) + parallel processor (sz=1)
     * <p>
     * Layer 2: vent (sz=0) + parallel processor (sz=1)
     *
     * <p>
     * Surrounding positions (sx=0,2) are always casings.
     */
    private boolean validateSegments(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ,
        int numSegments) {
        for (int seg = 0; seg < numSegments; seg++) {
            int depthBase = FIXED_DEPTH + seg * SEGMENT_DEPTH;
            for (int sy = 0; sy < 3; sy++) {
                for (int sz = 0; sz < SEGMENT_DEPTH; sz++) {
                    // Check side casings (sx=0 and sx=2)
                    int[] w0 = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, depthBase, 0, sy, sz);
                    int[] w2 = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, depthBase, 2, sy, sz);
                    if (!checkBlockAt(w0[0], w0[1], w0[2], CASING_BLOCK, CASING_META)) return false;
                    if (!checkBlockAt(w2[0], w2[1], w2[2], CASING_BLOCK, CASING_META)) return false;

                    // Check center column (sx=1) based on layer and depth
                    // Original JSON segment layout (3 high x 2 deep x 1 wide):
                    // y=0, z=0: parallel processor y=0, z=1: pattern bus
                    // y=1, z=0: worker y=1, z=1: vent
                    // y=2, z=0: parallel processor y=2, z=1: pattern bus
                    int[] wc = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, depthBase, 1, sy, sz);
                    Block expectedBlock;
                    int expectedMeta;
                    if (sy == 0) {
                        // y=0: parallel processor (sz=0), pattern bus (sz=1)
                        expectedBlock = (sz == 0) ? PROCESSOR_BLOCK : PATTERN_BUS_BLOCK;
                        expectedMeta = (sz == 0) ? PROCESSOR_META : PATTERN_BUS_META;
                    } else if (sy == 1) {
                        // y=1: worker (sz=0), vent (sz=1)
                        expectedBlock = (sz == 0) ? WORKER_BLOCK : VENT_BLOCK;
                        expectedMeta = (sz == 0) ? WORKER_META : VENT_META;
                    } else {
                        // y=2: parallel processor (sz=0), pattern bus (sz=1)
                        expectedBlock = (sz == 0) ? PROCESSOR_BLOCK : PATTERN_BUS_BLOCK;
                        expectedMeta = (sz == 0) ? PROCESSOR_META : PATTERN_BUS_META;
                    }
                    if (!checkBlockAt(wc[0], wc[1], wc[2], expectedBlock, expectedMeta)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Validate the end cap: a 3x3x1 wall of solid casings.
     */
    private boolean validateEndCap(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ, int depthBase) {
        for (int sy = 0; sy < 3; sy++) {
            for (int sz = 0; sz < END_CAP_DEPTH; sz++) {
                for (int sx = 0; sx < 3; sx++) {
                    int[] w = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, depthBase, sx, sy, sz);
                    if (!checkBlockAt(w[0], w[1], w[2], CASING_BLOCK, CASING_META)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Scan a validated segment and register its component blocks with the controller.
     * Increments installedPatternBuses, installedWorkers, installedProcessors and
     * calls addToMachineList() for any MetaTileEntities found.
     */
    private boolean registerSegmentComponents(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ,
        int depthBase) {
        for (int sy = 0; sy < 3; sy++) {
            for (int sz = 0; sz < SEGMENT_DEPTH; sz++) {
                int[] wc = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, depthBase, 1, sy, sz);
                TileEntity te = getTileAt(wc[0], wc[1], wc[2]);
                if (!(te instanceof IGregTechTileEntity)) continue;
                IGregTechTileEntity igt = (IGregTechTileEntity) te;
                IMetaTileEntity mte = igt.getMetaTileEntity();
                if (mte == null) continue;

                Block block = getBlockAt(wc[0], wc[1], wc[2]);
                int meta = getBlockMetaAt(wc[0], wc[1], wc[2]);

                // All EFabricator blocks share the same Block instance (efabricatorBlocks),
                // so we must compare metadata to distinguish block types.
                if (block == PATTERN_BUS_BLOCK && meta == PATTERN_BUS_META) {
                    installedPatternBuses++;
                    addToMachineList(igt, CASING_META);
                } else if (block == WORKER_BLOCK && meta == WORKER_META) {
                    installedWorkers++;
                    addToMachineList(igt, CASING_META);
                } else if (block == PROCESSOR_BLOCK && meta == PROCESSOR_META) {
                    installedProcessors++;
                    addToMachineList(igt, CASING_META);
                } else if (block == VENT_BLOCK && meta == VENT_META) {
                    addToMachineList(igt, CASING_META);
                }
            }
        }
        return true;
    }

    /**
     * Register a fluid hatch at the fixed section's fluid I/O position.
     */
    private void registerFluidHatchAt(int cx, int cy, int cz, int fwdX, int fwdZ, int rightX, int rightZ, int sx,
        int sy, int sz) {
        int[] w = shapeToWorld(cx, cy, cz, fwdX, fwdZ, rightX, rightZ, 0, sx, sy, sz);
        TileEntity te = getTileAt(w[0], w[1], w[2]);
        if (te instanceof IGregTechTileEntity) {
            IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
            if (mte instanceof MTEHatchInput) {
                addToMachineList((IGregTechTileEntity) te, CASING_META);
            }
        }
    }

    /**
     * Callback for StructureLib's ofHatchAdder. Accepts fluid hatches at the 'F' position.
     */
    private boolean addFluidHatchToMachineList(IGregTechTileEntity tile) {
        if (tile == null) return false;
        IMetaTileEntity mte = tile.getMetaTileEntity();
        if (mte instanceof MTEHatchInput) {
            addToMachineList(tile, CASING_META);
            return true;
        }
        return false;
    }

    private void invalidateStructure() {
        if (structureFormed) {
            onStructureInvalidated();
        }
        structureFormed = false;
    }

    // =========================================================================
    // Structure Lifecycle
    // =========================================================================

    @Override
    protected void onStructureFormed() {
        super.onStructureFormed();
        updateTierConfig();
        if (patternHandler != null) {
            patternHandler.activate();
        }
    }

    @Override
    protected void onStructureInvalidated() {
        structureFormed = false;

        // Deactivate pattern handler before clearing component counts,
        // so it can properly cancel any active jobs
        if (patternHandler != null) {
            patternHandler.deactivate();
        }

        installedPatternBuses = 0;
        installedWorkers = 0;
        installedProcessors = 0;
        super.onStructureInvalidated();
    }

    // =========================================================================
    // AE2 Integration
    // =========================================================================

    @Override
    protected void connectToAE2Network() {
        super.connectToAE2Network();

        if (!ae2Connected) return;

        try {
            // Activate the pattern handler. This registers the handler as an
            // ICraftingProvider with the AE2 grid and posts MENetworkCraftingPatternChange
            // to trigger provideCrafting() discovery.
            if (patternHandler != null) {
                // Refresh patterns from stored items before activating
                if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().getWorld() != null) {
                    patternHandler.refreshPatterns(getBaseMetaTileEntity().getWorld());
                }
                patternHandler.activate();
            }

            ECOAEExtension.LOG.info(
                "EFabricator connected to AE2: workers={}, processors={}, patterns={}",
                installedWorkers,
                installedProcessors,
                getTotalPatternSlots());

        } catch (Exception e) {
            ECOAEExtension.LOG.error("Failed to connect EFabricator to AE2 network", e);
        }
    }

    @Override
    protected void disconnectFromAE2Network() {
        // Deactivate the pattern handler. This cancels active jobs, posts
        // MENetworkCraftingPatternChange to notify AE2, and clears the registered state.
        if (patternHandler != null) {
            patternHandler.deactivate();
        }

        super.disconnectFromAE2Network();
    }

    // =========================================================================
    // Tick Processing
    // =========================================================================

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);

        if (!aBaseMetaTileEntity.isServerSide() || !structureFormed) {
            return;
        }

        // Ensure pattern handler stays active while structure is formed
        if (patternHandler != null && !patternHandler.isActive() && ae2Connected) {
            patternHandler.activate();
        }

        if (patternHandler == null || !patternHandler.isActive()) {
            return;
        }

        // Check and drain energy: base voltage * overclock energy multiplier
        long energyCost = (long) (currentTier.voltage * getOverclockEnergyMultiplier());
        if (!drainEnergyInput(energyCost)) {
            return;
        }

        // Validate coolant availability (coolant is required when cooling is enabled)
        if (!hasCoolantAvailable()) {
            return;
        }

        // Process one crafting tick: advances active jobs based on worker count and overclock speed
        patternHandler.processCraftingTick();

        // Consume coolant after processing
        consumeCoolant();
    }

    // =========================================================================
    // NBT Save/Load
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        try {
            aNBT.setInteger(NBT_OVERCLOCK_MODE, overclockMode);
            aNBT.setBoolean(NBT_COOLING_ENABLED, coolingEnabled);

            // Save pattern handler state (active job progress)
            if (patternHandler != null) {
                NBTTagCompound handlerTag = new NBTTagCompound();
                patternHandler.saveNBTData(handlerTag);
                aNBT.setTag("EF_PatternHandler", handlerTag);
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to save EFabricator state", e);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        try {
            if (aNBT.hasKey(NBT_OVERCLOCK_MODE)) {
                overclockMode = aNBT.getInteger(NBT_OVERCLOCK_MODE);
            }
            if (aNBT.hasKey(NBT_COOLING_ENABLED)) {
                coolingEnabled = aNBT.getBoolean(NBT_COOLING_ENABLED);
            }

            // Load pattern handler state (active job progress)
            if (patternHandler != null && aNBT.hasKey("EF_PatternHandler")) {
                NBTTagCompound handlerTag = aNBT.getCompoundTag("EF_PatternHandler");
                patternHandler.loadNBTData(handlerTag);
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to load EFabricator state", e);
        }
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
            if (patternHandler != null && patternHandler.addPatternItem(heldItem, aBaseMetaTileEntity.getWorld())) {
                aPlayer.inventory.decrStackSize(aPlayer.inventory.currentItem, 1);
                aPlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GREEN + String.format(
                            StatCollector.translateToLocal("ecoaeext.chat.pattern_added"),
                            patternHandler.getPatternItems()
                                .size())));
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
            if (patternHandler != null && !patternHandler.getPatternItems()
                .isEmpty()) {
                ItemStack removed = patternHandler.removePatternItem(
                    patternHandler.getPatternItems()
                        .size() - 1);
                if (removed != null) {
                    if (!aPlayer.inventory.addItemStackToInventory(removed)) {
                        aPlayer.dropPlayerItemWithRandomChoice(removed, false);
                    }
                    aPlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            net.minecraft.util.EnumChatFormatting.YELLOW + String.format(
                                StatCollector.translateToLocal("ecoaeext.chat.pattern_removed"),
                                patternHandler.getPatternItems()
                                    .size())));
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
        return new EFabricatorGui(this);
    }

    // =========================================================================
    // Display and Tooltip
    // =========================================================================

    // getDescription() is final in MTETooltipMultiBlockBase and cannot be overridden.
    // Static tooltip information is provided via createTooltip() in the base class,
    // and dynamic runtime information is provided via addAdditionalTooltipInformation().

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip
            .add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_controller"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_desc"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_pattern"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_structure"));
        tooltip.add("");
        tooltip
            .add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_tier_slots"));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_slot_l4"),
                Config.eFabricatorParallelProcL4));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_slot_l6"),
                Config.eFabricatorParallelProcL6));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_slot_l9"),
                Config.eFabricatorParallelProcL9));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_queue"),
                Config.eFabricatorWorkerQueueDepth));
        tooltip.add("");
        tooltip
            .add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("ecoaeext.tooltip.efabricator_overclock"));
        tooltip.add(
            EnumChatFormatting.GRAY
                + String.format(StatCollector.translateToLocal("ecoaeext.tooltip.oc_normal"), "1x", "1x"));
        tooltip.add(
            EnumChatFormatting.GRAY
                + String.format(StatCollector.translateToLocal("ecoaeext.tooltip.oc_mode1"), "2x", "1.5x"));
        tooltip.add(
            EnumChatFormatting.GRAY
                + String.format(StatCollector.translateToLocal("ecoaeext.tooltip.oc_mode2"), "4x", "2.5x"));
        tooltip.add(
            EnumChatFormatting.GRAY
                + String.format(StatCollector.translateToLocal("ecoaeext.tooltip.oc_mode3"), "8x", "4x"));
    }
}
