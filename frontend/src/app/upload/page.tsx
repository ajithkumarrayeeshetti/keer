'use client';

import { useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/components/layout/Sidebar';
import { uploadResume, uploadJobs } from '@/lib/api';
import { Upload, FileText, Table, CheckCircle, AlertCircle, ArrowRight, AlertTriangle } from 'lucide-react';

type UploadState = 'idle' | 'uploading' | 'success' | 'error';

interface FileUploadZoneProps {
  label: string; accept: string; icon: React.ReactNode; hint: string;
  state: UploadState; fileName?: string; onFile: (file: File) => void;
}

function FileUploadZone({ label, accept, icon, hint, state, fileName, onFile }: FileUploadZoneProps) {
  const ref = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const handle = (file: File) => { if (file) onFile(file); };

  return (
    <div
      onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => { e.preventDefault(); setDragging(false); const f = e.dataTransfer.files[0]; if (f) handle(f); }}
      onClick={() => ref.current?.click()}
      className={`relative border-2 border-dashed rounded-2xl p-8 cursor-pointer transition-all text-center
        ${dragging ? 'border-blue-400 bg-blue-50' : ''}
        ${state === 'success' ? 'border-green-400 bg-green-50' : ''}
        ${state === 'error'   ? 'border-red-400 bg-red-50'   : ''}
        ${state === 'idle' || state === 'uploading' ? 'border-gray-200 bg-white hover:border-blue-300 hover:bg-blue-50' : ''}
      `}
    >
      <input ref={ref} type="file" accept={accept} className="hidden"
        onChange={(e) => { const f = e.target.files?.[0]; if (f) handle(f); }} />
      <div className="flex flex-col items-center gap-3">
        <div className={`w-14 h-14 rounded-2xl flex items-center justify-center
          ${state === 'success' ? 'bg-green-100' : state === 'error' ? 'bg-red-100' : 'bg-gray-100'}`}>
          {state === 'success'   ? <CheckCircle className="w-7 h-7 text-green-600" /> :
           state === 'error'     ? <AlertCircle className="w-7 h-7 text-red-500" /> :
           state === 'uploading' ? <Upload className="w-7 h-7 text-blue-500 animate-bounce" /> :
           icon}
        </div>
        <div>
          <p className="font-semibold text-gray-900">{label}</p>
          {fileName && <p className="text-sm text-blue-600 mt-1 font-medium">{fileName}</p>}
          <p className="text-xs text-gray-400 mt-1">{hint}</p>
        </div>
        {state === 'uploading' && (
          <div className="w-full bg-gray-200 rounded-full h-1.5">
            <div className="bg-blue-500 h-1.5 rounded-full animate-pulse" style={{ width: '60%' }} />
          </div>
        )}
        {state === 'success' && <span className="text-xs text-green-600 font-medium">Uploaded successfully ✓</span>}
        {state === 'error'   && <span className="text-xs text-red-500 font-medium">Upload failed — see error below</span>}
      </div>
    </div>
  );
}

export default function UploadPage() {
  const router = useRouter();
  const [resumeState, setResumeState] = useState<UploadState>('idle');
  const [resumeName, setResumeName]   = useState('');
  const [jobsState, setJobsState]     = useState<UploadState>('idle');
  const [jobsName, setJobsName]       = useState('');
  const [jobCount, setJobCount]       = useState(0);
  const [csvErrors, setCsvErrors]     = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState('');
  const [message, setMessage]         = useState('');

  const handleResume = async (file: File) => {
    setResumeState('uploading'); setResumeName(file.name); setErrorMessage('');
    try {
      await uploadResume(file);
      setResumeState('success');
      setMessage('Resume parsed — AI extracted your skills, projects, and experience.');
    } catch (e: unknown) {
      setResumeState('error');
      const err = e as { response?: { data?: { message?: string } } };
      setErrorMessage(err.response?.data?.message || 'Resume upload failed. Please check the file is a valid PDF.');
    }
  };

  const handleJobs = async (file: File) => {
    setJobsState('uploading'); setJobsName(file.name);
    setErrorMessage(''); setCsvErrors([]);
    try {
      const res = await uploadJobs(file);
      const payload = res.data.data;
      // New API returns { imported, jobs, errors }
      const count  = payload?.imported ?? payload?.length ?? 0;
      const errors: string[] = payload?.errors ?? [];
      setJobCount(count);
      setCsvErrors(errors);
      setJobsState('success');
      setMessage(`${count} job${count !== 1 ? 's' : ''} imported from CSV.`);
    } catch (e: unknown) {
      setJobsState('error');
      const err = e as { response?: { data?: { message?: string } } };
      // Missing required columns → clear 400 message from server
      setErrorMessage(err.response?.data?.message || 'CSV upload failed. Check the file format.');
    }
  };

  const bothDone = resumeState === 'success' && jobsState === 'success';

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-8 max-w-3xl mx-auto">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-gray-900">Upload Files</h1>
            <p className="text-gray-500 mt-1">Upload your resume and jobs CSV to start generating personalized outreach</p>
          </div>

          {/* Steps */}
          <div className="flex items-center gap-2 mb-8">
            {['Upload Resume', 'Upload Jobs CSV', 'Generate Emails'].map((step, i) => (
              <div key={step} className="flex items-center gap-2 flex-1">
                <div className={`flex items-center gap-2 flex-1 p-3 rounded-xl text-sm font-medium
                  ${i === 0 && resumeState === 'success' ? 'bg-green-50 text-green-700' :
                    i === 1 && jobsState  === 'success' ? 'bg-green-50 text-green-700' :
                    i === 2 && bothDone                  ? 'bg-blue-50 text-blue-700' :
                    'bg-white text-gray-500 border border-gray-100'}`}>
                  <span className="w-6 h-6 rounded-full bg-current bg-opacity-10 flex items-center justify-center text-xs font-bold shrink-0"
                        style={{ color: 'inherit' }}>
                    {(i === 0 && resumeState === 'success') || (i === 1 && jobsState === 'success') ? '✓' : i + 1}
                  </span>
                  {step}
                </div>
                {i < 2 && <ArrowRight className="w-4 h-4 text-gray-300 shrink-0" />}
              </div>
            ))}
          </div>

          {/* Upload zones */}
          <div className="space-y-5">
            <FileUploadZone label="Resume PDF" accept=".pdf"
              icon={<FileText className="w-7 h-7 text-gray-400" />}
              hint="Drag & drop or click • PDF only • max 10 MB"
              state={resumeState} fileName={resumeName} onFile={handleResume} />

            <FileUploadZone label="Jobs CSV" accept=".csv"
              icon={<Table className="w-7 h-7 text-gray-400" />}
              hint="Drag & drop or click • Required columns: Company, Role, HR_Email"
              state={jobsState} fileName={jobsName} onFile={handleJobs} />
          </div>

          {/* Hard error (missing columns / parse failure) */}
          {errorMessage && (
            <div className="mt-4 bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
              <p className="font-semibold mb-1 flex items-center gap-1.5">
                <AlertCircle className="w-4 h-4" /> Error
              </p>
              <p className="whitespace-pre-wrap">{errorMessage}</p>
            </div>
          )}

          {/* Soft warnings (rows skipped) */}
          {csvErrors.length > 0 && (
            <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-4">
              <p className="text-sm font-semibold text-amber-800 mb-2 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4" /> {csvErrors.length} row{csvErrors.length > 1 ? 's' : ''} skipped
              </p>
              <ul className="space-y-1">
                {csvErrors.map((err, i) => (
                  <li key={i} className="text-xs text-amber-700 font-mono bg-amber-100 rounded px-2 py-1">{err}</li>
                ))}
              </ul>
            </div>
          )}

          {/* CSV format reference */}
          <div className="mt-6 bg-white rounded-2xl p-5 border border-gray-100">
            <p className="text-sm font-semibold text-gray-700 mb-2">CSV Column Reference</p>
            <div className="flex flex-wrap gap-2">
              {[
                { name: 'Company', required: true }, { name: 'Role', required: true },
                { name: 'HR_Email', required: true }, { name: 'Match_Score', required: false },
                { name: 'Location', required: false }, { name: 'Job_Type', required: false },
                { name: 'HR_Name', required: false }, { name: 'Recruiter_Name', required: false },
                { name: 'Recruiter_Linkedin', required: false }, { name: 'Company_Website', required: false },
                { name: 'Company_Size', required: false }, { name: 'Tech_Stack', required: false },
                { name: 'Job_Description', required: false }, { name: 'Why_Match', required: false },
                { name: 'Suggested_Subject', required: false }, { name: 'Application_Link', required: false },
              ].map(({ name, required }) => (
                <span key={name} className={`px-2 py-1 rounded-lg text-xs font-mono border
                  ${required
                    ? 'bg-blue-50 border-blue-200 text-blue-700 font-semibold'
                    : 'bg-gray-50 border-gray-200 text-gray-600'}`}>
                  {name}{required ? ' *' : ''}
                </span>
              ))}
            </div>
            <p className="text-xs text-gray-400 mt-3">* Required — rows without these will be skipped with an explanation.</p>
          </div>

          {/* Status message */}
          {message && !errorMessage && (
            <div className="mt-5 bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm text-blue-700">
              {message}
            </div>
          )}

          {/* CTA */}
          {bothDone && (
            <div className="mt-6 space-y-3">
              <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700 font-medium">
                ✅ Ready! Resume parsed + {jobCount} jobs imported.
                {csvErrors.length > 0 && ` (${csvErrors.length} rows skipped — see warnings above.)`}
              </div>
              <button onClick={() => router.push('/emails')}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2">
                Generate Emails <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
