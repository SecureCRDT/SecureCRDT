package pt.uporto.dcc.securecrdt.communication;

import lombok.Getter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static pt.uporto.dcc.securecrdt.communication.SocketMapper.getReceivingPortFromPlayerID;

@Getter
public class PlayerServer extends Thread {

    private final IOManager ioManager;
    private final int bindingPort;

    private final ServerSocket serverSocket;
    private final List<PlayerMessageHandler> clients;
    private final ClientMessageHandler controller;

    private boolean running;
    private final CountDownLatch threadLoopClosed;

    public PlayerServer(IOManager ioManager, int port) throws IOException {
        this.ioManager = ioManager;
        this.bindingPort = port;

        this.serverSocket = new ServerSocket(port);
        this.clients = new ArrayList<>();
        this.controller = new ClientMessageHandler();

        this.running = true;
        this.threadLoopClosed = new CountDownLatch(1);
    }

    public void shutdown() throws InterruptedException, IOException {
        while (controller.isRunning()) {
            System.out.println("Warning: Controller handler is still running.");
            Thread.sleep(500);
        }

        for (PlayerMessageHandler c : clients) {
            while (c.isRunning()) {
                System.out.println("Warning: A client handler is still running.");
                Thread.sleep(500);
            }
        }
        running = false;
        voidClient();
        threadLoopClosed.await();
        serverSocket.close();
    }

    public void forceShutdown() throws InterruptedException, IOException {
        running = false;
        voidClient();
        threadLoopClosed.await();
        serverSocket.close();
    }

    /*
     * Client to trigger loop to force thread to verify running state and close
     * server
     */
    private void voidClient() {
        int customPort = 49994 + ioManager.getOwner().getPlayerID();
        PlayerClient closeClient = new PlayerClient(this.ioManager, customPort, "localhost",
                bindingPort);
        try {
            closeClient.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeClient.shutdown();
    }

    @Override
    public void run() {
        System.out.println("Player server running on port " + getReceivingPortFromPlayerID(this.getIoManager().getOwner().getPlayerID()));
        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                if (clientSocket.getPort() == this.getIoManager().getOwner().getPlayerID() + 50001) {
                    this.controller.lateBuild(this, clientSocket);
                    controller.start();
                } else {
                    PlayerMessageHandler c = new PlayerMessageHandler(this, clientSocket);
                    c.start();
                    clients.add(c);
                }
            }
            threadLoopClosed.countDown();

        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

}
