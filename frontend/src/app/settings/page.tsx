'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/layout/Sidebar';
import api, { getSettings, updateSettings } from '@/lib/api';
import { Save, Eye, EyeOff, CheckCircle, Zap, Loader2, ExternalLink, Shield, Clock } from 'lucide-react';

const PROVIDERS = [
  { id: 'OLLAMA',   name: 'Ollama (Local)',  badge: '100% Free',         badgeColor: 'bg-green-100 text-green-700',
    description: 'Runs entirely on your machine. No internet needed for AI.', needsKey: false,
    keyLabel: '', keyLink: '', models: ['qwen3','gemma3','deepseek-r1','llama3','mistral','phi3'],
    hint: 'Install Ollama and run: ollama pull qwen3' },
  { id: 'GEMINI',   name: 'Google Gemini',   badge: '1,500 req/day free', badgeColor: 'bg-blue-100 text-blue-700',
    description: "Google's Gemini 1.5 Flash is very fast and generous on free tier.", needsKey: true,
    keyLabel: 'Gemini API Key', keyLink: 'https://aistudio.google.com/app/apikey',
    models: ['gemini-1.5-flash','gemini-2.0-flash','gemini-1.5-pro'], hint: 'Get free key at Google AI Studio — no credit card needed' },
  { id: 'GROQ',     name: 'Groq',            badge: '14,400 req/day free', badgeColor: 'bg-purple-100 text-purple-700',
    description: 'Extremely fast inference. Best free tier for high-volume sending.', needsKey: true,
    keyLabel: 'Groq API Key', keyLink: 'https://console.groq.com/keys',
    models: ['llama-3.3-70b-versatile','llama-3.1-8b-instant','meta-llama/llama-4-scout-17b-16e-instruct','compound-beta-mini'],
    hint: 'Sign up at console.groq.com — free, no credit card' },
  { id: 'TOGETHER', name: 'Together AI',     badge: '$1 free credit',     badgeColor: 'bg-orange-100 text-orange-700',
    description: 'Access to 100+ open-source models. $1 free credit on signup.', needsKey: true,
    keyLabel: 'Together AI API Key', keyLink: 'https://api.together.xyz/settings/api-keys',
    models: ['mistralai/Mixtral-8x7B-Instruct-v0.1','meta-llama/Llama-3-70b-chat-hf','google/gemma-2-27b-it'],
    hint: 'Sign up at together.ai — $1 credit, no subscription' },
  { id: 'OPENAI',   name: 'OpenAI',          badge: 'Paid',               badgeColor: 'bg-gray-100 text-gray-600',
    description: 'GPT-4o-mini is very affordable (~$0.00015/email). Best quality.', needsKey: true,
    keyLabel: 'OpenAI API Key', keyLink: 'https://platform.openai.com/api-keys',
    models: ['gpt-4o-mini','gpt-4o','gpt-3.5-turbo'], hint: 'Requires paid account. gpt-4o-mini costs ~$0.0001 per email.' },
];

export default function SettingsPage() {
  const [form, setForm] = useState({
    gmailAddress: '', gmailAppPassword: '',
    aiProvider: 'OLLAMA', aiApiKey: '', aiModel: '',
    ollamaModel: 'qwen3', ollamaUrl: 'http://localhost:11434',
    followupDay1: 7, followupDay2: 14,
    emailSignature: '',
    batchSendDelaySeconds: 45,
  });

  const [showGmailPw, setShowGmailPw] = useState(false);
  const [showApiKey,  setShowApiKey]  = useState(false);
  const [saving,  setSaving]  = useState(false);
  const [saved,   setSaved]   = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ ok: boolean; msg: string } | null>(null);
  const [error, setError] = useState('');

  const provider = PROVIDERS.find(p => p.id === form.aiProvider) || PROVIDERS[0];

  useEffect(() => {
    getSettings().then(res => { const d = res.data.data; setForm(f => ({ ...f, ...d })); }).catch(() => {});
  }, []);

  const setProvider = (id: string) => {
    const p = PROVIDERS.find(pr => pr.id === id)!;
    setForm(f => ({ ...f, aiProvider: id, aiModel: p.models[0] }));
    setTestResult(null);
  };
  const set = (key: string, value: string | number) => setForm(f => ({ ...f, [key]: value }));

  const handleSave = async () => {
    setSaving(true); setError('');
    try { await updateSettings(form); setSaved(true); setTimeout(() => setSaved(false), 2500); }
    catch { setError('Failed to save settings'); }
    finally { setSaving(false); }
  };

  const handleTestAI = async () => {
    setTesting(true); setTestResult(null);
    try {
      await updateSettings(form);
      const res = await api.post('/settings/test-ai');
      setTestResult({ ok: true, msg: res.data.data || 'Connection successful!' });
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setTestResult({ ok: false, msg: err.response?.data?.message || 'Connection failed' });
    } finally { setTesting(false); }
  };

  // Estimated batch time
  const batchDelay = form.batchSendDelaySeconds || 45;
  const exampleEmails = 10;
  const estimatedMins = Math.ceil((exampleEmails - 1) * batchDelay / 60);

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8 max-w-2xl mx-auto">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
            <p className="text-gray-500 mt-1">Configure your AI provider, Gmail, follow-up schedule, and security</p>
          </div>

          <div className="space-y-6">

            {/* ── AI PROVIDER ─────────────────────────────────────────────── */}
            <section className="bg-white rounded-2xl p-6 shadow-sm">
              <h2 className="text-base font-semibold text-gray-900 mb-1">AI Provider</h2>
              <p className="text-sm text-gray-500 mb-5">Choose which AI generates your emails</p>
              <div className="grid grid-cols-1 gap-3 mb-6">
                {PROVIDERS.map((p) => (
                  <label key={p.id} className={`flex items-start gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all
                    ${form.aiProvider === p.id ? 'border-blue-500 bg-blue-50' : 'border-gray-100 hover:border-gray-300'}`}>
                    <input type="radio" name="aiProvider" value={p.id}
                      checked={form.aiProvider === p.id} onChange={() => setProvider(p.id)}
                      className="mt-1 accent-blue-600" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-0.5">
                        <span className="font-semibold text-sm text-gray-900">{p.name}</span>
                        <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${p.badgeColor}`}>{p.badge}</span>
                      </div>
                      <p className="text-xs text-gray-500">{p.description}</p>
                    </div>
                  </label>
                ))}
              </div>
              <div className="border-t border-gray-100 pt-5 space-y-4">
                <div className="flex items-start gap-2 bg-blue-50 rounded-xl p-3 text-xs text-blue-700">
                  <Zap className="w-3.5 h-3.5 mt-0.5 shrink-0" /><span>{provider.hint}</span>
                  {provider.keyLink && (
                    <a href={provider.keyLink} target="_blank" rel="noreferrer"
                       className="ml-auto flex items-center gap-1 font-semibold whitespace-nowrap hover:underline">
                      Get key <ExternalLink className="w-3 h-3" />
                    </a>
                  )}
                </div>
                {provider.needsKey && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{provider.keyLabel}</label>
                    <div className="relative">
                      <input type={showApiKey ? 'text' : 'password'} value={form.aiApiKey || ''}
                        onChange={e => set('aiApiKey', e.target.value)} placeholder="Paste your API key here"
                        className="w-full px-4 py-3 pr-11 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                      <button type="button" onClick={() => setShowApiKey(v => !v)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                        {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                )}
                {form.aiProvider === 'OLLAMA' && (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Model</label>
                      <select value={form.ollamaModel || 'qwen3'} onChange={e => set('ollamaModel', e.target.value)}
                        className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
                        {provider.models.map(m => <option key={m}>{m}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Ollama URL</label>
                      <input value={form.ollamaUrl || ''} onChange={e => set('ollamaUrl', e.target.value)}
                        className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="http://localhost:11434" />
                    </div>
                  </div>
                )}
                {form.aiProvider !== 'OLLAMA' && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Model</label>
                    <select value={form.aiModel || provider.models[0]} onChange={e => set('aiModel', e.target.value)}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
                      {provider.models.map(m => <option key={m} value={m}>{m}</option>)}
                    </select>
                  </div>
                )}
                <button onClick={handleTestAI} disabled={testing}
                  className="flex items-center gap-2 px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-semibold rounded-xl transition-colors disabled:opacity-60">
                  {testing ? <><Loader2 className="w-4 h-4 animate-spin" /> Testing...</> : <><Zap className="w-4 h-4" /> Test AI Connection</>}
                </button>
                {testResult && (
                  <div className={`rounded-xl p-3 text-sm font-medium ${testResult.ok ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-600 border border-red-200'}`}>
                    {testResult.ok ? '✅ ' : '❌ '}{testResult.msg}
                  </div>
                )}
              </div>
            </section>

            {/* ── GMAIL ───────────────────────────────────────────────────── */}
            <section className="bg-white rounded-2xl p-6 shadow-sm">
              <div className="flex items-start justify-between mb-1">
                <h2 className="text-base font-semibold text-gray-900">Gmail (SMTP)</h2>
                <span className="flex items-center gap-1 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded-full">
                  <Shield className="w-3 h-3" /> Encrypted at rest
                </span>
              </div>
              <p className="text-sm text-gray-500 mb-5">
                Used to send emails and attach your resume.{' '}
                <a href="https://myaccount.google.com/apppasswords" target="_blank" rel="noreferrer"
                   className="text-blue-600 hover:underline inline-flex items-center gap-1">
                  Create App Password <ExternalLink className="w-3 h-3" />
                </a>
              </p>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Gmail Address</label>
                  <input type="email" value={form.gmailAddress || ''} onChange={e => set('gmailAddress', e.target.value)}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="you@gmail.com" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Gmail App Password</label>
                  <div className="relative">
                    <input type={showGmailPw ? 'text' : 'password'} value={form.gmailAppPassword || ''}
                      onChange={e => set('gmailAppPassword', e.target.value)}
                      className="w-full px-4 py-3 pr-11 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="xxxx xxxx xxxx xxxx" />
                    <button type="button" onClick={() => setShowGmailPw(v => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                      {showGmailPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  <p className="text-xs text-gray-400 mt-1.5">
                    Stored AES-256 encrypted — not your regular Gmail password.
                  </p>
                </div>
              </div>
            </section>

            {/* ── BATCH SEND RATE LIMITING ────────────────────────────────── */}
            <section className="bg-white rounded-2xl p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-1">
                <Clock className="w-4 h-4 text-gray-500" />
                <h2 className="text-base font-semibold text-gray-900">Batch Send Delay</h2>
              </div>
              <p className="text-sm text-gray-500 mb-5">
                Delay between emails when using "Send All". Prevents Gmail from flagging your account for bulk sending.
              </p>
              <div className="flex items-end gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Seconds between emails</label>
                  <input type="number" min={10} max={300} value={form.batchSendDelaySeconds || 45}
                    onChange={e => set('batchSendDelaySeconds', parseInt(e.target.value))}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div className="pb-3 text-sm text-gray-500 whitespace-nowrap">
                  {exampleEmails} emails ≈ <span className="font-semibold text-gray-700">{estimatedMins} min</span>
                </div>
              </div>
              <div className={`mt-3 flex items-center gap-1.5 text-xs rounded-lg px-3 py-2
                ${batchDelay < 20 ? 'bg-red-50 text-red-700' : batchDelay < 30 ? 'bg-amber-50 text-amber-700' : 'bg-green-50 text-green-700'}`}>
                {batchDelay < 20
                  ? '⚠️ Very risky — Gmail may suspend your account at this speed.'
                  : batchDelay < 30
                    ? '⚠️ Borderline — consider 30s minimum.'
                    : '✅ Safe — good balance of speed and account protection.'}
              </div>
            </section>

            {/* ── FOLLOW-UP SCHEDULE ──────────────────────────────────────── */}
            <section className="bg-white rounded-2xl p-6 shadow-sm">
              <h2 className="text-base font-semibold text-gray-900 mb-1">Follow-up Schedule</h2>
              <p className="text-sm text-gray-500 mb-5">
                AI writes and sends follow-ups automatically. Cancelled immediately on any reply or manual rejection.
              </p>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Follow-up 1 (days after sending)</label>
                  <input type="number" min={1} max={30} value={form.followupDay1 || 7}
                    onChange={e => set('followupDay1', parseInt(e.target.value))}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Follow-up 2 (days after sending)</label>
                  <input type="number" min={1} max={60} value={form.followupDay2 || 14}
                    onChange={e => set('followupDay2', parseInt(e.target.value))}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
              </div>
            </section>

            {/* ── EMAIL SIGNATURE ─────────────────────────────────────────── */}
            <section className="bg-white rounded-2xl p-6 shadow-sm">
              <h2 className="text-base font-semibold text-gray-900 mb-1">Email Signature</h2>
              <p className="text-sm text-gray-500 mb-4">Appended below every outgoing email (initial + follow-ups)</p>
              <textarea rows={4} value={form.emailSignature || ''} onChange={e => set('emailSignature', e.target.value)}
                className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder={"Best regards,\nJohn Doe\nLinkedIn: linkedin.com/in/johndoe\nGitHub: github.com/johndoe"} />
            </section>

            {error && <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600">{error}</div>}

            <button onClick={handleSave} disabled={saving}
              className="w-full py-3 flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold rounded-xl transition-colors">
              {saved   ? <><CheckCircle className="w-4 h-4" /> Saved!</> :
               saving  ? <><Loader2 className="w-4 h-4 animate-spin" /> Saving...</> :
               <><Save className="w-4 h-4" /> Save Settings</>}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}