package autumn.mapping;

public class MapFile {
    String filename;
    String extension;
    Byte[] content;

    public MapFile(String filename, String extension, Byte[] content) {
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

    public Byte[] getContent() {
        return content;
    }

    public void setContent(Byte[] content) {
        this.content = content;
    }

    
}
