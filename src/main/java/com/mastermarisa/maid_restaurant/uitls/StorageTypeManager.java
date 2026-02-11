package com.mastermarisa.maid_restaurant.uitls;

import com.mastermarisa.maid_restaurant.api.IStorageType;
import com.mastermarisa.maid_restaurant.task.storage.CommonStorage;
import com.mastermarisa.maid_restaurant.task.storage.TableStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class StorageTypeManager {
    private static final ConcurrentHashMap<String, IStorageType> typeMap;
    private static final ConcurrentHashMap<IStorageType, String> typePool;
    private static final List<IStorageType> ordered;

    public static void register(IStorageType type) {
        typeMap.put(type.getUID(),type);
        typePool.put(type,type.getUID());
        ordered.add(type);
    }

    public static IStorageType getType(String UID) {
        return typeMap.get(UID);
    }

    public static String getUID(IStorageType type) {
        return typePool.get(type);
    }

    public static @Nullable IItemHandler tryGetHandler(Level level, BlockPos pos) {
        for (IStorageType type : ordered) {
            if (type.isValid(level,pos)) {
                IItemHandler handler = type.getHandler(level,pos);
                if (handler != null) return handler;
            }
        }

        return null;
    }

    public static @Nullable IStorageType tryGetType(Level level, BlockPos pos) {
        for (IStorageType type : ordered)
            if (type.isValid(level,pos))
                if (type.getHandler(level,pos) != null) return type;

        return null;
    }

    static {
        typeMap = new ConcurrentHashMap<>();
        typePool = new ConcurrentHashMap<>();
        ordered = new ArrayList<>();
        register(new CommonStorage());
        register(new TableStorage());
    }
}
