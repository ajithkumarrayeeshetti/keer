package com.outreach.repository;

import com.outreach.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
