package com.mastermarisa.maid_restaurant.events;

import com.mastermarisa.maid_restaurant.uitls.BlockUsageManager;
import com.mastermarisa.maid_restaurant.uitls.CookTaskManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public class OnServerAboutToStart {
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event){
        BlockUsageManager.reset();
        CookTaskManager.register();
    }
}
