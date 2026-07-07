package com.outreach.util;

import com.outreach.entity.Job;
import com.outreach.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private static final int MAX_JD_LENGTH   = 600;
    private static final int MAX_RAW_LENGTH  = 4000;
    private static final int MAX_BODY_LENGTH = 8000;

    public String buildEmailGenerationPrompt(Resume resume, Job job) {
        return String.format("""
            You are an expert at writing highly personalized cold outreach emails for job applications.

            CANDIDATE PROFILE:
            Skills: %s
            Technologies: %s
            Projects: %s
            Experience: %s
            Education: %s

            JOB OPPORTUNITY:
            Company: %s
            Role: %s
            Tech Stack: %s
            Job Description: %s
            Why Candidate Matches: %s
            HR/Recruiter Name: %s
            Company Size: %s

            INSTRUCTIONS:
            Write a personalized cold email with these strict rules:
            1. Maximum 150 words in the email body
            2. Mention the company name naturally
            3. Mention the role explicitly
            4. Reference 2-3 specific matching technologies
            5. Mention one relevant project or experience
            6. Professional but conversational tone
            7. End with a clear call to action
            8. Never use generic phrases like "I am writing to express my interest"
            9. Make it feel written specifically for THIS company

            Output format (return ONLY this, no preamble, no markdown, no thinking):
            SUBJECT: [subject line here]
            BODY:
            [email body here]
            """,
            safe(resume.getSkills()),
            safe(resume.getTechnologies()),
            safe(resume.getProjects()),
            safe(resume.getExperience()),
            safe(resume.getEducation()),
            safe(job.getCompany()),
            safe(job.getRole()),
            safe(job.getTechStack()),
            truncate(safe(job.getJobDescription()), MAX_JD_LENGTH),
            safe(job.getWhyMatch()),
            safe(job.getHrName()),
            safe(job.getCompanySize())
        );
    }

    public String buildFollowUpPrompt(Resume resume, Job job, int followUpNumber) {
        return String.format("""
            Write a brief, professional follow-up email (maximum 100 words).

            Context: Candidate applied for %s role at %s %d week(s) ago.
            Candidate's key skills: %s
            HR/Recruiter: %s

            Rules:
            - Reference that you applied previously
            - Reiterate one key strength briefly
            - Politely ask for an update
            - No desperation, confident and professional
            - Maximum 100 words

            Output format (no preamble, no thinking):
            SUBJECT: Re: [original subject line]
            BODY:
            [follow up body]
            """,
            safe(job.getRole()),
            safe(job.getCompany()),
            followUpNumber == 1 ? 1 : 2,
            safe(resume.getSkills()),
            safe(job.getHrName())
        );
    }

    public String buildReplyClassificationPrompt(String replyBody) {
        return String.format("""
            Classify this email reply into exactly one of these categories:
            - INTERVIEW: Contains interview invitation, scheduling request, or wants to speak
            - POSITIVE: Interested, asking for more info, positive but not yet interview
            - REJECTION: Explicitly declining, not moving forward, no fit
            - NEUTRAL: Thank you but no commitment, auto-reply, acknowledgement only
            - UNKNOWN: Cannot determine intent clearly

            Email content:
            %s

            Respond with ONLY the single category word (INTERVIEW, POSITIVE, REJECTION, NEUTRAL, or UNKNOWN).
            No explanation, no punctuation, no other text.
            """, truncate(safe(replyBody), MAX_BODY_LENGTH));
    }

    public String buildResumeExtractionPrompt(String rawResumeText) {
        return String.format("""
            Extract structured information from this resume text and return ONLY valid JSON.
            No markdown code fences, no extra text, no thinking.

            Resume text:
            %s

            Return a JSON object with exactly these keys:
            {
              "skills": ["skill1", "skill2"],
              "technologies": ["tech1", "tech2"],
              "projects": [{"name": "proj", "description": "desc", "tech": ["t1"]}],
              "experience": [{"company": "co", "role": "role", "duration": "dur", "bullets": ["point1"]}],
              "education": [{"institution": "uni", "degree": "degree", "year": "year"}]
            }
            """, truncate(safe(rawResumeText), MAX_RAW_LENGTH));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}
