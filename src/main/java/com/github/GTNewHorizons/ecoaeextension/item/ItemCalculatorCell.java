package com.github.GTNewHorizons.ecoaeextension.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemCalculatorCell extends ItemECOAEBase {

    private final long storageBytes;

    public ItemCalculatorCell(String unlocalizedName, long storageBytes) {
        super(unlocalizedName);
        this.storageBytes = storageBytes;
        setMaxStackSize(1);
    }

    public long getStorageBytes() {
        return storageBytes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add("§7Crafting Storage: §a" + formatBytes(storageBytes));
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + " bytes";
    }
}
