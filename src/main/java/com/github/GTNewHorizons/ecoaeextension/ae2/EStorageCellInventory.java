package com.github.GTNewHorizons.ecoaeextension.ae2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.GTNewHorizons.ecoaeextension.ECOAEExtension;
import com.github.GTNewHorizons.ecoaeextension.item.ItemStorageCell;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

/**
 * NBT-backed cell inventory for ECOAE storage cells.
 *
 * <p>
 * Implements {@link ICellInventory} (which extends {@code IMEInventory<IAEItemStack>}) to
 * provide AE2 cell storage backed by a flat NBT tag list on the cell ItemStack. Each distinct
 * item+damage+nbt combination counts as one "type", consuming {@link #BYTES_PER_TYPE} bytes of
 * overhead per type.
 *
 * <p>
 * NBT format:
 * 
 * <pre>
 *   ecoae_items: TAG_List of TAG_Compound
 *     - id: TAG_String (item registry name)
 *     - dmg: TAG_Short (damage value)
 *     - cnt: TAG_Long (quantity)
 *     - nbt: TAG_Compound (optional, item NBT)
 *   ecoae_usedBytes: TAG_Long
 *   ecoae_usedTypes: TAG_Int
 *   ecoae_itemCount: TAG_Long
 * </pre>
 */
public class EStorageCellInventory implements ICellInventory {

    // =========================================================================
    // Constants
    // =========================================================================

    /** Number of bytes consumed per item stored. */
    private static final int BYTES_PER_ITEM = 1;

    /** Number of bytes consumed per distinct item type (metadata overhead). */
    private static final int BYTES_PER_TYPE = 8;

    /** Maximum number of distinct item types a cell can hold. */
    private static final int MAX_ITEM_TYPES = 63;

    /** NBT tag keys for persistence. */
    private static final String NBT_ITEMS = "ecoae_items";
    private static final String NBT_ITEM_ID = "id";
    private static final String NBT_ITEM_DAMAGE = "dmg";
    private static final String NBT_ITEM_COUNT = "cnt";
    private static final String NBT_ITEM_NBT = "nbt";
    private static final String NBT_USED_BYTES = "ecoae_usedBytes";
    private static final String NBT_USED_TYPES = "ecoae_usedTypes";
    private static final String NBT_ITEM_COUNT_TOTAL = "ecoae_itemCount";

    // =========================================================================
    // Fields
    // =========================================================================

    /** The ItemStack that represents this cell. */
    private final ItemStack cellItemStack;

    /** Save provider callback, invoked when data changes. */
    private final ISaveProvider saveProvider;

    /** Map of stored item stacks (as keys with stackSize=1) to their quantities. */
    private final HashMap<IAEItemStack, Long> storedItems = new HashMap<>();

    /** Cached total number of items stored. */
    private long storedItemCount;

    /** Cached number of distinct item types stored. */
    private int storedItemTypes;

    /** Cached number of bytes used. */
    private long usedBytes;

    /** Cached total byte capacity of this cell. */
    private final long totalBytes;

    /** Whether the inventory has been loaded from NBT. */
    private boolean isLoaded;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Create a new EStorageCellInventory.
     *
     * @param cellItemStack The ItemStack representing the storage cell
     * @param saveProvider  Callback to notify when data changes and needs saving
     */
    public EStorageCellInventory(ItemStack cellItemStack, ISaveProvider saveProvider) {
        this.cellItemStack = cellItemStack;
        this.saveProvider = saveProvider;

        if (cellItemStack.getItem() instanceof ItemStorageCell) {
            this.totalBytes = ((ItemStorageCell) cellItemStack.getItem()).getCapacityBytes();
        } else {
            this.totalBytes = 0;
        }

        this.isLoaded = false;
        this.storedItemCount = 0;
        this.storedItemTypes = 0;
        this.usedBytes = 0;
    }

    // =========================================================================
    // ICellInventory - Cell Metadata
    // =========================================================================

    @Override
    public ItemStack getItemStack() {
        return cellItemStack;
    }

    @Override
    public double getIdleDrain() {
        return 1.0;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public IInventory getConfigInventory() {
        return null;
    }

    @Override
    public IInventory getUpgradesInventory() {
        return null;
    }

    @Override
    public int getBytesPerType() {
        return BYTES_PER_TYPE;
    }

    @Override
    public boolean canHoldNewItem() {
        ensureLoaded();
        return storedItemTypes < MAX_ITEM_TYPES && (totalBytes - usedBytes) >= BYTES_PER_TYPE;
    }

    @Override
    public long getTotalBytes() {
        return totalBytes;
    }

    @Override
    public long getFreeBytes() {
        ensureLoaded();
        return Math.max(0, totalBytes - usedBytes);
    }

    @Override
    public long getUsedBytes() {
        ensureLoaded();
        return usedBytes;
    }

    @Override
    public long getTotalItemTypes() {
        return MAX_ITEM_TYPES;
    }

    @Override
    public long getStoredItemCount() {
        ensureLoaded();
        return storedItemCount;
    }

    @Override
    public long getStoredItemTypes() {
        ensureLoaded();
        return storedItemTypes;
    }

    @Override
    public long getRemainingItemTypes() {
        ensureLoaded();
        return Math.max(0, MAX_ITEM_TYPES - storedItemTypes);
    }

    @Override
    public long getRemainingItemCount() {
        ensureLoaded();
        long remainingBytes = totalBytes - usedBytes;
        if (remainingBytes < BYTES_PER_ITEM) return 0;
        return remainingBytes / BYTES_PER_ITEM;
    }

    @Override
    public long getRemainingItemsCountDist(IAEStack stack) {
        ensureLoaded();
        if (stack == null) return getRemainingItemCount();

        @SuppressWarnings("unchecked")
        IAEItemStack existingKey = findMatchingKey((IAEItemStack) stack);
        if (existingKey != null) {
            long remainingBytes = totalBytes - usedBytes;
            if (remainingBytes < BYTES_PER_ITEM) return 0;
            return remainingBytes / BYTES_PER_ITEM;
        } else {
            long remainingBytes = totalBytes - usedBytes;
            if (remainingBytes < BYTES_PER_TYPE + BYTES_PER_ITEM) return 0;
            if (storedItemTypes >= MAX_ITEM_TYPES) return 0;
            return (remainingBytes - BYTES_PER_TYPE) / BYTES_PER_ITEM;
        }
    }

    @Override
    public int getUnusedItemCount() {
        ensureLoaded();
        long remaining = getRemainingItemCount();
        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    @Override
    public int getStatusForCell() {
        ensureLoaded();
        if (storedItemCount == 0) return 0; // empty
        if (usedBytes >= totalBytes) return 3; // full
        if (usedBytes > totalBytes * 0.75) return 2; // nearly full
        return 1; // has items
    }

    @Override
    public String getOreFilter() {
        return "";
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - injectItems
    // =========================================================================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IAEItemStack injectItems(IAEStack input, Actionable actionable, BaseActionSource source) {
        IAEItemStack inputStack = (IAEItemStack) input;
        if (inputStack == null || inputStack.getStackSize() <= 0) return null;

        ensureLoaded();

        long requestedAmount = inputStack.getStackSize();

        // Find existing entry for this item
        IAEItemStack existingKey = findMatchingKey(inputStack);

        if (existingKey != null) {
            // Item type already exists in cell
            long currentAmount = storedItems.get(existingKey);

            // Calculate how many we can fit
            long availableBytes = totalBytes - usedBytes;
            long maxCanAdd = availableBytes / BYTES_PER_ITEM;
            long toInsert = Math.min(requestedAmount, maxCanAdd);

            if (toInsert <= 0) {
                return inputStack.copy();
            }

            if (actionable == Actionable.MODULATE) {
                storedItems.put(existingKey, currentAmount + toInsert);
                storedItemCount += toInsert;
                usedBytes += toInsert * BYTES_PER_ITEM;
                saveChanges();
            }

            if (toInsert < requestedAmount) {
                IAEItemStack remainder = inputStack.copy();
                remainder.setStackSize(requestedAmount - toInsert);
                return remainder;
            }

            return null; // All items inserted
        } else {
            // New item type
            if (storedItemTypes >= MAX_ITEM_TYPES) {
                return inputStack.copy();
            }

            long availableBytes = totalBytes - usedBytes;
            long bytesForItems = availableBytes - BYTES_PER_TYPE;

            if (bytesForItems < BYTES_PER_ITEM) {
                return inputStack.copy();
            }

            long maxCanAdd = bytesForItems / BYTES_PER_ITEM;
            long toInsert = Math.min(requestedAmount, maxCanAdd);

            if (toInsert <= 0) {
                return inputStack.copy();
            }

            if (actionable == Actionable.MODULATE) {
                IAEItemStack key = inputStack.copy();
                key.setStackSize(1); // Key stored with stackSize=1
                storedItems.put(key, toInsert);
                storedItemTypes++;
                storedItemCount += toInsert;
                usedBytes += BYTES_PER_TYPE + toInsert * BYTES_PER_ITEM;
                saveChanges();
            }

            if (toInsert < requestedAmount) {
                IAEItemStack remainder = inputStack.copy();
                remainder.setStackSize(requestedAmount - toInsert);
                return remainder;
            }

            return null; // All items inserted
        }
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - extractItems
    // =========================================================================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IAEItemStack extractItems(IAEStack request, Actionable actionable, BaseActionSource source) {
        IAEItemStack requestStack = (IAEItemStack) request;
        if (requestStack == null || requestStack.getStackSize() <= 0) return null;

        ensureLoaded();

        IAEItemStack existingKey = findMatchingKey(requestStack);
        if (existingKey == null) return null;

        long currentAmount = storedItems.get(existingKey);
        long toExtract = Math.min(requestStack.getStackSize(), currentAmount);

        if (toExtract <= 0) return null;

        if (actionable == Actionable.MODULATE) {
            long newAmount = currentAmount - toExtract;
            if (newAmount <= 0) {
                // Remove the type entirely
                storedItems.remove(existingKey);
                storedItemTypes--;
                usedBytes -= BYTES_PER_TYPE;
            } else {
                storedItems.put(existingKey, newAmount);
            }
            storedItemCount -= toExtract;
            usedBytes -= toExtract * BYTES_PER_ITEM;
            if (usedBytes < 0) usedBytes = 0;
            saveChanges();
        }

        IAEItemStack result = existingKey.copy();
        result.setStackSize(toExtract);
        return result;
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - getAvailableItems
    // =========================================================================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IItemList getAvailableItems(IItemList out) {
        ensureLoaded();

        for (Map.Entry<IAEItemStack, Long> entry : storedItems.entrySet()) {
            IAEItemStack stack = entry.getKey()
                .copy();
            stack.setStackSize(entry.getValue());
            out.add(stack);
        }

        return out;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IItemList getAvailableItems(IItemList out, int partitionIndex) {
        return getAvailableItems(out);
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - getAvailableItem
    // =========================================================================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IAEItemStack getAvailableItem(IAEStack request) {
        return getAvailableItem(request, 0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IAEItemStack getAvailableItem(IAEStack request, int partitionIndex) {
        if (request == null) return null;

        ensureLoaded();

        IAEItemStack existingKey = findMatchingKey((IAEItemStack) request);
        if (existingKey == null) return null;

        long amount = storedItems.get(existingKey);
        IAEItemStack result = existingKey.copy();
        result.setStackSize(amount);
        return result;
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - getSortedFuzzyItems
    // =========================================================================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Collection getSortedFuzzyItems(Collection out, IAEStack filter, FuzzyMode fuzzyMode, int maxResults) {
        ensureLoaded();

        int count = 0;
        for (Map.Entry<IAEItemStack, Long> entry : storedItems.entrySet()) {
            if (maxResults > 0 && count >= maxResults) break;
            IAEItemStack key = entry.getKey();
            if (filter == null || key.isSameType((IAEItemStack) filter)) {
                IAEItemStack stack = key.copy();
                stack.setStackSize(entry.getValue());
                out.add(stack);
                count++;
            }
        }

        return out;
    }

    // =========================================================================
    // IMEInventory<IAEItemStack> - getChannel
    // =========================================================================

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    // =========================================================================
    // Static factory - create a handler wrapper for AE2
    // =========================================================================

    /**
     * Create an {@link IMEInventoryHandler} wrapping this cell inventory. This is used by
     * {@link EStorageCellHandler#getCellInventory} to produce the handler that AE2 expects.
     */
    public IMEInventoryHandler<IAEItemStack> createHandler() {
        return new EStorageCellInventoryHandler(this);
    }

    // =========================================================================
    // NBT Persistence
    // =========================================================================

    /**
     * Load stored items from the cell ItemStack's NBT data. Called lazily on first access.
     */
    private void ensureLoaded() {
        if (isLoaded) return;
        isLoaded = true;

        storedItems.clear();
        storedItemCount = 0;
        storedItemTypes = 0;
        usedBytes = 0;

        NBTTagCompound nbt = cellItemStack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_ITEMS)) return;

        NBTTagList itemList = nbt.getTagList(NBT_ITEMS, 10); // 10 = TAG_Compound
        if (itemList.tagCount() == 0) return;

        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound itemTag = itemList.getCompoundTagAt(i);
            try {
                loadItemEntry(itemTag);
            } catch (Exception e) {
                ECOAEExtension.LOG.debug("Failed to load cell inventory entry at index {}", i, e);
            }
        }

        // Recalculate to ensure consistency after loading
        recalculate();
    }

    /**
     * Load a single item entry from NBT into the storedItems map.
     */
    private void loadItemEntry(NBTTagCompound tag) {
        String itemId = tag.getString(NBT_ITEM_ID);
        int damage = tag.getShort(NBT_ITEM_DAMAGE);
        long count = tag.getLong(NBT_ITEM_COUNT);

        if (itemId == null || itemId.isEmpty() || count <= 0) return;

        Item item = (Item) Item.itemRegistry.getObject(itemId);
        if (item == null) return;

        ItemStack vanillaStack = new ItemStack(item, 1, damage);
        if (tag.hasKey(NBT_ITEM_NBT)) {
            vanillaStack.setTagCompound(tag.getCompoundTag(NBT_ITEM_NBT));
        }

        IAEItemStack aeStack = AEApi.instance()
            .storage()
            .createItemStack(vanillaStack);
        if (aeStack == null) return;

        aeStack.setStackSize(1); // Use stackSize=1 as map key
        storedItems.put(aeStack, count);
    }

    /**
     * Save all stored items to the cell ItemStack's NBT data.
     */
    private void saveToNBT() {
        NBTTagCompound nbt = cellItemStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            cellItemStack.setTagCompound(nbt);
        }

        NBTTagList itemList = new NBTTagList();
        for (Map.Entry<IAEItemStack, Long> entry : storedItems.entrySet()) {
            IAEItemStack aeStack = entry.getKey();
            long count = entry.getValue();

            NBTTagCompound itemTag = new NBTTagCompound();
            ItemStack vanillaStack = aeStack.getItemStack();
            if (vanillaStack == null) continue;

            itemTag.setString(NBT_ITEM_ID, Item.itemRegistry.getNameForObject(vanillaStack.getItem()));
            itemTag.setShort(NBT_ITEM_DAMAGE, (short) vanillaStack.getItemDamage());
            itemTag.setLong(NBT_ITEM_COUNT, count);

            if (vanillaStack.getTagCompound() != null) {
                itemTag.setTag(
                    NBT_ITEM_NBT,
                    vanillaStack.getTagCompound()
                        .copy());
            }

            itemList.appendTag(itemTag);
        }

        nbt.setTag(NBT_ITEMS, itemList);
        nbt.setLong(NBT_USED_BYTES, usedBytes);
        nbt.setInteger(NBT_USED_TYPES, storedItemTypes);
        nbt.setLong(NBT_ITEM_COUNT_TOTAL, storedItemCount);
    }

    /**
     * Recalculate cached values from the actual stored data. Called after loading to ensure
     * consistency between the cached values and the actual map contents.
     */
    private void recalculate() {
        storedItemCount = 0;
        storedItemTypes = storedItems.size();
        usedBytes = 0;

        for (Map.Entry<IAEItemStack, Long> entry : storedItems.entrySet()) {
            long count = entry.getValue();
            storedItemCount += count;
            usedBytes += BYTES_PER_TYPE + count * BYTES_PER_ITEM;
        }
    }

    /**
     * Save changes to NBT and notify the save provider.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void saveChanges() {
        saveToNBT();
        if (saveProvider != null) {
            // ISaveProvider.save takes raw IMEInventory; cast required due to generics erasure
            saveProvider.saveChanges((IMEInventory) this);
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Find a key in storedItems that matches the given AE stack (same item, damage, and NBT).
     */
    private IAEItemStack findMatchingKey(IAEItemStack stack) {
        if (stack == null) return null;

        for (IAEItemStack key : storedItems.keySet()) {
            if (key.isSameType(stack)) {
                return key;
            }
        }
        return null;
    }

    // =========================================================================
    // Inner class: IMEInventoryHandler wrapper
    // =========================================================================

    /**
     * Minimal {@link IMEInventoryHandler} wrapper for {@link EStorageCellInventory}. Since
     * {@code CellInventoryHandler}'s constructor is package-private, we provide our own thin
     * handler that delegates all IMEInventory calls to the underlying cell inventory.
     */
    public static class EStorageCellInventoryHandler implements IMEInventoryHandler<IAEItemStack> {

        private final EStorageCellInventory cellInventory;

        public EStorageCellInventoryHandler(EStorageCellInventory cellInventory) {
            this.cellInventory = cellInventory;
        }

        @Override
        public AccessRestriction getAccess() {
            return AccessRestriction.READ_WRITE;
        }

        @Override
        public boolean isPrioritized(IAEItemStack input) {
            return false;
        }

        @Override
        public boolean canAccept(IAEItemStack input) {
            return true;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public boolean validForPass(int pass) {
            return true;
        }

        @Override
        public boolean getSticky() {
            return false;
        }

        @Override
        public boolean isAutoCraftingInventory() {
            return false;
        }

        @Override
        public IMEInventory<IAEItemStack> getInternal() {
            return cellInventory;
        }

        // Delegate all IMEInventory methods to the cell inventory

        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable actionable, BaseActionSource source) {
            return cellInventory.injectItems(input, actionable, source);
        }

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable actionable, BaseActionSource source) {
            return cellInventory.extractItems(request, actionable, source);
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
            return cellInventory.getAvailableItems(out);
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int partitionIndex) {
            return cellInventory.getAvailableItems(out, partitionIndex);
        }

        @Override
        public IAEItemStack getAvailableItem(IAEItemStack request) {
            return cellInventory.getAvailableItem(request);
        }

        @Override
        public IAEItemStack getAvailableItem(IAEItemStack request, int partitionIndex) {
            return cellInventory.getAvailableItem(request, partitionIndex);
        }

        @Override
        public Collection<IAEItemStack> getSortedFuzzyItems(Collection<IAEItemStack> out, IAEItemStack filter,
            FuzzyMode fuzzyMode, int maxResults) {
            return cellInventory.getSortedFuzzyItems(out, filter, fuzzyMode, maxResults);
        }

        @Override
        public StorageChannel getChannel() {
            return StorageChannel.ITEMS;
        }
    }
}
