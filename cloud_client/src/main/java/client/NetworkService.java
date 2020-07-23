package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkService {

    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public static final String EXIT_COMMAND = "./exit";
    public static final String AUTH_COMMAND = "./auth";
    public static final String UPLOAD_COMMAND = "./upload";
    public static final String DOWNLOAD_COMMAND = "./download";
    public static final String DIR_COMMAND = "./dir";
    public static final String END_OF_LIST_STRING = " - end of dir - ";


    public NetworkService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public String sendAuthMessage(String login, String password) throws IOException {
        out.writeUTF(String.format("%s %s %s", AUTH_COMMAND, login, password));
        return in.readUTF();
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public String readMessage() throws IOException {
        return in.readUTF();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public void writeLong(long i) throws IOException {
        out.writeLong(i);
    }

    public DataInputStream GetDataInputStream(){
        return in;
    }

    public DataOutputStream GetDataOutputStream(){
        return out;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
