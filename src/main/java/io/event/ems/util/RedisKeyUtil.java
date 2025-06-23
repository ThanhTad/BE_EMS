package io.event.ems.util;

import java.util.UUID;

public class RedisKeyUtil {
    private static final String HOLD_KEY_PREFIX = "hold::";
    private static final String GA_HELD_COUNT_PREFIX = "ga_held_count::";


    public static String getHoldKey(UUID holdId) {
        return HOLD_KEY_PREFIX + holdId.toString();
    }

    public static String getGeneralAdmissionHeldCountKey(UUID ticketId) {
        return GA_HELD_COUNT_PREFIX + ticketId.toString();
    }
}
