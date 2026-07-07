'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/layout/Sidebar';
import { getFollowUps, cancelFollowUp } from '@/lib/api';
import { Clock, Send, XCircle, RefreshCw, CalendarClock, Paperclip } from 'lucide-react';

interface FollowUp {
  id: number;
  sequenceNumber: number;
  subject: string;
  body: string;
  scheduledAt: string;
  sentAt: string | null;
  status: 'PENDING' | 'SENT' | 'CANCELLED';
  application: { company: string; role: string; hrEmail: string; };
}

const STATUS_CONFIG = {
  PENDING:   { label: 'Scheduled', color: 'bg-orange-100 text-orange-700', icon: <Clock    className="w-3.5 h-3.5" /> },
  SENT:      { label: 'Sent',      color: 'bg-green-100 text-green-700',   icon: <Send     className="w-3.5 h-3.5" /> },
  CANCELLED: { label: 'Cancelled', color: 'bg-gray-100 text-gray-400',     icon: <XCircle  className="w-3.5 h-3.5" /> },
};

export default function FollowUpsPage() {
  const [followUps, setFollowUps]   = useState<FollowUp[]>([]);
  const [loading, setLoading]       = useState(true);
  const [filter, setFilter]         = useState('ALL');
  const [expanded, setExpanded]     = useState<number | null>(null);
  const [cancelling, setCancelling] = useState<Record<number, boolean>>({});
  const [toast, setToast]           = useState('');

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

  const fetchFollowUps = async () => {
    setLoading(true);
    try {
      const res = await getFollowUps();
      setFollowUps(res.data.data || []);
    } catch { showToast('Failed to load follow-ups'); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchFollowUps(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCancel = async (id: number) => {
    setCancelling(c => ({ ...c, [id]: true }));
    try {
      await cancelFollowUp(id);
      setFollowUps(fus => fus.map(f => f.id === id ? { ...f, status: 'CANCELLED' as const } : f));
      showToast('Follow-up cancelled');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      showToast(err.response?.data?.message || 'Could not cancel follow-up');
    } finally { setCancelling(c => ({ ...c, [id]: false })); }
  };

  const filtered = filter === 'ALL' ? followUps : followUps.filter(f => f.status === filter);
  const pending   = followUps.filter(f => f.status === 'PENDING').length;
  const sent      = followUps.filter(f => f.status === 'SENT').length;
  const cancelled = followUps.filter(f => f.status === 'CANCELLED').length;
  const isOverdue = (scheduledAt: string) => new Date(scheduledAt) < new Date();

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Follow-Ups</h1>
              <p className="text-gray-500 mt-1">Auto-scheduled — resume attached — sent every 15 min</p>
            </div>
            <button onClick={fetchFollowUps}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm font-medium text-gray-600 hover:bg-gray-50">
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Refresh
            </button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4 mb-6">
            {[
              { label: 'Scheduled', value: pending,   color: 'bg-orange-50', tc: 'text-orange-600', icon: <Clock   className="w-5 h-5" /> },
              { label: 'Sent',      value: sent,      color: 'bg-green-50',  tc: 'text-green-600',  icon: <Send    className="w-5 h-5" /> },
              { label: 'Cancelled', value: cancelled, color: 'bg-gray-50',   tc: 'text-gray-400',   icon: <XCircle className="w-5 h-5" /> },
            ].map(({ label, value, color, tc, icon }) => (
              <div key={label} className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
                <div className={`w-10 h-10 ${color} ${tc} rounded-xl flex items-center justify-center mb-3`}>{icon}</div>
                <p className="text-2xl font-bold text-gray-900">{value}</p>
                <p className="text-sm text-gray-500">{label}</p>
              </div>
            ))}
          </div>

          {/* Info banner */}
          <div className="bg-blue-50 border border-blue-100 rounded-2xl p-4 mb-6 flex items-start gap-3">
            <CalendarClock className="w-5 h-5 text-blue-500 mt-0.5 shrink-0" />
            <p className="text-sm text-blue-700">
              Follow-ups are AI-written and sent automatically every 15 minutes.
              Each one <strong>includes your resume as an attachment</strong>.
              They are cancelled automatically on any reply or when you mark an application as rejected.
              You can also cancel individually below.
            </p>
          </div>

          {/* Filter tabs */}
          <div className="flex gap-2 mb-5">
            {['ALL', 'PENDING', 'SENT', 'CANCELLED'].map((key) => {
              const count = key === 'ALL' ? followUps.length
                : followUps.filter(f => f.status === key).length;
              return (
                <button key={key} onClick={() => setFilter(key)}
                  className={`px-4 py-2 rounded-xl text-sm font-medium transition-all
                    ${filter === key ? 'bg-blue-600 text-white' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'}`}>
                  {key === 'ALL' ? 'All' : key.charAt(0) + key.slice(1).toLowerCase()} ({count})
                </button>
              );
            })}
          </div>

          {/* List */}
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="bg-white rounded-2xl p-5 shadow-sm animate-pulse h-20" />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <div className="bg-white rounded-2xl p-12 text-center border border-gray-100">
              <CalendarClock className="w-12 h-12 text-gray-200 mx-auto mb-3" />
              <p className="text-gray-400 font-medium">No follow-ups yet.</p>
              <p className="text-gray-300 text-sm mt-1">They are generated automatically when you send an email.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filtered.map((fu) => {
                const cfg    = STATUS_CONFIG[fu.status];
                const overdue = fu.status === 'PENDING' && isOverdue(fu.scheduledAt);
                const isOpen  = expanded === fu.id;

                return (
                  <div key={fu.id}
                    className={`bg-white rounded-2xl shadow-sm border overflow-hidden transition-all
                      ${overdue ? 'border-orange-200' : 'border-gray-100'}`}>

                    <div className="flex items-center gap-4 p-5 cursor-pointer"
                      onClick={() => setExpanded(isOpen ? null : fu.id)}>
                      <div className="w-10 h-10 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center font-bold text-sm shrink-0">
                        F{fu.sequenceNumber}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                          <p className="font-semibold text-gray-900">{fu.application?.company}</p>
                          <span className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold ${cfg.color}`}>
                            {cfg.icon} {cfg.label}
                          </span>
                          <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-500">
                            <Paperclip className="w-3 h-3" /> Resume attached
                          </span>
                          {overdue && (
                            <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-orange-100 text-orange-600">
                              Sending soon…
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-500">{fu.application?.role}</p>
                      </div>
                      <div className="text-right shrink-0 flex items-center gap-3">
                        <div>
                          <p className="text-xs text-gray-500 font-medium">
                            {fu.status === 'SENT' && fu.sentAt
                              ? `Sent ${new Date(fu.sentAt).toLocaleDateString()}`
                              : fu.status === 'PENDING'
                                ? `Due ${new Date(fu.scheduledAt).toLocaleDateString()}`
                                : 'Cancelled'}
                          </p>
                          <p className="text-xs text-blue-500 mt-0.5">{isOpen ? 'Hide ▲' : 'Preview ▼'}</p>
                        </div>
                        {fu.status === 'PENDING' && (
                          <button
                            onClick={(e) => { e.stopPropagation(); handleCancel(fu.id); }}
                            disabled={cancelling[fu.id]}
                            className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-red-50 text-red-600 hover:bg-red-100 border border-red-100 disabled:opacity-50">
                            {cancelling[fu.id]
                              ? <RefreshCw className="w-3 h-3 animate-spin" />
                              : <XCircle   className="w-3 h-3" />}
                            Cancel
                          </button>
                        )}
                      </div>
                    </div>

                    {isOpen && (
                      <div className="border-t border-gray-100 p-5 bg-gray-50">
                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Email Preview</p>
                        <p className="text-sm font-medium text-gray-700 mb-2">
                          <span className="text-gray-400">Subject: </span>{fu.subject}
                        </p>
                        <div className="bg-white rounded-xl p-4 border border-gray-200 text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
                          {fu.body}
                        </div>
                        <p className="text-xs text-gray-400 mt-2 flex items-center gap-1">
                          <Paperclip className="w-3 h-3" /> Your resume will be attached when this sends.
                        </p>
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
