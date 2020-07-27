package server.NIO;

import server.auth.AuthService;
import server.auth.BaseAuthService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.stream.Stream;

public class NIOServer implements Runnable{
    private ServerSocketChannel server;
    private Selector selector;
    private AuthService authService;
    private String username;
    private final String serverRootDir = "/Users/oleg/Desktop/GeekBrains/Cloud";

    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        this.authService = new BaseAuthService();
        authService.start();
        username = null;
    }

    @Override
    public void run() {
        try {
            System.out.println("server started");
            while (server.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        System.out.println("client accepted");
                        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int count = ((SocketChannel)key.channel()).read(buffer);
                        if (count == -1) {
                            key.channel().close();
                            break;
                        }
                        buffer.flip();
                        StringBuilder s = new StringBuilder();
                        while (buffer.hasRemaining()) {
                            s.append((char)buffer.get());
                        }
                        System.out.println(s);
                        if(s.toString().startsWith("./exit")){
                            key.channel().close();
                            break;
                        }
                        String result = processCommand(s.toString(), (SocketChannel)key.channel());
                        ((SocketChannel) key.channel()).write(ByteBuffer.wrap(result.getBytes()));
                        /*for (SelectionKey key1 : selector.keys()) {
                            if (key1.channel() instanceof SocketChannel && key1.isReadable()) {
                                ((SocketChannel) key1.channel()).write(ByteBuffer.wrap(s.toString().getBytes()));
                            }
                        }*/

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String processCommand(String command, SocketChannel channel) throws IOException {
        String result;
        if(command.startsWith("./auth")) result = authentication(command);
        else if(command.startsWith("./dir")) result = cloudDir();
        else if(command.startsWith(("./upload"))) result = uploadFile(channel);
        else if(command.startsWith(("./download"))) result = downloadFile(channel);
        else result = "Unknown command: " + command;

        return result;
    }

    private String downloadFile(SocketChannel channel) throws IOException {
        String result = "Error during downloading file";
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int count;
        do {
            count = channel.read(buffer);
            if(count == -1) return result;
        } while (count == 0);
        buffer.flip();
        StringBuilder filename = new StringBuilder();
        while (buffer.hasRemaining()) filename.append((char)buffer.get());
        buffer.clear();
        Path path = Paths.get(serverRootDir + "/" + username + "/" + filename);
        long fileSize = 0l;
        if(Files.exists(path)) fileSize = Files.size(path);
        else return result;
        buffer.putLong(fileSize); buffer.rewind();
        channel.write(buffer);
        buffer.clear();
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        long nbytes = 0;
        do {
            count = fileChannel.read(buffer);
            buffer.flip();
            if(count == -1) break;
            nbytes += count;
            channel.write(buffer);
            buffer.clear();
        }while(nbytes < fileSize);
        fileChannel.close();
        result = "File " + filename.toString() + " was downloaded.";

        return result;
    }

    private String uploadFile(SocketChannel channel) throws IOException {
        String result = "Error during uploading file";
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int count;
        do {
            count = channel.read(buffer);
            if(count == -1) return result;
        } while (count == 0);
        buffer.flip();
        StringBuilder filename = new StringBuilder();
        while (buffer.hasRemaining()) filename.append((char)buffer.get());
        buffer.clear();
        do{
            count = channel.read(buffer);
            if(count == -1) return result;
        }while (count == 0);
        buffer.rewind();
        long fileSize = buffer.getLong();
        System.out.println("Filename:" + filename + " filesize: " + fileSize);
        buffer.clear();
        Path path = Paths.get(serverRootDir + "/" + username);
        try {
            if (!Files.exists(path)) Files.createDirectory(path);
            path = Paths.get(path.toString(), filename.toString());
            if (Files.exists(path)) Files.delete(path);
            Files.createFile(path);
            long nbytes = 0;
            do {
                do {
                    count = channel.read(buffer);
                    if(count == -1) return result;
                } while (count == 0);
                buffer.flip();
                while(buffer.hasRemaining())
                    Files.write(path, new byte[]{buffer.get()}, StandardOpenOption.APPEND);
                nbytes += count;
                buffer.clear();
            }while (nbytes < fileSize);
            result = "File " + filename.toString() + " was uploaded.";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String cloudDir() throws IOException {
        String result = "Directory is empty";
        Path path = Paths.get(serverRootDir + "/" + username);
        if(Files.exists(path)){
            Stream<Path> list = Files.list(path);
            StringBuilder s = new StringBuilder();
            list.forEach((p) ->s.append(p.getFileName() + "\n"));
            result = s.toString();
        }
        return result;
    }

    private String authentication(String command){
        String[] messageParts = command.split("\\s");
        String name = authService.getUsernameByLoginAndPassword(messageParts[1], messageParts[2]);
        if (name == null) {
            System.out.println("Incorrect login or password!");
            return "Incorrect login or password!";
        } else {
            username = name;
            return "./auth" + " " + username;
            //server.subscribe(this);
        }

    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }
}
