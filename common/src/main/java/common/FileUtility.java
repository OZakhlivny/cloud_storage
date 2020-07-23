package common;

import java.io.*;

public class FileUtility {

    public static File createFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public static File createDirectory(String dirName) throws IOException {
        File file = new File(dirName);
        if (!file.exists()) {
            file.mkdir();
        }

        return file;
    }
}