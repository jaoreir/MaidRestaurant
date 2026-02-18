package com.mastermarisa.maid_restaurant.event;

import com.mastermarisa.maid_restaurant.MaidRestaurant;
import com.mastermarisa.maid_restaurant.utils.RequestManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = MaidRestaurant.MOD_ID)
public class RequestDistributor {
    private static long serverTickCount = 0;

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        serverTickCount++;
        if (serverTickCount % 10 == 0) RequestManager.tryDistributeRequests((ServerLevel) event.getLevel());
    }

    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        serverTickCount++;
        if (serverTickCount % 10 != 0) return;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            RequestManager.tryDistributeRequests(level);
        }
    }
}
