package com.mastermarisa.maid_restaurant.uitls.component;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListItemHandler implements IItemHandler {
    protected final List<ItemStack> stacks;
    protected final int slotLimit;
    protected boolean allowInsert = true;
    protected boolean allowExtract = true;

    /**
     * 使用指定的列表创建包装器
     * @param stacks 要包装的列表
     * @param slotLimit 每个槽位的最大堆叠数
     */
    public ListItemHandler(List<ItemStack> stacks, int slotLimit) {
        this.stacks = new ArrayList<>(stacks);
        this.slotLimit = slotLimit;
    }

    /**
     * 使用默认堆叠数创建包装器
     */
    public ListItemHandler(List<ItemStack> stacks) {
        this(stacks, 64);
    }

    /**
     * 创建指定大小的空包装器
     */
    public ListItemHandler(int size, int slotLimit) {
        this.stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.stacks.add(ItemStack.EMPTY);
        }
        this.slotLimit = slotLimit;
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot).copy(); // 返回副本，避免外部修改
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!allowInsert || stack.isEmpty()) {
            return stack;
        }

        validateSlotIndex(slot);

        ItemStack existing = stacks.get(slot);

        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }

        int maxStackSize = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        int canAccept = maxStackSize - existing.getCount();

        if (canAccept <= 0) {
            return stack;
        }

        int toInsert = Math.min(stack.getCount(), canAccept);

        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack newStack = stack.copy();
                newStack.setCount(toInsert);
                stacks.set(slot, newStack);
            } else {
                existing.grow(toInsert);
            }
        }

        // 返回剩余物品
        if (toInsert == stack.getCount()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack remaining = stack.copy();
            remaining.shrink(toInsert);
            return remaining;
        }
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowExtract || amount <= 0) {
            return ItemStack.EMPTY;
        }

        validateSlotIndex(slot);

        ItemStack existing = stacks.get(slot);

        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);

        if (!simulate) {
            if (toExtract == existing.getCount()) {
                stacks.set(slot, ItemStack.EMPTY);
            } else {
                existing.shrink(toExtract);
            }
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        return slotLimit;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        return true;
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range [0," + stacks.size() + ")");
        }
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return result;
    }

    public void setAllowInsert(boolean allowInsert) {
        this.allowInsert = allowInsert;
    }

    public void setAllowExtract(boolean allowExtract) {
        this.allowExtract = allowExtract;
    }

    public void clear() {
        Collections.fill(stacks, ItemStack.EMPTY);
    }

    public List<ItemStack> getNonEmptyStacks() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
        }
        return result;
    }
}
