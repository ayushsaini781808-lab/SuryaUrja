import { useState } from 'react';
import { Shield, Key, Database, CloudOff } from 'lucide-react';
import { getGeminiKey, setGeminiKey, getApiKey } from '../api/index';
import { PLANTS } from '../data/solarData';

export default function AuthPage({ selectedPlant, onSelectPlant, showToast }) {
    const [geminiKey, setLocalGeminiKey] = useState(getGeminiKey());
    const [saved, setSaved] = useState(false);
    const [userEmail, setUserEmail] = useState(localStorage.getItem('suryaurja_user') || '');
    const [isDemo, setIsDemo] = useState(localStorage.getItem('suryaurja_demo') === 'true');

    const saveGeminiKey = () => {
        setGeminiKey(geminiKey);
        setSaved(true);
        setTimeout(() => setSaved(false), 2000);
        showToast?.('Gemini API key saved to browser storage');
    };

    const signInDemo = () => {
        const demo = 'demo.engineer@suryaurja.ai';
        localStorage.setItem('suryaurja_user', demo);
        localStorage.setItem('suryaurja_demo', 'true');
        setUserEmail(demo); setIsDemo(true);
        showToast?.('Signed in as Demo Solar Dispatch Engineer');
    };

    const signOut = () => {
        localStorage.removeItem('suryaurja_user');
        localStorage.removeItem('suryaurja_demo');
        setUserEmail(''); setIsDemo(false);
        showToast?.('Signed out');
    };

    const apiKey = getApiKey();

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 680 }}>

            {/* Account Status */}
            <div className="card card-amber">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>
                    👤 {isDemo ? `Signed in: ${userEmail}` : 'Account & Authentication'}
                </div>
                {isDemo ? (
                    <div>
                        <div className="flex items-center gap-12" style={{ marginBottom: 12 }}>
                            <span className="badge badge-emerald">Demo Mode Active</span>
                            <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Solar Dispatch Engineer role</span>
                        </div>
                        <button className="btn btn-outline" onClick={signOut}>Sign Out</button>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                        <input className="text-input" placeholder="Email (optional — local only)"
                            value={userEmail} onChange={e => setUserEmail(e.target.value)} />
                        <div className="flex gap-12">
                            <button className="btn btn-amber" onClick={() => {
                                localStorage.setItem('suryaurja_user', userEmail);
                                showToast?.('Profile saved locally');
                            }}>Save Profile</button>
                            <button className="btn btn-outline-amber" onClick={signInDemo}>
                                🔑 Sign In as Demo Engineer
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {/* Gemini Key Config */}
            <div className="card">
                <div className="flex items-center gap-12" style={{ marginBottom: 14 }}>
                    <Key size={16} color="var(--amber)" />
                    <div style={{ fontWeight: 700, fontSize: 14 }}>Gemini AI API Key</div>
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 12, lineHeight: 1.6 }}>
                    Required for the AI Assistant tab. Get your key free at{' '}
                    <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer"
                        style={{ color: 'var(--cyan)' }}>aistudio.google.com</a>.
                    Your key is stored only in this browser — never sent to any server.
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                    <input className="text-input" type="password"
                        placeholder="AIza… (paste your Gemini API key)"
                        value={geminiKey}
                        onChange={e => setLocalGeminiKey(e.target.value)}
                    />
                    <button className="btn btn-amber" onClick={saveGeminiKey}>
                        {saved ? '✓ Saved!' : 'Save'}
                    </button>
                </div>
                {geminiKey && (
                    <div style={{ marginTop: 8, fontSize: 11, color: 'var(--emerald)' }}>
                        ✓ Key configured — AI Assistant is ready
                    </div>
                )}
            </div>

            {/* Project API Key (from env) */}
            <div className="card">
                <div className="flex items-center gap-12" style={{ marginBottom: 14 }}>
                    <Shield size={16} color="var(--cyan)" />
                    <div style={{ fontWeight: 700, fontSize: 14 }}>Project API Key (VITE_API_KEY)</div>
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 12, lineHeight: 1.6 }}>
                    This key is loaded from <code style={{ background: 'rgba(255,255,255,0.06)', padding: '1px 6px', borderRadius: 4 }}>.env</code> at build time.
                    It is available as a fallback for AI and other service calls.
                </div>
                <div className="location-coords" style={{ wordBreak: 'break-all' }}>
                    {apiKey ? `Configured: ${apiKey.slice(0, 6)}${'•'.repeat(Math.min(16, apiKey.length - 6))}` : 'Not set — add VITE_API_KEY to .env'}
                </div>
            </div>

            {/* Plant Management */}
            <div className="card">
                <div className="flex items-center gap-12" style={{ marginBottom: 14 }}>
                    <Database size={16} color="var(--emerald)" />
                    <div style={{ fontWeight: 700, fontSize: 14 }}>Plant Profile Management</div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {PLANTS.map(p => (
                        <div key={p.id} className="api-feed" style={{
                            border: p.id === selectedPlant.id ? '1px solid rgba(245,158,11,0.4)' : undefined,
                            background: p.id === selectedPlant.id ? 'rgba(245,158,11,0.05)' : undefined,
                            cursor: 'pointer',
                        }} onClick={() => { onSelectPlant(p); showToast?.(`Active plant: ${p.name}`); }}>
                            <div className="flex items-center justify-between">
                                <div>
                                    <div className="api-feed-name">{p.name}</div>
                                    <div className="api-feed-type" style={{ marginTop: 2 }}>
                                        {p.capacityKw} kW • {p.panelType} • {p.location} • Tilt {p.tiltAngle}° • {p.commissionDays} days operational
                                    </div>
                                </div>
                                {p.id === selectedPlant.id && <span className="badge badge-amber">ACTIVE</span>}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Data Persistence Summary */}
            <div className="card" style={{ borderColor: 'rgba(255,255,255,0.05)' }}>
                <div className="flex items-center gap-12" style={{ marginBottom: 12 }}>
                    <CloudOff size={16} color="var(--text-muted)" />
                    <div style={{ fontWeight: 700, fontSize: 13 }}>Storage & Persistence</div>
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.8 }}>
                    {[
                        ['Chat history', 'localStorage (suryaurja_chat) — up to 100 messages'],
                        ['Active alerts', 'localStorage (suryaurja_alerts) — persists across sessions'],
                        ['Plant selection', 'localStorage (suryaurja_plant)'],
                        ['Gemini API key', 'localStorage (suryaurja_gemini_key, plain text)'],
                        ['Forecast data', 'In-memory + Open-Meteo API (re-fetched on sync)'],
                    ].map(([k, v]) => (
                        <div key={k} className="flex gap-12" style={{ borderBottom: '1px solid rgba(255,255,255,0.04)', paddingBottom: 6, marginBottom: 6 }}>
                            <span style={{ color: 'var(--text-primary)', fontWeight: 600, minWidth: 130 }}>{k}</span>
                            <span>{v}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
