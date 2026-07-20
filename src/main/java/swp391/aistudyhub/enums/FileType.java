package swp391.aistudyhub.enums;

public enum FileType {
    pdf("pdf"),
    docx("docx"),
    pptx("pptx"),
    ppt("ppt"),
    jpg("jpg"),
    png("png"),
    mp4("mp4"),
    zip("zip"),
    txt("txt"),
    jpeg("jpeg"),
    doc("doc"),
    xlsx("xlsx"),
    xls("xls");
    private final String extension;

    // Constructor
    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
