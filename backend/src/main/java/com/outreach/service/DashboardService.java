package com.outreach.service;

import com.outreach.dto.response.DashboardStatsResponse;
import com.outreach.entity.*;
import com.outreach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository         jobRepository;
    private final EmailRepository       emailRepository;
    private final ApplicationRepository applicationRepository;
    private final FollowUpRepository    followUpRepository;
    private final ReplyRepository       replyRepository;

    public DashboardStatsResponse getStats(User user) {
        Long userId = user.getId();

        long totalJobs    = jobRepository.countByUserId(userId);
        long generated    = emailRepository.countByUserIdAndStatus(userId, Email.EmailStatus.DRAFT)
                          + emailRepository.countByUserIdAndStatus(userId, Email.EmailStatus.APPROVED);
        long sent         = emailRepository.countByUserIdAndStatus(userId, Email.EmailStatus.SENT);
        long totalApps    = applicationRepository.countByUserId(userId);
        long interviews   = applicationRepository.countByUserIdAndStatus(userId, Application.ApplicationStatus.INTERVIEW);
        long rejections   = applicationRepository.countByUserIdAndStatus(userId, Application.ApplicationStatus.REJECTED);
        long replies      = replyRepository.countByUserIdAndClassification(userId, Reply.ReplyClassification.POSITIVE)
                          + replyRepository.countByUserIdAndClassification(userId, Reply.ReplyClassification.INTERVIEW)
                          + replyRepository.countByUserIdAndClassification(userId, Reply.ReplyClassification.REJECTION);
        long followupsDue = followUpRepository.countByUserIdAndStatus(userId, FollowUp.FollowUpStatus.PENDING);

        double responseRate = totalApps > 0 ? Math.round((double) replies / totalApps * 100.0) : 0.0;

        return DashboardStatsResponse.builder()
                .totalJobsUploaded((int) totalJobs)
                .emailsGenerated((int) generated)
                .emailsSent((int) sent)
                .replies((int) replies)
                .interviews((int) interviews)
                .rejections((int) rejections)
                .followUpsPending((int) followupsDue)
                .responseRate(responseRate)
                .build();
    }
}
