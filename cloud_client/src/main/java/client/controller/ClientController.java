package client.controller;

import client.CompileType;
import client.NetworkService;
import common.FileUtility;

import java.io.*;

public class ClientController {

    private final NetworkService networkService;
    private BufferedReader input;

    public ClientController(String serverHost, int serverPort) {
        this.networkService = new NetworkService(serverHost, serverPort);
    }

    public void runApplication() throws IOException {
        connectToServer();
        input = new BufferedReader(new InputStreamReader(System.in));
        if(runAuthProcess()) readCommands();
        input.close();
        shutdown();
    }

    private boolean runAuthProcess() throws IOException {
        boolean result = false;
        System.out.print("\nInput login: ");
        String login = null, password = null;
        login = input.readLine().trim();
        System.out.print("\nInput password: ");
        password = input.readLine().trim();
        String message = sendAuthMessage(login, password);
        if(message.startsWith(NetworkService.AUTH_COMMAND)){
            result = true;
            System.out.println("Login successful");
        }
        return result;
    }


    private void connectToServer() throws IOException {
        try {
            networkService.connect();
        } catch (IOException e) {
            System.err.println("Failed to establish server connection");
            throw e;
        }
    }

    public String sendAuthMessage(String login, String pass) throws IOException {
        return networkService.sendAuthMessage(login, pass);
    }

    public void readCommands(){
        String fileName = null;
        try {
            while (true) {
                System.out.println("Input command (./download, ./upload, ./dir, ./exit):");
                String command = input.readLine();
                networkService.sendMessage(command);

                if (command.equals(NetworkService.DOWNLOAD_COMMAND)) {
                    System.out.println("Input filename:");
                    fileName = input.readLine();
                    System.out.println("Path for download:");
                    String destinationPath = input.readLine();
                    downloadFile(fileName, destinationPath);
                } else if (command.equals(NetworkService.UPLOAD_COMMAND)) {
                    System.out.println("Input filename:");
                    fileName = input.readLine();
                    uploadFile(fileName);
                } else if (command.equals(NetworkService.DIR_COMMAND)) {
                    printFileList();
                } else if (command.equals(NetworkService.EXIT_COMMAND)) {
                    break;
                }
            }
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    public void downloadFile(String fileName, String destinationPath) throws IOException, InterruptedException {
        networkService.sendMessage(fileName);
        long fileSize = networkService.readLong();
        File destination = FileUtility.createDirectory(destinationPath);
        File downloadFile = FileUtility.createFile(destination.getAbsolutePath() + "/" + fileName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(downloadFile)){
            byte[] bufferIn = new byte[8192];
            for(long i = 0l, nbytes = 0l; i < fileSize; ){
                nbytes = networkService.GetDataInputStream().read(bufferIn);
                if(nbytes > 0) {
                    fileOutputStream.write(bufferIn, 0, (int) nbytes);
                    i += nbytes;
                } else break;
            }
            fileOutputStream.flush();
        }catch (IOException e){
            System.out.println("Error during download");
            e.printStackTrace();
        }
        System.out.println("Download is completed");
    }

    public void uploadFile(String fileName) throws IOException, InterruptedException {
        File uploadFile = FileUtility.createFile(fileName);
        networkService.sendMessage(uploadFile.getName());

        long fileSize = uploadFile.length();
        networkService.writeLong(fileSize);
        if(!CompileType.isIO) Thread.sleep(2000);
        try (FileInputStream fileInputStream = new FileInputStream(uploadFile)) {
            byte[] bufferOut = new byte[8192];
            for(long i = 0l, nbytes = 0l; i < fileSize; ){
                nbytes = fileInputStream.read(bufferOut);
                if(nbytes > 0) {
                    networkService.GetDataOutputStream().write(bufferOut, 0, (int) nbytes);
                    i += nbytes;
                } else break;
            }
            networkService.GetDataOutputStream().flush();
        } catch (IOException e) {
            System.out.println("Error during upload");
            e.printStackTrace();
        }
        System.out.println("Upload is completed ");
    }

    public void printFileList() throws IOException {
        String filelist = null;
        System.out.println("Cloud dir:");
        if(CompileType.isIO) {
            do {
                filelist = networkService.readMessage();
                System.out.println(filelist);
            } while (!filelist.equals(NetworkService.END_OF_LIST_STRING));
        }
        else{
            filelist = networkService.readMessage();
            System.out.println(filelist);
        }
    }

    public void shutdown() {
        networkService.close();
    }
}
