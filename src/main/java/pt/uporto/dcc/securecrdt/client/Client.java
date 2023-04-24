package pt.uporto.dcc.securecrdt.client;

import lombok.Getter;
import lombok.Setter;
import pt.uminho.haslab.smpc.sharemindImp.Integer.IntSharemindDealer;
import pt.uporto.dcc.securecrdt.http.HttpServerForLocust;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getIPAddressFromPlayerID;
import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getReceivingPortFromPlayerID;
import static pt.uporto.dcc.securecrdt.util.Standards.*;

@Getter
@Setter
public class Client {

    private HttpServerForLocust httpServerForLocust;
    private final ClientCommunication[] clients;
    private final IntSharemindDealer dealer;

    private CrdtClient crdtClient;

    private Client(String type) {
        this.httpServerForLocust = new HttpServerForLocust(this);
        this.clients = new ClientCommunication[3];
        this.dealer = new IntSharemindDealer();

        switch (type) {
            case REGISTER_PROTOCOL_NAME: {
                this.crdtClient = new RegisterCrdtClient(this);
                break;
            }
            case GCOUNTER_PROTOCOL_NAME: {
                this.crdtClient = new GCounterCrdtClient(this);
                break;
            }
            case PNCOUNTER_PROTOCOL_NAME: {
                this.crdtClient = new PNCounterCrdtClient(this);
                break;
            }
            case MAXVALUE_PROTOCOL_NAME: {
                this.crdtClient = new MaxvalueCrdtClient(this);
                break;
            }
            case MINBOUNDEDCOUNTER_PROTOCOL_NAME: {
                this.crdtClient = new MinBoundedCounterCrdtClient(this);
                break;
            }
            case EVERGROWINGSET_PROTOCOL_NAME: {
                this.crdtClient = new EverGrowingSetCrdtClient(this);
                break;
            }
            case SETWITHLEAKAGE_PROTOCOL_NAME: {
                this.crdtClient = new SetWithLeakageCrdtClient(this);
                break;
            }
            default: {
                System.out.println("No such CRDT exists");
                System.exit(0);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    protected void waitForResponse() {
        synchronized (this) {
            while (!Arrays.equals(crdtClient.gotPlayerResults, new boolean[]{true, true, true})) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected int unshare(int[] shares) {
        return dealer.unshare(shares);
    }

    protected ClientCommunication getClient(int targetPlayerID) {

        if (clients[targetPlayerID] == null) {
            int sendingPort = (50001 + targetPlayerID);
            if (LOCAL_DEPLOYMENT) {
                clients[targetPlayerID] = new ClientCommunication(this, sendingPort,
                        "localhost", getReceivingPortFromPlayerID(targetPlayerID));
            } else {
                clients[targetPlayerID] = new ClientCommunication(this, sendingPort,
                        getIPAddressFromPlayerID(targetPlayerID), getReceivingPortFromPlayerID(targetPlayerID));
            }
            try {
                clients[targetPlayerID].connect();
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            clients[targetPlayerID].start();
        }
        return clients[targetPlayerID];

    }

    private void shutdown() throws IOException, InterruptedException {
        System.out.println("Shutting down ...");
        for (ClientCommunication c : this.clients) {
            c.sendPlayerShutdownRequest();
        }
        for (ClientCommunication c : this.clients) {
            c.shutdown();
        }
    }

    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);

        if (args.length != 1 && args.length !=2) {
            System.out.println("Either one or two arguments required.");
            return;
        } else if (args.length == 2 && args[1].equals("local")) {
            LOCAL_DEPLOYMENT = true;
        }

        Client client = new Client(args[0]);

        System.out.println("Client ready to go.");

        /*
        while (true) {

            System.out.print("Please input a command: ");
            String line = userInput.nextLine();
            
            switch (line.split("\\s+")[0]) {
                case "update":{
                    client.crdtClient.update(line);
                    break;
                }
                case "query": {
                    int currentValues = client.crdtClient.query(line);
                    break;
                }
                case "propagate": {
                    PlayerState[] states = client.crdtClient.propagate();
                    System.out.println(Arrays.toString(states));
                    break;
                }
                case "merge": {
                    client.crdtClient.merge();
                    break;
                }
                case "exit": {
                    System.exit(0);
                }
                default: {
                    System.out.println("Invalid command.");
                    break;
                }
            }
        }
         */
    }
}
