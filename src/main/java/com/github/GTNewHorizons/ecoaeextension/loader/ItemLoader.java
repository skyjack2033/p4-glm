package com.github.GTNewHorizons.ecoaeextension.loader;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.item.ItemCalculatorCell;
import com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.IItemContainer;

/**
 * Central item registry for ECOAE Extension. Uses the IItemContainer enum pattern standard in GTNH.
 */
public class ItemLoader {

    public static Item itemStorageCell16M;
    public static Item itemStorageCell64M;
    public static Item itemStorageCell256M;

    public static Item itemCalculatorCell64M;
    public static Item itemCalculatorCell1024M;
    public static Item itemCalculatorCell16384M;

    public static void registerItems() {
        ECOAEExtension.LOG.info("Registering ECOAE Extension items...");

        itemStorageCell16M = new ItemStorageCell("storage_cell_16m", 16_000_000L, 1);
        GameRegistry.registerItem(itemStorageCell16M, "storage_cell_16m");
        ECOAEItemList.STORAGE_CELL_16M.set(new ItemStack(itemStorageCell16M));

        itemStorageCell64M = new ItemStorageCell("storage_cell_64m", 64_000_000L, 2);
        GameRegistry.registerItem(itemStorageCell64M, "storage_cell_64m");
        ECOAEItemList.STORAGE_CELL_64M.set(new ItemStack(itemStorageCell64M));

        itemStorageCell256M = new ItemStorageCell("storage_cell_256m", 256_000_000L, 3);
        GameRegistry.registerItem(itemStorageCell256M, "storage_cell_256m");
        ECOAEItemList.STORAGE_CELL_256M.set(new ItemStack(itemStorageCell256M));

        itemCalculatorCell64M = new ItemCalculatorCell("calculator_cell_64m", 64_000_000L);
        GameRegistry.registerItem(itemCalculatorCell64M, "calculator_cell_64m");
        ECOAEItemList.CALCULATOR_CELL_64M.set(new ItemStack(itemCalculatorCell64M));

        itemCalculatorCell1024M = new ItemCalculatorCell("calculator_cell_1024m", 1_024_000_000L);
        GameRegistry.registerItem(itemCalculatorCell1024M, "calculator_cell_1024m");
        ECOAEItemList.CALCULATOR_CELL_1024M.set(new ItemStack(itemCalculatorCell1024M));

        itemCalculatorCell16384M = new ItemCalculatorCell("calculator_cell_16384m", 16_384_000_000L);
        GameRegistry.registerItem(itemCalculatorCell16384M, "calculator_cell_16384m");
        ECOAEItemList.CALCULATOR_CELL_16384M.set(new ItemStack(itemCalculatorCell16384M));

        ECOAEExtension.LOG.info("ECOAE Extension items registered.");
    }

    public static void registerItemRenderers() {
        // TODO: Register item renderers when items are implemented
    }

    /**
     * IItemContainer enum for centralized item access. Follows GTNH pattern.
     */
    public enum ECOAEItemList implements IItemContainer {

        STORAGE_CELL_16M,
        STORAGE_CELL_64M,
        STORAGE_CELL_256M,
        CALCULATOR_CELL_64M,
        CALCULATOR_CELL_1024M,
        CALCULATOR_CELL_16384M,
        COMPONENT_CIRCUIT_L4,
        COMPONENT_CIRCUIT_L6,
        COMPONENT_CIRCUIT_L9;

        private ItemStack mStack;
        private boolean mHasNotBeenSet = true;

        @Override
        public IItemContainer set(ItemStack aStack) {
            if (aStack != null) {
                mStack = aStack.copy();
                mHasNotBeenSet = false;
            }
            return this;
        }

        @Override
        public IItemContainer set(Item aItem) {
            if (aItem != null) {
                mStack = new ItemStack(aItem);
                mHasNotBeenSet = false;
            }
            return this;
        }

        @Override
        public ItemStack get(long aAmount, Object... aReplacements) {
            if (mStack == null) return null;
            ItemStack stack = mStack.copy();
            stack.stackSize = (int) aAmount;
            return stack;
        }

        @Override
        public ItemStack getWildcard(long aAmount, Object... aReplacements) {
            return get(aAmount, aReplacements);
        }

        @Override
        public ItemStack getUndamaged(long aAmount, Object... aReplacements) {
            return get(aAmount, aReplacements);
        }

        @Override
        public ItemStack getAlmostBroken(long aAmount, Object... aReplacements) {
            return get(aAmount, aReplacements);
        }

        @Override
        public ItemStack getWithName(long aAmount, String aDisplayName, Object... aReplacements) {
            ItemStack stack = get(aAmount, aReplacements);
            if (stack != null) stack.setStackDisplayName(aDisplayName);
            return stack;
        }

        @Override
        public ItemStack getWithCharge(long aAmount, int aCharged, Object... aReplacements) {
            return get(aAmount, aReplacements);
        }

        @Override
        public ItemStack getWithDamage(long aAmount, long aDamageValue, Object... aReplacements) {
            ItemStack stack = get(aAmount, aReplacements);
            if (stack != null) stack.setItemDamage((int) aDamageValue);
            return stack;
        }

        @Override
        public boolean hasBeenSet() {
            return !mHasNotBeenSet;
        }

        @Override
        public Item getItem() {
            return mStack != null ? mStack.getItem() : null;
        }

        @Override
        public net.minecraft.block.Block getBlock() {
            return null;
        }

        @Override
        public boolean isStackEqual(Object aStack) {
            return isStackEqual(aStack, false, false);
        }

        @Override
        public boolean isStackEqual(Object aStack, boolean aWildcard, boolean aIgnoreNBT) {
            if (mStack == null || aStack == null) return false;
            if (!(aStack instanceof ItemStack)) return false;
            ItemStack other = (ItemStack) aStack;
            if (aIgnoreNBT) {
                return mStack.getItem() == other.getItem()
                    && (aWildcard || mStack.getItemDamage() == other.getItemDamage());
            }
            return ItemStack.areItemStacksEqual(mStack, other);
        }

        @Override
        public IItemContainer registerOre(Object... aOreNames) {
            return this;
        }

        public IItemContainer registerUnificationEntry(Object... aOreNames) {
            return this;
        }

        @Override
        public IItemContainer registerWildcardAsOre(Object... aOreNames) {
            return this;
        }
    }
}
