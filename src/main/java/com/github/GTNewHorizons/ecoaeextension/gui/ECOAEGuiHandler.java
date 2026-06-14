package com.github.GTNewHorizons.ecoaeextension.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.github.GTNewHorizons.ecoaeextension.multiblock.ecalculator.ECalculatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.efabricator.EFabricatorController;
import com.github.GTNewHorizons.ecoaeextension.multiblock.estorage.EStorageController;

import cpw.mods.fml.common.network.IGuiHandler;

public class ECOAEGuiHandler implements IGuiHandler {

    public static final int GUI_ID_ESTORAGE = 0;
    public static final int GUI_ID_ECALCULATOR = 1;
    public static final int GUI_ID_EFABRICATOR = 2;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);

        switch (ID) {
            case GUI_ID_ESTORAGE:
                if (tileEntity instanceof EStorageController) {
                    return new ContainerECOAE(player.inventory);
                }
                return null;
            case GUI_ID_ECALCULATOR:
                if (tileEntity instanceof ECalculatorController) {
                    return new ContainerECOAE(player.inventory);
                }
                return null;
            case GUI_ID_EFABRICATOR:
                if (tileEntity instanceof EFabricatorController) {
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

        switch (ID) {
            case GUI_ID_ESTORAGE:
                if (tileEntity instanceof EStorageController) {
                    return new GUIEStorageController(player.inventory, (EStorageController) tileEntity);
                }
                return null;
            case GUI_ID_ECALCULATOR:
                if (tileEntity instanceof ECalculatorController) {
                    return new GUIECalculatorController(player.inventory, (ECalculatorController) tileEntity);
                }
                return null;
            case GUI_ID_EFABRICATOR:
                if (tileEntity instanceof EFabricatorController) {
                    return new GUIEFabricatorController(player.inventory, (EFabricatorController) tileEntity);
                }
                return null;
            default:
                return null;
        }
    }
}
