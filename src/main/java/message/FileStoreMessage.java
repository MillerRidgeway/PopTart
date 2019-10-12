package message;

import java.io.File;

public class FileStoreMessage extends Message {
    private String fileId;
    private byte [] contents;
    private File f;

    public FileStoreMessage(String fileId, File f, byte[] contents) {
        this.fileId = fileId;
        this.contents = contents;
        this.f = f;
    }

    public String getFileId() {
        return this.fileId;
    }

    public byte [] getContents() {
        return this.contents;
    }

    public File getFile(){
        return this.f;
    }
}
