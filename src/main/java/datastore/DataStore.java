package datastore;

import java.io.IOException;
import java.util.ArrayList;

public class DataStore {
    private String dir;
    private ArrayList<String> files;
    public DataStore(String dir){
        this.dir = dir;
        this.files = new ArrayList<>();
    }

    public void writeFile(String filename) throws IOException {

    }

    public String getDir(){
        return this.dir;
    }

    public ArrayList<String> getFiles(){
        return this.files;
    }

}
