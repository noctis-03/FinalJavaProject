package com.blople.model.subject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 과목 모델. subject 테이블과 매핑.
 */
public class Subject {

    public static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String semesterId;
    private String name;
    private String professor;
    private String classroom;
    private String color;      // #RRGGBB
    private int    sortOrder;
    private String createdAt;

    public Subject(String semesterId, String name, String color) {
        this.id         = UUID.randomUUID().toString();
        this.semesterId = semesterId;
        this.name       = name;
        this.color      = color;
        this.sortOrder  = 0;
        this.createdAt  = LocalDateTime.now().format(TS_FMT);
    }

    /** DAO 복원용. */
    public static Subject reconstruct(String id, String semesterId, String name,
                                      String professor, String classroom, String color,
                                      int sortOrder, String createdAt) {
        Subject s = new Subject(semesterId, name, color);
        s.id         = id;
        s.professor  = professor;
        s.classroom  = classroom;
        s.sortOrder  = sortOrder;
        s.createdAt  = createdAt;
        return s;
    }

    public String getId()                          { return id; }
    public String getSemesterId()                  { return semesterId; }
    public String getName()                        { return name; }
    public void   setName(String name)             { this.name = name; }
    public String getProfessor()                   { return professor; }
    public void   setProfessor(String p)           { this.professor = p; }
    public String getClassroom()                   { return classroom; }
    public void   setClassroom(String c)           { this.classroom = c; }
    public String getColor()                       { return color; }
    public void   setColor(String c)               { this.color = c; }
    public int    getSortOrder()                   { return sortOrder; }
    public void   setSortOrder(int o)              { this.sortOrder = o; }
    public String getCreatedAt()                   { return createdAt; }

    @Override
    public String toString() {
        return "Subject{name=" + name + ", color=" + color + "}";
    }
}
