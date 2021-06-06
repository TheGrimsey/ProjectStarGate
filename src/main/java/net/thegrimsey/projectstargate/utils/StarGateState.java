package net.thegrimsey.projectstargate.utils;

public enum StarGateState {
    IDLE,
    DIALING,
    CONNECTED;

    public static byte toID(StarGateState state)
    {
        return (byte) state.ordinal();
    }
    public static StarGateState fromID(byte id)
    {
        return enumValues[id];
    }

    private static final StarGateState[] enumValues = StarGateState.values();
}
