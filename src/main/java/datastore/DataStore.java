package datastore;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DataStore {
    private String storageDir;
    private ArrayList<String> files;

    public DataStore(String dir) {
        this.storageDir = dir;
        this.files = new ArrayList<>();
    }

    public void writeFile(String fileName, Object contents) throws IOException {
        files.add(fileName);
        File newFile = new File(storageDir + fileName);
        FileOutputStream fs = new FileOutputStream(newFile);
        fs.write((byte[]) contents);
        fs.close();
    }

    public ArrayList<File> getFilesForSending() {
        ArrayList<File> listAsFiles = new ArrayList<>();
        for (String s : files) {
            File f = new File(s);
            listAsFiles.add(f);
        }
        return listAsFiles;
    }

    public void deleteFile(String fileName) {
        File f = new File(fileName);
        f.delete();
        this.files.remove(fileName);
    }

    public String getStorageDir() {
        return this.storageDir;
    }

    public ArrayList<String> getFiles() {
        return this.files;
    }

}
