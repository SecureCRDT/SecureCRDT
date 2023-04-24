package pt.uporto.dcc.securecrdt.communication;

import pt.uporto.dcc.securecrdt.util.Standards;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getPlayerIDFromSendingPort;
import static pt.uporto.dcc.securecrdt.util.Standards.getLocalIPLastSection;

public class PlayerClient extends Thread {

    private final IOManager ioManager;

    private final int outPort;
    private final String targetAddress;
    private final int targetPort;

    private boolean running;
    private boolean connected;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public PlayerClient(IOManager ioManager, int outPort, String targetAddress, int targetPort) {
        this.ioManager = ioManager;
        this.outPort = outPort;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;

        this.running = true;
        this.connected = false;
    }

    public void connect() throws IOException {
        if (Standards.LOCAL_DEPLOYMENT) {
            socket = new Socket(this.targetAddress, this.targetPort,
                    InetAddress.getByName("localhost"), this.outPort);
        } else {
            byte[] ipAddr = new byte[]{(byte) 192, (byte) 168, (byte) 70, (byte) getLocalIPLastSection(this.ioManager.getOwner().getPlayerID())};
            socket = new Socket(this.targetAddress, this.targetPort,
                    InetAddress.getByAddress(ipAddr), this.outPort);
        }
        socket.setReuseAddress(true);
        connected = true;
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public void shutdown() {
        running = false;
        sendClosingConnectionWarning();
    }

    public void send(int type, byte[] message) {
        try {
            out.writeInt(message.length);
            out.writeInt(type);
            out.write(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public void sendTestMessage(String message) {
        this.send(0, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendSecretsMessage(IntShareMessage message) {
        this.send(1, message.serialize());
    }

    public void sendClosingConnectionWarning() {
        this.send(99, new byte[]{});
    }


    @Override
    public synchronized void run() {
        while (running) {
            try {
                int vRead = in.readInt();
                if (vRead == -99) {
                    running = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }

        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public int getTargetPort() {
        return targetPort;
    }
}
