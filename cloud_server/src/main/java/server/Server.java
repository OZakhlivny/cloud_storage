package server;

import server.auth.AuthService;
import server.auth.BaseAuthService;
import server.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private int port;
    private AuthService authService;
    private List<ClientHandler> clients;


    public Server(int port) {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            this.authService = new BaseAuthService();
            authService.start();
            clients = new CopyOnWriteArrayList<>();
            System.out.println("Server is running on port " + port);
            while (true) {
                System.out.println("Waiting the client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client is connected.");
                new ClientHandler(this, clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Server error.");
            e.printStackTrace();
        } finally {
            if (authService != null) authService.stop();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

}
