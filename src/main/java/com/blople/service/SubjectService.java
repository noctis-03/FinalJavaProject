package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.SemesterDAO;
import com.blople.database.dao.SubjectDAO;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 학기·과목 관리 서비스.
 */
public class SubjectService {

    private final SemesterDAO semesterDAO;
    private final SubjectDAO  subjectDAO;
    private final EventBus    eventBus = EventBus.getInstance();

    public SubjectService(SemesterDAO semesterDAO, SubjectDAO subjectDAO) {
        this.semesterDAO = semesterDAO;
        this.subjectDAO  = subjectDAO;
    }

    // ===== Semester =====
    public Semester createSemester(String name, LocalDate startDate, LocalDate endDate) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("학기명을 입력해주세요.");
        Semester s = new Semester(name, startDate, endDate);
        semesterDAO.insert(s);
        return s;
    }

    public void setActiveSemester(String semesterId) {
        semesterDAO.setActive(semesterId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, "active-semester-changed");
    }

    public Optional<Semester> getActiveSemester() {
        return semesterDAO.findActive();
    }

    public List<Semester> getAllSemesters() {
        return semesterDAO.findAll();
    }

    /** 활성 학기가 없으면 "기본 학기" 를 만들고 활성화. */
    public Semester ensureActiveSemester() {
        Optional<Semester> active = getActiveSemester();
        if (active.isPresent()) return active.get();

        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int term  = today.getMonthValue() <= 6 ? 1 : 2;
        Semester s = createSemester(year + "-" + term + "학기",
                today,
                today.plusWeeks(16));
        semesterDAO.setActive(s.getId());
        s.setActive(true);
        return s;
    }

    // ===== Subject =====
    public Subject createSubject(String semesterId, String name, String professor,
                                 String classroom, String color) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("과목명을 입력해주세요.");

        // 같은 학기 내 동일 이름 과목 방지
        Optional<Subject> dup = subjectDAO.findByNameInSemester(semesterId, name);
        if (dup.isPresent()) return dup.get();  // 이미 있으면 그것을 그대로 반환

        Subject s = new Subject(semesterId, name, color);
        s.setProfessor(professor);
        s.setClassroom(classroom);
        subjectDAO.insert(s);
        eventBus.publish(EventBus.SUBJECT_CHANGED, s);
        return s;
    }

    public void updateSubject(Subject subject) {
        subjectDAO.update(subject);
        eventBus.publish(EventBus.SUBJECT_CHANGED, subject);
    }

    public void deleteSubject(String subjectId) {
        // ON DELETE CASCADE 로 timetable_entry, assignment 등도 함께 정리됨
        subjectDAO.delete(subjectId);
        eventBus.publish(EventBus.SUBJECT_CHANGED, subjectId);
    }

    public List<Subject> getSubjectsBySemester(String semesterId) {
        return subjectDAO.findBySemester(semesterId);
    }

    public Optional<Subject> getSubjectById(String subjectId) {
        return subjectDAO.findById(subjectId);
    }
}
