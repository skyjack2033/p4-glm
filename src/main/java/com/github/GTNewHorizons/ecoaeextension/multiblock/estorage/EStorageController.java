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

import com.github.GTNewHorizons.ecoaeextension.Config;
import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.ae2.AE2StorageHelper;
import com.github.GTNewHorizons.ecoaeextension.gui.mui2.EStorageGui;
import com.github.GTNewHorizons.ecoaeextension.loader.BlockLoader;
import com.github.GTNewHorizons.ecoaeextension.multiblock.ECOAEExtendedPowerMultiBlockBase;
import com.github.GTNewHorizons.ecoaeextension.util.ECOAETier;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

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
                maxCellDrives = Config.eStorageMaxCellDrivesL9;
                break;
            case L6:
                baseCapacity = Config.eStorageEnergyCellCapacityL6;
                maxCellDrives = Config.eStorageMaxCellDrivesL6;
                break;
            case L4:
            default:
                baseCapacity = Config.eStorageEnergyCellCapacityL4;
                maxCellDrives = Config.eStorageMaxCellDrivesL4;
                break;
        }

        // Storage capacity = base capacity * number of stackable layers
        // This encourages building taller structures for more storage
        storageCapacity = baseCapacity * Math.max(1, segmentCount);
    }

    // =========================================================================
    // Structure Definition - Layered/Stackable Design (Distillation Tower pattern)
    // =========================================================================

    // Structure piece names
    private static final String STRUCTURE_PIECE_BASE = "base";
    private static final String STRUCTURE_PIECE_LAYER = "layer";
    private static final String STRUCTURE_PIECE_TOP = "top";

    // Shape definitions using [y][z][x] convention
    // Base layer: Controller at (0,0,0), ME channel at (1,0,1), casings elsewhere
    private static final String[][] BASE_SHAPE = new String[][] { { "~C", "CM" } // y=0: z=0: "~C", z=1: "CM"
    };

    // Stackable layer: 2x2 of casings (cell drives, energy cells, vents accepted)
    private static final String[][] LAYER_SHAPE = new String[][] { { "CC", "CC" } // y=0: z=0: "CC", z=1: "CC"
    };

    // Top layer: 2x2 of casings (end cap)
    private static final String[][] TOP_SHAPE = new String[][] { { "CC", "CC" } // y=0: z=0: "CC", z=1: "CC"
    };

    @Override
    public IStructureDefinition<EStorageController> getStructureDefinition() {
        return StructureDefinition.<EStorageController>builder()
            .addShape(STRUCTURE_PIECE_BASE, StructureUtility.transpose(BASE_SHAPE))
            .addShape(STRUCTURE_PIECE_LAYER, StructureUtility.transpose(LAYER_SHAPE))
            .addShape(STRUCTURE_PIECE_TOP, StructureUtility.transpose(TOP_SHAPE))
            .addElement('C', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_CASING))
            .addElement('M', ofBlock(BlockLoader.estorageBlocks, BlockLoader.ESTORAGE_META_ME_CHANNEL))
            .build();
    }

    @Override
    public String[][] getStructurePattern() {
        return BASE_SHAPE;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        // Build base layer
        this.buildPiece(STRUCTURE_PIECE_BASE, stackSize, hintsOnly, 0, 0, 0);
        // Build one stackable layer
        this.buildPiece(STRUCTURE_PIECE_LAYER, stackSize, hintsOnly, 0, 1, 0);
        // Build top layer
        this.buildPiece(STRUCTURE_PIECE_TOP, stackSize, hintsOnly, 0, 2, 0);
    }

    // =========================================================================
    // Structure Validation - checkMachine() using Distillation Tower pattern
    // =========================================================================

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        // Reset component counters for fresh validation
        installedCellDrives = 0;
        installedEnergyCells = 0;
        installedVents = 0;
        segmentCount = 0;

        // Validate base layer
        if (!checkPiece(STRUCTURE_PIECE_BASE, 0, 0, 0)) {
            return false;
        }

        // Scan for stackable layers above the base
        int layerCount = 0;
        while (layerCount < MAX_SEGMENTS) {
            if (!checkPiece(STRUCTURE_PIECE_LAYER, 0, layerCount + 1, 0)) {
                break;
            }
            layerCount++;
        }

        if (layerCount < MIN_SEGMENTS) {
            return false;
        }

        segmentCount = layerCount;

        // Validate top layer (end cap)
        if (!checkPiece(STRUCTURE_PIECE_TOP, 0, segmentCount + 1, 0)) {
            return false;
        }

        // Count components in stackable layers by scanning the world
        countComponents(aBaseMetaTileEntity);

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
     * Count components in the stackable layers by scanning the world.
     */
    private void countComponents(IGregTechTileEntity base) {
        int cx = base.getXCoord();
        int cy = base.getYCoord();
        int cz = base.getZCoord();

        // Scan each stackable layer (y=1 to y=segmentCount)
        for (int layer = 1; layer <= segmentCount; layer++) {
            int currentY = cy + layer;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    Block block = base.getWorld()
                        .getBlock(cx + dx, currentY, cz + dz);
                    int meta = base.getWorld()
                        .getBlockMetadata(cx + dx, currentY, cz + dz);

                    if (block == BlockLoader.estorageBlocks) {
                        if (meta == BlockLoader.ESTORAGE_META_CELL_DRIVE) {
                            installedCellDrives++;
                        } else if (meta == BlockLoader.ESTORAGE_META_ENERGY_CELL) {
                            installedEnergyCells++;
                        } else if (meta == BlockLoader.ESTORAGE_META_VENT) {
                            installedVents++;
                        }
                    }
                }
            }
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
        super.connectToAE2Network();
        registerCellProvider();
    }

    @Override
    protected void disconnectFromAE2Network() {
        unregisterCellProvider();
        super.disconnectFromAE2Network();
    }

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
        unregisterCellProvider();

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

    private void processPeriodicUpdate() {
        if (!ae2Connected) return;
        // Power management is handled by AE2's network power system
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

    public long getStorageCapacity() {
        return storageCapacity * getParallelCount();
    }

    public int getEnergyCellCapacity() {
        return maxCellDrives;
    }

    public int getInstalledCellDrives() {
        return installedCellDrives;
    }

    public int getInstalledEnergyCells() {
        return installedEnergyCells;
    }

    public int getInstalledVents() {
        return installedVents;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    // =========================================================================
    // Cell Management
    // =========================================================================

    public boolean insertCell(ItemStack cellStack) {
        if (cellStack == null) return false;
        if (!(cellStack.getItem() instanceof com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell))
            return false;

        for (int i = 0; i < cellStacks.size(); i++) {
            if (cellStacks.get(i) == null) {
                cellStacks.set(i, cellStack.copy());
                return true;
            }
        }
        return false;
    }

    public ItemStack removeCell(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= cellStacks.size()) return null;
        ItemStack removed = cellStacks.get(slotIndex);
        cellStacks.set(slotIndex, null);
        return removed;
    }

    public List<ItemStack> getCellStacks() {
        return cellStacks;
    }

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
                Config.eStorageMaxCellDrivesL4));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.estorage_tier_l6"),
                formatBytes(Config.eStorageEnergyCellCapacityL6),
                Config.eStorageMaxCellDrivesL6));
        tooltip.add(
            EnumChatFormatting.YELLOW + String.format(
                StatCollector.translateToLocal("ecoaeext.tooltip.estorage_tier_l9"),
                formatBytes(Config.eStorageEnergyCellCapacityL9),
                Config.eStorageMaxCellDrivesL9));
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

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + "B";
    }

}
