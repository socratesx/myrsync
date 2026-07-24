package com.linminitools.myrsync;

/**
 * Encodes scheduler id as {@code configId * SLOT_MULT + slotIndex} so each schedule
 * under a configuration has a stable, derivable id.
 */
public final class SchedulerIds {

    /** Max schedules per configuration (slot index 0 … SLOT_MULT - 1). */
    public static final int SLOT_MULT = 100;

    private SchedulerIds() {}

    public static int makeId(int configId, int slotIndex) {
        return configId * SLOT_MULT + slotIndex;
    }

    public static int configIdFromSchedulerId(int schedulerId) {
        return schedulerId / SLOT_MULT;
    }

    public static int slotIndexFromSchedulerId(int schedulerId) {
        return schedulerId % SLOT_MULT;
    }

    /** Short label for logs / empty name field (not shown as a user-facing “name”). */
    public static String internalLabel(int configId, int slotIndex) {
        return "c" + configId + "_s" + slotIndex;
    }
}
