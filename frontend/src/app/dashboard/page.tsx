'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/components/layout/Sidebar';
import { getDashboardStats } from '@/lib/api';
import { DashboardStats } from '@/types';
import {
  Briefcase, Send, MessageSquare, Calendar, TrendingUp,
  Clock, XCircle, Upload
} from 'lucide-react';

export default function DashboardPage() {
  const router = useRouter();
  const [stats, setStats]     = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [userName, setUserName] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) { router.push('/'); return; }
    setUserName(localStorage.getItem('userName') || '');

    getDashboardStats()
      .then(res => setStats(res.data.data))
      .catch(() => {/* stats unavailable — show zeros */})
      .finally(() => setLoading(false));
  }, [router]);

  const s = stats;

  const statCards = [
    { label: 'Jobs Uploaded',    value: s?.totalJobsUploaded ?? 0, icon: <Briefcase    className="w-5 h-5" />, color: 'bg-blue-50 text-blue-600'    },
    { label: 'Emails Sent',      value: s?.emailsSent        ?? 0, icon: <Send         className="w-5 h-5" />, color: 'bg-indigo-50 text-indigo-600' },
    { label: 'Replies',          value: s?.replies           ?? 0, icon: <MessageSquare className="w-5 h-5" />, color: 'bg-green-50 text-green-600'  },
    { label: 'Interviews',       value: s?.interviews        ?? 0, icon: <Calendar      className="w-5 h-5" />, color: 'bg-emerald-50 text-emerald-600' },
    { label: 'Response Rate',    value: `${s?.responseRate   ?? 0}%`, icon: <TrendingUp className="w-5 h-5" />, color: 'bg-purple-50 text-purple-600' },
    { label: 'Follow-ups Due',   value: s?.followUpsPending  ?? 0, icon: <Clock        className="w-5 h-5" />, color: 'bg-orange-50 text-orange-600' },
    { label: 'Rejections',       value: s?.rejections        ?? 0, icon: <XCircle      className="w-5 h-5" />, color: 'bg-red-50 text-red-500'       },
    { label: 'Emails Generated', value: s?.emailsGenerated   ?? 0, icon: <Briefcase    className="w-5 h-5" />, color: 'bg-gray-50 text-gray-500'     },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-gray-900">
              {userName ? `Welcome back, ${userName.split(' ')[0]} 👋` : 'Dashboard'}
            </h1>
            <p className="text-gray-500 mt-1">Here's an overview of your outreach campaign</p>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="bg-white rounded-2xl p-5 shadow-sm animate-pulse h-28" />
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
              {statCards.map(({ label, value, icon, color }) => (
                <div key={label} className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
                  <div className={`w-10 h-10 ${color} rounded-xl flex items-center justify-center mb-3`}>
                    {icon}
                  </div>
                  <p className="text-2xl font-bold text-gray-900">{value}</p>
                  <p className="text-sm text-gray-500 mt-0.5">{label}</p>
                </div>
              ))}
            </div>
          )}

          {/* Quick-start CTA when no data yet */}
          {!loading && (s?.totalJobsUploaded ?? 0) === 0 && (
            <div className="bg-white rounded-2xl p-8 border border-gray-100 shadow-sm text-center">
              <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Upload className="w-8 h-8 text-blue-500" />
              </div>
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Get started in 2 steps</h2>
              <p className="text-gray-500 text-sm mb-6 max-w-md mx-auto">
                Upload your resume and a CSV of job opportunities to start generating personalized cold emails with AI.
              </p>
              <button
                onClick={() => router.push('/upload')}
                className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors">
                Upload Resume & Jobs →
              </button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
