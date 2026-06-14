package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

import cpw.mods.fml.common.network.IGuiHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class ECOAEGuiHandler implements IGuiHandler {

    public static final int GUI_ID_ESTORAGE = 0;
    public static final int GUI_ID_ECALCULATOR = 1;
    public static final int GUI_ID_EFABRICATOR = 2;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);

        if (!(tileEntity instanceof IGregTechTileEntity)) {
            return null;
        }

        IGregTechTileEntity gte = (IGregTechTileEntity) tileEntity;
        IMetaTileEntity mte = gte.getMetaTileEntity();

        switch (ID) {
            case GUI_ID_ESTORAGE:
                if (mte instanceof EStorageController) {
                    return new ContainerECOAE(player.inventory);
                }
                return null;
            case GUI_ID_ECALCULATOR:
                if (mte instanceof ECalculatorController) {
                    return new ContainerECOAE(player.inventory);
                }
                return null;
            case GUI_ID_EFABRICATOR:
                if (mte instanceof EFabricatorController) {
                    return new ContainerECOAE(player.inventory);
                }
                return null;
            default:
                return null;
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);

        if (!(tileEntity instanceof IGregTechTileEntity)) {
            return null;
        }

        IGregTechTileEntity gte = (IGregTechTileEntity) tileEntity;
        IMetaTileEntity mte = gte.getMetaTileEntity();

        switch (ID) {
            case GUI_ID_ESTORAGE:
                if (mte instanceof EStorageController) {
                    return new GUIEStorageController(player.inventory, (EStorageController) mte);
                }
                return null;
            case GUI_ID_ECALCULATOR:
                if (mte instanceof ECalculatorController) {
                    return new GUIECalculatorController(player.inventory, (ECalculatorController) mte);
                }
                return null;
            case GUI_ID_EFABRICATOR:
                if (mte instanceof EFabricatorController) {
                    return new GUIEFabricatorController(player.inventory, (EFabricatorController) mte);
                }
                return null;
            default:
                return null;
        }
    }
}
