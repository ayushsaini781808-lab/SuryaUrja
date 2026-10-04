import {
    LineChart, Line, Area, AreaChart, XAxis, YAxis, CartesianGrid,
    Tooltip, Legend, ResponsiveContainer, ReferenceLine,
} from 'recharts';

const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
        <div style={{
            background: 'rgba(8,14,28,0.97)', border: '1px solid rgba(255,255,255,0.12)',
            borderRadius: 10, padding: '10px 14px', fontSize: 11,
        }}>
            <div style={{ color: 'var(--amber)', fontWeight: 700, marginBottom: 6 }}>{label}</div>
            {payload.map(p => (
                <div key={p.dataKey} className="flex items-center gap-8" style={{ marginBottom: 3 }}>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', background: p.color, display: 'inline-block' }} />
                    <span style={{ color: 'var(--text-secondary)' }}>{p.name}:</span>
                    <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}> {typeof p.value === 'number' ? p.value.toFixed(1) : p.value} kW</span>
                </div>
            ))}
        </div>
    );
};

export default function ForecastChart({
    points = [],
    capacityKw = 200,
    showActual = true,
    showPredicted = true,
    showConfidence = true,
    showClearSky = true,
    selectedHour = null,
    onHourSelect = null,
}) {
    const data = points.map(p => ({
        time: p.timeLabel,
        hour: p.hour,
        predicted: p.predictedKw,
        actual: p.actualKw,
        lower: p.lowerBoundKw,
        upper: p.upperBoundKw,
        clearSky: p.clearSkyKw,
        band: [p.lowerBoundKw, p.upperBoundKw],
    })).filter(p => p.hour >= 4 && p.hour <= 21);

    return (
        <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={data} margin={{ top: 8, right: 16, left: -20, bottom: 0 }}
                onClick={e => {
                    if (onHourSelect && e?.activePayload?.[0]) {
                        const h = e.activePayload[0].payload.hour;
                        onHourSelect(points.find(p => p.hour === h) || null);
                    }
                }}
            >
                <defs>
                    <linearGradient id="pGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#F59E0B" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#F59E0B" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="aGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#22D3EE" stopOpacity={0.2} />
                        <stop offset="95%" stopColor="#22D3EE" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="ciGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#A78BFA" stopOpacity={0.12} />
                        <stop offset="95%" stopColor="#A78BFA" stopOpacity={0} />
                    </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#64748B' }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#64748B' }} tickLine={false} axisLine={false} domain={[0, capacityKw]} />
                <Tooltip content={<CustomTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11, paddingTop: 8 }} />
                <ReferenceLine y={capacityKw} stroke="rgba(244,63,94,0.4)" strokeDasharray="4 4" label={{ value: 'Capacity', fontSize: 9, fill: '#F43F5E' }} />
                {selectedHour !== null && <ReferenceLine x={String(selectedHour).padStart(2, '0') + ':00'} stroke="var(--amber)" strokeDasharray="4 4" />}

                {showConfidence && (
                    <Area dataKey="upper" name="Upper CI" fill="url(#ciGrad)" stroke="rgba(167,139,250,0.3)" strokeWidth={0} activeDot={false} legendType="none" />
                )}
                {showConfidence && (
                    <Area dataKey="lower" name="Lower CI" fill="rgba(167,139,250,0)" stroke="rgba(167,139,250,0.3)" strokeWidth={0} activeDot={false} legendType="none" />
                )}
                {showClearSky && (
                    <Area dataKey="clearSky" name="Clear-Sky" type="monotone" stroke="rgba(255,255,255,0.18)" strokeDasharray="4 3" dot={false} fill="transparent" strokeWidth={1} />
                )}
                {showPredicted && (
                    <Area dataKey="predicted" name="CNN-LSTM+ENN Forecast" type="monotone" stroke="#F59E0B" strokeWidth={2.5} fill="url(#pGrad)" dot={false} activeDot={{ r: 4, fill: '#F59E0B' }} />
                )}
                {showActual && (
                    <Area dataKey="actual" name="Actual SCADA" type="monotone" stroke="#22D3EE" strokeWidth={2} fill="url(#aGrad)" dot={false} activeDot={{ r: 4, fill: '#22D3EE' }} />
                )}
            </AreaChart>
        </ResponsiveContainer>
    );
}
