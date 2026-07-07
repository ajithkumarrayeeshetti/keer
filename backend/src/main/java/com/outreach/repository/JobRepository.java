package com.outreach.repository;

import com.outreach.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByUserIdOrderByMatchScoreDesc(Long userId);

    List<Job> findByUserIdAndStatus(Long userId, Job.JobStatus status);

    long countByUserId(Long userId);
}
