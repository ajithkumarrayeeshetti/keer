export interface AuthResponse {
  token: string;
  name: string;
  email: string;
}

export interface Job {
  id: number;
  company: string;
  role: string;
  matchScore: number;
  location: string;
  jobType: string;
  hrName: string;
  hrEmail: string;
  techStack: string;
  jobDescription: string;
  whyMatch: string;
  status: 'PENDING' | 'EMAIL_GENERATED' | 'SENT' | 'REPLIED' | 'REJECTED' | 'SKIPPED' | 'FAILED';
  createdAt: string;
}

export interface EmailPreview {
  emailId: number;
  jobId: number;
  company: string;
  role: string;
  hrName: string;
  hrEmail: string;
  subject: string;
  body: string;
  status: 'DRAFT' | 'APPROVED' | 'SENT' | 'FAILED';
  openedAt?: string | null;
}

export interface Application {
  id: number;
  company: string;
  role: string;
  hrName: string;
  hrEmail: string;
  subject: string;
  emailContent?: string;
  sentAt: string;
  status: 'SENT' | 'REPLIED_POSITIVE' | 'INTERVIEW' | 'REJECTED' | 'NO_RESPONSE';
  openedAt?: string | null;
}

export interface DashboardStats {
  totalJobsUploaded: number;
  emailsGenerated: number;
  emailsSent: number;
  replies: number;
  interviews: number;
  rejections: number;
  followUpsPending: number;
  responseRate: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface Settings {
  gmailAddress: string;
  gmailAppPassword: string;
  aiProvider: string;
  aiApiKey: string;
  aiModel: string;
  ollamaModel: string;
  ollamaUrl: string;
  followupDay1: number;
  followupDay2: number;
  emailSignature: string;
  batchSendDelaySeconds: number;
}
