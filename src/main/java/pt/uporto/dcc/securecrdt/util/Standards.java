package pt.uporto.dcc.securecrdt.util;

public class Standards {

    /*
    Sets the amount of CRDT replica servers
     */
    public static final int NUMBER_OF_REPLICAS = 2;

    /*
    Defines if local or distributed deployment
     */
    public static boolean LOCAL_DEPLOYMENT = false;

    /*
    Defines id there is resharing before propagate
     */
    public static boolean RESHARING_BEFORE_PROPAGATE = false;

    /*
    Sets the IP address for each service
     */
    public static String CLIENT_IP = "192.168.70.16";
    public static String PLAYER_0_IP = "192.168.70.17";
    public static String PLAYER_1_IP = "192.168.70.18";
    public static String PLAYER_2_IP = "192.168.70.19";

    public static int getLocalIPLastSection(int localPlayer) {
        return 17 + localPlayer;
    }


    /*
    Sets the protocol name strings
     */
    public static final String REGISTER_PROTOCOL_NAME = "register";
    public static final String GCOUNTER_PROTOCOL_NAME = "gcounter";
    public static final String PNCOUNTER_PROTOCOL_NAME = "pncounter";
    public static final String MAXVALUE_PROTOCOL_NAME = "maxvalue";
    public static final String MINBOUNDEDCOUNTER_PROTOCOL_NAME = "minboundedcounter";
    public static final String EVERGROWINGSET_PROTOCOL_NAME = "evergrowingset";
    public static final String SETWITHLEAKAGE_PROTOCOL_NAME = "setwithleakage";
}
