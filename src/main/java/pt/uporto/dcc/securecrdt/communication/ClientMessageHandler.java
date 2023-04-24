package pt.uporto.dcc.securecrdt.communication;

import lombok.Getter;
import pt.uminho.haslab.smpc.exceptions.InvalidSecretValue;

import java.io.*;
import java.net.Socket;

@Getter
public class ClientMessageHandler extends Thread {

    private PlayerServer server;
    private Socket socket;

    private boolean running;
    private boolean toClose;

    public ClientMessageHandler() {
        this.server = null;
        this.socket = null;
        this.running = false;
    }

    public DataInputStream getInStream() throws IOException {
        return new DataInputStream(new BufferedInputStream(
                this.socket.getInputStream()));
    }
    public DataOutputStream getOutStream() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(
                this.socket.getOutputStream()));
    }

    public void handleMessage(int type, byte[] message) throws IOException, InterruptedException, InvalidSecretValue {
        server.getIoManager().getOwner().handleMessage(type, message);
    }


    public void close() throws IOException {
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

    public void lateBuild(PlayerServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.running = true;
        System.out.println("Incoming connection from controller");
    }

    public boolean isRunning() {
        return this.running;
    }
}
