package server.client;

import common.FileUtility;
import server.Server;
import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String serverRootDir = "/Users/oleg/Desktop/GeekBrains/Cloud";
    private File userDir;

    private String username;

    public static final String EXIT_COMMAND = "./exit";
    public static final String AUTH_COMMAND = "./auth";
    public static final String UPLOAD_COMMAND = "./upload";
    public static final String DOWNLOAD_COMMAND = "./download";
    public static final String DIR_COMMAND = "./dir";
    public static final String END_OF_LIST_STRING = " - end of dir - ";

    public ClientHandler(Server server, Socket socket){
        try {
            this.server = server;
            this.clientSocket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.username = "";
            new Thread(() -> {
                try {
                    authentication();
                    userDir = new File(serverRootDir + "/" + username);
                    if (!userDir.exists()) userDir.mkdir();
                    readCommands();
                } catch (IOException e) {
                    System.out.println("Connection with client " + username + " was closed!");
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        server.unsubscribe(this);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCommands() throws IOException {
        while (true) {
            String command = in.readUTF();
            System.out.printf("From %s: %s%n", username, command);

            if (EXIT_COMMAND.equals(command)) return;

            if(command.startsWith(UPLOAD_COMMAND)) uploadFile();
            else if (command.startsWith(DOWNLOAD_COMMAND)) downloadFile();
            else if (command.startsWith(DIR_COMMAND)) cloudDir();
            else System.out.printf("Unknown command: %s%n", command);
        }
    }

    private void downloadFile() throws IOException {
        String fileName = in.readUTF();
        File downloadFile = FileUtility.createFile(userDir + "/" + fileName);
        long fileSize = downloadFile.length();
        out.writeLong(fileSize);
        byte[] buffer = new byte[8192];
        FileInputStream fileInputStream = new FileInputStream(downloadFile);
        for (long i = 0; i <= fileSize / 8192; i++) {
            int nbytes = fileInputStream.read(buffer);
            out.write(buffer, 0, nbytes);
            out.flush();
        }
        fileInputStream.close();
        out.flush();
    }

    private void uploadFile() throws IOException {
        String fileName = in.readUTF();
        long fileSize = in.readLong();
        byte[] buffer = new byte[8192];
        File uploadFile = new File(userDir + "/" + fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(uploadFile);
        for (long i = 0; i <= fileSize / 8192; i++) {
            int nbytes = in.read(buffer);
            fileOutputStream.write(buffer, 0, nbytes);
            fileOutputStream.flush();
        }
        fileOutputStream.close();
        if (uploadFile.length() != fileSize) out.writeUTF("Error during file " + fileName + " upload!");
        else out.writeUTF("File " + fileName + " was uploaded.");
        out.flush();
    }

    private void cloudDir() throws IOException {
        File cloudDir = FileUtility.createDirectory(serverRootDir + "/" + username);
        File[] files = cloudDir.listFiles();
        for(File file:files) out.writeUTF(file.getName());
        out.writeUTF(END_OF_LIST_STRING);
    }

    private void authentication() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_COMMAND)) {
                String[] messageParts = message.split("\\s");
                String name = server.getAuthService().getUsernameByLoginAndPassword(messageParts[1], messageParts[2]);
                if (name == null) {
                    System.out.println("Incorrect login or password!");
                    sendMessage("Incorrect login or password!");
                } else {
                    username = name;
                    sendMessage(AUTH_COMMAND + " " + username);
                    server.subscribe(this);
                    break;
                }
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }
}
