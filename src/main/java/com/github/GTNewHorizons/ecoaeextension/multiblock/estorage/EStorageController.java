package com.github.GTNewHorizons.ecoaeextension.multiblock.estorage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.GTNewHorizons.ecoaeextension.Config;
import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.ae2.AE2StorageHelper;
import com.github.GTNewHorizons.ecoaeextension.loader.BlockLoader;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ECOAEExtendedPowerMultiBlockBase;
import com.github.GTNewHorizons.ecoaeextension.util.ECOAETier;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * EStorage Controller - An extendable AE2-integrated storage multiblock.
 *
 * <p>Structure: Linear multiblock with a fixed 3x3x3 section containing the controller and ME
 * channel, repeating segments with cell drives, energy cells, and vents extending from it, and an
 * end cap of casings.
 *
 * <p>Features:
 * <ul>
 *   <li>Custom storage cells for items, fluids, and gases (Mekanism)</li>
 *   <li>Energy cells for AE2 power management</li>
 *   <li>Cell drives for holding storage cells</li>
 *   <li>ME network connectivity via dedicated channel blocks</li>
 *   <li>Three tiers: L4 (HV), L6 (IV), L9 (LuV) affecting capacity and cell count</li>
 * </ul>
 */
public class EStorageController extends ECOAEExtendedPowerMultiBlockBase<EStorageController>
    implements ICellProvider {

    // =========================================================================
    // Constants
    // =========================================================================

    /** Minimum number of repeating segments required */
    private static final int MIN_SEGMENTS = 1;

    /** Maximum number of repeating segments allowed (hardware limit) */
    private static final int MAX_SEGMENTS = 16;

    /** Tick interval for periodic updates (once per second at 20 TPS) */
    private static final int UPDATE_INTERVAL = 20;

    /** NBT tag keys */
    private static final String NBT_INSTALLED_DRIVES = "ecoae_installedDrives";
    private static final String NBT_INSTALLED_ENERGY = "ecoae_installedEnergyCells";
    private static final String NBT_INSTALLED_VENTS = "ecoae_installedVents";
    private static final String NBT_SEGMENT_COUNT = "ecoae_segmentCount";

    // =========================================================================
    // Fields - Component Tracking
    // =========================================================================

    /** Number of cell drives found in the current structure */
    private int installedCellDrives;

    /** Number of energy cells found in the current structure */
    private int installedEnergyCells;

    /** Number of cooling vents found in the current structure */
    private int installedVents;

    /** Number of repeating segments found in the current structure */
    private int segmentCount;

    // =========================================================================
    // Fields - Tier Configuration
    // =========================================================================

    /** Base storage capacity in bytes, determined by tier */
    private long storageCapacity;

    /** Maximum number of cell drives allowed by the current tier */
    private int maxCellDrives;

    // =========================================================================
    // Fields - State Tracking
    // =========================================================================

    /** Whether we are currently registered as an AE2 cell provider */
    private boolean cellProviderRegistered;

    /** Tick counter for periodic processing */
    private int tickCounter;

    // =========================================================================
    // Constructors
    // =========================================================================

    public EStorageController(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public EStorageController(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new EStorageController(this.mName);
    }

    // =========================================================================
    // Tier Configuration
    // =========================================================================

    @Override
    protected void onTierChanged(ECOAETier newTier) {
        super.onTierChanged(newTier);
        updateTierConfig();
    }

    /**
     * Update tier-dependent configuration values.
     */
    private void updateTierConfig() {
        switch (currentTier) {
            case L9:
                storageCapacity = Config.eStorageBaseCapacityL9;
                maxCellDrives = Config.eStorageMaxCellDrivesL9;
                break;
            case L6:
                storageCapacity = Config.eStorageBaseCapacityL6;
                maxCellDrives = Config.eStorageMaxCellDrivesL6;
                break;
            case L4:
            default:
                storageCapacity = Config.eStorageBaseCapacityL4;
                maxCellDrives = Config.eStorageMaxCellDrivesL4;
                break;
        }
    }

    // =========================================================================
    // Structure Definition
    // =========================================================================

    @Override
    public IStructureDefinition<EStorageController> getStructureDefinition() {
        return StructureDefinition.<EStorageController>builder()
            .addShape("main", getStructurePattern())
            // Structure validation is performed manually in checkMachine()
            .build();
    }

    @Override
    public String[][] getStructurePattern() {
        // Fixed 3x3x3 section only (y, z, x convention for StructureLib)
        // Layer y=0 (bottom): CCC / CMC / CCC
        // Layer y=1 (middle): CCC / CEC / CCC  (E = controller at center)
        // Layer y=2 (top):    CCC / CCC / CCC
        return new String[][] {
            { "CCC", "CMC", "CCC" },
            { "CCC", "CEC", "CCC" },
            { "CCC", "CCC", "CCC" }
        };
    }

    // =========================================================================
    // Structure Validation - checkMachine()
    // =========================================================================

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        // Reset component counters for fresh validation
        installedCellDrives = 0;
        installedEnergyCells = 0;
        installedVents = 0;
        segmentCount = 0;

        if (aBaseMetaTileEntity == null || aBaseMetaTileEntity.getWorld() == null) {
            return false;
        }

        int controllerX = aBaseMetaTileEntity.getXCoord();
        int controllerY = aBaseMetaTileEntity.getYCoord();
        int controllerZ = aBaseMetaTileEntity.getZCoord();

        // Step 1: Find the fixed 3x3 section by scanning all directions from the controller.
        // The controller is at the center of the middle layer (y+1) of the fixed section.
        // We look for the section origin at (controller - 1) in each direction.
        ForgeDirection scanDir = ForgeDirection.UNKNOWN;
        int fixedX = 0;
        int fixedY = 0;
        int fixedZ = 0;
        boolean foundFixedSection = false;

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            // Calculate the section origin such that the controller is at local (1,1,1).
            // For positive scan direction: origin = controller - 2 in scan axis, -1 in others
            // For negative scan direction: origin = controller + 0 in scan axis, -1 in others
            int testX = controllerX + (dir.offsetX > 0 ? -2 : (dir.offsetX < 0 ? 0 : -1));
            int testY = controllerY - 1; // Controller is at y=1 of the section
            int testZ = controllerZ + (dir.offsetZ > 0 ? -2 : (dir.offsetZ < 0 ? 0 : -1));

            if (validateFixedSection(aBaseMetaTileEntity, testX, testY, testZ, dir)) {
                fixedX = testX;
                fixedY = testY;
                fixedZ = testZ;
                scanDir = dir;
                foundFixedSection = true;
                break;
            }
        }

        if (!foundFixedSection || scanDir == ForgeDirection.UNKNOWN) {
            return false;
        }

        // Step 2: Scan outward from the fixed section for repeating segments.
        // Segments start one block past the front face of the fixed section.
        // For positive direction: start at section origin + 3 (front face) + 1
        // For negative direction: start at section origin + 0 (front face) - 1
        int segmentEdgeX = fixedX + (scanDir.offsetX > 0 ? 3 : (scanDir.offsetX < 0 ? -1 : 1));
        int segmentEdgeY = fixedY; // Same base Y as fixed section
        int segmentEdgeZ = fixedZ + (scanDir.offsetZ > 0 ? 3 : (scanDir.offsetZ < 0 ? -1 : 1));

        ScanResult result = scanSegments(aBaseMetaTileEntity, segmentEdgeX, segmentEdgeY, segmentEdgeZ, scanDir);

        if (!result.valid || result.count < MIN_SEGMENTS) {
            return false;
        }

        segmentCount = result.count;

        // Step 3: Validate the end cap after the last segment
        if (!validateEndCap(aBaseMetaTileEntity, result.endX, result.endY, result.endZ, scanDir)) {
            return false;
        }

        // All validation passed - notify base class
        onStructureFormed();
        return true;
    }

    // =========================================================================
    // Structure Validation - Fixed Section
    // =========================================================================

    /**
     * Validate the fixed 3x3x3 section at the given origin. The scan direction determines which
     * face is the "front" (where segments will extend from) and which corner gets the ME channel.
     *
     * <p>Layout (relative to origin, with scanDir as "front"):
     * <pre>
     * Layer y=0 (bottom): CCC / ... / CMC   M = ME channel at front-right corner
     * Layer y=1 (middle): CCC / CEC / CCC   E = controller at center
     * Layer y=2 (top):    CCC / CCC / CCC
     * </pre>
     *
     * @return true if the section is valid
     */
    private boolean validateFixedSection(IGregTechTileEntity base, int ox, int oy, int oz,
        ForgeDirection scanDir) {

        if (base.getWorld() == null) return false;

        // Calculate the local coordinates for the front face and right face.
        // Front face: the face at the maximum offset in the scan direction.
        // Right face: perpendicular to front, on the right when looking at the front face.
        int frontLocal = (scanDir.offsetX + scanDir.offsetZ) > 0 ? 2 : 0;
        ForgeDirection rightDir = getRightDirection(scanDir);
        int rightLocal = (rightDir.offsetX + rightDir.offsetZ) > 0 ? 2 : 0;

        // Validate all 27 positions in the 3x3x3 section
        for (int dy = 0; dy < 3; dy++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    int wx = ox + dx;
                    int wy = oy + dy;
                    int wz = oz + dz;

                    // Layer y=1, center position: controller
                    if (dy == 1 && dz == 1 && dx == 1) {
                        if (!isControllerBlock(base, wx, wy, wz)) {
                            return false;
                        }
                        continue;
                    }

                    // Layer y=0, front-right corner: ME channel
                    // Front face is at frontLocal in the scan axis.
                    // Right face is at rightLocal in the perpendicular axis.
                    boolean onFrontFace = isOnFace(dx, dz, scanDir, frontLocal);
                    boolean onRightFace = isOnFace(dx, dz, rightDir, rightLocal);
                    if (dy == 0 && onFrontFace && onRightFace) {
                        if (!isMEChannelBlock(base, wx, wy, wz)) {
                            return false;
                        }
                        continue;
                    }

                    // All other positions: casings
                    if (!isCasingBlock(base, wx, wy, wz)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check if the local (dx, dz) position is on the face defined by the given direction at the
     * specified local offset.
     */
    private boolean isOnFace(int dx, int dz, ForgeDirection dir, int localOffset) {
        if (dir.offsetX != 0) {
            return dx == localOffset;
        } else if (dir.offsetZ != 0) {
            return dz == localOffset;
        }
        return false;
    }

    // =========================================================================
    // Structure Validation - Repeating Segments
    // =========================================================================

    /**
     * Scan outward from the segment edge for repeating segments.
     *
     * <p>Each segment is 3 high x 2 deep x 1 wide, positioned at the center column of the
     * fixed section's cross-section:
     * <pre>
     * Near depth: .E. / .D. / .E.   (energy cells at y=0,y=2; cell drive at y=1)
     * Far depth:  .D. / .V. / .D.   (cell drives at y=0,y=2; vent at y=1)
     * </pre>
     *
     * @return A ScanResult containing validity, count, and end position
     */
    private ScanResult scanSegments(IGregTechTileEntity base, int startX, int startY, int startZ,
        ForgeDirection scanDir) {

        int count = 0;
        int currentX = startX;
        int currentY = startY;
        int currentZ = startZ;

        while (count < MAX_SEGMENTS) {
            if (!validateSegment(base, currentX, currentY, currentZ, scanDir)) {
                break;
            }
            count++;
            // Advance to the next segment position (2 blocks deep in scan direction)
            currentX += scanDir.offsetX * 2;
            currentY += scanDir.offsetY * 2;
            currentZ += scanDir.offsetZ * 2;
        }

        return new ScanResult(count >= MIN_SEGMENTS, count, currentX, currentY, currentZ);
    }

    /**
     * Validate a single repeating segment at the given position.
     *
     * @return true if the segment is valid
     */
    private boolean validateSegment(IGregTechTileEntity base, int ox, int oy, int oz,
        ForgeDirection scanDir) {

        if (base.getWorld() == null) return false;

        // Near depth block (first position in scan direction)
        int nearX = ox;
        int nearY = oy;
        int nearZ = oz;

        // Far depth block (second position in scan direction)
        int farX = ox + scanDir.offsetX;
        int farY = oy + scanDir.offsetY;
        int farZ = oz + scanDir.offsetZ;

        // Validate near depth column (3 high)
        // y=0: energy cell
        if (!isEnergyCellBlock(base, nearX, nearY, nearZ)) return false;
        installedEnergyCells++;

        // y=1: cell drive
        if (!isCellDriveBlock(base, nearX, nearY + 1, nearZ)) return false;
        installedCellDrives++;

        // y=2: energy cell
        if (!isEnergyCellBlock(base, nearX, nearY + 2, nearZ)) return false;
        installedEnergyCells++;

        // Validate far depth column (3 high)
        // y=0: cell drive
        if (!isCellDriveBlock(base, farX, farY, farZ)) return false;
        installedCellDrives++;

        // y=1: vent
        if (!isVentBlock(base, farX, farY + 1, farZ)) return false;
        installedVents++;

        // y=2: cell drive
        if (!isCellDriveBlock(base, farX, farY + 2, farZ)) return false;
        installedCellDrives++;

        return true;
    }

    // =========================================================================
    // Structure Validation - End Cap
    // =========================================================================

    /**
     * Validate the end cap (3 high x 2 deep x 1 wide, all casings).
     * The depth direction follows the scan direction.
     *
     * @return true if the end cap is valid
     */
    private boolean validateEndCap(IGregTechTileEntity base, int ox, int oy, int oz,
        ForgeDirection scanDir) {

        if (base.getWorld() == null) return false;

        // End cap is 1 wide x 2 deep x 3 high, all casings
        for (int dy = 0; dy < 3; dy++) {
            for (int dd = 0; dd < 2; dd++) {
                int wx = ox + scanDir.offsetX * dd;
                int wy = oy + dy;
                int wz = oz + scanDir.offsetZ * dd;
                if (!isCasingBlock(base, wx, wy, wz)) {
                    return false;
                }
            }
        }

        return true;
    }

    // =========================================================================
    // Block Type Checkers (using BlockLoader references)
    // =========================================================================

    private boolean isControllerBlock(IGregTechTileEntity base, int x, int y, int z) {
        // Controller is the MTE itself, already validated by position
        return base.getWorld().getBlock(x, y, z) == base.getBlockType()
            && base.getWorld().getBlockMetadata(x, y, z) == base.getMetaTileEntityID();
    }

    private boolean isCasingBlock(IGregTechTileEntity base, int x, int y, int z) {
        return base.getWorld().getBlock(x, y, z) == BlockLoader.estorageBlocks
            && base.getWorld().getBlockMetadata(x, y, z) == BlockLoader.ESTORAGE_META_CASING;
    }

    private boolean isCellDriveBlock(IGregTechTileEntity base, int x, int y, int z) {
        return base.getWorld().getBlock(x, y, z) == BlockLoader.estorageBlocks
            && base.getWorld().getBlockMetadata(x, y, z) == BlockLoader.ESTORAGE_META_CELL_DRIVE;
    }

    private boolean isEnergyCellBlock(IGregTechTileEntity base, int x, int y, int z) {
        return base.getWorld().getBlock(x, y, z) == BlockLoader.estorageBlocks
            && base.getWorld().getBlockMetadata(x, y, z) == BlockLoader.ESTORAGE_META_ENERGY_CELL;
    }

    private boolean isVentBlock(IGregTechTileEntity base, int x, int y, int z) {
        return base.getWorld().getBlock(x, y, z) == BlockLoader.estorageBlocks
            && base.getWorld().getBlockMetadata(x, y, z) == BlockLoader.ESTORAGE_META_VENT;
    }

    private boolean isMEChannelBlock(IGregTechTileEntity base, int x, int y, int z) {
        return base.getWorld().getBlock(x, y, z) == BlockLoader.estorageBlocks
            && base.getWorld().getBlockMetadata(x, y, z) == BlockLoader.ESTORAGE_META_ME_CHANNEL;
    }

    // =========================================================================
    // Direction Utilities
    // =========================================================================

    /**
     * Get the "right" direction relative to the given front direction. Uses the cross product of
     * UP x FRONT to determine the right-hand direction.
     */
    private ForgeDirection getRightDirection(ForgeDirection front) {
        switch (front) {
            case NORTH: return ForgeDirection.EAST;
            case EAST:  return ForgeDirection.SOUTH;
            case SOUTH: return ForgeDirection.WEST;
            case WEST:  return ForgeDirection.NORTH;
            default:    return ForgeDirection.EAST;
        }
    }

    // =========================================================================
    // AE2 Integration - ICellProvider
    // =========================================================================

    /**
     * Called by the AE2 storage grid to enumerate available cell inventories.
     * Each installed cell drive can contribute storage cells to the network.
     */
    @Override
    public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
        List<IMEInventoryHandler> cells = new ArrayList<>();

        // TODO: Iterate over cell drive tile entities in the structure and collect their cells
        // Each cell drive holds storage cells that provide IMEInventoryHandler for the channel.
        // The cell inventory resolution is handled by EStorageCellHandler.

        return cells;
    }

    @Override
    protected void connectToAE2Network() {
        // Call base class to set up the proxy
        super.connectToAE2Network();

        // Register as a cell provider with the AE2 storage grid
        registerCellProvider();
    }

    @Override
    protected void disconnectFromAE2Network() {
        // Unregister cell provider before disconnecting
        unregisterCellProvider();

        // Call base class to tear down the proxy
        super.disconnectFromAE2Network();
    }

    /**
     * Register this controller as an AE2 cell provider, exposing cell drive inventories to the
     * network.
     */
    private void registerCellProvider() {
        if (cellProviderRegistered) return;
        if (aeProxy == null || !aeProxy.isActive()) return;

        try {
            IGrid grid = aeProxy.getGrid();
            if (grid == null) return;

            IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
            if (storageGrid != null) {
                AE2StorageHelper.registerCellProvider(storageGrid, this);
                cellProviderRegistered = true;
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to register EStorage cell provider", e);
            cellProviderRegistered = false;
        }
    }

    /**
     * Unregister this controller from the AE2 storage grid.
     */
    private void unregisterCellProvider() {
        if (!cellProviderRegistered) return;
        if (aeProxy == null) return;

        try {
            IGrid grid = aeProxy.getGrid();
            if (grid == null) return;

            IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
            if (storageGrid != null) {
                AE2StorageHelper.unregisterCellProvider(storageGrid, this);
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to unregister EStorage cell provider", e);
        }
        cellProviderRegistered = false;
    }

    // =========================================================================
    // Structure Lifecycle
    // =========================================================================

    @Override
    protected void onStructureInvalidated() {
        // Unregister cell provider before disconnecting
        unregisterCellProvider();

        // Reset component counters
        installedCellDrives = 0;
        installedEnergyCells = 0;
        installedVents = 0;
        segmentCount = 0;

        super.onStructureInvalidated();
    }

    // =========================================================================
    // Tick Processing
    // =========================================================================

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);

        if (aBaseMetaTileEntity.isServerSide() && mMachine) {
            tickCounter++;

            if (tickCounter >= UPDATE_INTERVAL) {
                tickCounter = 0;
                processPeriodicUpdate();
            }
        }
    }

    /**
     * Perform periodic updates: AE2 power usage and energy cell distribution.
     */
    private void processPeriodicUpdate() {
        if (!ae2Connected) return;

        // Distribute power across energy cells
        // Each segment contributes energy cells that power the AE2 network
        if (installedEnergyCells > 0) {
            // Power management is handled by the AE2 proxy and energy hatches.
            // Energy cells in the structure contribute to the overall power buffer.
            // The actual power draw is managed by AE2's network power system.
        }
    }

    // =========================================================================
    // NBT Persistence
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger(NBT_INSTALLED_DRIVES, installedCellDrives);
        aNBT.setInteger(NBT_INSTALLED_ENERGY, installedEnergyCells);
        aNBT.setInteger(NBT_INSTALLED_VENTS, installedVents);
        aNBT.setInteger(NBT_SEGMENT_COUNT, segmentCount);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        installedCellDrives = aNBT.getInteger(NBT_INSTALLED_DRIVES);
        installedEnergyCells = aNBT.getInteger(NBT_INSTALLED_ENERGY);
        installedVents = aNBT.getInteger(NBT_INSTALLED_VENTS);
        segmentCount = aNBT.getInteger(NBT_SEGMENT_COUNT);
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Get the total storage capacity accounting for parallel multiplier from tier.
     */
    public long getStorageCapacity() {
        return storageCapacity * getParallelCount();
    }

    /**
     * Get the maximum number of cell drives allowed by the current tier.
     */
    public int getMaxCellDrives() {
        return maxCellDrives;
    }

    /**
     * Get the number of cell drives found in the current structure.
     */
    public int getInstalledCellDrives() {
        return installedCellDrives;
    }

    /**
     * Get the number of energy cells found in the current structure.
     */
    public int getInstalledEnergyCells() {
        return installedEnergyCells;
    }

    /**
     * Get the number of cooling vents found in the current structure.
     */
    public int getInstalledVents() {
        return installedVents;
    }

    /**
     * Get the number of repeating segments found in the current structure.
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    // =========================================================================
    // GUI
    // =========================================================================

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        aPlayer.openGui(
            ECOAEExtension.instance,
            com.github.GTNewHorizons.ecoaeextension.gui.ECOAEGuiHandler.GUI_ID_ESTORAGE,
            aBaseMetaTileEntity.getWorld(),
            aBaseMetaTileEntity.getXCoord(),
            aBaseMetaTileEntity.getYCoord(),
            aBaseMetaTileEntity.getZCoord()
        );
        return true;
    }

    // =========================================================================
    // Display
    // =========================================================================

    @Override
    public String[] getDescription() {
        return new String[] {
            EnumChatFormatting.AQUA + "EStorage Controller",
            EnumChatFormatting.GRAY + "Extendable AE2 Storage System",
            EnumChatFormatting.GRAY + "Tier: " + EnumChatFormatting.YELLOW + currentTier.name,
            EnumChatFormatting.GRAY + "Capacity: " + EnumChatFormatting.GREEN + formatBytes(getStorageCapacity()),
            EnumChatFormatting.GRAY + "Segments: " + EnumChatFormatting.GREEN + segmentCount,
            EnumChatFormatting.GRAY + "Cell Drives: " + EnumChatFormatting.YELLOW + installedCellDrives
                + EnumChatFormatting.GRAY + "/" + EnumChatFormatting.YELLOW + maxCellDrives,
            EnumChatFormatting.GRAY + "Energy Cells: " + EnumChatFormatting.YELLOW + installedEnergyCells,
            EnumChatFormatting.GRAY + "Vents: " + EnumChatFormatting.YELLOW + installedVents,
            EnumChatFormatting.GRAY + "AE2: " + (ae2Connected ? EnumChatFormatting.GREEN + "Connected"
                : EnumChatFormatting.RED + "Disconnected")
        };
    }

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip.add(EnumChatFormatting.AQUA + "EStorage Controller");
        tooltip.add(EnumChatFormatting.GRAY + "Extendable AE2-integrated storage system");
        tooltip.add(EnumChatFormatting.GRAY + "Supports item, fluid, and gas storage cells");
        tooltip.add(EnumChatFormatting.YELLOW + "L4 (HV): " + formatBytes(Config.eStorageBaseCapacityL4)
            + EnumChatFormatting.GRAY + ", " + Config.eStorageMaxCellDrivesL4 + " drives");
        tooltip.add(EnumChatFormatting.YELLOW + "L6 (IV): " + formatBytes(Config.eStorageBaseCapacityL6)
            + EnumChatFormatting.GRAY + ", " + Config.eStorageMaxCellDrivesL6 + " drives");
        tooltip.add(EnumChatFormatting.YELLOW + "L9 (LuV): " + formatBytes(Config.eStorageBaseCapacityL9)
            + EnumChatFormatting.GRAY + ", " + Config.eStorageMaxCellDrivesL9 + " drives");
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return new String[] {
            "EStorage Multiblock Structure:",
            "- 3x3x3 fixed section with controller and ME channel",
            "- Repeating segments (min " + MIN_SEGMENTS + ", max " + MAX_SEGMENTS + "):",
            "  Each segment: cell drives, energy cells, and vents",
            "- End cap of casings at the far end",
            "- Tier determined by energy hatch voltage (L4/L6/L9)"
        };
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
    // Inner Classes
    // =========================================================================

    /**
     * Result of scanning for repeating segments.
     */
    private static class ScanResult {

        final boolean valid;
        final int count;
        final int endX;
        final int endY;
        final int endZ;

        ScanResult(boolean valid, int count, int endX, int endY, int endZ) {
            this.valid = valid;
            this.count = count;
            this.endX = endX;
            this.endY = endY;
            this.endZ = endZ;
        }
    }
}
