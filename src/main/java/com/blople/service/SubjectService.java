package com.blople.service;

import com.blople.common.EventBus;
import com.blople.database.dao.SemesterDAO;
import com.blople.database.dao.SubjectDAO;
import com.blople.model.subject.Semester;
import com.blople.model.subject.Subject;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SubjectService {

    private final SemesterDAO semesterDAO;
    private final SubjectDAO  subjectDAO;
    private final EventBus    eventBus = EventBus.getInstance();

    public SubjectService(SemesterDAO semesterDAO, SubjectDAO subjectDAO) {
        this.semesterDAO = semesterDAO;
        this.subjectDAO  = subjectDAO;
    }

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

    /** 활성 학기 보장 — 없으면 현재 시기에 맞춰 자동 생성. */
    public Semester ensureActiveSemester() {
        Optional<Semester> active = getActiveSemester();
        if (active.isPresent()) return active.get();

        LocalDate today = LocalDate.now();
        Semester.Term term = (today.getMonthValue() <= 7) ? Semester.Term.FIRST : Semester.Term.SECOND;
        return ensureSemester(today.getYear(), term, true);
    }

    /**
     * 지정한 연도/학기에 해당하는 Semester 를 보장(없으면 생성).
     * @param activate true 면 활성 학기로 전환
     */
    public Semester ensureSemester(int year, Semester.Term term, boolean activate) {
        String name = year + "-" + term.number + "학기";
        Optional<Semester> existing = semesterDAO.findAll().stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst();
        Semester s;
        if (existing.isPresent()) {
            s = existing.get();
        } else {
            s = createSemester(name, term.defaultStart(year), term.defaultEnd(year));
        }
        if (activate) {
            semesterDAO.setActive(s.getId());
            s.setActive(true);
            eventBus.publish(EventBus.SUBJECT_CHANGED, "active-semester-changed");
        }
        return s;
    }

    public Subject createSubject(String semesterId, String name, String professor,
                                 String classroom, String color) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("과목명을 입력해주세요.");

        Optional<Subject> dup = subjectDAO.findByNameInSemester(semesterId, name);
        if (dup.isPresent()) return dup.get();

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
