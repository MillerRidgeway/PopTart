package message;

import java.io.File;

public class FileStoreMessage extends Message {
    private String fileId;
    private Object contents;
    private File f;

    public FileStoreMessage(String fileId, File f, Object contents) {
        this.fileId = fileId;
        this.contents = contents;
        this.f = f;
    }

    public String getFileId() {
        return this.fileId;
    }

    public Object getContents() {
        return this.contents;
    }

    public File getFile(){
        return this.f;
    }
}
