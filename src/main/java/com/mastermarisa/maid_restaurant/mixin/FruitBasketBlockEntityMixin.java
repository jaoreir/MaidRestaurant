package com.mastermarisa.maid_restaurant.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.decoration.FruitBasketBlockEntity;
import com.mastermarisa.maid_restaurant.data.TagItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FruitBasketBlockEntity.class})
public class FruitBasketBlockEntityMixin {
    @Inject(method = "putOn", at = @At("HEAD"), cancellable = true)
    public void putOn(ItemStack stack, CallbackInfo ci) {
        if (stack.is(TagItem.BASKET_BLACKLIST)) {
            ci.cancel();
        }
    }
}
