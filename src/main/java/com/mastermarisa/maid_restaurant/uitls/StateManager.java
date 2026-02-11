package com.mastermarisa.maid_restaurant.uitls;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mastermarisa.maid_restaurant.uitls.component.StackPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class StateManager {
    public static CookState cookState(EntityMaid maid, Level level) {
        return RequestManager.peekCookRequest(maid).map(request ->
            CookTaskManager.getTask(request.type).map(iCookTask -> {
                List<StackPredicate> required = iCookTask.getIngredients(level.getRecipeManager().byKeyTyped(iCookTask.getType(),request.id));
                required.addAll(iCookTask.getKitchenWares());
                List<ItemStack> handler = MaidInvUtils.toStacks(maid.getAvailableInv(false));
                Optional<PositionTracker> cached = BehaviorUtils.getCachedWorkBlock(maid);
                if (cached.isPresent()) {
                    BlockPos pos = cached.get().currentBlockPosition();
                    if (BlockUsageManager.getUserCount(pos) <= 0 || BlockUsageManager.isUsing(pos,maid.getUUID()))
                        handler.addAll(iCookTask.getCurrentInput(maid.level(),pos,maid));
                }
                return MaidInvUtils.getRequired(required,handler).isEmpty() ? CookState.COOK : CookState.STORAGE;
            }).orElse(CookState.IDLE)
        ).orElse(CookState.IDLE);
    }

    public static enum CookState {
        IDLE(0),
        STORAGE(1),
        COOK(2);

        private CookState(int state) { this.state = state; }
        private final int state;

        public int getState() {
            return state;
        }
    }
}
