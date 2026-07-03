package swp391.aistudyhub.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import swp391.aistudyhub.enums.FileType;

@Converter(autoApply = true)
public class FileTypeConverter implements AttributeConverter<FileType, String> {

    @Override
    public String convertToDatabaseColumn(FileType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public FileType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return FileType.valueOf(dbData.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
