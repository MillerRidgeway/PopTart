package datastore;

import message.Message;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DataStore {
    private String storageDir;
    private ArrayList<String> files;

    public DataStore(String dir){
        this.storageDir = dir;
        this.files = new ArrayList<>();
    }

    public void writeFile(int fileSize,byte[] fileContents, String filename) throws IOException {
        files.add(filename);
    }

    public String getStorageDir(){
        return this.storageDir;
    }

    public ArrayList<String> getFiles(){
        return this.files;
    }

}
