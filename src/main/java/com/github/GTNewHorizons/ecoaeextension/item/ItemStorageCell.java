package com.github.GTNewHorizons.ecoaeextension.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemStorageCell extends ItemECOAEBase {

    private final long capacityBytes;
    private final int tierLevel; // 1=A, 2=B, 3=C

    public ItemStorageCell(String unlocalizedName, long capacityBytes, int tierLevel) {
        super(unlocalizedName);
        this.capacityBytes = capacityBytes;
        this.tierLevel = tierLevel;
        setMaxStackSize(1);
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add("§7Capacity: §a" + formatBytes(capacityBytes));
        tooltip.add("§7Tier Level: §e" + (tierLevel == 1 ? "A" : tierLevel == 2 ? "B" : "C"));
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return (bytes / 1_000_000_000) + "B";
        if (bytes >= 1_000_000) return (bytes / 1_000_000) + "M";
        if (bytes >= 1_000) return (bytes / 1_000) + "K";
        return bytes + " bytes";
    }
}
