package com.github.GTNewHorizons.ecoaeextension.machine;

import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.loader.ItemLoader;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
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
 * AE2 Storage Bus Hatch - Connects the EStorage multiblock to an AE2 network.
 *
 * <p>
 * This hatch acts as an AE2 storage bus, allowing the EStorage controller's
 * cell drives to be exposed to the ME network.
 * </p>
 */
public class HatchAEStorageBus extends MTEHatch implements IGridProxyable, IActionHost {

    private final int machineID;
    private AENetworkProxy gridProxy;
    private boolean additionalConnection = false;

    public HatchAEStorageBus(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            0,
            0,
            "AE2 Storage Bus for EStorage - Connects cell drives to ME network",
            TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN));
        this.machineID = aID;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new HatchAEStorageBus(machineID, mName, mName);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN) };
    }

    // =========================================================================
    // AE2 Grid Integration
    // =========================================================================

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(
                this,
                "ecoaeStorageBus",
                ItemLoader.ECOAEItemList.STORAGE_CELL_16M.get(1),
                true);
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
    // Lifecycle
    // =========================================================================

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aBaseMetaTileEntity.isServerSide()) {
            getProxy().onReady();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aTick % 20 == 0) {
            try {
                getProxy().onReady();
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("AE2 proxy update failed in storage bus hatch", e);
            }
        }
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (gridProxy != null) {
            gridProxy.invalidate();
        }
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
    // NBT
    // =========================================================================

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("additionalConnection", additionalConnection);
        if (gridProxy != null) {
            gridProxy.writeToNBT(aNBT);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        additionalConnection = aNBT.getBoolean("additionalConnection");
        if (gridProxy != null) {
            gridProxy.readFromNBT(aNBT);
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
        try {
            return getProxy().isActive() && getProxy().isPowered();
        } catch (Exception e) {
            return false;
        }
    }
}
