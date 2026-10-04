import { useState, useEffect } from 'react';
import { AlertTriangle, Info, Zap, CheckCircle, Filter } from 'lucide-react';
import { saveAlerts } from '../api/index';

const SEVERITY_CONFIG = {
    CRITICAL: { color: 'var(--rose)', bg: 'rgba(244,63,94,0.08)', border: 'rgba(244,63,94,0.4)', icon: Zap, badge: 'badge-rose' },
    WARNING: { color: 'var(--amber)', bg: 'rgba(245,158,11,0.05)', border: 'rgba(245,158,11,0.3)', icon: AlertTriangle, badge: 'badge-amber' },
    INFO: { color: 'var(--cyan)', bg: 'rgba(34,211,238,0.04)', border: 'rgba(34,211,238,0.25)', icon: Info, badge: 'badge-cyan' },
};

const THRESHOLDS = [
    { label: 'R² Alert Threshold', key: 'r2', unit: '', default: 0.998, min: 0.98, max: 1, step: 0.001 },
    { label: 'MAE Alert Threshold', key: 'mae', unit: ' kW', default: 2.5, min: 0.5, max: 10, step: 0.1 },
    { label: 'Power Deviation Alert', key: 'dev', unit: '%', default: 15, min: 5, max: 50, step: 1 },
];

function formatTime(ts) {
    return new Date(ts).toLocaleString('en-IN', { hour12: false, day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export default function AlertsPage({ alerts, onAcknowledge, showToast }) {
    const [filter, setFilter] = useState('ALL');
    const [thresholds, setThresh] = useState({ r2: 0.998, mae: 2.5, dev: 15 });

    // Persist alerts whenever they change
    useEffect(() => { saveAlerts(alerts); }, [alerts]);

    const visible = alerts.filter(a =>
        filter === 'ALL' ? true :
            filter === 'ACTIVE' ? !a.isAcknowledged :
                filter === a.severity
    );

    const critCount = alerts.filter(a => !a.isAcknowledged && a.severity === 'CRITICAL').length;
    const warnCount = alerts.filter(a => !a.isAcknowledged && a.severity === 'WARNING').length;
    const infoCount = alerts.filter(a => !a.isAcknowledged && a.severity === 'INFO').length;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* Stats Row */}
            <div className="metric-grid">
                {[
                    { label: 'Critical', count: critCount, total: alerts.filter(a => a.severity === 'CRITICAL').length, badgeClass: 'badge-rose', emoji: '🔴' },
                    { label: 'Warning', count: warnCount, total: alerts.filter(a => a.severity === 'WARNING').length, badgeClass: 'badge-amber', emoji: '🟡' },
                    { label: 'Info', count: infoCount, total: alerts.filter(a => a.severity === 'INFO').length, badgeClass: 'badge-cyan', emoji: '🔵' },
                    { label: 'Total Active', count: alerts.filter(a => !a.isAcknowledged).length, total: alerts.length, badgeClass: 'badge-purple', emoji: '📊' },
                ].map(({ label, count, total, badgeClass, emoji }) => (
                    <div className="metric-card" key={label}>
                        <div className="metric-card-header">
                            <span className="metric-label">{label}</span>
                            <span>{emoji}</span>
                        </div>
                        <div className="metric-value">{count}</div>
                        <span className={`badge ${badgeClass}`}>{total} total</span>
                    </div>
                ))}
            </div>

            {/* Filter Chips */}
            <div className="card" style={{ padding: '12px 16px' }}>
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-12">
                        <Filter size={14} color="var(--text-muted)" />
                        <div className="chip-row">
                            {['ALL', 'ACTIVE', 'CRITICAL', 'WARNING', 'INFO'].map(f => (
                                <button key={f} className={`chip ${filter === f ? 'active-amber' : ''}`} onClick={() => setFilter(f)}>{f}</button>
                            ))}
                        </div>
                    </div>
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{visible.length} alerts shown</span>
                </div>
            </div>

            {/* Alert List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {visible.length === 0 && (
                    <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
                        <CheckCircle size={32} color="var(--emerald)" style={{ marginBottom: 10 }} />
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>All Clear</div>
                        <div style={{ fontSize: 12, marginTop: 4 }}>No alerts match the current filter.</div>
                    </div>
                )}
                {visible.map(alert => {
                    const cfg = SEVERITY_CONFIG[alert.severity] || SEVERITY_CONFIG.INFO;
                    const AlertIcon = cfg.icon;
                    return (
                        <div key={alert.id} className="alert-card" style={{
                            background: alert.isAcknowledged ? 'var(--bg-card)' : cfg.bg,
                            borderColor: alert.isAcknowledged ? 'rgba(255,255,255,0.06)' : cfg.border,
                            opacity: alert.isAcknowledged ? 0.65 : 1,
                        }}>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-12">
                                    <AlertIcon size={16} color={alert.isAcknowledged ? 'var(--text-muted)' : cfg.color} />
                                    <div>
                                        <div className="flex items-center gap-8" style={{ marginBottom: 3 }}>
                                            <span style={{ fontWeight: 700, fontSize: 13, color: alert.isAcknowledged ? 'var(--text-secondary)' : 'var(--text-primary)' }}>{alert.type}</span>
                                            <span className={`badge ${cfg.badge}`}>{alert.severity}</span>
                                            {alert.isAcknowledged && <span className="badge badge-emerald">✓ ACK</span>}
                                        </div>
                                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 6 }}>{formatTime(alert.timestamp)}</div>
                                        <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.6 }}>{alert.message}</div>
                                    </div>
                                </div>
                                {!alert.isAcknowledged && (
                                    <button className="btn btn-outline-amber btn-sm" onClick={() => onAcknowledge(alert.id)}>
                                        Acknowledge
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Threshold Config */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 16 }}>⚙️ Alert Threshold Configuration</div>
                {THRESHOLDS.map(t => (
                    <div key={t.key} className="slider-wrap">
                        <div className="slider-labels">
                            <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t.label}</span>
                            <span style={{ fontWeight: 700, color: 'var(--amber)' }}>{thresholds[t.key]}{t.unit}</span>
                        </div>
                        <input
                            type="range" min={t.min} max={t.max} step={t.step}
                            value={thresholds[t.key]}
                            onChange={e => {
                                const v = parseFloat(e.target.value);
                                setThresh(prev => ({ ...prev, [t.key]: v }));
                                showToast?.(`Threshold updated: ${t.label} → ${v}${t.unit}`);
                            }}
                        />
                    </div>
                ))}
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 6 }}>
                    Thresholds are stored in session. Future iterations will sync to Firestore.
                </div>
            </div>
        </div>
    );
}
