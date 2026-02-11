package com.mastermarisa.maid_restaurant.task.waiter;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.decoration.FruitBasketBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.decoration.TableBlockEntity;
import com.google.common.collect.ImmutableMap;
import com.mastermarisa.maid_restaurant.MaidRestaurant;
import com.mastermarisa.maid_restaurant.api.IStorageType;
import com.mastermarisa.maid_restaurant.data.TagBlock;
import com.mastermarisa.maid_restaurant.entity.attachment.ServeRequest;
import com.mastermarisa.maid_restaurant.init.InitEntities;
import com.mastermarisa.maid_restaurant.uitls.*;
import com.mastermarisa.maid_restaurant.uitls.component.StackPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MaidServeMealTask extends Behavior<EntityMaid> {
    private final double closeEnoughDist;

    public MaidServeMealTask(double closeEnoughDist) {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
        this.closeEnoughDist = closeEnoughDist;
    }

    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return BehaviorUtils.getType(maid) == 4 && RequestManager.peekServeRequest(maid).isPresent() && maid.distanceToSqr(BehaviorUtils.getTargetPos(maid).get().currentPosition()) <= Math.pow(closeEnoughDist, 2.0D);
    }

    protected void start(ServerLevel level, EntityMaid maid, long gameTimeIn) {
        BlockPos pos = BehaviorUtils.getTargetPos(maid).get().currentBlockPosition();
        BlockState state = level.getBlockState(pos);
        ServeRequest request = RequestManager.peekServeRequest(maid).get();
        StackPredicate blockMeal = StackPredicate.of(s -> s.is(request.toServe.getItem()) && s.getItem() instanceof BlockItem);
        StackPredicate normalMeal = StackPredicate.of(request.toServe.getItem());

        BehaviorUtils.eraseTargetPos(maid);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        if (!BehaviorUtils.isValidServeBlock(level,pos)) {
            removeTargetTable(level,maid,pos,request);
            return;
        }

        if (state.is(TagBlock.SERVE_MEAL_BLOCK)) {
            if (level.getBlockEntity(pos) instanceof TableBlockEntity table) {
                ItemStackHandler tableItems = table.getItems();
                int count = getEmptySlots(tableItems);

                if (count == tableItems.getSlots()) {
                    ItemStack meal = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false), 1, blockMeal, false);
                    if (!meal.isEmpty()) {
                        serveBlockMeal(level,maid,request,pos,meal);
                    } else {
                        meal = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false), Math.min(request.toServe.getCount(),tableItems.getSlots()), normalMeal, false);
                        if (!meal.isEmpty()) {
                            serveMealItemToTable(level,maid,request,pos,table,meal);
                        }
                    }
                } else if (count > 0) {
                    ItemStack meal = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false),Math.min(request.toServe.getCount(),count),normalMeal,false);
                    if (!meal.isEmpty())
                        serveMealItemToTable(level,maid,request,pos,table,meal);
                } else {
                    removeTargetTable(level,maid,pos,request);
                }
            } else if (level.getBlockEntity(pos) instanceof FruitBasketBlockEntity basket) {
                int pre = MaidInvUtils.count(maid.getAvailableInv(false),normalMeal);
                MaidInvUtils.tryTakeFrom(maid.getAvailableInv(false),basket.getItems(),normalMeal,Math.min(request.toServe.getCount(),pre));
                request.toServe.split(pre - MaidInvUtils.count(maid.getAvailableInv(false),normalMeal));
                basket.setChanged();
                basket.refresh();
                removeTargetTable(level,maid,pos,request);
            }
            else {
                ItemStack meal = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false), 1, blockMeal, false);
                if (!meal.isEmpty())
                    serveBlockMeal(level,maid,request,pos,meal);
            }
        } else {
            IStorageType iStorageType = StorageTypeManager.tryGetType(level,pos);
            if (iStorageType == null) return;
            ItemStack remainder = request.toServe.copy();
            remainder = iStorageType.insert(level,pos,remainder,true);
            ItemStack toInsert = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false),request.toServe.getCount() - remainder.getCount(),normalMeal,false);
            request.toServe.split(toInsert.getCount());
            iStorageType.insert(level,pos,toInsert,false);
            removeTargetTable(level,maid,pos,request);
        }


        MaidRestaurant.LOGGER.debug("Count:" + request.toServe.getCount());
    }

    private static void serveBlockMeal(ServerLevel level, EntityMaid maid, ServeRequest request, BlockPos pos, ItemStack meal) {
        BlockPos maidPos = maid.blockPosition();
        Direction dir = DirectionUtils.getHorizontalDirection(pos.getX() - maidPos.getX(), pos.getZ() - maidPos.getZ());
        maid.placeItemBlock(InteractionHand.OFF_HAND, pos.above(), dir, meal);
        request.toServe.split(1);
        if (request.toServe.getCount() <= 0) {
            RequestManager.popServeRequest(maid);
        } else {
            removeTargetTable(level,maid,pos,request);
        }
    }

    private static void serveMealItemToTable(ServerLevel level, EntityMaid maid, ServeRequest request, BlockPos pos, TableBlockEntity table, ItemStack meal) {
        ItemStackHandler tableItems = table.getItems();
        int tableIndex = tableItems.getSlots() - getEmptySlots(tableItems);

        while (tableIndex < tableItems.getSlots() && meal.getCount() > 0) {
            tableItems.setStackInSlot(tableIndex,meal.split(1));
            tableIndex++;
            request.toServe.split(1);
            table.refresh();
        }
        table.refresh();
        if (request.toServe.isEmpty()) {
            RequestManager.popServeRequest(maid);
        } else {
            removeTargetTable(level,maid,pos,request);
        }
    }

    private static void removeTargetTable(ServerLevel level, EntityMaid maid, BlockPos pos, ServeRequest request) {
        request.targetTables.removeFirst();
        if (request.targetTables.isEmpty()) {
            RequestManager.popServeRequest(maid);
            if (request.toServe.getCount() > 0) {
                ItemStack meal = MaidInvUtils.tryExtractSingleSlot(maid.getAvailableInv(false),request.toServe.getCount(),StackPredicate.of(request.toServe.getItem()),false);
                if (!meal.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + 0.5f, pos.getZ(), meal);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    private static int getEmptySlots(ItemStackHandler itemStackHandler) {
        int count = 0;
        for (int i = 0;i < itemStackHandler.getSlots();i++)
            if (itemStackHandler.getStackInSlot(i).isEmpty())
                count++;

        return count;
    }
}
