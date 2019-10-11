package datastore;

import message.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class DataStore {
    private String storageDir;
    private ArrayList<String> files;

    public DataStore(String dir) {
        this.storageDir = dir;
        this.files = new ArrayList<>();
    }

    public void writeFile(String fileName, byte[] contents) throws IOException {
        File newFile = new File(storageDir + fileName);
        FileOutputStream fs = new FileOutputStream(newFile);
        fs.write(contents);
        fs.close();
    }

    public String getStorageDir() {
        return this.storageDir;
    }

    public ArrayList<String> getFiles() {
        return this.files;
    }

}
