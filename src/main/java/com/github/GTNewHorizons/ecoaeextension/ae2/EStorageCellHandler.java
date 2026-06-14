package com.github.GTNewHorizons.ecoaeextension.ae2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell;

import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.implementations.tiles.IChestOrDrive;

/**
 * Cell handler for ECOAE storage cells.
 *
 * <p>Implements the AE2 {@link ICellHandler} interface to provide custom cell inventories for
 * ECOAE storage cells. Each cell stores items in an NBT-backed flat array via
 * {@link EStorageCellInventory}.
 */
public class EStorageCellHandler implements ICellHandler {

    public static final EStorageCellHandler INSTANCE = new EStorageCellHandler();

    @Override
    public boolean isCell(ItemStack cell) {
        return cell != null && cell.getItem() instanceof ItemStorageCell;
    }

    @Override
    public IMEInventoryHandler getCellInventory(ItemStack cell, ISaveProvider saveProvider, StorageChannel channel) {
        if (!isCell(cell)) return null;
        if (channel != StorageChannel.ITEMS) return null;

        EStorageCellInventory cellInventory = new EStorageCellInventory(cell, saveProvider);
        return cellInventory.createHandler();
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
        // ECOAE cells use the standard terminal GUI; no custom chest GUI needed.
    }

    @Override
    public int getStatusForCell(ItemStack cell, IMEInventory handler) {
        if (!isCell(cell)) return 0;
        if (handler instanceof EStorageCellInventory) {
            return ((EStorageCellInventory) handler).getStatusForCell();
        }
        if (handler instanceof EStorageCellInventory.EStorageCellInventoryHandler) {
            IMEInventory<appeng.api.storage.data.IAEItemStack> internal =
                ((EStorageCellInventory.EStorageCellInventoryHandler) handler).getInternal();
            if (internal instanceof EStorageCellInventory) {
                return ((EStorageCellInventory) internal).getStatusForCell();
            }
        }
        return 0;
    }

    @Override
    public double cellIdleDrain(ItemStack cell, IMEInventory handler) {
        if (!isCell(cell)) return 0;
        return 1.0;
    }
}
