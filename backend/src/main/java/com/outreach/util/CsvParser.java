package com.outreach.util;

import com.outreach.entity.Job;
import com.outreach.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class CsvParser {

    /** Columns that must be present and non-empty to parse a row. */
    private static final Set<String> REQUIRED_COLUMNS = Set.of("Company", "Role", "HR_Email");

    /** All columns the parser understands. */
    private static final String[] KNOWN_HEADERS = {
        "Company", "Role", "Match_Score", "Location", "Job_Type",
        "HR_Name", "HR_Email", "Recruiter_Name", "Recruiter_Linkedin",
        "Company_Website", "Company_Size", "Tech_Stack", "Job_Description",
        "Why_Match", "Suggested_Subject", "Application_Link"
    };

    public record ParseResult(List<Job> jobs, List<String> errors) {}

    /**
     * Parse jobs from CSV.
     * Missing required columns → throws immediately with a clear message.
     * Individual rows that fail validation are skipped and collected in errors.
     */
    public ParseResult parseJobs(InputStream inputStream, User user) throws IOException {
        List<Job>    jobs   = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // ── Validate that required columns exist ──────────────────────────
            Set<String> headerKeys = parser.getHeaderMap().keySet();
            List<String> missing = REQUIRED_COLUMNS.stream()
                    .filter(req -> headerKeys.stream().noneMatch(h -> h.equalsIgnoreCase(req)))
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                    "CSV is missing required column(s): " + String.join(", ", missing)
                    + ". Required columns are: " + String.join(", ", REQUIRED_COLUMNS)
                    + ". Found columns: " + String.join(", ", headerKeys));
            }

            // ── Parse rows ─────────────────────────────────────────────────────
            for (CSVRecord record : parser) {
                long lineNum = parser.getCurrentLineNumber();
                try {
                    String company  = getRequired(record, "Company",  lineNum);
                    String role     = getRequired(record, "Role",     lineNum);
                    String hrEmail  = getRequired(record, "HR_Email", lineNum);

                    // Basic email format check
                    if (!hrEmail.contains("@")) {
                        errors.add("Row " + lineNum + ": HR_Email '" + hrEmail + "' is not a valid email address — skipped.");
                        continue;
                    }

                    Job job = Job.builder()
                            .user(user)
                            .company(company)
                            .role(role)
                            .matchScore(parseIntSafe(getField(record, "Match_Score")))
                            .location(getField(record, "Location"))
                            .jobType(getField(record, "Job_Type"))
                            .hrName(getField(record, "HR_Name"))
                            .hrEmail(hrEmail)
                            .recruiterName(getField(record, "Recruiter_Name"))
                            .recruiterLinkedin(getField(record, "Recruiter_Linkedin"))
                            .companyWebsite(getField(record, "Company_Website"))
                            .companySize(getField(record, "Company_Size"))
                            .techStack(getField(record, "Tech_Stack"))
                            .jobDescription(getField(record, "Job_Description"))
                            .whyMatch(getField(record, "Why_Match"))
                            .suggestedSubject(getField(record, "Suggested_Subject"))
                            .applicationLink(getField(record, "Application_Link"))
                            .status(Job.JobStatus.PENDING)
                            .build();
                    jobs.add(job);

                } catch (IllegalArgumentException e) {
                    errors.add(e.getMessage());
                } catch (Exception e) {
                    errors.add("Row " + lineNum + ": unexpected error — " + e.getMessage() + " (skipped)");
                    log.warn("Skipping CSV row {}: {}", lineNum, e.getMessage());
                }
            }
        }
        return new ParseResult(jobs, errors);
    }

    private String getRequired(CSVRecord record, String name, long line) {
        String value = getField(record, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "Row " + line + ": required column '" + name + "' is empty — skipped.");
        }
        return value;
    }

    private String getField(CSVRecord record, String name) {
        try {
            String v = record.get(name);
            return (v == null || v.isBlank()) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSafe(String value) {
        if (value == null) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return null; }
    }
}
