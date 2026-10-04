import { Download } from 'lucide-react';
import ForecastChart from '../components/ForecastChart';
import { WEATHER_CONDITIONS } from '../data/solarData';

export default function MultiDayPage({
    daySummaries, selectedDayIndex, onSelectDay,
    activeHourPoint, onSelectHour,
    selectedPlant, onExportCsv,
}) {
    const selectedDay = daySummaries[selectedDayIndex] || daySummaries[0];
    const capacity = selectedPlant?.capacityKw || 200;

    const daylightPts = (selectedDay?.hourlyPoints || []).filter(p => p.hour >= 5 && p.hour <= 19);

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <div className="section-title">7-Day Multi-Day Solar Forecast</div>
                    <div className="section-sub">Spatiotemporal CNN-LSTM hourly projections with ENN refinement</div>
                </div>
                <button className="btn btn-amber" onClick={onExportCsv}>
                    <Download size={14} /> Export CSV
                </button>
            </div>

            {/* Day Carousel */}
            <div className="day-carousel">
                {daySummaries.map((day, idx) => {
                    const wc = WEATHER_CONDITIONS[day.predominantWeather] || WEATHER_CONDITIONS.CLEAR_SUNNY;
                    const isHistorical = idx === 0;
                    return (
                        <div key={idx} className={`day-card ${selectedDayIndex === idx ? 'active' : ''}`} onClick={() => onSelectDay(idx)}>
                            <div className="flex justify-between items-center">
                                <span className="dc-label" style={{ color: selectedDayIndex === idx ? 'var(--amber)' : 'var(--text-muted)' }}>
                                    {isHistorical ? 'Yesterday' : idx === 1 ? 'Today' : `Day +${idx}`}
                                </span>
                                <span>{wc.icon}</span>
                            </div>
                            <div className="dc-peak" style={{ color: selectedDayIndex === idx ? 'var(--amber)' : 'var(--text-primary)' }}>
                                {Math.round(day.peakPowerKw)} kW
                            </div>
                            <div className="dc-total">{Math.round(day.totalEnergyKwh)} kWh</div>
                            <div style={{ marginTop: 4 }}>
                                <span className={`badge ${isHistorical ? 'badge-cyan' : 'badge-emerald'}`} style={{ fontSize: 9 }}>
                                    {isHistorical ? 'Actual SCADA' : 'ENN Forecast'}
                                </span>
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Selected Day Chart */}
            <div className="card">
                <div style={{ marginBottom: 12 }}>
                    <div style={{ fontWeight: 700, fontSize: 14 }}>{selectedDay?.dayName} Hourly Profile</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        Peak: {selectedDay?.peakPowerKw} kW • Total: {selectedDay?.totalEnergyKwh} kWh • {WEATHER_CONDITIONS[selectedDay?.predominantWeather]?.label}
                    </div>
                </div>
                <ForecastChart
                    points={selectedDay?.hourlyPoints || []}
                    capacityKw={capacity}
                    showActual={selectedDayIndex === 0}
                    showPredicted
                    showConfidence
                    showClearSky
                    selectedHour={activeHourPoint?.hour}
                    onHourSelect={onSelectHour}
                />
            </div>

            {/* Hourly Table */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Hourly Breakdown & Confidence Intervals (kW)</div>
                <div style={{ overflowX: 'auto' }}>
                    <table className="forecast-table">
                        <thead>
                            <tr>
                                <th>Hour</th>
                                <th>Condition</th>
                                <th>Predicted</th>
                                <th>CI Range</th>
                                {selectedDayIndex === 0 && <th>Actual SCADA</th>}
                                {selectedDayIndex === 0 && <th>Δ Error</th>}
                                <th>GHI W/m²</th>
                                <th>Temp °C</th>
                                <th>Cloud %</th>
                            </tr>
                        </thead>
                        <tbody>
                            {daylightPts.map(pt => {
                                const wc = WEATHER_CONDITIONS[pt.condition] || {};
                                const delta = pt.actualKw != null ? pt.actualKw - pt.predictedKw : null;
                                const isSelected = activeHourPoint?.hour === pt.hour;
                                return (
                                    <tr key={pt.hour} className={isSelected ? 'row-selected' : ''} onClick={() => onSelectHour(pt)}>
                                        <td style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{pt.timeLabel}</td>
                                        <td>{wc.icon} <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>{wc.label}</span></td>
                                        <td style={{ color: 'var(--amber)', fontWeight: 700 }}>{pt.predictedKw}</td>
                                        <td style={{ fontSize: 10, color: 'var(--text-muted)', fontFamily: 'monospace' }}>[{pt.lowerBoundKw} – {pt.upperBoundKw}]</td>
                                        {selectedDayIndex === 0 && <td style={{ color: 'var(--cyan)', fontWeight: 600 }}>{pt.actualKw ?? '—'}</td>}
                                        {selectedDayIndex === 0 && (
                                            <td style={{ color: delta != null ? (Math.abs(delta) > 10 ? 'var(--rose)' : 'var(--emerald)') : 'var(--text-muted)', fontWeight: 600 }}>
                                                {delta != null ? (delta >= 0 ? `+${delta.toFixed(1)}` : delta.toFixed(1)) : '—'}
                                            </td>
                                        )}
                                        <td style={{ color: 'var(--text-secondary)' }}>{pt.ghi}</td>
                                        <td style={{ color: 'var(--text-secondary)' }}>{pt.temperature}</td>
                                        <td style={{ color: 'var(--text-muted)' }}>{pt.cloudCover}%</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
    );
}
