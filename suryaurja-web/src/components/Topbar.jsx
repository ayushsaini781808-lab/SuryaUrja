import { useState } from 'react';
import { ChevronDown, MapPin, CheckCircle, Sun, Activity } from 'lucide-react';

const PAGE_TITLES = {
    dashboard: 'Dashboard',
    homesolar: 'Solar Calculator',
    multiday:  '7-Day Forecast',
    models:    'Model Evaluation',
    alerts:    'Grid Alerts',
    weather:   'Weather & Data Pipeline',
    chat:      'AI Solar Assistant',
    auth:      'Account',
};

const PAGE_SUBTITLES = {
    dashboard: 'Live plant overview & forecasting',
    homesolar: 'Real-time weather-based energy prediction',
    multiday:  'CNN-LSTM + ENN 7-day solar forecast',
    models:    'Benchmarking & neural architecture',
    alerts:    'Anomaly detection & grid monitoring',
    weather:   'Meteorological pipeline & inference engine',
    chat:      'Gemini-powered solar domain assistant',
    auth:      'Firebase authentication & cloud sync',
};

export default function Topbar({ selectedPlant, plants, onSelectPlant, overview, isSynced, locationName, page }) {
    const [showPlants, setShowPlants] = useState(false);

    return (
        <div className="topbar">
            {/* Left: breadcrumb + title */}
            <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 1 }}>
                    <Sun size={12} color="var(--green-dark)" strokeWidth={2.5} />
                    <span style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        SuryaUrja
                    </span>
                    <span style={{ color: 'var(--border-strong)', fontSize: 10 }}>›</span>
                    <span style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 600 }}>{PAGE_TITLES[page] || 'Platform'}</span>
                </div>
                <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1.2 }}>
                    {PAGE_TITLES[page] || 'SuryaUrja Platform'}
                </div>
                <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 500 }}>
                    {PAGE_SUBTITLES[page] || 'CNN-LSTM + ENN Solar Intelligence'}
                </div>
            </div>

            {/* Location pill */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 6,
                fontSize: 11, fontWeight: 600,
                color: isSynced ? 'var(--green-dark)' : 'var(--text-muted)',
                background: isSynced ? 'var(--green-dim)' : 'var(--bg-surface)',
                padding: '5px 12px', borderRadius: 20,
                border: `1px solid ${isSynced ? 'rgba(34,197,94,0.3)' : 'var(--border)'}`,
                maxWidth: 200,
            }}>
                {isSynced
                    ? <CheckCircle size={12} color="var(--green-dark)" strokeWidth={2.5} />
                    : <MapPin size={12} color="var(--text-muted)" />
                }
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {locationName}
                </span>
            </div>

            {/* Model accuracy */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 6,
                background: 'rgba(15,37,87,0.06)', border: '1px solid rgba(15,37,87,0.12)',
                borderRadius: 8, padding: '5px 12px',
            }}>
                <Activity size={11} color="var(--navy)" />
                <span style={{ fontSize: 11, fontWeight: 800, color: 'var(--navy)' }}>R² {overview.modelR2}</span>
                <span style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 500 }}>MAE {overview.dayAheadMaeKw} kW</span>
            </div>

            {/* Plant selector */}
            <div style={{ position: 'relative' }}>
                <button
                    style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        background: 'var(--navy)', color: '#fff',
                        border: 'none', borderRadius: 8, padding: '7px 14px',
                        cursor: 'pointer', fontSize: 12, fontWeight: 700,
                        boxShadow: 'var(--shadow-sm)', transition: 'all 0.2s',
                    }}
                    onClick={() => setShowPlants(v => !v)}
                    onMouseEnter={e => e.currentTarget.style.background = 'var(--navy-mid)'}
                    onMouseLeave={e => e.currentTarget.style.background = 'var(--navy)'}
                >
                    <Sun size={13} strokeWidth={2.5} />
                    <span style={{ maxWidth: 130, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {selectedPlant.name}
                    </span>
                    <ChevronDown size={12} />
                </button>

                {showPlants && (
                    <div style={{
                        position: 'absolute', top: 'calc(100% + 8px)', right: 0, zIndex: 200,
                        background: '#fff', border: '1px solid var(--border)',
                        borderRadius: 14, overflow: 'hidden', minWidth: 280,
                        boxShadow: 'var(--shadow-xl)',
                    }}>
                        <div style={{ padding: '10px 16px', borderBottom: '1px solid var(--border)', fontSize: 10, fontWeight: 800, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Select Solar Plant
                        </div>
                        {plants.map(p => (
                            <div
                                key={p.id}
                                onClick={() => { onSelectPlant(p); setShowPlants(false); }}
                                style={{
                                    padding: '11px 16px', cursor: 'pointer', transition: 'background 0.15s',
                                    background: p.id === selectedPlant.id ? 'rgba(15,37,87,0.05)' : 'transparent',
                                    borderBottom: '1px solid var(--border)',
                                    borderLeft: p.id === selectedPlant.id ? '3px solid var(--navy)' : '3px solid transparent',
                                }}
                                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
                                onMouseLeave={e => e.currentTarget.style.background = p.id === selectedPlant.id ? 'rgba(15,37,87,0.05)' : 'transparent'}
                            >
                                <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>{p.name}</div>
                                <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 3, fontWeight: 500 }}>
                                    {p.capacityKw} kW · {p.panelType} · {p.location}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Live indicator */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="dot-live" />
                <span style={{ fontSize: 11, color: 'var(--green-dark)', fontWeight: 700 }}>LIVE</span>
            </div>
        </div>
    );
}
