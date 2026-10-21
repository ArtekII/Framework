package autumn.mapping;

public class MapFile {
    private String filename;
    private String extension;
    private byte[] content;

    public MapFile(String filename, String extension, byte[] content) {
        this.filename = filename;
        this.extension = extension;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getExtension() {
        return extension;
    }
    public void setExtension(String extension) {
        this.extension = extension;
    }
    public byte[] getContent() {
        return content;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
}
