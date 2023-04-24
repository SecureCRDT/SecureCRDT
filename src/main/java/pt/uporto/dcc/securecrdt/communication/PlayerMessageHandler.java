package pt.uporto.dcc.securecrdt.communication;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getPlayerIDFromSendingPort;

public class PlayerMessageHandler extends Thread {

    private final PlayerServer server;
    private final Socket socket;

    private boolean running;
    private boolean toClose;

    public PlayerMessageHandler(PlayerServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.running = true;
        if (getPlayerIDFromSendingPort(socket.getPort()) >= 0)
            System.out.println("Incoming connection from player " + getPlayerIDFromSendingPort(socket.getPort()));
    }

    private DataInputStream getInStream() throws IOException {
        return new DataInputStream(new BufferedInputStream(
                this.socket.getInputStream()));
    }
    private DataOutputStream getOutStream() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(
                this.socket.getOutputStream()));
    }

    public void handleMessage(int type, byte[] message) {
        switch (type) {
            // Mensagem de teste
            case 0: {
                String readableMessage = new String(message, StandardCharsets.UTF_8);
                System.out.println("Player " + getPlayerIDFromSendingPort(socket.getPort()) +
                        " says: " + readableMessage);
                break;
            }
            // Mensagem com segredos int
            case 1: {
                IntShareMessage msg = IntShareMessage.deserialize(message);
                int playerDest = msg.getDestPlayer();
                int playerSource = msg.getSourcePlayer();
                int[] vals = msg.getValues();
                this.server.getIoManager().getOwner().getSmpcPlayer().storeValues(playerDest, playerSource, vals);
                break;
            }
            // Aviso de fecho de conexão
            case 99: {
                if (getPlayerIDFromSendingPort(socket.getPort()) >= 0)
                    System.out.println("Warning: closing incoming connection from player " + getPlayerIDFromSendingPort(socket.getPort()));
                toClose = true;
                break;
            }
        }
    }

    private void close() throws IOException {
        running = false;
        socket.close();
    }

    @Override
    public void run() {
        DataInputStream in = null;
        DataOutputStream out;
        try {
            in = getInStream();
            out = getOutStream();

            while (running) {
                int messageSize = in.readInt();
                int messageType = in.readInt();
                byte[] message = new byte[messageSize];
                in.readFully(message);

                handleMessage(messageType, message);

                if (toClose) {
                    out.writeInt(-99);
                    out.flush();
                    this.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return this.running;
    }
}
