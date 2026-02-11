package com.mastermarisa.maid_restaurant.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IStorageType {
    String getUID();

    ItemStack getIcon();

    boolean isValid(Level level, BlockPos pos);

    @Nullable IItemHandler getHandler(Level level, BlockPos pos);

    @NotNull ItemStack extract(Level level, BlockPos pos, int slot, int amount, boolean simulate);

    @NotNull ItemStack insert(Level level, BlockPos pos, ItemStack stack, boolean simulate);
}
