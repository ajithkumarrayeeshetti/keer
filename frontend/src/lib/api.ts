import axios from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api = axios.create({ baseURL: `${API_URL}/api` });

api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('userName');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

// AUTH
export const login    = (email: string, password: string) => api.post('/auth/login',    { email, password });
export const register = (name: string, email: string, password: string) => api.post('/auth/register', { name, email, password });

// RESUME
export const uploadResume = (file: File) => {
  const fd = new FormData(); fd.append('file', file);
  return api.post('/resume/upload', fd);
};
export const getResume = () => api.get('/resume');

// JOBS
export const uploadJobs = (file: File) => {
  const fd = new FormData(); fd.append('file', file);
  return api.post('/jobs/upload', fd);
};
export const getJobs     = ()          => api.get('/jobs');
export const deleteJob   = (id: number) => api.delete(`/jobs/${id}`);

// EMAILS
export const generateEmail    = (jobId: number)                              => api.post(`/emails/generate/${jobId}`);
export const generateAllEmails = ()                                           => api.post('/emails/generate/all');
export const getEmailPreview  = (jobId: number)                              => api.get(`/emails/${jobId}`);
export const getFullPreview   = (emailId: number)                            => api.get(`/emails/${emailId}/full-preview`);
export const editEmail        = (emailId: number, subject: string, body: string) => api.put(`/emails/${emailId}`, { subject, body });
export const approveEmail     = (emailId: number)                            => api.patch(`/emails/${emailId}/approve`);
export const skipEmail        = (emailId: number)                            => api.patch(`/emails/${emailId}/skip`);
export const sendEmail        = (emailId: number)                            => api.post(`/emails/${emailId}/send`);
export const sendBatch        = ()                                           => api.post('/emails/send/batch');
export const retryFailedEmails = ()                                          => api.post('/emails/retry-failed');

// APPLICATIONS
export const getApplications     = ()                                               => api.get('/applications');
export const updateAppStatus     = (id: number, status: string)                     => api.patch(`/applications/${id}/status?status=${status}`);

// FOLLOW-UPS
export const getFollowUps     = () => api.get('/followups/pending');
export const cancelFollowUp   = (id: number) => api.delete(`/followups/${id}/cancel`);

// DASHBOARD
export const getDashboardStats = () => api.get('/dashboard/stats');

// SETTINGS
export const getSettings    = ()                 => api.get('/settings');
export const updateSettings = (settings: object) => api.put('/settings', settings);

export default api;
