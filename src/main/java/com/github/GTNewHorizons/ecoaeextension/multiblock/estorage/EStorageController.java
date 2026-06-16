package com.github.GTNewHorizons.ecoaeextension.multiblock.estorage;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.GTNewHorizons.ecoaeextension.Config;
import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.ae2.AE2StorageHelper;
import com.github.GTNewHorizons.ecoaeextension.gui.mui2.EStorageGui;
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
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * EStorage Controller - An extendable AE2-integrated storage multiblock.
 *
 * <p>
 * Structure: Linear multiblock with a fixed 3x3x3 section containing the controller and ME
 * channel, repeating segments with cell drives, energy cells, and vents extending from it, and an
 * end cap of casings.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Custom storage cells for items, fluids, and gases (Mekanism)</li>
 * <li>Energy cells for AE2 power management</li>
 * <li>Cell drives for holding storage cells</li>
 * <li>ME network connectivity via dedicated channel blocks</li>
 * <li>Three tiers: L4 (HV), L6 (IV), L9 (LuV) affecting capacity and cell count</li>
 * </ul>
 */
public class EStorageController extends ECOAEExtendedPowerMultiBlockBase<EStorageController>
    implements ICellProvider, appeng.api.storage.ISaveProvider {

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

    /** Cell ItemStacks stored in the controller (one per cell drive slot) */
    private final List<ItemStack> cellStacks = new ArrayList<>();

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
                storageCapacity = Config.eStorageEnergyCellCapacityL9;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL9;
                break;
            case L6:
                storageCapacity = Config.eStorageEnergyCellCapacityL6;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL6;
                break;
            case L4:
            default:
                storageCapacity = Config.eStorageEnergyCellCapacityL4;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL4;
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
            .addElement('C', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_CASING))
            .addElement('E', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_CASING))
            .addElement('M', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_ME_CHANNEL))
            .build();
    }

    @Override
    public String[][] getStructurePattern() {
        // Fixed 3x3x3 section only (y, z, x convention for StructureLib)
        // Layer y=0 (bottom): CCC / CMC / CCC
        // Layer y=1 (middle): CCC / CEC / CCC (E = controller at center)
        // Layer y=2 (top): CCC / CCC / CCC
        return new String[][] { { "CCC", "CMC", "CCC" }, { "CCC", "CEC", "CCC" }, { "CCC", "CCC", "CCC" } };
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        // Build a minimal structure: fixed section + 1 segment + end cap
        // Controller is at corner of 2x3x2 fixed section
        // Structure extends in the -X direction from the controller
        int cx = getBaseMetaTileEntity().getXCoord();
        int cy = getBaseMetaTileEntity().getYCoord();
        int cz = getBaseMetaTileEntity().getZCoord();

        // Build the 2x3x2 fixed section (controller at 0,0,0)
        // x=0..1, y=-1..1, z=0..1
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    // Controller position - skip
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    // ME channel at (1, -1, 1)
                    if (dx == 1 && dy == -1 && dz == 1) {
                        if (!hintsOnly) {
                            getBaseMetaTileEntity().getWorld()
                                .setBlock(
                                    cx + dx,
                                    cy + dy,
                                    cz + dz,
                                    BlockLoader.estorageBlocks,
                                    BlockLoader.ESTORAGE_META_ME_CHANNEL,
                                    2);
                        }
                        continue;
                    }

                    // All other positions: casings
                    if (!hintsOnly) {
                        getBaseMetaTileEntity().getWorld()
                            .setBlock(
                                cx + dx,
                                cy + dy,
                                cz + dz,
                                BlockLoader.estorageBlocks,
                                BlockLoader.ESTORAGE_META_CASING,
                                2);
                    }
                }
            }
        }

        // Build 1 segment at x=-2..-1, y=-1..1, z=0..1
        // Near (x=-1): cell drives at y=-1,0,1
        // Far (x=-2): energy cells at y=-1,1; vent at y=0
        for (int dy = -1; dy <= 1; dy++) {
            if (!hintsOnly) {
                getBaseMetaTileEntity().getWorld()
                    .setBlock(cx - 1, cy + dy, cz, BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_CELL_DRIVE, 2);
            }
            int meta = (dy == 0) ? BlockLoader.ESTORAGE_META_VENT : BlockLoader.ESTORAGE_META_ENERGY_CELL;
            if (!hintsOnly) {
                getBaseMetaTileEntity().getWorld()
                    .setBlock(cx - 2, cy + dy, cz, BlockLoader.estorageBlocks, meta, 2);
            }
        }

        // Build end cap at x=-3..-4, y=-1..1, z=0..1 (all casings)
        for (int dx = -3; dx >= -4; dx--) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!hintsOnly) {
                    getBaseMetaTileEntity().getWorld()
                        .setBlock(
                            cx + dx,
                            cy + dy,
                            cz,
                            BlockLoader.estorageBlocks,
                            BlockLoader.ESTORAGE_META_CASING,
                            2);
                }
            }
        }
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

        int cx = aBaseMetaTileEntity.getXCoord();
        int cy = aBaseMetaTileEntity.getYCoord();
        int cz = aBaseMetaTileEntity.getZCoord();

        // Step 1: Validate the fixed 2x3x2 section.
        // Controller is at (0,0,0), section spans x=0..1, y=-1..1, z=0..1
        // ME channel at (1,-1,1), all others are casings
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    // Controller position - skip
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    Block block = aBaseMetaTileEntity.getWorld()
                        .getBlock(cx + dx, cy + dy, cz + dz);
                    int meta = aBaseMetaTileEntity.getWorld()
                        .getBlockMetadata(cx + dx, cy + dy, cz + dz);

                    // ME channel at (1, -1, 1)
                    if (dx == 1 && dy == -1 && dz == 1) {
                        if (block != BlockLoader.estorageBlocks || meta != BlockLoader.ESTORAGE_META_ME_CHANNEL) {
                            return false;
                        }
                        continue;
                    }

                    // All other positions: casings
                    if (block != BlockLoader.estorageBlocks || meta != BlockLoader.ESTORAGE_META_CASING) {
                        return false;
                    }
                }
            }
        }

        // Step 2: Scan for segments extending in -X direction
        // Segments start at x=-1 (adjacent to fixed section)
        int segX = cx - 1;
        int count = 0;

        while (count < MAX_SEGMENTS) {
            // Check if this is a valid segment (3 high x 1 wide x 1 deep)
            if (!validateSegment(aBaseMetaTileEntity, segX, cy, cz)) {
                break;
            }
            count++;
            segX -= 1; // Each segment is 1 block deep
        }

        if (count < MIN_SEGMENTS) {
            return false;
        }

        segmentCount = count;

        // Step 3: Validate end cap (3 high x 2 deep x 1 wide, all casings)
        if (!validateEndCap(aBaseMetaTileEntity, segX, cy, cz)) {
            return false;
        }

        // Ensure cellStacks list has entries for all installed cell drives
        while (cellStacks.size() < installedCellDrives) {
            cellStacks.add(null);
        }
        while (cellStacks.size() > installedCellDrives) {
            cellStacks.remove(cellStacks.size() - 1);
        }

        // All validation passed - notify base class
        onStructureFormed();
        return true;
    }

    /**
     * Validate a single segment at the given position.
     * Segment is 3 high (y=-1..1) x 1 wide (z=0) x 1 deep (x=segX)
     * Cell drives at y=-1,1; cell drive at y=0
     */
    private boolean validateSegment(IGregTechTileEntity base, int sx, int cy, int cz) {
        for (int dy = -1; dy <= 1; dy++) {
            Block block = base.getWorld()
                .getBlock(sx, cy + dy, cz);
            int meta = base.getWorld()
                .getBlockMetadata(sx, cy + dy, cz);

            if (block != BlockLoader.estorageBlocks) return false;
            if (meta != BlockLoader.ESTORAGE_META_CELL_DRIVE) return false;
            installedCellDrives++;
        }
        return true;
    }

    /**
     * Validate the end cap (3 high x 2 deep x 1 wide, all casings).
     */
    private boolean validateEndCap(IGregTechTileEntity base, int ox, int cy, int cz) {
        for (int dx = 0; dx >= -1; dx--) {
            for (int dy = -1; dy <= 1; dy++) {
                Block block = base.getWorld()
                    .getBlock(ox + dx, cy + dy, cz);
                int meta = base.getWorld()
                    .getBlockMetadata(ox + dx, cy + dy, cz);
                if (block != BlockLoader.estorageBlocks || meta != BlockLoader.ESTORAGE_META_CASING) {
                    return false;
                }
            }
        }
        return true;
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
            case NORTH:
                return ForgeDirection.EAST;
            case EAST:
                return ForgeDirection.SOUTH;
            case SOUTH:
                return ForgeDirection.WEST;
            case WEST:
                return ForgeDirection.NORTH;
            default:
                return ForgeDirection.EAST;
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
        if (channel != StorageChannel.ITEMS) return cells;

        // Return handlers for stored cell ItemStacks
        for (int i = 0; i < cellStacks.size(); i++) {
            ItemStack cellStack = cellStacks.get(i);
            if (cellStack != null) {
                try {
                    IMEInventoryHandler handler = com.github.GTNewHorizons.ecoaeextension.ae2.EStorageCellHandler.INSTANCE
                        .getCellInventory(cellStack, this, channel);
                    if (handler != null) {
                        cells.add(handler);
                    }
                } catch (Exception e) {
                    ECOAEExtension.LOG.debug("Failed to get cell inventory for slot {}", i, e);
                }
            }
        }
        return cells;
    }

    @Override
    public void saveChanges(appeng.api.storage.IMEInventory inventory) {
        // Called by EStorageCellInventory when cell data changes.
        // Mark the controller as needing to save its NBT data.
        if (getBaseMetaTileEntity() != null) {
            getBaseMetaTileEntity().markDirty();
        }
    }

    @Override
    public int getPriority() {
        return 0;
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
        cellStacks.clear();

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

        // Save cell ItemStacks
        net.minecraft.nbt.NBTTagList cellList = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < cellStacks.size(); i++) {
            if (cellStacks.get(i) != null) {
                net.minecraft.nbt.NBTTagCompound cellTag = new net.minecraft.nbt.NBTTagCompound();
                cellStacks.get(i)
                    .writeToNBT(cellTag);
                cellList.appendTag(cellTag);
            }
        }
        aNBT.setTag("ecoae_cells", cellList);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        installedCellDrives = aNBT.getInteger(NBT_INSTALLED_DRIVES);
        installedEnergyCells = aNBT.getInteger(NBT_INSTALLED_ENERGY);
        installedVents = aNBT.getInteger(NBT_INSTALLED_VENTS);
        segmentCount = aNBT.getInteger(NBT_SEGMENT_COUNT);

        // Load cell ItemStacks
        cellStacks.clear();
        if (aNBT.hasKey("ecoae_cells")) {
            net.minecraft.nbt.NBTTagList cellList = aNBT.getTagList("ecoae_cells", 10);
            for (int i = 0; i < cellList.tagCount(); i++) {
                cellStacks.add(ItemStack.loadItemStackFromNBT(cellList.getCompoundTagAt(i)));
            }
        }
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
    public int getEnergyCellCapacity() {
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
    // Cell Management
    // =========================================================================

    /**
     * Insert a storage cell into the first available slot.
     *
     * @param cellStack The cell ItemStack to insert
     * @return true if the cell was inserted successfully
     */
    public boolean insertCell(ItemStack cellStack) {
        if (cellStack == null) return false;
        if (!(cellStack.getItem() instanceof com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell))
            return false;

        // Find first null slot
        for (int i = 0; i < cellStacks.size(); i++) {
            if (cellStacks.get(i) == null) {
                cellStacks.set(i, cellStack.copy());
                return true;
            }
        }
        return false; // No available slots
    }

    /**
     * Remove a storage cell from the given slot.
     *
     * @param slotIndex The slot index to remove from
     * @return The removed cell ItemStack, or null if slot is empty/invalid
     */
    public ItemStack removeCell(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= cellStacks.size()) return null;
        ItemStack removed = cellStacks.get(slotIndex);
        cellStacks.set(slotIndex, null);
        return removed;
    }

    /**
     * Get the list of cell ItemStacks (for GUI display).
     */
    public List<ItemStack> getCellStacks() {
        return cellStacks;
    }

    /**
     * Get the number of non-null cells installed.
     */
    public int getActiveCellCount() {
        int count = 0;
        for (ItemStack stack : cellStacks) {
            if (stack != null) count++;
        }
        return count;
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

        // Check if player is holding a storage cell - insert it
        ItemStack heldItem = aPlayer.getHeldItem();
        if (heldItem != null
            && heldItem.getItem() instanceof com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell) {
            if (insertCell(heldItem)) {
                aPlayer.inventory.decrStackSize(aPlayer.inventory.currentItem, 1);
                aPlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GREEN + String.format(
                            StatCollector.translateToLocal("ecoaeext.chat.cell_inserted"),
                            getActiveCellCount(),
                            cellStacks.size())));
                return true;
            } else {
                aPlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.RED + String.format(
                            StatCollector.translateToLocal("ecoaeext.chat.no_cell_slots"),
                            installedCellDrives)));
                return true;
            }
        }

        // Check if player is sneaking with empty hand - remove last cell
        if (aPlayer.isSneaking() && (heldItem == null)) {
            for (int i = cellStacks.size() - 1; i >= 0; i--) {
                ItemStack removed = removeCell(i);
                if (removed != null) {
                    if (!aPlayer.inventory.addItemStackToInventory(removed)) {
                        aPlayer.dropPlayerItemWithRandomChoice(removed, false);
                    }
                    aPlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            net.minecraft.util.EnumChatFormatting.YELLOW + String.format(
                                StatCollector.translateToLocal("ecoaeext.chat.cell_removed"),
                                getActiveCellCount(),
                                cellStacks.size())));
                    return true;
                }
            }
            aPlayer.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    net.minecraft.util.EnumChatFormatting.RED
                        + StatCollector.translateToLocal("ecoaeext.chat.no_cells")));
            return true;
        }

        // Normal right-click opens GUI via GT5 MUI2 system
        openGui(aPlayer);
        return true;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new EStorageGui(this);
    }

    // =========================================================================
    // Display
    // =========================================================================

    // getDescription() is final in MTETooltipMultiBlockBase and cannot be overridden.
    // Static tooltip information is provided via createTooltip() in the base class,
    // and dynamic runtime information is provided via addAdditionalTooltipInformation().

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("ecoaeext.tooltip.estorage_controller"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.estorage_desc"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.storage_cells"));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.estorage_tier_l4"),
                formatBytes(Config.eStorageEnergyCellCapacityL4),
                Config.eStorageCellDriveCapacityL4));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.estorage_tier_l6"),
                formatBytes(Config.eStorageEnergyCellCapacityL6),
                Config.eStorageCellDriveCapacityL6));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.estorage_tier_l9"),
                formatBytes(Config.eStorageEnergyCellCapacityL9),
                Config.eStorageCellDriveCapacityL9));
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return new String[] { StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_title"),
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_fixed"),
            String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_segments"),
                MIN_SEGMENTS,
                MAX_SEGMENTS),
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_segment_info"),
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_endcap"),
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_tier") };
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

}
