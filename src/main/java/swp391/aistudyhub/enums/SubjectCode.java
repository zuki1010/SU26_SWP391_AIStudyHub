package swp391.aistudyhub.enums;

public enum SubjectCode {
    PRF192(Semester.SEMESTER_1),
    MAE101(Semester.SEMESTER_1),
    CEA201(Semester.SEMESTER_1),
    CSI104(Semester.SEMESTER_1),
    SSG104(Semester.SEMESTER_2),
    PRO192(Semester.SEMESTER_2),
    MAD101(Semester.SEMESTER_2),
    OSG202(Semester.SEMESTER_2),
    CSD201(Semester.SEMESTER_3),
    DBI202(Semester.SEMESTER_3),
    LAB211(Semester.SEMESTER_3),
    JPD113(Semester.SEMESTER_3),
    JPD123(Semester.SEMESTER_4),
    PRJ301(Semester.SEMESTER_4),
    MAS291(Semester.SEMESTER_4),
    SWR302(Semester.SEMESTER_5),
    SWT301(Semester.SEMESTER_5),
    PRN212(Semester.SEMESTER_5),
    OTHER(Semester.OTHERS);
    private final Semester semester;

    SubjectCode(Semester semester) {
        this.semester = semester;
    }

    public Semester getSemester() {
        return semester;
    }
}
