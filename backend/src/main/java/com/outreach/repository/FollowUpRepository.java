package com.outreach.repository;

import com.outreach.entity.FollowUp;
import com.outreach.entity.FollowUp.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    @Query("SELECT f FROM FollowUp f WHERE f.status = 'PENDING' AND f.scheduledAt <= :now")
    List<FollowUp> findDueFollowUps(LocalDateTime now);

    List<FollowUp> findByApplicationIdAndStatus(Long appId, FollowUpStatus status);

    /** All follow-ups for a user, newest scheduled first — used by the UI list. */
    List<FollowUp> findByUserIdOrderByScheduledAtDesc(Long userId);

    long countByUserIdAndStatus(Long userId, FollowUpStatus status);
}
