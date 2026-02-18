package com.mastermarisa.maid_restaurant.request.world;

import com.mastermarisa.maid_restaurant.request.CookRequest;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WorldCookRequestPool extends SavedData {
    public List<CookRequest> requests;

    public WorldCookRequestPool() {
        requests = new ArrayList<>();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (CookRequest request : requests) listTag.add(request.serializeNBT(provider));
        compoundTag.put("requests",listTag);
        return compoundTag;
    }

    public static WorldCookRequestPool load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        WorldCookRequestPool pool = new WorldCookRequestPool();
        if (compoundTag.contains("requests")) {
            ListTag listTag = compoundTag.getList("requests", Tag.TAG_COMPOUND);
            for (int i = 0;i < listTag.size();i++) {
                CookRequest request = new CookRequest();
                request.deserializeNBT(provider,listTag.getCompound(i));
                pool.requests.add(request);
            }
        }

        return pool;
    }

    public static WorldCookRequestPool get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WorldCookRequestPool::new, WorldCookRequestPool::load),
                "world_cook_request_pool"
        );
    }
}
