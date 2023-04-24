package pt.uporto.dcc.securecrdt.communication;

import pt.uporto.dcc.securecrdt.util.Standards;

public class SocketMapper {

    public static int getReceivingPortFromPlayerID(int playerID) {
        return 50004 + (3  * playerID);
    }

    public static int getPlayerIDFromReceivingPort(int port) {
        return (port - 50004) / 3;
    }

    public static int getPlayerIDFromSendingPort(int port) {
        return (port - 50005) / 3;
    }

    public static String getIPAddressFromPlayerID(int playerID) {
        if (playerID == 0) {
            return Standards.PLAYER_0_IP;
        } else if (playerID == 1) {
            return Standards.PLAYER_1_IP;
        } else if (playerID == 2) {
            return Standards.PLAYER_2_IP;
        }
        return "localhost";
    }
}
