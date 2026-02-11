package com.mastermarisa.maid_restaurant.task.cooker;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.google.common.collect.ImmutableMap;
import com.mastermarisa.maid_restaurant.api.ICookTask;
import com.mastermarisa.maid_restaurant.api.IStorageType;
import com.mastermarisa.maid_restaurant.entity.attachment.CookRequest;
import com.mastermarisa.maid_restaurant.init.InitEntities;
import com.mastermarisa.maid_restaurant.task.storage.TableStorage;
import com.mastermarisa.maid_restaurant.uitls.*;
import com.mastermarisa.maid_restaurant.uitls.component.StackPredicate;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Optional;

public class MaidTakeFromStorageTask extends Behavior<EntityMaid> {
    private final double closeEnoughDist;

    public MaidTakeFromStorageTask(double closeEnoughDist) {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
        this.closeEnoughDist = closeEnoughDist;
    }

    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return BehaviorUtils.getType(maid) == TargetType.STORAGE_BLOCK.type && maid.distanceToSqr(BehaviorUtils.getTargetPos(maid).get().currentPosition()) <= Math.pow(closeEnoughDist,2.0D);
    }

    protected void start(ServerLevel level, EntityMaid maid, long gameTimeIn) {
        BlockPos pos = BehaviorUtils.getTargetPos(maid).get().currentBlockPosition();
        BlockState state = level.getBlockState(pos);

        BehaviorUtils.eraseTargetPos(maid);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        if (StateManager.cookState(maid,level) != StateManager.CookState.STORAGE) return;
        IStorageType iStorageType = StorageTypeManager.tryGetType(level,pos);
        if (iStorageType == null || !iStorageType.isValid(level,pos)) return;
        IItemHandler handler = iStorageType.getHandler(level,pos);
        if (handler == null) return;

        CookRequest request = RequestManager.peekCookRequest(maid).get();
        ICookTask iCookTask = RequestManager.getCurrentTask(maid).get();
        List<StackPredicate> required = iCookTask.getIngredients(level.getRecipeManager().byKeyTyped(request.type,request.id));
        List<ItemStack> stacks = MaidInvUtils.toStacks(maid.getAvailableInv(false));

        Optional<PositionTracker> cached = BehaviorUtils.getCachedWorkBlock(maid);
        if (cached.isPresent()) {
            BlockPos p = cached.get().currentBlockPosition();
            if (BlockUsageManager.getUserCount(p) <= 0 || BlockUsageManager.isUsing(p,maid.getUUID()))
                stacks.addAll(iCookTask.getCurrentInput(maid.level(),p,maid));
        }

        List<Pair<StackPredicate,Integer>> filtered;
        if (iCookTask.getType().equals(ModRecipes.STOCKPOT_RECIPE))
            filtered = MaidInvUtils.filterByCountStockpot(required,stacks,request.count);
        else
            filtered = MaidInvUtils.filterByCount(required,stacks,request.count);
        for (var pair : filtered) {
            MaidInvUtils.tryTakeFrom(level,pos,iStorageType,maid.getAvailableInv(false),pair.left(),pair.right());
        }
        CheckRateManager.setNextCheckTick(maid.getUUID() + MaidSearchWorkBlockTask.UID,5);
        CheckRateManager.setNextCheckTick(maid.getUUID() + MaidSearchStorageTask.UID,5);
    }
}
