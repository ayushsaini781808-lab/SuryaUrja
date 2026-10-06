import { useState, useEffect } from 'react';
import { ChevronDown, MapPin, CheckCircle, Sun, Activity, Zap, Edit3 } from 'lucide-react';

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

export default function Topbar({ selectedPlant, plants, onSelectPlant, onUpdateCapacity, overview, isSynced, locationName, page }) {
    const [showPlants, setShowPlants] = useState(false);
    const [customKwInput, setCustomKwInput] = useState(String(selectedPlant.capacityKw));

    useEffect(() => {
        setCustomKwInput(String(selectedPlant.capacityKw));
    }, [selectedPlant]);

    const handleCapacityChange = (val) => {
        setCustomKwInput(val);
        const num = parseFloat(val);
        if (!isNaN(num) && num > 0 && num <= 50000) {
            onUpdateCapacity?.(num);
        }
    };

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

            {/* Real kW Capacity Keyboard Input Badge */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 4,
                background: '#eaf6ee', border: '1px solid rgba(22,163,74,0.3)',
                borderRadius: 8, padding: '3px 8px',
            }} title="Directly edit Real Solar Plant Capacity (kW)">
                <Zap size={12} color="#16a34a" />
                <span style={{ fontSize: 10, fontWeight: 700, color: '#15803d' }}>kW:</span>
                <input
                    type="number"
                    min="0.1"
                    max="50000"
                    step="0.5"
                    value={customKwInput}
                    onChange={(e) => handleCapacityChange(e.target.value)}
                    style={{
                        width: 55, border: 'none', background: 'transparent',
                        fontWeight: 900, fontSize: 12, color: '#0f5132',
                        outline: 'none', textAlign: 'center',
                    }}
                />
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
                        borderRadius: 14, overflow: 'hidden', minWidth: 300,
                        boxShadow: 'var(--shadow-xl)',
                    }}>
                        {/* Custom kW Section */}
                        <div style={{ padding: '12px 16px', background: '#f8fbf9', borderBottom: '1px solid var(--border)' }}>
                            <div style={{ fontSize: 10, fontWeight: 800, color: '#15803d', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 6, display: 'flex', alignItems: 'center', gap: 4 }}>
                                <Zap size={11} /> Real Plant Capacity (Keyboard Input)
                            </div>
                            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                                <input
                                    type="number"
                                    min="0.1"
                                    max="50000"
                                    step="0.5"
                                    value={customKwInput}
                                    onChange={(e) => handleCapacityChange(e.target.value)}
                                    placeholder="e.g. 200"
                                    style={{
                                        flex: 1, padding: '6px 10px', borderRadius: 8,
                                        border: '1px solid rgba(22,163,74,0.4)', background: '#fff',
                                        fontSize: 14, fontWeight: 800, color: '#0f5132', outline: 'none',
                                    }}
                                />
                                <span style={{ fontSize: 12, fontWeight: 700, color: '#15803d' }}>kW</span>
                            </div>

                            {/* Preset Buttons */}
                            <div style={{ display: 'flex', gap: 4, marginTop: 8, flexWrap: 'wrap' }}>
                                {[50, 100, 200, 500, 1000].map(kw => (
                                    <button
                                        key={kw}
                                        type="button"
                                        onClick={() => handleCapacityChange(String(kw))}
                                        style={{
                                            background: Number(customKwInput) === kw ? '#dcfce7' : '#fff',
                                            border: `1px solid ${Number(customKwInput) === kw ? '#16a34a' : '#d4ebd9'}`,
                                            color: Number(customKwInput) === kw ? '#15803d' : '#537359',
                                            padding: '2px 8px', borderRadius: 6, fontSize: 10, fontWeight: 700, cursor: 'pointer',
                                        }}
                                    >
                                        {kw} kW
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Preset Plants List */}
                        <div style={{ padding: '10px 16px', borderBottom: '1px solid var(--border)', fontSize: 10, fontWeight: 800, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Select Preset Solar Plant
                        </div>
                        {plants.map(p => (
                            <div
                                key={p.id}
                                onClick={() => {
                                    onSelectPlant(p);
                                    setCustomKwInput(String(p.capacityKw));
                                    setShowPlants(false);
                                }}
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
