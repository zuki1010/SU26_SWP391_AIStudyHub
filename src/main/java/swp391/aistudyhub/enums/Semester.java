package swp391.aistudyhub.enums;

public enum Semester {
    SEMESTER_1("Học kỳ 1"),
    SEMESTER_2("Học kỳ 2"),
    SEMESTER_3("Học kỳ 3"),
    SEMESTER_4("Học kỳ 4"),
    SEMESTER_5("Học kỳ 5"),
    SEMESTER_6("Học kỳ 6"),
    SEMESTER_7("Học kỳ 7"),
    SEMESTER_8("Học kỳ 8"),
    SEMESTER_9("Học kỳ 9");

    private final String displayName;

    Semester(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
