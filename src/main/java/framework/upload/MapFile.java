package framework.upload;

public class MapFile {

    private String fileName;
    private String extension;
    private byte[] data;

    public MapFile(String fileName, String extension, byte[] data) {
        this.fileName = fileName;
        this.extension = extension;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getExtension() {
        return extension;
    }
    public void setExtension(String extension) {
        this.extension = extension;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    public int getSize() {
        if (data == null) {
            return 0;
        }
        return data.length;
    }
}