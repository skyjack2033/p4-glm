package com.github.GTNewHorizons.ecoaeextension.multiblock.estorage;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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
 * Structure: 2x2 cross-section multiblock growing vertically. Base layer contains the controller
 * and ME channel, repeating stackable layers with cell drives, energy cells, vents, or casings
 * (1-16 layers), and a top end cap of casings. GT5U hatches (energy, maintenance, including
 * debug variants) are accepted at casing positions in all layers.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Custom storage cells for items, fluids, and gases (Mekanism)</li>
 * <li>Energy cells for AE2 power management</li>
 * <li>Cell drives for holding storage cells</li>
 * <li>ME network connectivity via dedicated channel blocks</li>
 * <li>Three tiers: L4 (HV), L6 (IV), L9 (LuV) affecting capacity and cell count</li>
 * <li>Storage capacity scales with energy hatch voltage and stackable layer count</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
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
     * Storage capacity scales with voltage tier and number of stackable layers.
     */
    private void updateTierConfig() {
        long baseCapacity;
        switch (currentTier) {
            case L9:
                baseCapacity = Config.eStorageEnergyCellCapacityL9;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL9;
                break;
            case L6:
                baseCapacity = Config.eStorageEnergyCellCapacityL6;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL6;
                break;
            case L4:
            default:
                baseCapacity = Config.eStorageEnergyCellCapacityL4;
                maxCellDrives = (int) Config.eStorageCellDriveCapacityL4;
                break;
        }

        // Storage capacity = base capacity * number of stackable layers
        // This encourages building taller structures for more storage
        storageCapacity = baseCapacity * Math.max(1, segmentCount);
    }

    // =========================================================================
    // Structure Definition - Layered/Stackable Design
    // =========================================================================

    // Structure offsets: controller position in the shape array
    private static final int HORIZONTAL_OFF_SET = 0;
    private static final int VERTICAL_OFF_SET = 0;
    private static final int DEPTH_OFF_SET = 0;
    private static final String STRUCTURE_PIECE_MAIN = "main";

    // Base layer: Controller + ME channel + Casings
    // Stackable layers: Cell drives + Energy cells + Casings
    // Top layer: End cap casings
    //
    // Shape definition: [y][z][x] convention
    // y=0 (base): ~C / CM (~ = controller, M = ME channel)
    // y=1+ (stackable): CC / CC (cell drives or casings)
    // y=top: CC / CC (end cap)
    private static final String[][] baseShape = new String[][] { { "~C", "CM" }, // y=0 (base layer)
        { "CC", "CC" } // y=1 (first stackable layer)
    };

    @Override
    public IStructureDefinition<EStorageController> getStructureDefinition() {
        return StructureDefinition.<EStorageController>builder()
            .addShape(
                STRUCTURE_PIECE_MAIN,
                com.gtnewhorizon.structurelib.structure.StructureUtility.transpose(baseShape))
            .addElement('C', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_CASING))
            .addElement('M', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_ME_CHANNEL))
            .build();
    }

    @Override
    public String[][] getStructurePattern() {
        return baseShape;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        // Build base structure with 1 stackable layer
        this.buildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            hintsOnly,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET);
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

        // Validate base layer (y=0): Controller at (0,0,0), Casings, ME channel at (1,0,1)
        if (!validateBaseLayer(aBaseMetaTileEntity, cx, cy, cz)) {
            return false;
        }

        // Scan for stackable layers above the base
        int layerCount = 0;
        int currentY = cy + 1;

        while (layerCount < MAX_SEGMENTS) {
            if (!validateStackableLayer(aBaseMetaTileEntity, cx, currentY, cz)) {
                break;
            }
            layerCount++;
            currentY++;
        }

        if (layerCount < MIN_SEGMENTS) {
            return false;
        }

        segmentCount = layerCount;

        // Validate top layer (end cap)
        if (!validateTopLayer(aBaseMetaTileEntity, cx, currentY, cz)) {
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
     * Validate base layer: Controller at (0,0,0), Casings or GT5U hatches, ME channel at (1,0,1)
     *
     * <p>
     * Casing positions accept GT5U hatches (energy, maintenance, including debug variants)
     * in addition to EStorage casings.
     * </p>
     */
    private boolean validateBaseLayer(IGregTechTileEntity base, int cx, int cy, int cz) {
        // Check controller position (0,0,0) - should be the controller itself
        // Check ME channel at (1,0,1)
        Block meBlock = base.getWorld()
            .getBlock(cx + 1, cy, cz + 1);
        int meMeta = base.getWorld()
            .getBlockMetadata(cx + 1, cy, cz + 1);
        if (meBlock != BlockLoader.estorageBlocks || meMeta != BlockLoader.ESTORAGE_META_ME_CHANNEL) {
            return false;
        }

        // Check casings or hatches at other positions
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Controller position
                if (dx == 1 && dz == 1) continue; // ME channel

                Block block = base.getWorld()
                    .getBlock(cx + dx, cy, cz + dz);
                int meta = base.getWorld()
                    .getBlockMetadata(cx + dx, cy, cz + dz);

                // Check for GT5U hatch first
                TileEntity te = base.getWorld()
                    .getTileEntity(cx + dx, cy, cz + dz);
                if (te instanceof IGregTechTileEntity) {
                    IGregTechTileEntity gtte = (IGregTechTileEntity) te;
                    if (gtte.getMetaTileEntity() != null && addToMachineList(gtte, meta)) {
                        continue; // Valid hatch
                    }
                }

                // Must be an EStorage casing
                if (block != BlockLoader.estorageBlocks || meta != BlockLoader.ESTORAGE_META_CASING) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validate stackable layer: Cell drives, casings, or GT5U hatches (energy, maintenance, etc.)
     * Each layer is 2x2 blocks at (cx, y, cz) to (cx+1, y, cz+1)
     *
     * <p>
     * GT5U hatches (including debug variants) are accepted at any position and registered
     * via addToMachineList(). This allows players to use debug maintenance hatches, debug
     * energy hatches, and other GT5U utility blocks within the structure.
     * </p>
     */
    private boolean validateStackableLayer(IGregTechTileEntity base, int cx, int cy, int cz) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                Block block = base.getWorld()
                    .getBlock(cx + dx, cy, cz + dz);
                int meta = base.getWorld()
                    .getBlockMetadata(cx + dx, cy, cz + dz);

                // First check if this position has a GT5U hatch (energy, maintenance, etc.)
                // GT5U hatches are MetaTileEntities on vanilla GT blocks, not our custom blocks.
                // This includes debug hatches (infinite energy, debug maintenance, etc.)
                TileEntity te = base.getWorld()
                    .getTileEntity(cx + dx, cy, cz + dz);
                if (te instanceof IGregTechTileEntity) {
                    IGregTechTileEntity gtte = (IGregTechTileEntity) te;
                    if (gtte.getMetaTileEntity() != null && addToMachineList(gtte, meta)) {
                        // Valid GT5U hatch - registered with the multiblock
                        continue;
                    }
                }

                // Not a hatch - must be an EStorage block
                if (block != BlockLoader.estorageBlocks) return false;

                // Count components based on meta
                if (meta == BlockLoader.ESTORAGE_META_CELL_DRIVE) {
                    installedCellDrives++;
                } else if (meta == BlockLoader.ESTORAGE_META_ENERGY_CELL) {
                    installedEnergyCells++;
                } else if (meta == BlockLoader.ESTORAGE_META_VENT) {
                    installedVents++;
                } else if (meta != BlockLoader.ESTORAGE_META_CASING) {
                    return false; // Invalid block type
                }
            }
        }
        return true;
    }

    /**
     * Validate top layer: All casings (end cap)
     */
    private boolean validateTopLayer(IGregTechTileEntity base, int cx, int cy, int cz) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                Block block = base.getWorld()
                    .getBlock(cx + dx, cy, cz + dz);
                int meta = base.getWorld()
                    .getBlockMetadata(cx + dx, cy, cz + dz);
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
            EnumChatFormatting.GRAY + StatCollector.translateToLocal("ecoaeext.tooltip.estorage_capacity_scaling"));
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
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_tier"),
            StatCollector.translateToLocal("ecoaeext.tooltip.structure.estorage_hatch") };
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
