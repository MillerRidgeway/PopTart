package message;

import java.io.File;

public class FileStoreMessage extends Message {
    private String fileId;
    private byte [] contents;
    private File f;
    private DiscoverMessage info;

    public FileStoreMessage(String fileId, File f, byte[] contents, DiscoverMessage info) {
        this.fileId = fileId;
        this.contents = contents;
        this.info = info;
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

    public DiscoverMessage getInfo() {
        return this.info;
    }
}
