package com.resumeanalyzer.repository;

import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserOrderByCreatedAtDesc(User user);
    
    long countByUser(User user);
    
    List<Resume> findTop5ByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT AVG(r.atsScore) FROM Resume r WHERE r.user = :user")
    Double getAverageScoreByUser(@Param("user") User user);
    
    @Query("SELECT AVG(r.atsScore) FROM Resume r")
    Double getAverageAtsScore();
    
    List<Resume> findByUserAndStatus(User user, String status);
    
    List<Resume> findByUserOrderByVersionNumberAsc(User user);
    
    List<Resume> findTop10ByOrderByCreatedAtDesc();

}
