import { useState } from 'react';
import { FileText, CheckCircle } from 'lucide-react';
import { RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

export default function ModelsPage({ benchmarks, neuralLayers, featureImportances }) {
    const [showModelCard, setShowModelCard] = useState(false);

    const radarData = [
        { metric: 'R² Score', CNN_LSTM: 99.91, LSTM: 98.5, RF: 97.2, XGB: 96.8 },
        { metric: 'MAE', CNN_LSTM: 97, LSTM: 90, RF: 84, XGB: 80 },
        { metric: 'RMSE', CNN_LSTM: 96, LSTM: 89, RF: 82, XGB: 79 },
        { metric: 'MAPE', CNN_LSTM: 97.5, LSTM: 91, RF: 85, XGB: 83 },
        { metric: 'Latency', CNN_LSTM: 72, LSTM: 88, RF: 98, XGB: 99 },
    ];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <div className="section-title">Model Evaluation & Benchmarking</div>
                    <div className="section-sub">Published College 200 kW PV Dataset (646 Days Holdout)</div>
                </div>
                <button className="btn btn-outline-amber" onClick={() => setShowModelCard(true)}>
                    <FileText size={14} /> Model Card
                </button>
            </div>

            {/* Goal Banner */}
            <div className="card card-emerald" style={{ padding: '14px 18px' }}>
                <div className="flex items-center gap-12">
                    <CheckCircle size={24} color="var(--emerald)" />
                    <div>
                        <div style={{ fontWeight: 700, color: 'var(--emerald)', fontSize: 13 }}>Business Goals Exceeded</div>
                        <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>
                            Goal: R² ≥ 0.998 & MAE ≤ 2.5 kW → <strong style={{ color: 'var(--text-primary)' }}>Achieved: R² 0.9991 & MAE 1.579 kW</strong> with CNN-LSTM + ENN.
                        </div>
                    </div>
                </div>
            </div>

            {/* Benchmark Cards */}
            <div>
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Model Benchmark Rankings</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {benchmarks.map((b, i) => (
                        <div key={b.tag} className={`benchmark-card ${b.isPrimary ? 'primary' : ''}`}>
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-12">
                                    <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>#{i + 1} {b.name}</span>
                                    {b.isPrimary && <span className="badge badge-amber">RECOMMENDED</span>}
                                </div>
                                <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>{b.latencyMs}ms latency</span>
                            </div>
                            <div className="bm-metrics">
                                {[
                                    { label: 'R² Score', val: b.r2.toFixed(4), good: b.r2 >= 0.998 },
                                    { label: 'MAE', val: `${b.maeKw} kW`, good: b.maeKw <= 2.5 },
                                    { label: 'RMSE', val: `${b.rmseKw} kW`, good: b.rmseKw <= 2.5 },
                                    { label: 'MAPE', val: `${b.mapePercent}%`, good: b.mapePercent <= 3.5 },
                                ].map(({ label, val, good }) => (
                                    <div className="bm-pill" key={label}>
                                        <div className="bm-metric-label">{label}</div>
                                        <div className="bm-metric-val" style={{ color: good ? 'var(--emerald)' : 'var(--text-primary)' }}>{val}</div>
                                    </div>
                                ))}
                            </div>
                            <div style={{ marginTop: 10, fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.5 }}>{b.description}</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Charts Row */}
            <div className="grid-2">
                {/* Radar */}
                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 12 }}>Model Performance Radar</div>
                    <ResponsiveContainer width="100%" height={240}>
                        <RadarChart data={radarData} cx="50%" cy="50%" outerRadius="70%">
                            <PolarGrid stroke="rgba(255,255,255,0.07)" />
                            <PolarAngleAxis dataKey="metric" tick={{ fontSize: 10, fill: '#64748B' }} />
                            <Radar name="CNN-LSTM+ENN" dataKey="CNN_LSTM" stroke="var(--amber)" fill="rgba(245,158,11,0.15)" />
                            <Radar name="LSTM" dataKey="LSTM" stroke="var(--cyan)" fill="rgba(34,211,238,0.08)" />
                            <Radar name="RF" dataKey="RF" stroke="var(--emerald)" fill="rgba(16,185,129,0.06)" />
                        </RadarChart>
                    </ResponsiveContainer>
                    <div className="flex gap-12" style={{ justifyContent: 'center', fontSize: 10, marginTop: 6 }}>
                        {[['CNN-LSTM+ENN', 'var(--amber)'], ['LSTM', 'var(--cyan)'], ['RF', 'var(--emerald)']].map(([n, c]) => (
                            <div key={n} className="flex items-center gap-8"><span style={{ width: 8, height: 8, borderRadius: '50%', background: c, display: 'inline-block' }} />{n}</div>
                        ))}
                    </div>
                </div>

                {/* Bar chart */}
                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 12 }}>MAE Comparison (kW)</div>
                    <ResponsiveContainer width="100%" height={240}>
                        <BarChart data={benchmarks.map(b => ({ name: b.tag.toUpperCase(), mae: b.maeKw, primary: b.isPrimary }))} margin={{ left: -20, bottom: 0 }}>
                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                            <XAxis dataKey="name" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                            <YAxis tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                            <Tooltip contentStyle={{ background: 'rgba(8,14,28,0.97)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, fontSize: 11 }} />
                            <Bar dataKey="mae" name="MAE (kW)" fill="var(--cyan)" radius={[4, 4, 0, 0]}
                                label={{ position: 'top', fontSize: 9, fill: '#94A3B8' }}
                            />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Neural Architecture */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>🧠 Neural Architecture Viewer</div>
                <div className="layer-flow">
                    {neuralLayers.map(l => (
                        <div className="layer-item" key={l.layerNumber}>
                            <div className="layer-num">{l.layerNumber}</div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div className="flex items-center justify-between">
                                    <span className="layer-type">{l.type}</span>
                                    <span className="layer-shape">{l.outputShape}</span>
                                </div>
                                <div className="layer-config">{l.configuration}</div>
                                <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 2 }}>{l.purpose}</div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Feature Importances */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 16 }}>📊 Input Feature Importance (SHAP/Permutation)</div>
                {featureImportances.map(f => (
                    <div className="feat-bar-wrap" key={f.name}>
                        <div className="feat-bar-label">
                            <span style={{ color: 'var(--text-secondary)', fontSize: 11 }}>{f.name}</span>
                            <div className="flex items-center gap-8">
                                <span className={`badge ${f.category === 'Solar' ? 'badge-amber' : f.category === 'Temporal' ? 'badge-cyan' : f.category === 'Weather' ? 'badge-emerald' : 'badge-purple'}`} style={{ fontSize: 9 }}>{f.category}</span>
                                <span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 11 }}>{(f.score * 100).toFixed(0)}%</span>
                            </div>
                        </div>
                        <div className="feat-bar-track">
                            <div className="feat-bar-fill" style={{
                                width: `${f.score * 100}%`,
                                background: f.category === 'Solar' ? 'var(--amber)' : f.category === 'Temporal' ? 'var(--cyan)' : f.category === 'Weather' ? 'var(--emerald)' : 'var(--purple)'
                            }} />
                        </div>
                    </div>
                ))}
            </div>

            {/* Model Card Modal */}
            {showModelCard && (
                <div className="modal-overlay" onClick={() => setShowModelCard(false)}>
                    <div className="modal-box" onClick={e => e.stopPropagation()}>
                        <div className="modal-title"><span style={{ fontSize: 18 }}>⚡</span> Model Card: SolarForecast-Hybrid-v1</div>
                        {[
                            ['Architecture', 'CNN-LSTM + Ensemble Neural Network'],
                            ['Input Dimension', '24 hours × 12 features (Irradiance, Weather, Lags)'],
                            ['Output Horizon', '24–168h hourly power forecasts (kW)'],
                            ['Training Data', '646 days, College 200 kW PV Plant'],
                            ['Validation R²', '0.9991 (Superior to standalone LSTM: 0.985)'],
                            ['Validation MAE', '1.579 kW (vs 2.401 kW baseline)'],
                            ['Validation RMSE', '1.850 kW'],
                            ['Inference Latency', '< 200ms per day-ahead forecast'],
                            ['Intended Use', 'Operational dispatch, grid stability, spot market trading'],
                            ['Known Limitations', 'Performance degrades under severe dust storms; requires plant calibration'],
                            ['Ethical/Privacy', 'Zero PII; advisory forecasts for decision support'],
                        ].map(([k, v]) => (
                            <div key={k} style={{ padding: '8px 0', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                                <div style={{ fontSize: 10, color: 'var(--amber)', fontWeight: 600 }}>{k}</div>
                                <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{v}</div>
                            </div>
                        ))}
                        <div style={{ marginTop: 16, textAlign: 'right' }}>
                            <button className="btn btn-amber" onClick={() => setShowModelCard(false)}>Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
