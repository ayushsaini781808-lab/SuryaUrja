import { Bolt, TrendingUp, Leaf, Settings, CalendarDays, BrainCircuit, CloudSun } from 'lucide-react';
import ForecastChart from '../components/ForecastChart';
import { WEATHER_CONDITIONS } from '../data/solarData';

const PERSONAS = [
    { id: 'PLANT_OPERATOR', label: 'Plant Operator', role: 'Operations & Maintenance', focus: 'Daily Peak & Curtailment Risk', icon: '🔧' },
    { id: 'GRID_MANAGER', label: 'Grid Manager', role: 'System Stability & Balancing', focus: 'Ramp-Rate & Voltage Variance', icon: '🔌' },
    { id: 'ENERGY_TRADER', label: 'Energy Trader', role: 'Market Bidding & Hedging', focus: 'Day-Ahead Margin & CI Bounds', icon: '📈' },
    { id: 'RESEARCHER', label: 'Researcher / DS', role: 'Model Evaluation & AI', focus: 'Loss Metrics (R², MAE, RMSE)', icon: '🔬' },
];

const PERSONA_INSIGHTS = {
    PLANT_OPERATOR: (peak, cap) => `Peak ENN forecast ${peak.toFixed(1)} kW exceeds 88% of ${cap} kW capacity. Schedule inverter maintenance during 00:00–05:00 low-irradiance window.`,
    GRID_MANAGER: (peak, cap) => `Solar ramp-rate at 09:00–11:00 projected at +${(peak * 0.06).toFixed(1)} kW/hour. Pre-activate reserve generators.`,
    ENERGY_TRADER: (peak, cap) => `Day-ahead spot bid window: 10:00–15:00. CNN-LSTM+ENN CI bounds ±${(peak * 0.06).toFixed(1)} kW. Recommend risk-adjusted 92% bid volume.`,
    RESEARCHER: () => `CNN spatial block extracts 64 irradiance feature maps. LSTM captures 168h temporal horizon. ENN achieves R² 0.9991 on 646-day holdout.`,
};

function MetricCard({ title, value, badge, badgeClass, icon: Icon, iconColor, onClick }) {
    return (
        <div className="metric-card" style={{ cursor: onClick ? 'pointer' : 'default' }} onClick={onClick}>
            <div className="metric-card-header">
                <span className="metric-label">{title}</span>
                <Icon size={16} color={iconColor} />
            </div>
            <div className="metric-value">{value}</div>
            <span className={`badge ${badgeClass}`}>{badge}</span>
        </div>
    );
}

function SunPathWidget({ hour, ghi, dni, dhi }) {
    const sunAngle = ((hour - 6) / 12) * Math.PI; // 6am to 6pm
    const cx = 50 + Math.cos(Math.PI - sunAngle) * 40;
    const cy = 95 - Math.sin(Math.max(0, sunAngle)) * 70;
    const isDaylight = hour >= 6 && hour <= 18;

    return (
        <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 10 }}>☀ Solar Arc & Sun Path</div>
            <div className="sun-arc" style={{ height: 100, borderRadius: 10, marginBottom: 0 }}>
                <svg viewBox="0 0 100 100" width="100%" height="100%">
                    <path d="M5 90 Q50 20 95 90" stroke="rgba(245,158,11,0.2)" strokeWidth="1.5" fill="none" strokeDasharray="4 3" />
                    <line x1="5" y1="90" x2="95" y2="90" stroke="rgba(255,255,255,0.06)" strokeWidth="1" />
                    {isDaylight && (
                        <>
                            <circle cx={cx} cy={cy} r="5" fill="var(--amber)" filter="url(#glow)" />
                            <circle cx={cx} cy={cy} r="9" fill="rgba(245,158,11,0.2)" />
                        </>
                    )}
                    <text x="5" y="98" fill="#475569" fontSize="6">06:00</text>
                    <text x="44" y="18" fill="#64748B" fontSize="6">NOON</text>
                    <text x="80" y="98" fill="#475569" fontSize="6">18:00</text>
                    <defs><filter id="glow"><feGaussianBlur stdDeviation="2" result="coloredBlur" /><feMerge><feMergeNode in="coloredBlur" /><feMergeNode in="SourceGraphic" /></feMerge></filter></defs>
                </svg>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginTop: 10 }}>
                {[['GHI', ghi, 'W/m²', 'var(--amber)'], ['DNI', dni, 'W/m²', 'var(--cyan)'], ['DHI', dhi, 'W/m²', 'var(--emerald)']].map(([l, v, u, c]) => (
                    <div key={l} style={{ textAlign: 'center', background: 'rgba(255,255,255,0.03)', borderRadius: 8, padding: '6px 4px' }}>
                        <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>{l}</div>
                        <div style={{ fontSize: 15, fontWeight: 700, color: c }}>{v}</div>
                        <div style={{ fontSize: 9, color: 'var(--text-muted)' }}>{u}</div>
                    </div>
                ))}
            </div>
        </div>
    );
}

function WeatherMetrics({ point }) {
    if (!point) return null;
    const wc = WEATHER_CONDITIONS[point.condition] || WEATHER_CONDITIONS.CLEAR_SUNNY;
    const items = [
        { label: 'Temperature', value: point.temperature, unit: '°C', color: 'var(--amber)' },
        { label: 'Humidity', value: point.humidity, unit: '%', color: 'var(--cyan)' },
        { label: 'Wind Speed', value: point.windSpeed, unit: 'm/s', color: 'var(--text-primary)' },
        { label: 'Cloud Cover', value: point.cloudCover, unit: '%', color: 'var(--text-secondary)' },
        { label: 'Condition', value: wc.icon, unit: wc.label, color: 'var(--amber)', isText: true },
        { label: 'Model', value: 'CNN+ENN', unit: '', color: 'var(--purple)', isText: true },
    ];
    return (
        <div className="weather-grid">
            {items.map(({ label, value, unit, color, isText }) => (
                <div className="weather-cell" key={label}>
                    <div className="weather-label">{label}</div>
                    <div className="weather-value" style={{ color }}>{isText ? `${value} ` : value}</div>
                    <div className="weather-unit">{unit}</div>
                </div>
            ))}
        </div>
    );
}

export default function DashboardPage({
    plants, selectedPlant, onSelectPlant,
    selectedPersona, onSelectPersona,
    daySummaries, selectedDayIndex,
    activeHourPoint, onSelectHour,
    overview, setPage,
}) {
    const currentDay = daySummaries[selectedDayIndex] || daySummaries[0];
    const persona = PERSONAS.find(p => p.id === selectedPersona) || PERSONAS[0];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* Metric Grid */}
            <div className="metric-grid">
                <MetricCard title="Current Generation" value={`${overview.currentGenerationKw} kW`}
                    badge={`${overview.capacityUtilizationPercent}% Capacity`} badgeClass="badge-amber"
                    icon={Bolt} iconColor="var(--amber)" />
                <MetricCard title="Day-Ahead Peak" value={`${overview.nextDayPeakKw} kW`}
                    badge="Peak at 12:30" badgeClass="badge-cyan"
                    icon={TrendingUp} iconColor="var(--cyan)" />
                <MetricCard title="Day Expected Total" value={`${Math.round(overview.todayTotalKwh)} kWh`}
                    badge="Clean Solar Power" badgeClass="badge-emerald"
                    icon={Leaf} iconColor="var(--emerald)" />
                <MetricCard title="CNN-LSTM + ENN" value={`R² ${overview.modelR2}`}
                    badge={`MAE ${overview.dayAheadMaeKw} kW`} badgeClass="badge-purple"
                    icon={BrainCircuit} iconColor="var(--purple)" onClick={() => setPage('models')} />
            </div>

            {/* Persona Selector */}
            <div className="card">
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Operational View Mode</div>
                <div className="persona-grid">
                    {PERSONAS.map(p => (
                        <div key={p.id} className={`persona-card ${selectedPersona === p.id ? 'active' : ''}`}
                            onClick={() => onSelectPersona(p.id)}>
                            <div style={{ fontSize: 16, marginBottom: 3 }}>{p.icon}</div>
                            <div className="p-title">{p.label}</div>
                            <div className="p-role">{p.role}</div>
                        </div>
                    ))}
                </div>
                <div style={{ marginTop: 12, padding: '10px 14px', background: 'rgba(245,158,11,0.07)', borderRadius: 8, border: '1px solid rgba(245,158,11,0.2)', fontSize: 11, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                    <span style={{ color: 'var(--amber)', fontWeight: 600 }}>💡 {persona.focus}: </span>
                    {PERSONA_INSIGHTS[selectedPersona]?.(overview.nextDayPeakKw, selectedPlant.capacityKw)}
                </div>
            </div>

            {/* Forecast Chart */}
            <div className="card">
                <div className="flex items-center justify-between mb-12">
                    <div>
                        <div className="section-title" style={{ marginBottom: 2 }}>Hourly Power: Actual vs. Predicted</div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Interactive — click the chart to inspect SCADA vs. CNN-LSTM telemetry</div>
                    </div>
                    <button className="btn btn-outline btn-sm" onClick={() => setPage('multiday')} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <CalendarDays size={12} /> 7-Day
                    </button>
                </div>

                <ForecastChart
                    points={currentDay?.hourlyPoints || []}
                    capacityKw={selectedPlant.capacityKw}
                    showActual={selectedDayIndex === 0}
                    showPredicted
                    showConfidence
                    showClearSky
                    selectedHour={activeHourPoint?.hour}
                    onHourSelect={onSelectHour}
                />
                <div style={{ marginTop: 10, padding: '6px 10px', background: 'rgba(15,23,42,0.8)', borderRadius: 8, fontSize: 10, color: '#94A3B8' }}>
                    💡 Click any point on the chart to view live telemetry for that hour. Toggle legend items to show/hide curves.
                </div>
            </div>

            {/* Two-column: Sun Path + Weather Metrics */}
            <div className="grid-2">
                <SunPathWidget hour={activeHourPoint?.hour ?? 12} ghi={activeHourPoint?.ghi ?? 780} dni={activeHourPoint?.dni ?? 640} dhi={activeHourPoint?.dhi ?? 140} />
                <div className="card">
                    <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 10 }}>
                        🌡 Meteorological Conditions – {activeHourPoint?.timeLabel ?? '12:00'}
                    </div>
                    <WeatherMetrics point={activeHourPoint} />
                </div>
            </div>

        </div>
    );
}
