package com.outreach.repository;

import com.outreach.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    List<Email> findByUserIdAndStatus(Long userId, Email.EmailStatus status);

    Optional<Email> findByJobId(Long jobId);

    Optional<Email> findByTrackingToken(String trackingToken);

    long countByUserIdAndStatus(Long userId, Email.EmailStatus status);

    @Query("SELECT e FROM Email e WHERE e.status = 'FAILED' AND (e.retryCount IS NULL OR e.retryCount < 3)")
    List<Email> findRetryableFailures();
}
