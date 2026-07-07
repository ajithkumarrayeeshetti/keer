package com.outreach.service;

import com.outreach.entity.Job;
import com.outreach.entity.User;
import com.outreach.repository.JobRepository;
import com.outreach.util.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final CsvParser csvParser;

    public record UploadResult(List<Job> jobs, List<String> errors) {}

    /**
     * Parse and save jobs from a CSV upload.
     * Returns both saved jobs and any per-row validation errors so the
     * UI can display them instead of silently skipping bad rows.
     *
     * @throws IllegalArgumentException if required columns are missing entirely
     */
    public UploadResult uploadJobs(MultipartFile file, User user) throws IOException {
        CsvParser.ParseResult parsed = csvParser.parseJobs(file.getInputStream(), user);
        List<Job> saved = jobRepository.saveAll(parsed.jobs());
        if (!parsed.errors().isEmpty()) {
            log.warn("CSV upload for user {} had {} row error(s): {}", user.getId(), parsed.errors().size(), parsed.errors());
        }
        return new UploadResult(saved, parsed.errors());
    }

    public List<Job> getJobsByUser(User user) {
        return jobRepository.findByUserIdOrderByMatchScoreDesc(user.getId());
    }

    public Job getJobById(Long id, User user) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (!job.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        return job;
    }

    public Job updateJobStatus(Long id, Job.JobStatus status, User user) {
        Job job = getJobById(id, user);
        job.setStatus(status);
        return jobRepository.save(job);
    }

    public void deleteJob(Long id, User user) {
        jobRepository.delete(getJobById(id, user));
    }
}
