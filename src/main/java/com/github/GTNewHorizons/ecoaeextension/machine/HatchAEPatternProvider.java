package com.github.GTNewHorizons.ecoaeextension.machine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * AE2 Pattern Provider Hatch - Connects the EFabricator multiblock to AE2 crafting.
 *
 * <p>
 * This hatch acts as an AE2 pattern provider, exposing stored crafting patterns
 * to the ME network. The EFabricator can then receive and process crafting jobs
 * from the AE2 autocrafting system.
 * </p>
 */
public class HatchAEPatternProvider extends MTEHatch implements IGridProxyable, IActionHost, ICraftingProvider {

    private final int machineID;
    private AENetworkProxy gridProxy;
    private boolean additionalConnection = false;
    private boolean active = false;

    /** Pattern items stored in this hatch */
    private final List<ItemStack> patternItems = new ArrayList<>();

    /** Decoded patterns registered with AE2 */
    private final List<ICraftingPatternDetails> storedPatterns = new ArrayList<>();

    /** Machine action source for AE2 security */
    private MachineSource machineSource;

    public HatchAEPatternProvider(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            0,
            0,
            "AE2 Pattern Provider for EFabricator - Stores crafting patterns for AE2 autocrafting",
            TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT));
        this.machineID = aID;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new HatchAEPatternProvider(machineID, mName, mName);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT) };
    }

    // =========================================================================
    // AE2 Grid Integration
    // =========================================================================

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "ecoaePatternProvider", getStackForm(1), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            updateValidGridProxySides();
            if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().getWorld() != null) {
                gridProxy.setOwner(
                    getBaseMetaTileEntity().getWorld()
                        .getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()));
            }
        }
        return gridProxy;
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return getProxy().getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.DENSE;
    }

    @Override
    public DimensionalCoord getLocation() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return null;
        return new DimensionalCoord(base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    @Override
    public void gridChanged() {}

    @Override
    public void securityBreak() {
        // Called when AE2 security breaks this machine
    }

    // =========================================================================
    // ICraftingProvider - Expose patterns to AE2 crafting
    // =========================================================================

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (!active) return;
        for (ICraftingPatternDetails pattern : storedPatterns) {
            if (pattern != null) {
                craftingTracker.addCraftingOption(this, pattern);
            }
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        // This hatch only provides patterns, it doesn't process them.
        // Processing is handled by the EFabricator controller.
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    // =========================================================================
    // Pattern Management
    // =========================================================================

    /**
     * Add a pattern item to this hatch.
     */
    public boolean addPatternItem(ItemStack patternStack, World world) {
        if (patternStack == null) return false;
        if (!(patternStack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem)) return false;

        try {
            ICraftingPatternDetails details = ((appeng.api.implementations.ICraftingPatternItem) patternStack.getItem())
                .getPatternForItem(patternStack, world);
            if (details == null) return false;

            patternItems.add(patternStack.copy());
            storedPatterns.add(details);
            notifyPatternChange();
            return true;
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to decode pattern item in hatch", e);
            return false;
        }
    }

    /**
     * Remove a pattern item at the given index.
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
     * Get the list of pattern items stored in this hatch.
     */
    public List<ItemStack> getPatternItems() {
        return patternItems;
    }

    /**
     * Get the decoded pattern details.
     */
    public List<ICraftingPatternDetails> getStoredPatterns() {
        return storedPatterns;
    }

    /**
     * Refresh stored patterns by re-decoding all pattern items.
     */
    public void refreshPatterns(World world) {
        storedPatterns.clear();
        for (ItemStack stack : patternItems) {
            if (stack != null && stack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem) {
                try {
                    ICraftingPatternDetails details = ((appeng.api.implementations.ICraftingPatternItem) stack
                        .getItem()).getPatternForItem(stack, world);
                    if (details != null) {
                        storedPatterns.add(details);
                    }
                } catch (Exception e) {
                    ECOAEExtension.LOG.debug("Failed to refresh pattern in hatch", e);
                }
            }
        }
        notifyPatternChange();
    }

    /**
     * Notify AE2 that patterns have changed.
     */
    private void notifyPatternChange() {
        if (gridProxy == null) return;
        try {
            IGridNode node = gridProxy.getNode();
            if (node != null) {
                IGrid grid = node.getGrid();
                if (grid != null) {
                    grid.postEvent(new MENetworkCraftingPatternChange(this, node));
                }
            }
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to notify pattern change in hatch", e);
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aBaseMetaTileEntity.isServerSide()) {
            getProxy().onReady();
            machineSource = new MachineSource(this);
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aTick % 20 == 0) {
            try {
                getProxy().onReady();
                active = getProxy().isActive() && getProxy().isPowered();
            } catch (Exception e) {
                active = false;
            }
        }
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (gridProxy != null) {
            gridProxy.invalidate();
        }
        active = false;
    }

    /**
     * Activate this hatch (called when the multiblock forms).
     */
    public void activate() {
        if (active) return;
        active = true;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().getWorld() != null) {
            refreshPatterns(getBaseMetaTileEntity().getWorld());
        }
    }

    /**
     * Deactivate this hatch (called when the multiblock is invalidated).
     */
    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    // =========================================================================
    // Connection Control
    // =========================================================================

    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack tool) {
        additionalConnection = !additionalConnection;
        updateValidGridProxySides();
        aPlayer.addChatComponentMessage(
            new net.minecraft.util.ChatComponentText(
                additionalConnection ? "Additional connections enabled" : "Additional connections disabled"));
        return true;
    }

    private void updateValidGridProxySides() {
        if (additionalConnection) {
            getProxy().setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        } else {
            getProxy().setValidSides(EnumSet.of(getBaseMetaTileEntity().getFrontFacing()));
        }
    }

    // =========================================================================
    // Player Interaction
    // =========================================================================

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) return true;

        ItemStack heldItem = aPlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof appeng.api.implementations.ICraftingPatternItem) {
            if (addPatternItem(heldItem, aBaseMetaTileEntity.getWorld())) {
                aPlayer.inventory.decrStackSize(aPlayer.inventory.currentItem, 1);
                aPlayer.addChatComponentMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GREEN + "Pattern added. Stored: " + patternItems.size()));
                return true;
            } else {
                aPlayer.addChatComponentMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.RED + "Failed to add pattern."));
                return true;
            }
        }

        if (aPlayer.isSneaking() && (heldItem == null)) {
            if (!patternItems.isEmpty()) {
                ItemStack removed = removePatternItem(patternItems.size() - 1);
                if (removed != null) {
                    if (!aPlayer.inventory.addItemStackToInventory(removed)) {
                        aPlayer.dropPlayerItemWithRandomChoice(removed, false);
                    }
                    aPlayer.addChatComponentMessage(
                        new net.minecraft.util.ChatComponentText(
                            net.minecraft.util.EnumChatFormatting.YELLOW + "Pattern removed. Stored: "
                                + patternItems.size()));
                    return true;
                }
            }
            aPlayer.addChatComponentMessage(
                new net.minecraft.util.ChatComponentText(
                    net.minecraft.util.EnumChatFormatting.RED + "No patterns to remove."));
            return true;
        }

        return true;
    }

    // =========================================================================
    // NBT
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("additionalConnection", additionalConnection);
        aNBT.setBoolean("active", active);

        if (gridProxy != null) {
            gridProxy.writeToNBT(aNBT);
        }

        NBTTagList patternList = new NBTTagList();
        for (ItemStack stack : patternItems) {
            if (stack != null) {
                NBTTagCompound stackTag = new NBTTagCompound();
                stack.writeToNBT(stackTag);
                patternList.appendTag(stackTag);
            }
        }
        aNBT.setTag("ecoae_patterns", patternList);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        additionalConnection = aNBT.getBoolean("additionalConnection");
        active = aNBT.getBoolean("active");

        if (gridProxy != null) {
            gridProxy.readFromNBT(aNBT);
        }

        patternItems.clear();
        storedPatterns.clear();
        if (aNBT.hasKey("ecoae_patterns")) {
            NBTTagList patternList = aNBT.getTagList("ecoae_patterns", 10);
            for (int i = 0; i < patternList.tagCount(); i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(patternList.getCompoundTagAt(i));
                if (stack != null) {
                    patternItems.add(stack);
                }
            }
        }
    }

    // =========================================================================
    // Display
    // =========================================================================

    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    /**
     * Check if this hatch is connected to an active AE2 network.
     */
    public boolean isAE2Connected() {
        return active;
    }
}
