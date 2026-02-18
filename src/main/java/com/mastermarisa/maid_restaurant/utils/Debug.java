package com.mastermarisa.maid_restaurant.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class Debug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARK = "[maid_restaurant-DEBUG]";

    public static void Log(String msg) {
        LOGGER.debug(MARK + msg);
    }

    public static void Log(String msg, Object[] args) {
        LOGGER.debug(MARK + msg, args);
    }
}
