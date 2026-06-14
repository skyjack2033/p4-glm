package com.github.GTNewHorizons.ecoaeextension.multiblock;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.util.ECOAETier;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridProxyable;
import appeng.api.networking.IGridStorage;
import appeng.me.helpers.AENetworkProxy;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnergyHatch;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

/**
 * Abstract base class for all ECOAE Extension multiblocks. Provides:
 * - Tier detection from energy hatch voltage (L4/L6/L9)
 * - Common structure checking boilerplate
 * - AE2 grid proxy setup/teardown via {@link AENetworkProxy}
 * - Shared parallel-count calculation based on tier
 * - Common status display text
 * - NBT persistence for AE2 proxy state
 *
 * @param <T> The concrete multiblock class (CRTP pattern)
 */
public abstract class ECOAEExtendedPowerMultiBlockBase<T extends ECOAEExtendedPowerMultiBlockBase<T>>
    extends MTEExtendedPowerMultiBlockBase<T>
    implements IGridProxyable {

    // =========================================================================
    // Constants
    // =========================================================================

    /** NBT tag key for the AE2 proxy data compound */
    private static final String NBT_AE2_PROXY = "ECOAE_AE2_Proxy";

    // =========================================================================
    // Fields
    // =========================================================================

    /** Current tier determined from energy hatches */
    protected ECOAETier currentTier = ECOAETier.L4;

    /** Whether the multiblock is connected to an AE2 network */
    protected boolean ae2Connected = false;

    /** AE2 network proxy managing grid node lifecycle and connection */
    protected AENetworkProxy aeProxy;

    // =========================================================================
    // Constructors
    // =========================================================================

    public ECOAEExtendedPowerMultiBlockBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        initializeAEProxy();
    }

    public ECOAEExtendedPowerMultiBlockBase(String aName) {
        super(aName);
        initializeAEProxy();
    }

    /**
     * Create and configure the AE2 network proxy. Called from constructors so the
     * proxy exists even before the tile entity is placed in the world.
     */
    private void initializeAEProxy() {
        aeProxy = new AENetworkProxy(
            this,               // host: the IGridProxyable implementing class
            "ecoaeproxy",       // unique sub-network key for this machine type
            null                // output stack for security terminal display (null = no security)
        );
        aeProxy.setFlags(); // no special flags (not world-accessible by default)
    }

    // =========================================================================
    // IGridProxyable - Required for AE2 grid node management
    // =========================================================================

    /**
     * Returns this machine's AE2 network proxy. Used by AE2 to access the grid node,
     * set up cable connections, and manage the grid lifecycle.
     */
    @Override
    public AENetworkProxy getProxy() {
        return aeProxy;
    }

    /**
     * Returns the AE2 grid this machine is currently connected to, or null if not
     * connected.
     */
    @Override
    public IGrid getGrid() {
        if (aeProxy == null) return null;
        try {
            return aeProxy.getGrid();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Called by AE2 when this machine's grid storage needs to be saved.
     * We delegate to the proxy's built-in serialization.
     */
    @Override
    public IGridStorage getGridStorage(IGridStorage destination) {
        return aeProxy != null ? aeProxy.getGridStorage(destination) : destination;
    }

    /**
     * Called by AE2 when this machine's grid storage is being loaded.
     */
    @Override
    public void setGridStorage(IGridStorage source) {
        if (aeProxy != null) {
            aeProxy.setGridStorage(source);
        }
    }

    // =========================================================================
    // Lifecycle - AE2 Proxy World Setup and Teardown
    // =========================================================================

    /**
     * Called once when the tile entity first ticks. Sets the world on the AE2 proxy
     * so it can properly register its grid node with AE2's spatial lookup.
     */
    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aeProxy != null) {
            try {
                World world = aBaseMetaTileEntity.getWorld();
                if (world != null) {
                    aeProxy.setWorld(world);
                }
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to set AE2 proxy world on first tick", e);
            }
        }
    }

    /**
     * Called every tick after normal processing. Updates the AE2 proxy to ensure the
     * grid node stays registered and the connection state is current.
     */
    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aeProxy != null && aBaseMetaTileEntity.isServerSide()) {
            try {
                // Ensure the proxy world is set (handles edge cases with chunk loading)
                if (aeProxy.getWorld() == null) {
                    World world = aBaseMetaTileEntity.getWorld();
                    if (world != null) {
                        aeProxy.setWorld(world);
                    }
                }
                aeProxy.update();
                ae2Connected = aeProxy.isPowered() && aeProxy.isActive();
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("AE2 proxy update failed on tick " + aTick, e);
            }
        }
    }

    /**
     * Called when the multiblock controller block is removed from the world.
     * Disconnects from the AE2 grid and invalidates the grid node.
     */
    @Override
    public void onBlockRemoval() {
        super.onBlockRemoval();
        disconnectAEProxy();
    }

    /**
     * Tear down the AE2 proxy connection. Called from onBlockRemoval and from
     * disconnectFromAE2Network.
     */
    protected void disconnectAEProxy() {
        if (aeProxy != null) {
            try {
                aeProxy.invalidate();
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to invalidate AE2 proxy", e);
            }
        }
        ae2Connected = false;
    }

    // =========================================================================
    // NBT Save/Load - AE2 Proxy State Persistence
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (aeProxy != null) {
            try {
                NBTTagCompound proxyTag = new NBTTagCompound();
                aeProxy.writeToNBT(proxyTag);
                aNBT.setTag(NBT_AE2_PROXY, proxyTag);
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to save AE2 proxy NBT data", e);
            }
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aeProxy != null && aNBT.hasKey(NBT_AE2_PROXY)) {
            try {
                NBTTagCompound proxyTag = aNBT.getCompoundTag(NBT_AE2_PROXY);
                aeProxy.readFromNBT(proxyTag);
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to load AE2 proxy NBT data", e);
            }
        }
    }

    // =========================================================================
    // Tier Management
    // =========================================================================

    /**
     * Update the current tier based on the maximum voltage tier of attached energy hatches.
     * Called automatically when the structure forms or energy hatches change.
     */
    protected void updateTier() {
        int maxVoltageTier = getMaxVoltageTier();
        ECOAETier newTier = ECOAETier.fromVoltageTier(maxVoltageTier);
        if (newTier != currentTier) {
            currentTier = newTier;
            onTierChanged(newTier);
        }
    }

    /**
     * Get the maximum voltage tier from all attached energy hatches.
     */
    protected int getMaxVoltageTier() {
        if (mEnergyHatches == null || mEnergyHatches.isEmpty()) return 0;
        int maxTier = 0;
        for (MTEEnergyHatch hatch : mEnergyHatches) {
            if (hatch != null) {
                maxTier = Math.max(maxTier, hatch.mTier);
            }
        }
        return maxTier;
    }

    /**
     * Called when the tier changes. Subclasses can override to update internal state.
     */
    protected void onTierChanged(ECOAETier newTier) {
        // Default implementation does nothing
    }

    /**
     * Get the current tier of this multiblock.
     */
    public ECOAETier getCurrentTier() {
        return currentTier;
    }

    /**
     * Get the parallel count based on the current tier.
     * Subclasses should override to provide tier-specific parallelism.
     */
    public int getParallelCount() {
        return currentTier.parallelMultiplier;
    }

    // =========================================================================
    // Structure Checking - Base Implementation
    // =========================================================================

    /**
     * Base checkMachine that performs structure validation. On successful structure
     * check, calls onStructureFormed() for tier updates and AE2 connection.
     *
     * Subclasses should call {@code super.checkMachine(aBaseMetaTileEntity, aStack)}
     * at the end of their own checkMachine implementations, or use this as a
     * post-validation hook.
     *
     * @return true if the structure is valid (default: delegates to subclass structure definition)
     */
    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        // Subclasses override this to do actual structure checking.
        // When a subclass's check returns true, it should call onStructureFormed().
        return false;
    }

    /**
     * Called when the structure forms successfully. Updates tier and connects to AE2.
     * Subclasses that override checkMachine should call this when the structure is valid.
     */
    protected void onStructureFormed() {
        updateTier();
        connectToAE2Network();
    }

    /**
     * Called when the structure is invalidated. Disconnects from AE2.
     */
    protected void onStructureInvalidated() {
        disconnectFromAE2Network();
    }

    // =========================================================================
    // AE2 Integration Hooks
    // =========================================================================

    /**
     * Connect to the AE2 network via the proxy. Ensures the proxy has a valid world
     * reference and marks the machine as connected.
     */
    protected void connectToAE2Network() {
        if (aeProxy == null) return;
        try {
            IGregTechTileEntity base = getBaseMetaTileEntity();
            if (base != null && base.getWorld() != null) {
                aeProxy.setWorld(base.getWorld());
            }
            aeProxy.update();
            ae2Connected = aeProxy.isActive();
        } catch (Exception e) {
            ECOAEExtension.LOG.debug("Failed to connect to AE2 network", e);
            ae2Connected = false;
        }
    }

    /**
     * Disconnect from the AE2 network. Invalidates the proxy grid node.
     */
    protected void disconnectFromAE2Network() {
        disconnectAEProxy();
    }

    /**
     * Check if the multiblock is connected to an AE2 network.
     */
    public boolean isAE2Connected() {
        return ae2Connected;
    }

    // =========================================================================
    // Structure Definition (abstract - subclasses must provide)
    // =========================================================================

    /**
     * Get the structure definition for this multiblock.
     * Each subclass must provide its own structure definition.
     */
    @Override
    public abstract IStructureDefinition<T> getStructureDefinition();

    /**
     * Get the structure dimension strings for this multiblock.
     * Each subclass must provide its own structure shape.
     */
    public abstract String[][] getStructurePattern();

    // =========================================================================
    // Processing Logic
    // =========================================================================

    /**
     * ECOAE multiblocks don't use standard GT recipe processing.
     * They interact directly with AE2 for storage/crafting/computing.
     * Returns null to indicate no standard processing.
     */
    @Override
    public gregtech.api.logic.ProcessingLogic createProcessingLogic() {
        return null;
    }

    // =========================================================================
    // Display and UI
    // =========================================================================

    @Override
    public String[] getDescription() {
        return new String[] {
            "ECOAE Extension Multiblock",
            "Tier: " + currentTier.name,
            "Parallel: " + getParallelCount() + "x",
            "AE2 Connected: " + (ae2Connected ? "Yes" : "No")
        };
    }

    @Override
    public void addAdditionalTooltipInformation(ItemStack stack, List<String> tooltip) {
        tooltip.add(EnumChatFormatting.AQUA + "ECOAE Extension Multiblock");
        tooltip.add(EnumChatFormatting.GRAY + "Integrates with Applied Energistics 2");
        tooltip.add(EnumChatFormatting.GRAY + "Tier scales with energy hatch voltage");
        tooltip.add(EnumChatFormatting.YELLOW + "L4 (HV) | L6 (IV) | L9 (LuV)");
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        // TODO: Open GUI
        return true;
    }

    // =========================================================================
    // Utility
    // =========================================================================

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true; // ECOAE machines don't have "parts" in the GT sense
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000; // 100% efficiency
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0; // No component damage
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false; // No explosions
    }

    @Override
    public boolean supportsVoidProtection() {
        return false; // ECOAE machines don't output items/fluids in the traditional sense
    }

    @Override
    public boolean supportsBatchMode() {
        return false; // Batch mode doesn't apply to AE2 integration
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false; // No recipe locking for AE2-integrated machines
    }
}
