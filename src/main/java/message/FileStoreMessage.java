package message;

import java.io.File;

public class FileStoreMessage extends Message {
    private String fileId;
    private File file;
    private DiscoverMessage info;

    public FileStoreMessage(String fileId, File f, DiscoverMessage info) {
        this.fileId = fileId;
        this.file = f;
        this.info = info;
    }

    public String getFileId() {
        return this.fileId;
    }

    public File getFile() {
        return this.file;
    }

    public DiscoverMessage getInfo() {
        return this.info;
    }
}
