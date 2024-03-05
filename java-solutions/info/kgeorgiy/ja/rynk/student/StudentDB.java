package info.kgeorgiy.ja.rynk.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private static final Comparator<Student> STUDENT_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName).reversed()
            .thenComparing(Student::compareTo);

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private static <T> T getFilterSortedCollect(Collection<Student> students,
                                                Predicate<Student> pred,
                                                Collector<Student, ?, T> collector) {
        return students.stream().filter(pred).sorted(STUDENT_COMPARATOR).collect(collector);
    }

    private static List<Student> getFilterSortedToList(Collection<Student> students,
                                                Predicate<Student> pred) {
        return getFilterSortedCollect(students, pred, Collectors.toList());
    }


    private static <S, T extends Collection<S>> T getMapCollect(Collection<Student> students,
                                                                Function<Student, S> mapper,
                                                                Collector<S, ?, T> collector) {
        return students.stream().map(mapper).collect(collector);
    }

    private static <T> Predicate<Student> compareBySth(Function<Student, T> func, T sth) {
        return (student -> func.apply(student).equals(sth));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getMapCollect(students, Student::getFirstName, Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getMapCollect(students, Student::getLastName, Collectors.toList());
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getMapCollect(students, Student::getGroup, Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getMapCollect(students, this::getFullName, Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getMapCollect(students, Student::getFirstName, Collectors.toUnmodifiableSet());
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.naturalOrder()).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(STUDENT_COMPARATOR).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getFilterSortedToList(students, compareBySth(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getFilterSortedToList(students, compareBySth(Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return getFilterSortedToList(students, compareBySth(Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return getFilterSortedCollect(
                students, compareBySth(Student::getGroup, group),
                Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(Comparator.naturalOrder()))
        );
    }
}
