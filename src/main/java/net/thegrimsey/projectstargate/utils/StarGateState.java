package net.thegrimsey.projectstargate.utils;

public enum StarGateState {
    IDLE,
    DIALING,
    CONNECTED;

    private static final StarGateState[] enumValues = StarGateState.values();

    public static byte toID(StarGateState state) {
        return (byte) state.ordinal();
    }

    public static StarGateState fromID(byte id) {
        return enumValues[id];
    }
}

