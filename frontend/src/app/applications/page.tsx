'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/layout/Sidebar';
import { getApplications, updateAppStatus } from '@/lib/api';
import { Application } from '@/types';
import {
  Calendar, CheckCircle, XCircle, Clock,
  MessageSquare, RefreshCw, Building2, Mail, ChevronDown, ChevronUp
} from 'lucide-react';

const STATUS_CONFIG: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  SENT:             { label: 'Sent',           color: 'bg-blue-100 text-blue-700',       icon: <Clock           className="w-3.5 h-3.5" /> },
  REPLIED_POSITIVE: { label: 'Positive Reply', color: 'bg-green-100 text-green-700',     icon: <MessageSquare   className="w-3.5 h-3.5" /> },
  INTERVIEW:        { label: 'Interview! 🎉',  color: 'bg-emerald-100 text-emerald-700', icon: <Calendar        className="w-3.5 h-3.5" /> },
  REJECTED:         { label: 'Rejected',        color: 'bg-red-100 text-red-700',         icon: <XCircle         className="w-3.5 h-3.5" /> },
  NO_RESPONSE:      { label: 'No Response',     color: 'bg-gray-100 text-gray-500',       icon: <Clock           className="w-3.5 h-3.5" /> },
};

const MANUAL_TRANSITIONS: Record<string, { label: string; next: string; color: string }[]> = {
  SENT:             [
    { label: 'Mark Interview', next: 'INTERVIEW', color: 'text-emerald-600 hover:bg-emerald-50' },
    { label: 'Mark Rejected',  next: 'REJECTED',  color: 'text-red-500 hover:bg-red-50' },
  ],
  NO_RESPONSE:      [
    { label: 'Mark Rejected',  next: 'REJECTED',  color: 'text-red-500 hover:bg-red-50' },
  ],
  REPLIED_POSITIVE: [
    { label: 'Mark Interview', next: 'INTERVIEW', color: 'text-emerald-600 hover:bg-emerald-50' },
    { label: 'Mark Rejected',  next: 'REJECTED',  color: 'text-red-500 hover:bg-red-50' },
  ],
};

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading]   = useState(true);
  const [filter, setFilter]     = useState('ALL');
  const [expanded, setExpanded] = useState<Record<number, boolean>>({});
  const [updating, setUpdating] = useState<Record<number, boolean>>({});
  const [toast, setToast]       = useState('');

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const res = await getApplications();
      setApplications(res.data.data || []);
    } catch { showToast('Failed to load applications'); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchApplications(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleStatusUpdate = async (appId: number, status: string, company: string) => {
    setUpdating(u => ({ ...u, [appId]: true }));
    try {
      await updateAppStatus(appId, status);
      setApplications(apps => apps.map(a => a.id === appId ? { ...a, status: status as Application['status'] } : a));
      if (status === 'REJECTED') {
        showToast(`${company} marked as rejected — pending follow-ups cancelled automatically`);
      } else {
        showToast(`Status updated to ${status.toLowerCase().replace('_', ' ')}`);
      }
    } catch { showToast('Failed to update status'); }
    finally { setUpdating(u => ({ ...u, [appId]: false })); }
  };

  const filtered = filter === 'ALL' ? applications : applications.filter(a => a.status === filter);

  const counts: Record<string, number> = {
    ALL:              applications.length,
    SENT:             applications.filter(a => a.status === 'SENT').length,
    INTERVIEW:        applications.filter(a => a.status === 'INTERVIEW').length,
    REPLIED_POSITIVE: applications.filter(a => a.status === 'REPLIED_POSITIVE').length,
    REJECTED:         applications.filter(a => a.status === 'REJECTED').length,
    NO_RESPONSE:      applications.filter(a => a.status === 'NO_RESPONSE').length,
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Applications</h1>
              <p className="text-gray-500 mt-1">Track sent emails and manage their status</p>
            </div>
            <button onClick={fetchApplications}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm font-medium text-gray-600 hover:bg-gray-50">
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Refresh
            </button>
          </div>

          {/* Filter tabs */}
          <div className="flex gap-2 mb-6 flex-wrap">
            {[
              { key: 'ALL',              label: 'All' },
              { key: 'SENT',             label: 'Sent' },
              { key: 'INTERVIEW',        label: 'Interviews 🎉' },
              { key: 'REPLIED_POSITIVE', label: 'Positive' },
              { key: 'REJECTED',         label: 'Rejected' },
              { key: 'NO_RESPONSE',      label: 'No Response' },
            ].map(({ key, label }) => (
              <button key={key} onClick={() => setFilter(key)}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all
                  ${filter === key ? 'bg-blue-600 text-white shadow-sm' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'}`}>
                {label}
                <span className={`px-1.5 py-0.5 rounded-full text-xs font-bold
                  ${filter === key ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-600'}`}>
                  {counts[key] ?? 0}
                </span>
              </button>
            ))}
          </div>

          {/* Interview banner */}
          {counts.INTERVIEW > 0 && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 mb-6 flex items-center gap-3">
              <div className="w-10 h-10 bg-emerald-100 rounded-xl flex items-center justify-center">
                <Calendar className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <p className="font-semibold text-emerald-800">
                  🎉 {counts.INTERVIEW} Interview{counts.INTERVIEW > 1 ? 's' : ''} Scheduled!
                </p>
                <p className="text-sm text-emerald-600">Your outreach is working.</p>
              </div>
            </div>
          )}

          {/* List */}
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="bg-white rounded-2xl p-5 shadow-sm animate-pulse h-20" />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <div className="bg-white rounded-2xl p-12 text-center border border-gray-100">
              <Building2 className="w-12 h-12 text-gray-200 mx-auto mb-3" />
              <p className="text-gray-400 font-medium">
                {filter === 'ALL'
                  ? 'No applications yet. Send some emails first.'
                  : `No ${filter.toLowerCase().replace('_', ' ')} applications.`}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filtered.map((app) => {
                const cfg      = STATUS_CONFIG[app.status] || STATUS_CONFIG.SENT;
                const actions  = MANUAL_TRANSITIONS[app.status] || [];
                const isOpen   = expanded[app.id];

                return (
                  <div key={app.id}
                    className={`bg-white rounded-2xl shadow-sm border transition-all
                      ${app.status === 'INTERVIEW' ? 'border-emerald-200 ring-1 ring-emerald-100' : 'border-gray-100'}`}>

                    <div className="flex items-center gap-4 p-5">
                      <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center text-white font-bold text-sm shrink-0">
                        {app.company?.charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                          <p className="font-semibold text-gray-900">{app.company}</p>
                          <span className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold ${cfg.color}`}>
                            {cfg.icon} {cfg.label}
                          </span>
                          {app.openedAt && (
                            <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700">
                              <Mail className="w-3 h-3" /> Opened ✓
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-500">{app.role}</p>
                        <p className="text-xs text-gray-400 mt-0.5">
                          {app.hrName ? `${app.hrName} · ` : ''}{app.hrEmail}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <p className="text-xs text-gray-400">
                          {app.sentAt
                            ? new Date(app.sentAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                            : '—'}
                        </p>
                        <button
                          onClick={() => setExpanded(e => ({ ...e, [app.id]: !e[app.id] }))}
                          className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                          {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>

                    {isOpen && (
                      <div className="border-t border-gray-50 px-5 pb-5 pt-4 bg-gray-50 rounded-b-2xl">
                        {app.subject && (
                          <p className="text-xs text-gray-500 mb-3">
                            <span className="font-semibold text-gray-700">Subject:</span> {app.subject}
                          </p>
                        )}
                        {actions.length > 0 && (
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="text-xs text-gray-400 mr-1">Update status:</span>
                            {actions.map(action => (
                              <button
                                key={action.next}
                                onClick={() => handleStatusUpdate(app.id, action.next, app.company)}
                                disabled={updating[app.id]}
                                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-gray-200 bg-white transition-colors disabled:opacity-50 ${action.color}`}>
                                {updating[app.id]
                                  ? <RefreshCw className="w-3 h-3 animate-spin" />
                                  : action.next === 'REJECTED'
                                    ? <XCircle    className="w-3 h-3" />
                                    : <CheckCircle className="w-3 h-3" />}
                                {action.label}
                              </button>
                            ))}
                            {actions.some(a => a.next === 'REJECTED') && (
                              <span className="text-xs text-gray-400 italic">(cancels pending follow-ups)</span>
                            )}
                          </div>
                        )}
                        {app.status === 'REJECTED' && (
                          <p className="text-xs text-gray-400 italic">Follow-ups cancelled.</p>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
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
