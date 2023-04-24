package pt.uporto.dcc.securecrdt.communication;

import lombok.Getter;
import pt.uporto.dcc.securecrdt.crdt.CrdtPlayer;

import java.io.IOException;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.*;
import static pt.uporto.dcc.securecrdt.util.Standards.LOCAL_DEPLOYMENT;

@Getter
public class IOManager {
    private final CrdtPlayer owner;

    private final PlayerServer server;
    private final PlayerClient[] clients;

    public IOManager (CrdtPlayer owner) throws IOException {
        this.owner = owner;

        this.server = new PlayerServer(this, (50004 + (3  * owner.getPlayerID())));
        server.start();

        this.clients = new PlayerClient[2];

    }

    public void stopAllCommunication() throws IOException, InterruptedException {
        for (PlayerClient c : clients) {
            if (c != null) {
                System.out.println("Shutting down outgoing communication to player " + getPlayerIDFromReceivingPort(c.getTargetPort()));
                c.shutdown();
            }
        }
        server.shutdown();
    }

    public PlayerClient getPlayerClient(int targetPlayerID) {

        int d1 = 3 + targetPlayerID;
        int position = targetPlayerID >= this.owner.getPlayerID() + 1 && targetPlayerID <= this.owner.getPlayerID() + 2
                ?  targetPlayerID - this.owner.getPlayerID() - 1 : d1 - this.owner.getPlayerID() - 1;

        if (clients[position] == null) {
            int sendingPort = (50005 + position + (3 * this.owner.getPlayerID()));
            if (LOCAL_DEPLOYMENT) {
                clients[position] = new PlayerClient(this, sendingPort,
                        "localhost", getReceivingPortFromPlayerID(targetPlayerID));
            } else {
                clients[position] = new PlayerClient(this, sendingPort,
                        getIPAddressFromPlayerID(targetPlayerID), getReceivingPortFromPlayerID(targetPlayerID));
            }

            try {
                clients[position].connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return clients[position];
    }
}
