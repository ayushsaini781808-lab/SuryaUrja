import { useState, useCallback } from 'react';
import { RefreshCw, MapPin, Cloud, Thermometer, Wind, Droplets } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { fetchOpenMeteoWeather, mapOpenMeteoToHourlyPoints, extractMeta } from '../api/index';
import { SOLAR_PARK_PRESETS, WEATHER_CONDITIONS } from '../data/solarData';

const PIPELINE_STEPS = [
    { title: '1. GPS / Preset Location', desc: 'Select a preset solar park or enter custom lat/lon. The FusedLocationProvider interface is abstracted for future GPS integration.' },
    { title: '2. Open-Meteo API Fetch (Live)', desc: 'Free solar irradiance + weather NWP model. Returns hourly GHI, DNI, DHI, temperature, cloudcover for 7 days — no API key required.' },
    { title: '3. Feature Engineering', desc: 'GHI/DNI/DHI → clearness index. Temperature derating: η = ηₛ × (1 − 0.004 × (T − 25)). Lag features t−1, t−24 appended.' },
    { title: '4. CNN-LSTM + ENN Inference', desc: 'Spatiotemporal CNN block extracts 64 irradiance maps → LSTM 256→128 units models temporal dynamics → ENN 3-head ensemble refines day-ahead uncertainty.' },
    { title: '5. Forecast Committed to Dashboard', desc: '24-hour + 7-day profiles with confidence intervals deployed to chart components. SCADA actuals streamed when available.' },
];

const DATA_SOURCES = [
    { name: 'Open-Meteo NWP', type: 'Solar Irradiance + Weather', status: 'LIVE', color: 'var(--emerald)' },
    { name: 'NASA POWER API', type: 'Long-Range Climatological', status: 'FALLBACK', color: 'var(--cyan)' },
    { name: 'SCADA Telemetry', type: 'On-site Actual Generation', status: 'SIMULATED', color: 'var(--amber)' },
    { name: 'Pyranometer Feed', type: 'In-situ GHI Measurement', status: 'SIMULATED', color: 'var(--purple)' },
];

function SimPanel({ cloudDelta, tempDelta, isSimulating, onSimulate, onResetSim }) {
    const [localCloud, setLocalCloud] = useState(cloudDelta);
    const [localTemp, setLocalTemp] = useState(tempDelta);

    return (
        <div className="card card-amber">
            <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 4 }}>🌦 CNN-LSTM What-If Simulator</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 16, lineHeight: 1.6 }}>
                Adjust cloud cover and temperature offsets to simulate alternative weather scenarios. ENN re-infers within ~184ms.
            </div>

            <div className="slider-wrap">
                <div className="slider-labels">
                    <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Cloud Cover Δ</span>
                    <span style={{ fontWeight: 700, color: 'var(--amber)' }}>{localCloud >= 0 ? '+' : ''}{Math.round(localCloud)}%</span>
                </div>
                <input type="range" min={-50} max={80} step={1} value={localCloud}
                    onChange={e => setLocalCloud(parseFloat(e.target.value))} />
            </div>

            <div className="slider-wrap">
                <div className="slider-labels">
                    <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Temperature Δ</span>
                    <span style={{ fontWeight: 700, color: 'var(--cyan)' }} >{localTemp >= 0 ? '+' : ''}{localTemp.toFixed(1)}°C</span>
                </div>
                <input className="slider-cyan" type="range" min={-10} max={15} step={0.5} value={localTemp}
                    onChange={e => setLocalTemp(parseFloat(e.target.value))} />
            </div>

            <div className="flex gap-12" style={{ marginTop: 4 }}>
                <button className="btn btn-amber" disabled={isSimulating} style={{ flex: 1 }}
                    onClick={() => onSimulate(localCloud, localTemp)}>
                    {isSimulating ? '⚡ Re-inferring…' : '⚡ Run CNN-LSTM Inference'}
                </button>
                <button className="btn btn-outline" onClick={() => { setLocalCloud(0); setLocalTemp(0); onResetSim(); }}>
                    Reset
                </button>
            </div>
        </div>
    );
}

export default function WeatherPage({
    selectedPlant, cloudDelta, tempDelta, isSimulating, onSimulate, onResetSim,
    locationName, latitude, longitude, isSyncing, isSynced, syncTime,
    onSyncWeather, showToast,
}) {
    const [liveData, setLiveData] = useState(null);
    const [liveLoading, setLiveLoading] = useState(false);
    const [liveError, setLiveError] = useState(null);
    const [liveTimezone, setLiveTimezone] = useState(null);

    const doFetch = useCallback(async (lat, lon, name) => {
        setLiveLoading(true); setLiveError(null);
        try {
            const raw = await fetchOpenMeteoWeather(lat, lon);
            const pts = mapOpenMeteoToHourlyPoints(raw, selectedPlant.capacityKw);
            const meta = extractMeta(raw);
            setLiveData(pts); setLiveTimezone(meta.timezone);
            onSyncWeather(lat, lon, name);
            showToast?.(`Live Open-Meteo data synced for ${name} (TZ: ${meta.timezone})`);
        } catch (e) {
            setLiveError(e.message);
            showToast?.(`Weather fetch failed: ${e.message}`);
        } finally {
            setLiveLoading(false);
        }
    }, [selectedPlant.capacityKw, onSyncWeather, showToast]);

    const chartData = (liveData || [])
        .filter(p => p.hour >= 5 && p.hour <= 19)
        .map(p => ({ time: p.timeLabel, ghi: p.ghi, power: p.predictedKw, temp: p.temperature, cloud: p.cloudCover }));

    const currentLive = liveData?.find(p => p.hour === new Date().getHours()) || liveData?.[12];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* Location Selector */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>📍 Location & Sync</div>
                <div style={{ marginBottom: 12 }}>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 6 }}>Preset Solar Parks</div>
                    <div className="chip-row">
                        {SOLAR_PARK_PRESETS.map(p => (
                            <button key={p.name} className={`chip ${locationName === p.name ? 'active-amber' : ''}`}
                                onClick={() => doFetch(p.latitude, p.longitude, p.name)}>
                                {p.name.split('(')[0].trim()}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="location-coords">
                    📍 {locationName}<br />
                    Lat: {latitude.toFixed(4)}° N  |  Lon: {longitude.toFixed(4)}° E
                    {liveTimezone && `  |  TZ: ${liveTimezone}`}
                    {syncTime && `  |  Last sync: ${syncTime}`}
                </div>

                <button className="btn btn-amber" disabled={liveLoading}
                    onClick={() => doFetch(latitude, longitude, locationName)}>
                    <RefreshCw size={13} style={{ animation: liveLoading ? 'spin 1s linear infinite' : 'none' }} />
                    {liveLoading ? 'Fetching Open-Meteo…' : 'Sync Live Weather'}
                </button>

                {liveError && (
                    <div style={{ marginTop: 10, fontSize: 11, color: 'var(--rose)' }}>⚠ {liveError}</div>
                )}
                {isSynced && !liveError && (
                    <div style={{ marginTop: 10, fontSize: 11, color: 'var(--emerald)' }}>✓ Live weather data active from Open-Meteo</div>
                )}
            </div>

            {/* Live conditions if fetched */}
            {currentLive && (
                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 12 }}>🌡 Current Conditions</div>
                    <div className="weather-grid">
                        {[
                            { label: 'GHI', value: currentLive.ghi, unit: 'W/m²', Icon: Cloud, color: 'var(--amber)' },
                            { label: 'DNI', value: currentLive.dni, unit: 'W/m²', Icon: Cloud, color: 'var(--cyan)' },
                            { label: 'Temp', value: currentLive.temperature, unit: '°C', Icon: Thermometer, color: 'var(--rose)' },
                            { label: 'Cloud', value: currentLive.cloudCover, unit: '%', Icon: Cloud, color: 'var(--text-secondary)' },
                            { label: 'Wind', value: currentLive.windSpeed, unit: 'm/s', Icon: Wind, color: 'var(--emerald)' },
                            { label: 'Humidity', value: currentLive.humidity, unit: '%', Icon: Droplets, color: 'var(--purple)' },
                        ].map(({ label, value, unit, Icon, color }) => (
                            <div className="weather-cell" key={label}>
                                <div className="flex gap-8 items-center mb-8"><Icon size={12} color={color} /><span className="weather-label">{label}</span></div>
                                <div className="weather-value" style={{ color }}>{value}</div>
                                <div className="weather-unit">{unit}</div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Live chart */}
            {chartData.length > 0 && (
                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 12 }}>📊 Live GHI & Forecast Power (Open-Meteo)</div>
                    <ResponsiveContainer width="100%" height={220}>
                        <LineChart data={chartData} margin={{ left: -20, right: 10 }}>
                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                            <XAxis dataKey="time" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                            <YAxis tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                            <Tooltip contentStyle={{ background: 'rgba(8,14,28,0.97)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, fontSize: 11 }} />
                            <Line dataKey="ghi" name="GHI (W/m²)" stroke="var(--amber)" strokeWidth={2} dot={false} />
                            <Line dataKey="power" name="PV Power (kW)" stroke="var(--cyan)" strokeWidth={2} dot={false} />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            )}

            {/* Simulation Panel */}
            <SimPanel cloudDelta={cloudDelta} tempDelta={tempDelta} isSimulating={isSimulating}
                onSimulate={onSimulate} onResetSim={onResetSim} />

            {/* Two column: Data Sources + Pipeline */}
            <div className="grid-2">
                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 14 }}>🔌 Data Pipeline Sources</div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                        {DATA_SOURCES.map(src => (
                            <div key={src.name} className="api-feed">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <div className="api-feed-name">{src.name}</div>
                                        <div className="api-feed-type">{src.type}</div>
                                    </div>
                                    <div className="api-feed-status" style={{ color: src.color }}>{src.status}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="card">
                    <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 14 }}>⚙️ Inference Pipeline</div>
                    {PIPELINE_STEPS.map(s => (
                        <div className="pipeline-step" key={s.title}>
                            <div className="pipeline-step-title">{s.title}</div>
                            <div className="pipeline-step-desc">{s.desc}</div>
                        </div>
                    ))}
                </div>
            </div>

            <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
        </div>
    );
}
