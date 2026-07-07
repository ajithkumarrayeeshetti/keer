'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/layout/Sidebar';
import {
  getJobs, generateEmail, generateAllEmails, getEmailPreview,
  getFullPreview, editEmail, approveEmail, skipEmail, sendEmail,
  sendBatch, retryFailedEmails
} from '@/lib/api';
import { Job, EmailPreview } from '@/types';
import {
  Wand2, Send, Check, X, Edit3, ChevronDown, ChevronUp,
  Loader2, Eye, Mail, RotateCcw
} from 'lucide-react';

type PanelState = 'closed' | 'preview' | 'editing' | 'fullPreview';
interface FullPreview { subject: string; bodyWithSignature: string; resumeFilename: string | null; }

function StatusBadge({ status, openedAt }: { status: string; openedAt?: string | null }) {
  const map: Record<string, string> = {
    PENDING: 'bg-gray-100 text-gray-600',
    EMAIL_GENERATED: 'bg-indigo-100 text-indigo-700',
    SENT: 'bg-green-100 text-green-700',
    REPLIED: 'bg-yellow-100 text-yellow-700',
    REJECTED: 'bg-red-100 text-red-700',
    SKIPPED: 'bg-gray-100 text-gray-400',
    APPROVED: 'bg-blue-100 text-blue-700',
    DRAFT: 'bg-orange-100 text-orange-700',
    FAILED: 'bg-red-100 text-red-600',
  };
  return (
    <span className="flex items-center gap-1.5 flex-wrap">
      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${map[status] || 'bg-gray-100 text-gray-600'}`}>
        {status.replace('_', ' ')}
      </span>
      {openedAt && (
        <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700">
          <Mail className="w-3 h-3" /> Opened ✓
        </span>
      )}
    </span>
  );
}

export default function EmailsPage() {
  const [jobs, setJobs]                   = useState<Job[]>([]);
  const [previews, setPreviews]           = useState<Record<number, EmailPreview>>({});
  const [fullPreviews, setFullPreviews]   = useState<Record<number, FullPreview>>({});
  const [panel, setPanel]                 = useState<Record<number, PanelState>>({});
  const [generating, setGenerating]       = useState<Record<number, boolean>>({});
  const [sending, setSending]             = useState<Record<number, boolean>>({});
  const [loadingFull, setLoadingFull]     = useState<Record<number, boolean>>({});
  const [editDraft, setEditDraft]         = useState<Record<number, { subject: string; body: string }>>({});
  const [generatingAll, setGeneratingAll] = useState(false);
  const [sendingAll, setSendingAll]       = useState(false);
  const [retrying, setRetrying]           = useState(false);
  const [toast, setToast]                 = useState('');

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 3500); };

  const loadJobs = async () => {
    try { const res = await getJobs(); setJobs(res.data.data || []); }
    catch { showToast('Failed to load jobs'); }
  };

  useEffect(() => { loadJobs(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const loadPreview = async (jobId: number) => {
    try { const res = await getEmailPreview(jobId); setPreviews(p => ({ ...p, [jobId]: res.data.data })); }
    catch { /* not generated yet */ }
  };

  const loadFullPreview = async (jobId: number, emailId: number) => {
    if (fullPreviews[jobId]) { setPanel(p => ({ ...p, [jobId]: 'fullPreview' })); return; }
    setLoadingFull(l => ({ ...l, [jobId]: true }));
    try {
      const res = await getFullPreview(emailId);
      setFullPreviews(fp => ({ ...fp, [jobId]: res.data.data }));
      setPanel(p => ({ ...p, [jobId]: 'fullPreview' }));
    } catch { showToast('Could not load full preview'); }
    finally { setLoadingFull(l => ({ ...l, [jobId]: false })); }
  };

  const handleGenerate = async (jobId: number) => {
    setGenerating(g => ({ ...g, [jobId]: true }));
    try {
      const res = await generateEmail(jobId);
      setPreviews(p => ({ ...p, [jobId]: res.data.data }));
      setPanel(p => ({ ...p, [jobId]: 'preview' }));
      await loadJobs();
    } catch { showToast('AI generation failed. Is your AI provider running?'); }
    finally { setGenerating(g => ({ ...g, [jobId]: false })); }
  };

  const handleGenerateAll = async () => {
    setGeneratingAll(true);
    try {
      const res = await generateAllEmails();
      const list: EmailPreview[] = res.data.data || [];
      const map: Record<number, EmailPreview> = {};
      list.forEach(e => { map[e.jobId] = e; });
      setPreviews(p => ({ ...p, ...map }));
      await loadJobs();
      showToast(`${list.length} email${list.length !== 1 ? 's' : ''} generated`);
    } catch { showToast('Batch generation failed'); }
    finally { setGeneratingAll(false); }
  };

  const handleApprove = async (jobId: number) => {
    const preview = previews[jobId]; if (!preview) return;
    try {
      await approveEmail(preview.emailId);
      setPreviews(p => ({ ...p, [jobId]: { ...p[jobId], status: 'APPROVED' } }));
      showToast('Email approved ✓');
    } catch { showToast('Approve failed'); }
  };

  const handleSkip = async (jobId: number) => {
    const preview = previews[jobId]; if (!preview) return;
    try {
      await skipEmail(preview.emailId);
      setPanel(p => ({ ...p, [jobId]: 'closed' }));
      await loadJobs();
      showToast('Job skipped');
    } catch { showToast('Skip failed'); }
  };

  const handleSaveEdit = async (jobId: number) => {
    const preview = previews[jobId]; const draft = editDraft[jobId];
    if (!preview || !draft) return;
    try {
      const res = await editEmail(preview.emailId, draft.subject, draft.body);
      setPreviews(p => ({ ...p, [jobId]: { ...p[jobId], subject: res.data.data.subject, body: res.data.data.body } }));
      setFullPreviews(fp => { const copy = { ...fp }; delete copy[jobId]; return copy; });
      setPanel(p => ({ ...p, [jobId]: 'preview' }));
      showToast('Email saved');
    } catch { showToast('Save failed'); }
  };

  const handleSend = async (jobId: number) => {
    const preview = previews[jobId]; if (!preview) return;
    setSending(s => ({ ...s, [jobId]: true }));
    try {
      await sendEmail(preview.emailId);
      setPreviews(p => ({ ...p, [jobId]: { ...p[jobId], status: 'SENT' } }));
      await loadJobs();
      showToast(`Email sent to ${preview.hrEmail}`);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      showToast(err.response?.data?.message || 'Send failed — check Gmail settings.');
    } finally { setSending(s => ({ ...s, [jobId]: false })); }
  };

  const handleSendBatch = async () => {
    setSendingAll(true);
    showToast('Batch send started — emails spaced ~45s apart to protect your Gmail account');
    try {
      const res = await sendBatch();
      const count = res.data.data?.length || 0;
      await loadJobs();
      showToast(`${count} email${count !== 1 ? 's' : ''} sent`);
    } catch { showToast('Batch send failed'); }
    finally { setSendingAll(false); }
  };

  const handleRetryFailed = async () => {
    setRetrying(true);
    try {
      await retryFailedEmails();
      await loadJobs();
      showToast('Retrying failed emails…');
    } catch { showToast('Retry failed'); }
    finally { setRetrying(false); }
  };

  const togglePanel = async (jobId: number) => {
    if (panel[jobId] && panel[jobId] !== 'closed') {
      setPanel(p => ({ ...p, [jobId]: 'closed' }));
    } else {
      if (!previews[jobId]) await loadPreview(jobId);
      setPanel(p => ({ ...p, [jobId]: 'preview' }));
    }
  };

  const approvedCount = Object.values(previews).filter(p => p.status === 'APPROVED').length;
  const failedCount   = jobs.filter(j => j.status === 'FAILED').length;

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8">
          {/* Header */}
          <div className="flex items-start justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Email Outreach</h1>
              <p className="text-gray-500 mt-1">{jobs.length} jobs — generate, review, and send</p>
            </div>
            <div className="flex gap-2 flex-wrap justify-end">
              {failedCount > 0 && (
                <button onClick={handleRetryFailed} disabled={retrying}
                  className="flex items-center gap-2 px-4 py-2 bg-red-50 border border-red-200 text-red-700 rounded-xl text-sm font-medium hover:bg-red-100 disabled:opacity-50">
                  <RotateCcw className={`w-4 h-4 ${retrying ? 'animate-spin' : ''}`} />
                  Retry {failedCount} failed
                </button>
              )}
              <button onClick={handleGenerateAll} disabled={generatingAll}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50">
                {generatingAll ? <Loader2 className="w-4 h-4 animate-spin" /> : <Wand2 className="w-4 h-4" />}
                Generate All
              </button>
              {approvedCount > 0 && (
                <button onClick={handleSendBatch} disabled={sendingAll}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50">
                  {sendingAll ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  Send {approvedCount} Approved
                </button>
              )}
            </div>
          </div>

          {/* Rate-limiting notice */}
          {approvedCount > 1 && (
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 mb-5 text-sm text-amber-800 flex items-center gap-2">
              <span>⏱</span>
              <span>
                Batch send spaces emails ~45s apart to protect your Gmail account.
                {approvedCount} emails ≈ {Math.ceil((approvedCount - 1) * 45 / 60)} min total.
              </span>
            </div>
          )}

          {/* Job list */}
          <div className="space-y-3">
            {jobs.length === 0 && (
              <div className="bg-white rounded-2xl p-12 text-center border border-gray-100">
                <Wand2 className="w-12 h-12 text-gray-200 mx-auto mb-3" />
                <p className="text-gray-400 font-medium">No jobs yet. Upload a CSV to get started.</p>
              </div>
            )}
            {jobs.map(job => {
              const preview = previews[job.id];
              const p = panel[job.id] || 'closed';
              const fp = fullPreviews[job.id];

              return (
                <div key={job.id} className="bg-white rounded-2xl shadow-sm border border-gray-100">
                  {/* Row */}
                  <div className="flex items-center gap-4 p-5">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center text-white font-bold text-sm shrink-0">
                      {job.company?.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-0.5">
                        <p className="font-semibold text-gray-900">{job.company}</p>
                        <StatusBadge
                          status={preview?.status || job.status}
                          openedAt={preview?.openedAt}
                        />
                      </div>
                      <p className="text-sm text-gray-500">{job.role}</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {!preview && job.status === 'PENDING' && (
                        <button onClick={() => handleGenerate(job.id)} disabled={generating[job.id]}
                          className="flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50 text-indigo-700 rounded-lg text-xs font-medium hover:bg-indigo-100 disabled:opacity-50">
                          {generating[job.id] ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wand2 className="w-3.5 h-3.5" />}
                          Generate
                        </button>
                      )}
                      {(preview || job.status === 'EMAIL_GENERATED') && (
                        <button onClick={() => togglePanel(job.id)}
                          className="flex items-center gap-1 px-3 py-1.5 bg-gray-100 text-gray-600 rounded-lg text-xs font-medium hover:bg-gray-200">
                          {p !== 'closed' ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                          {p !== 'closed' ? 'Hide' : 'View'}
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Preview panel */}
                  {p !== 'closed' && preview && (
                    <div className="border-t border-gray-100 p-5 bg-gray-50 rounded-b-2xl">
                      {p === 'fullPreview' && fp ? (
                        <div>
                          <div className="flex items-center justify-between mb-3">
                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                              Full Preview — exactly as HR receives it
                            </p>
                            <button onClick={() => setPanel(pp => ({ ...pp, [job.id]: 'preview' }))}
                              className="text-xs text-blue-600 hover:underline">← Back</button>
                          </div>
                          <div className="bg-white rounded-xl border border-gray-200 p-4">
                            <p className="text-sm font-semibold text-gray-700 mb-1">Subject: {fp.subject}</p>
                            {fp.resumeFilename && (
                              <p className="text-xs text-gray-500 mb-3 flex items-center gap-1">
                                📎 <span className="font-medium">{fp.resumeFilename}</span>
                              </p>
                            )}
                            <hr className="mb-3" />
                            <pre className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed font-sans">
                              {fp.bodyWithSignature}
                            </pre>
                          </div>
                          {preview.status === 'APPROVED' && (
                            <button onClick={() => handleSend(job.id)} disabled={sending[job.id]}
                              className="mt-3 flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50">
                              {sending[job.id] ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                              Send Now
                            </button>
                          )}
                        </div>
                      ) : p === 'editing' ? (
                        <div className="space-y-3">
                          <input
                            value={editDraft[job.id]?.subject || ''}
                            onChange={e => setEditDraft(d => ({ ...d, [job.id]: { ...d[job.id], subject: e.target.value } }))}
                            className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="Subject"
                          />
                          <textarea
                            rows={10}
                            value={editDraft[job.id]?.body || ''}
                            onChange={e => setEditDraft(d => ({ ...d, [job.id]: { ...d[job.id], body: e.target.value } }))}
                            className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                          />
                          <div className="flex gap-2">
                            <button onClick={() => handleSaveEdit(job.id)}
                              className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-medium hover:bg-blue-700">
                              <Check className="w-4 h-4" /> Save
                            </button>
                            <button onClick={() => setPanel(pp => ({ ...pp, [job.id]: 'preview' }))}
                              className="flex items-center gap-1.5 px-3 py-2 bg-white border border-gray-200 text-gray-600 rounded-xl text-sm hover:bg-gray-50">
                              <X className="w-4 h-4" /> Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="mb-3">
                            <p className="text-xs text-gray-500 mb-1">
                              <span className="font-semibold text-gray-700">Subject:</span> {preview.subject}
                            </p>
                            <div className="bg-white rounded-xl p-4 border border-gray-200 text-sm text-gray-700 whitespace-pre-wrap leading-relaxed max-h-60 overflow-y-auto">
                              {preview.body}
                            </div>
                          </div>
                          <div className="flex items-center gap-2 flex-wrap">
                            {preview.status === 'DRAFT' && (
                              <button onClick={() => handleApprove(job.id)}
                                className="flex items-center gap-1.5 px-3 py-1.5 bg-green-600 text-white rounded-lg text-xs font-medium hover:bg-green-700">
                                <Check className="w-3.5 h-3.5" /> Approve
                              </button>
                            )}
                            <button
                              onClick={() => loadFullPreview(job.id, preview.emailId)}
                              disabled={loadingFull[job.id]}
                              className="flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50 text-indigo-700 rounded-lg text-xs font-medium hover:bg-indigo-100 disabled:opacity-50">
                              {loadingFull[job.id] ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Eye className="w-3.5 h-3.5" />}
                              Full Preview
                            </button>
                            <button
                              onClick={() => {
                                setEditDraft(d => ({ ...d, [job.id]: { subject: preview.subject, body: preview.body } }));
                                setPanel(pp => ({ ...pp, [job.id]: 'editing' }));
                              }}
                              className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-gray-200 text-gray-600 rounded-lg text-xs font-medium hover:bg-gray-50">
                              <Edit3 className="w-3.5 h-3.5" /> Edit
                            </button>
                            {preview.status === 'APPROVED' && (
                              <button onClick={() => handleSend(job.id)} disabled={sending[job.id]}
                                className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700 disabled:opacity-50">
                                {sending[job.id] ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                                Send Now
                              </button>
                            )}
                            <button onClick={() => handleSkip(job.id)}
                              className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-gray-100 text-gray-400 rounded-lg text-xs hover:text-red-500">
                              <X className="w-3.5 h-3.5" /> Skip
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </main>

      {toast && (
        <div className="fixed bottom-6 right-6 bg-gray-900 text-white px-4 py-3 rounded-xl shadow-xl text-sm z-50 max-w-sm">
          {toast}
        </div>
      )}
    </div>
  );
}
