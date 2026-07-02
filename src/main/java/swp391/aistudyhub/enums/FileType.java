package swp391.aistudyhub.enums;

public enum FileType {
    PDF("pdf"),
    DOCX("docx"),
    PPTX("pptx"),
    PPT("ppt"),
    JPG("jpg"),
    PNG("png"),
    MP4("mp4"),
    ZIP("zip"),
    TXT("txt"),
    JPEG("jpeg"),
    DOC("doc");

    private final String extension;

    // Constructor
    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
