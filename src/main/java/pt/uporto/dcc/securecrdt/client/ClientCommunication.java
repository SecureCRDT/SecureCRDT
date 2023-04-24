package pt.uporto.dcc.securecrdt.client;

import pt.uporto.dcc.securecrdt.messages.*;
import pt.uporto.dcc.securecrdt.messages.states.*;
import pt.uporto.dcc.securecrdt.util.Standards;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getPlayerIDFromReceivingPort;

public class ClientCommunication extends Thread {
    private final Client owner;

    private final int outPort;
    private final String targetAddress;
    private final int targetPort;

    private boolean running;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private final CountDownLatch threadLoopClosed;

    public ClientCommunication(Client owner, int outPort, String targetAddress, int targetPort) {
        this.owner = owner;
        this.outPort = outPort;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;

        this.running = true;
        this.threadLoopClosed = new CountDownLatch(1);
    }

    public void connect() throws IOException {
        if (Standards.LOCAL_DEPLOYMENT) {
            socket = new Socket(this.targetAddress, this.targetPort,
                    InetAddress.getByName("localhost"), this.outPort);
        } else {
            byte[] ipAddr = new byte[]{(byte) 192, (byte) 168, (byte) 70, (byte) 16};
            socket = new Socket(this.targetAddress, this.targetPort,
                    InetAddress.getByAddress(ipAddr), this.outPort);
        }
        socket.setReuseAddress(true);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        running = true;
    }

    public void shutdown() throws IOException {
        running = false;
        socket.close();
        out.close();
        in.close();
        System.out.println("Closed connection to player " + getPlayerIDFromReceivingPort(targetPort));
    }

    public void send(int type, byte[] message) throws IOException {
        out.writeInt(message.length);
        out.writeInt(type);
        out.write(message);
        out.flush();
    }

    public void sendTestMessage(String message) throws IOException {
        this.send(0, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendUpdateRequest(IntProtocolMessage message) throws IOException {
        this.send(1, message.serialize());
    }

    public void sendQueryRequest(IntProtocolMessage message) throws IOException {
        this.send(2, message.serialize());
    }

    public void sendPropagateRequest(IntProtocolMessage message) throws IOException {
        this.send(3, message.serialize());
    }

    public void sendMergeRequest(IntProtocolMessage message) throws IOException {
        this.send(4, message.serialize());
    }

    public void sendPlayerShutdownRequest() throws IOException {
        this.send(99, new byte[]{});
    }


    @Override
    public synchronized void run() {
        while (running) {
            /*
              For POC only
             */
            try {

                int resSize = in.readInt();
                byte[] res = new byte[resSize];
                for (int i = 0; i < resSize; i++) res[i] = in.readByte();
                IntProtocolMessage response = IntProtocolMessage.deserialize(res);

                if (owner.getCrdtClient() instanceof RegisterCrdtClient) {
                    RegisterPlayerState playerState = RegisterPlayerState.deserialize(response.getData());
                    ((RegisterCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof GCounterCrdtClient) {
                    GCounterPlayerState playerState = GCounterPlayerState.deserialize(response.getData());
                    ((GCounterCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof PNCounterCrdtClient) {
                    PNCounterPlayerState playerState = PNCounterPlayerState.deserialize(response.getData());
                    ((PNCounterCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof MaxvalueCrdtClient) {
                    RegisterPlayerState playerState = RegisterPlayerState.deserialize(response.getData());
                    ((MaxvalueCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof  MinBoundedCounterCrdtClient) {
                    MinBoundedCounterPlayerState playerState = MinBoundedCounterPlayerState.deserialize(response.getData());
                    ((MinBoundedCounterCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof EverGrowingSetCrdtClient) {
                    SetPlayerState playerState = SetPlayerState.deserialize(response.getData());
                    ((EverGrowingSetCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }
                else if (owner.getCrdtClient() instanceof SetWithLeakageCrdtClient) {
                    SetPlayerState playerState = SetPlayerState.deserialize(response.getData());
                    ((SetWithLeakageCrdtClient) owner.getCrdtClient())
                            .responses[getPlayerIDFromReceivingPort(targetPort)] = playerState;
                }

                owner.getCrdtClient().gotPlayerResults[getPlayerIDFromReceivingPort(targetPort)] = true;
                synchronized (this.owner) {
                    this.owner.notify();
                }
            } catch (IOException ignored) {

            }

        }
        threadLoopClosed.countDown();
    }
}
