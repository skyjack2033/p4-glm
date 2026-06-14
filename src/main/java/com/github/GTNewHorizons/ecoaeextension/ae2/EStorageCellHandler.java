package com.github.GTNewHorizons.ecoaeextension.ae2;

import net.minecraft.util.IIcon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell;

import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.implementations.tiles.IChestOrDrive;

public class EStorageCellHandler implements ICellHandler {

    public static final EStorageCellHandler INSTANCE = new EStorageCellHandler();

    @Override
    public boolean isCell(ItemStack cell) {
        return cell != null && cell.getItem() instanceof ItemStorageCell;
    }

    @Override
    public IMEInventoryHandler getCellInventory(ItemStack cell, ISaveProvider saveProvider, StorageChannel channel) {
        if (!isCell(cell)) return null;
        // TODO: Implement custom cell inventory for ECOAE storage cells
        return null;
    }

    @Override
    public IIcon getTopTexture_Light() {
        return null;
    }

    @Override
    public IIcon getTopTexture_Medium() {
        return null;
    }

    @Override
    public IIcon getTopTexture_Dark() {
        return null;
    }

    @Override
    public void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler handler,
            IMEInventoryHandler inv, ItemStack cell, StorageChannel channel) {
        // TODO: Implement custom GUI for ECOAE storage cells
    }

    @Override
    public int getStatusForCell(ItemStack cell, IMEInventory handler) {
        if (!isCell(cell)) return 0;
        // TODO: Check actual cell contents
        // 0 = empty, 1 = has items, 2 = nearly full, 3 = full
        return 0;
    }

    @Override
    public double cellIdleDrain(ItemStack cell, IMEInventory handler) {
        if (!isCell(cell)) return 0;
        // Base idle drain for ECOAE cells
        return 1.0;
    }
}
