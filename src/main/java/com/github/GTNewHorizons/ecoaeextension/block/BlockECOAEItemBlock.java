package com.github.GTNewHorizons.ecoaeextension.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * ItemBlock for ECOAE meta blocks. Provides proper sub-item names.
 */
public class BlockECOAEItemBlock extends ItemBlock {

    private final BlockECOAEMeta metaBlock;

    public BlockECOAEItemBlock(Block block) {
        super(block);
        this.metaBlock = (BlockECOAEMeta) block;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + metaBlock.getSubName(stack.getItemDamage());
    }
}
