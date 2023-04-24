package pt.uporto.dcc.securecrdt.crdt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.Getter;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;
import pt.uporto.dcc.securecrdt.communication.ClientMessageHandler;
import pt.uporto.dcc.securecrdt.communication.IOManager;
import pt.uporto.dcc.securecrdt.messages.IntProtocolMessage;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getPlayerIDFromSendingPort;
import static pt.uporto.dcc.securecrdt.util.Standards.*;

@Getter
public class CrdtPlayer {
    protected final int replicaId;

    private final int playerID;
    private final IOManager ioManager;
    private final SmpcPlayer smpcPlayer;

    private final RegisterCrdt registerCrdt;
    private final GCounterCrdt gCounterCrdt;
    private final PNCounterCrdt pnCounterCrdt;
    private final MaxValueCrdt maxValueCrdt;
    private final MinBoundedCounterCrdt minBoundedCounterCrdt;
    private final EverGrowingSetCrdt everGrowingSetCrdt;
    private final SetWithLeakageCrdt setWithLeakageCrdt;

    public CrdtPlayer(int playerID) throws IOException {
        this.replicaId = 0;
        this.playerID = playerID;
        this.ioManager = new IOManager(this);
        this.smpcPlayer = new SmpcPlayer(playerID, this);

        this.registerCrdt = new RegisterCrdt(this);
        this.gCounterCrdt = new GCounterCrdt(this);
        this.pnCounterCrdt = new PNCounterCrdt(this);
        this.maxValueCrdt = new MaxValueCrdt(this);
        this.minBoundedCounterCrdt = new MinBoundedCounterCrdt(this);
        this.everGrowingSetCrdt = new EverGrowingSetCrdt(this);
        this.setWithLeakageCrdt = new SetWithLeakageCrdt(this);
    }

    public void handleMessage(int type, byte[] message) throws IOException, InterruptedException, InvalidSecretValue {
        ClientMessageHandler clientMessageHandler = getIoManager().getServer().getController();

        switch (type) {
            // Mensagem de teste
            case 0: {
                String readableMessage = new String(message, StandardCharsets.UTF_8);
                System.out.println("Player " + getPlayerIDFromSendingPort(clientMessageHandler.getSocket().getPort()) +
                        " says: " + readableMessage);
                break;
            }
            // Pedido de término de jogador vindo do controlador
            case 99: {
                System.out.println("Closing order received from controller.");
                clientMessageHandler.close();
                clientMessageHandler.getServer().getIoManager().stopAllCommunication();
                System.exit(0);
            }
            default: {
                IntProtocolMessage req = IntProtocolMessage.deserialize(message);
                switch (req.getProtocol()) {
                    case REGISTER_PROTOCOL_NAME: {
                        registerCrdt.handleMessage(type, req);
                        break;
                    }
                    case GCOUNTER_PROTOCOL_NAME: {
                        gCounterCrdt.handleMessage(type, req);
                        break;
                    }
                    case PNCOUNTER_PROTOCOL_NAME: {
                        pnCounterCrdt.handleMessage(type, req);
                        break;
                    }
                    case MAXVALUE_PROTOCOL_NAME: {
                        maxValueCrdt.handleMessage(type, req);
                        break;
                    }
                    case MINBOUNDEDCOUNTER_PROTOCOL_NAME: {
                        minBoundedCounterCrdt.handleMessage(type, req);
                        break;
                    }
                    case EVERGROWINGSET_PROTOCOL_NAME: {
                        everGrowingSetCrdt.handleMessage(type, req);
                        break;
                    }
                    case SETWITHLEAKAGE_PROTOCOL_NAME: {
                        setWithLeakageCrdt.handleMessage(type, req);
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1 && args.length !=2 && args.length !=3) {
            System.out.println("Either one, two  or three arguments required.");
            return;
        } else if ((args.length == 2 || args.length == 3) && args[1].equals("local")) {
            LOCAL_DEPLOYMENT = true;
            if (args.length == 3 && args[2].equals("reshare")) {
                RESHARING_BEFORE_PROPAGATE = true;
            }
        }

        new CrdtPlayer(Integer.parseInt(args[0]));

    }

}
