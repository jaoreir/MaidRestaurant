package com.mastermarisa.maid_restaurant.compat.create;

import net.neoforged.fml.ModList;

public class CreateCompat {
    public static final boolean LOADED = ModList.get().isLoaded("create");

    public static void register() {
        if (LOADED) {
            //DepotStorage.register();
        }
    }
}
