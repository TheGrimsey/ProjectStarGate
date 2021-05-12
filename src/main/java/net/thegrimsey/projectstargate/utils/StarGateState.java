package net.thegrimsey.projectstargate.utils;

public enum StarGateState {
    IDLE,
    DIALING,
    CONNECTED;

    public static byte toID(StarGateState state)
    {
        switch (state)
        {
            case IDLE:
                return 0;
            case DIALING:
                return 1;
            case CONNECTED:
                return 2;
            default:
                assert (false);
                return 127;
        }
    }

    public static StarGateState fromID(byte Id)
    {
        switch (Id)
        {
            case 0:
                return IDLE;
            case 1:
                return DIALING;
            case 2:
                return CONNECTED;
            default:
                assert (false);
                return StarGateState.IDLE;
        }
    }
};
