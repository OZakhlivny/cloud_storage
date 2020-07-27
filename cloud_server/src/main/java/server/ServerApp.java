package server;

public class ServerApp {
    private static final int DEFAULT_PORT = 8189;

    public static void main(String[] args) {

        new Server(DEFAULT_PORT);
        //new Thread(new NIOServer()).start();
    }
}
