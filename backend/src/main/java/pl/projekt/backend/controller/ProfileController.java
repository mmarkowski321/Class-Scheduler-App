package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user.get());
    }
    
    @PutMapping("/student/{id}")
    public ResponseEntity<?> updateStudentProfile(
            @PathVariable Long id,
            @RequestBody Student studentData) {
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Student)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid student ID"));
        }
        
        Student student = (Student) userOpt.get();
        
        // Update profile fields
        if (studentData.getSchool() != null) student.setSchool(studentData.getSchool());
        if (studentData.getGrade() != null) student.setGrade(studentData.getGrade());
        if (studentData.getTrack() != null) student.setTrack(studentData.getTrack());
        if (studentData.getPhone() != null) student.setPhone(studentData.getPhone());
        if (studentData.getLanguages() != null) student.setLanguages(studentData.getLanguages());
        if (studentData.getTimezone() != null) student.setTimezone(studentData.getTimezone());
        if (studentData.getAboutMe() != null) student.setAboutMe(studentData.getAboutMe());
        if (studentData.getGoals() != null) student.setGoals(studentData.getGoals());
        if (studentData.getStrengths() != null) student.setStrengths(studentData.getStrengths());
        if (studentData.getDifficulties() != null) student.setDifficulties(studentData.getDifficulties());
        if (studentData.getPreferredSubjects() != null) student.setPreferredSubjects(studentData.getPreferredSubjects());
        if (studentData.getAvoidSubjects() != null) student.setAvoidSubjects(studentData.getAvoidSubjects());
        if (studentData.getLearningStyle() != null) student.setLearningStyle(studentData.getLearningStyle());
        if (studentData.getCity() != null) student.setCity(studentData.getCity());
        if (studentData.getMeetingMode() != null) student.setMeetingMode(studentData.getMeetingMode());
        if (studentData.getPreferredTools() != null) student.setPreferredTools(studentData.getPreferredTools());
        if (studentData.getOtherTool() != null) student.setOtherTool(studentData.getOtherTool());
        if (studentData.getPreferredDays() != null) student.setPreferredDays(studentData.getPreferredDays());
        if (studentData.getAvailabilityNote() != null) student.setAvailabilityNote(studentData.getAvailabilityNote());
        if (studentData.getGuardianName() != null) student.setGuardianName(studentData.getGuardianName());
        if (studentData.getGuardianEmail() != null) student.setGuardianEmail(studentData.getGuardianEmail());
        if (studentData.getShareProfile() != null) student.setShareProfile(studentData.getShareProfile());
        
        Student updated = userRepository.save(student);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/tutor/{id}")
    public ResponseEntity<?> updateTutorProfile(
            @PathVariable Long id,
            @RequestBody Tutor tutorData) {
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Tutor)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid tutor ID"));
        }
        
        Tutor tutor = (Tutor) userOpt.get();
        
        // Update all fields - simplified approach
        if (tutorData.getEducation() != null) tutor.setEducation(tutorData.getEducation());
        if (tutorData.getExperienceYears() != null) tutor.setExperienceYears(tutorData.getExperienceYears());
        if (tutorData.getPhotoUrl() != null) tutor.setPhotoUrl(tutorData.getPhotoUrl());
        if (tutorData.getSubjects() != null) tutor.setSubjects(tutorData.getSubjects());
        if (tutorData.getExamResults() != null) tutor.setExamResults(tutorData.getExamResults());
        if (tutorData.getHourlyRate() != null) tutor.setHourlyRate(tutorData.getHourlyRate());
        if (tutorData.getLessonDuration() != null) tutor.setLessonDuration(tutorData.getLessonDuration());
        if (tutorData.getTeachingLanguages() != null) tutor.setTeachingLanguages(tutorData.getTeachingLanguages());
        if (tutorData.getLessonModes() != null) tutor.setLessonModes(tutorData.getLessonModes());
        if (tutorData.getCity() != null) tutor.setCity(tutorData.getCity());
        if (tutorData.getTravelRadius() != null) tutor.setTravelRadius(tutorData.getTravelRadius());
        if (tutorData.getTeachingMethods() != null) tutor.setTeachingMethods(tutorData.getTeachingMethods());
        if (tutorData.getBio() != null) tutor.setBio(tutorData.getBio());
        if (tutorData.getCertificates() != null) tutor.setCertificates(tutorData.getCertificates());
        if (tutorData.getWebsite() != null) tutor.setWebsite(tutorData.getWebsite());
        if (tutorData.getLinkedIn() != null) tutor.setLinkedIn(tutorData.getLinkedIn());
        if (tutorData.getMaxLessonsPerDay() != null) tutor.setMaxLessonsPerDay(tutorData.getMaxLessonsPerDay());
        if (tutorData.getBufferTime() != null) tutor.setBufferTime(tutorData.getBufferTime());
        if (tutorData.getPreferredDays() != null) tutor.setPreferredDays(tutorData.getPreferredDays());
        if (tutorData.getCalendarUrl() != null) tutor.setCalendarUrl(tutorData.getCalendarUrl());
        
        Tutor updated = userRepository.save(tutor);
        return ResponseEntity.ok(updated);
    }
}

