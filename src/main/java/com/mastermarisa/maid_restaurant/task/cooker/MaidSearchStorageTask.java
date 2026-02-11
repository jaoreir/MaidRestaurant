package com.mastermarisa.maid_restaurant.task.cooker;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.google.common.collect.ImmutableMap;
import com.mastermarisa.maid_restaurant.api.ICookTask;
import com.mastermarisa.maid_restaurant.api.IStorageType;
import com.mastermarisa.maid_restaurant.entity.attachment.CookRequest;
import com.mastermarisa.maid_restaurant.init.InitEntities;
import com.mastermarisa.maid_restaurant.task.api.MaidCheckRateTask;
import com.mastermarisa.maid_restaurant.task.storage.TableStorage;
import com.mastermarisa.maid_restaurant.uitls.*;
import com.mastermarisa.maid_restaurant.uitls.component.StackPredicate;
import com.mastermarisa.maid_restaurant.uitls.BlockUsageManager;
import com.mastermarisa.maid_restaurant.uitls.RequestManager;
import com.mastermarisa.maid_restaurant.uitls.StateManager;
import com.mastermarisa.maid_restaurant.uitls.StorageTypeManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

public class MaidSearchStorageTask extends MaidCheckRateTask {
    public static final String UID = "SearchStorage";
    private final float movementSpeed;
    private final int verticalSearchRange;

    public MaidSearchStorageTask(EntityMaid maid, int maxCheckRate, float movementSpeed, int verticalSearchRange) {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT),maid.getUUID().toString() + UID,maxCheckRate,60);
        this.movementSpeed = movementSpeed;
        this.verticalSearchRange = verticalSearchRange;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid){
        return super.checkExtraStartConditions(level,maid) && StateManager.cookState(maid,level) == StateManager.CookState.STORAGE
                && search(level,maid);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return BehaviorUtils.getType(maid) == TargetType.STORAGE_BLOCK.type;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid entity, long gameTime) {
        if (BehaviorUtils.getType(entity) == TargetType.STORAGE_BLOCK.type) BehaviorUtils.eraseTargetPos(entity);
    }

    protected boolean search(ServerLevel level, EntityMaid maid) {
        BlockPos center = BehaviorUtils.getSearchPos(maid);
        int searchRange = (int)maid.getRestrictRadius();
        List<BlockPos> foundStorages = BlockPosUtils.search(center,searchRange,verticalSearchRange,(pos)->{
            IItemHandler handler = StorageTypeManager.tryGetHandler(level,pos);
            return handler != null && containsRequired(level,maid,handler)
                    && !BlockPosUtils.getAllRelativeGround(level,pos,1).stream().filter(maid::canPathReach).toList().isEmpty();
        });

        return foundStorages.stream().min(Comparator.comparingDouble(p->p.distSqr(maid.blockPosition()))).map(pos->{
            List<BlockPos> available = new ArrayList<>(BlockPosUtils.getAllRelativeGround(level,pos,2));
            Collections.shuffle(available);
            BlockPos walkTar = available.getFirst();
            BehaviorUtils.setTargetPos(maid,new BlockPosTracker(pos),TargetType.STORAGE_BLOCK);
            BehaviorUtils.eraseChairPos(maid);
            BehaviorUtils.setWalkAndLookTargetMemories(maid,walkTar,pos,movementSpeed,0);
            return true;
        }).orElse(false);
    }

    protected boolean containsRequired(ServerLevel level, EntityMaid maid, IItemHandler itemHandler) {
        ICookTask iCookTask = RequestManager.getCurrentTask(maid).get();
        CookRequest request = RequestManager.peekCookRequest(maid).get();
        List<StackPredicate> required = iCookTask.getIngredients(level.getRecipeManager().byKeyTyped(request.type,request.id));
        List<ItemStack> handler = MaidInvUtils.toStacks(maid.getAvailableInv(false));
        Optional<PositionTracker> cached = BehaviorUtils.getCachedWorkBlock(maid);
        if (cached.isPresent()) {
            BlockPos pos = cached.get().currentBlockPosition();
            if (BlockUsageManager.getUserCount(pos) <= 0 || BlockUsageManager.isUsing(pos,maid.getUUID()))
                handler.addAll(iCookTask.getCurrentInput(maid.level(),pos,maid));
        }
        List<Pair<StackPredicate,Integer>> filtered;
        if (iCookTask.getType().equals(ModRecipes.STOCKPOT_RECIPE))
            filtered = MaidInvUtils.filterByCountStockpot(required,handler,request.count);
        else
            filtered = MaidInvUtils.filterByCount(required,handler,request.count);
        return filtered.stream().anyMatch(s -> MaidInvUtils.isStackIn(itemHandler,s.left()));
    }
}
