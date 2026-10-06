
// ─── Plants ───────────────────────────────────────────────────────────────────
export const PLANTS = [
    { id: "plant_college_200", name: "Campus 200kW Solar Plant", capacityKw: 200, panelType: "Mono PERC", location: "Delhi, India", tiltAngle: 28, azimuth: 180, commissionDays: 646 },
    { id: "plant_rooftop_50", name: "Rooftop 50kW Array", capacityKw: 50, panelType: "Poly Si", location: "Jaipur, India", tiltAngle: 22, azimuth: 175, commissionDays: 387 },
    { id: "plant_field_500", name: "Field Farm 500kW Park", capacityKw: 500, panelType: "Bifacial", location: "Chennai, India", tiltAngle: 12, azimuth: 185, commissionDays: 210 },
];

// ─── Weather Conditions ───────────────────────────────────────────────────────
export const WEATHER_CONDITIONS = {
    CLEAR_SUNNY: { label: "Clear Sky", icon: "☀️", clearnessIndex: 0.95 },
    PARTLY_CLOUDY: { label: "Partly Cloudy", icon: "⛅", clearnessIndex: 0.75 },
    SCATTERED_CLOUDS: { label: "Scattered Clouds", icon: "🌤️", clearnessIndex: 0.65 },
    OVERCAST: { label: "Overcast", icon: "☁️", clearnessIndex: 0.35 },
    RAIN_HAZE: { label: "Atmospheric Rain/Dust", icon: "🌧️", clearnessIndex: 0.20 },
};

// ─── Generate hourly profile ──────────────────────────────────────────────────
function generateHourlyPoints(capacityKw, cloudDelta = 0, tempDelta = 0, includeActual = false) {
    const ghiProfile = [0, 0, 0, 0, 0, 60, 140, 320, 520, 680, 780, 840, 860, 820, 760, 640, 480, 300, 140, 60, 0, 0, 0, 0];
    return Array.from({ length: 24 }, (_, h) => {
        const rawGhi = Math.max(0, ghiProfile[h] * (1 - (cloudDelta * 0.012)));
        const ghi = Math.round(rawGhi);
        const dni = Math.round(rawGhi * 0.82);
        const dhi = Math.round(rawGhi * 0.18);
        const clearSky = Math.round((rawGhi / 1000) * capacityKw * 0.80 * 10) / 10;
        const baseKw = Math.round(((rawGhi / 1000) * capacityKw * 0.75 * (1 - Math.max(0, (25 + tempDelta - 25) * 0.0035))) * 10) / 10;
        const lower = Math.round((baseKw * 0.94) * 10) / 10;
        const upper = Math.round((baseKw * 1.06) * 10) / 10;
        const cloudPct = Math.min(100, Math.max(0, (h >= 5 && h <= 19 ? 15 + cloudDelta : 0)));
        let cond = "CLEAR_SUNNY";
        if (cloudPct > 60) cond = "OVERCAST";
        else if (cloudPct > 40) cond = "PARTLY_CLOUDY";
        else if (cloudPct > 20) cond = "SCATTERED_CLOUDS";

        return {
            hour: h,
            timeLabel: `${String(h).padStart(2, "0")}:00`,
            actualKw: includeActual && h >= 5 && h <= 19 ? Math.round((baseKw * (0.96 + Math.random() * 0.08)) * 10) / 10 : null,
            predictedKw: baseKw,
            lowerBoundKw: lower,
            upperBoundKw: upper,
            clearSkyKw: clearSky,
            ghi, dni, dhi,
            temperature: Math.round((28 + tempDelta + (h >= 10 && h <= 16 ? 6 : 0) + (Math.random() * 2 - 1)) * 10) / 10,
            humidity: Math.round(45 + cloudDelta * 0.5),
            windSpeed: Math.round((3.2 + Math.random()) * 10) / 10,
            cloudCover: Math.round(cloudPct),
            condition: cond,
            modelUsed: "CNN-LSTM + ENN",
        };
    });
}

// ─── 7-Day Summaries ─────────────────────────────────────────────────────────
function makeDaySummary(daysFromNow, capacityKw, cloudDelta = 0, tempDelta = 0) {
    const d = new Date();
    d.setDate(d.getDate() + daysFromNow);
    const dayName = daysFromNow === -1 ? "Yesterday" : daysFromNow === 0 ? "Today" : d.toLocaleDateString("en-US", { weekday: "long" });
    const dateStr = d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
    const pts = generateHourlyPoints(capacityKw, cloudDelta, tempDelta, daysFromNow === -1);
    const peakKw = Math.max(...pts.map(p => p.predictedKw));
    const totalKwh = Math.round(pts.reduce((s, p) => s + p.predictedKw, 0) * 10) / 10;
    const cloudPct = cloudDelta + 15;
    let weather = "CLEAR_SUNNY";
    if (cloudPct > 60) weather = "OVERCAST";
    else if (cloudPct > 40) weather = "PARTLY_CLOUDY";
    else if (cloudPct > 20) weather = "SCATTERED_CLOUDS";

    return {
        dateString: dateStr, dayName, peakPowerKw: peakKw, totalEnergyKwh: totalKwh,
        averageR2: 0.9991, maeKw: 1.579, predominantWeather: weather, hourlyPoints: pts
    };
}

export function buildDaySummaries(capacityKw, extraCloud = 0, extraTemp = 0) {
    return [
        makeDaySummary(-1, capacityKw, 5 + extraCloud, 0 + extraTemp),    // Yesterday
        makeDaySummary(0, capacityKw, 0 + extraCloud, 0 + extraTemp),    // Today
        makeDaySummary(1, capacityKw, 10 + extraCloud, 2 + extraTemp),
        makeDaySummary(2, capacityKw, 25 + extraCloud, 4 + extraTemp),
        makeDaySummary(3, capacityKw, 0 + extraCloud, -1 + extraTemp),
        makeDaySummary(4, capacityKw, 35 + extraCloud, 0 + extraTemp),
        makeDaySummary(5, capacityKw, 5 + extraCloud, 1 + extraTemp),
    ];
}

// ─── Model Benchmarks ─────────────────────────────────────────────────────────
export const BENCHMARKS = [
    { name: "CNN-LSTM + ENN (Hybrid)", tag: "primary", r2: 0.9991, maeKw: 1.579, rmseKw: 1.850, mapePercent: 1.23, latencyMs: 184, isPrimary: true, description: "Spatiotemporal CNN extracts irradiance feature maps; LSTM captures temporal dependencies; ENN refines day-ahead predictions." },
    { name: "Standalone LSTM", tag: "lstm", r2: 0.985, maeKw: 2.401, rmseKw: 2.890, mapePercent: 2.41, latencyMs: 120, isPrimary: false, description: "Long Short-Term Memory baseline with 3-layer stacked architecture and 128 hidden units per layer." },
    { name: "Random Forest Ensemble", tag: "rf", r2: 0.972, maeKw: 3.104, rmseKw: 3.750, mapePercent: 3.18, latencyMs: 45, isPrimary: false, description: "100-tree Random Forest on lagged meteorological features. High interpretability, moderate accuracy." },
    { name: "XGBoost Gradient Boost", tag: "xgb", r2: 0.968, maeKw: 3.842, rmseKw: 4.120, mapePercent: 3.89, latencyMs: 32, isPrimary: false, description: "Extreme Gradient Boosting with depth-4 trees, learning rate 0.05." },
    { name: "Linear Regression (Baseline)", tag: "lr", r2: 0.921, maeKw: 6.201, rmseKw: 7.450, mapePercent: 6.82, latencyMs: 3, isPrimary: false, description: "OLS baseline using GHI, temperature, and time-of-day. Reference benchmark only." },
];

// ─── Neural Architecture ──────────────────────────────────────────────────────
export const NEURAL_LAYERS = [
    { layerNumber: 1, type: "Input", configuration: "24h × 12 features", outputShape: "(B, 24, 12)", purpose: "Weather + lagged PV sequences" },
    { layerNumber: 2, type: "1D CNN Block", configuration: "3×[Conv1D(64, k=3) + BN + ReLU] + MaxPool", outputShape: "(B, 12, 64)", purpose: "Local irradiance spatial patterns" },
    { layerNumber: 3, type: "LSTM Stack", configuration: "[LSTM(256, dropout=0.2) → LSTM(128)]", outputShape: "(B, 128)", purpose: "Temporal dependency modeling" },
    { layerNumber: 4, type: "ENN Refiner", configuration: "3-head ensemble: [Dense(64) × 3] → weighted avg", outputShape: "(B, 24)", purpose: "Day-ahead uncertainty refinement" },
    { layerNumber: 5, type: "Output", configuration: "Linear + Clamp(0, capacity_kW)", outputShape: "(B, 24)", purpose: "Hourly PV power forecasts (kW)" },
];

export const FEATURE_IMPORTANCES = [
    { name: "GHI (Global Horizontal Irradiance)", score: 0.92, category: "Solar" },
    { name: "DNI (Direct Normal Irradiance)", score: 0.88, category: "Solar" },
    { name: "PV Lag t-1 (1h lag)", score: 0.85, category: "Temporal" },
    { name: "Clear-Sky Index kt", score: 0.81, category: "Solar" },
    { name: "DHI (Diffuse Horizontal)", score: 0.74, category: "Solar" },
    { name: "Cloud Cover %", score: 0.71, category: "Weather" },
    { name: "Ambient Temperature °C", score: 0.68, category: "Weather" },
    { name: "Hour-of-day (sin/cos)", score: 0.65, category: "Cyclical" },
    { name: "PV Lag t-24 (24h lag)", score: 0.60, category: "Temporal" },
    { name: "Wind Speed m/s", score: 0.42, category: "Weather" },
    { name: "Relative Humidity %", score: 0.38, category: "Weather" },
    { name: "Day-of-year (sin/cos)", score: 0.31, category: "Cyclical" },
];

// ─── Alerts ──────────────────────────────────────────────────────────────────
export const DEFAULT_ALERTS = [
    { id: 1, plantId: "plant_college_200", type: "Model Accuracy", severity: "WARNING", message: "CNN-LSTM + ENN R² dipped to 0.9971 during 11:00–13:00 window due to sudden cloud cover spike (+35%). ENN confidence bounds widened. Manual dispatch review recommended.", timestamp: Date.now() - 3600000 * 2, isAcknowledged: false },
    { id: 2, plantId: "plant_college_200", type: "Generation Ramp", severity: "CRITICAL", message: "Actual generation at 14:00 was 141.2 kW vs forecasted 178.5 kW. Deviation Δ = −37.3 kW (−20.9%). Possible inverter throttling or dust accumulation on modules.", timestamp: Date.now() - 3600000, isAcknowledged: false },
    { id: 3, plantId: "plant_college_200", type: "Weather Anomaly", severity: "INFO", message: "Open-Meteo GHI reading at 10:00 diverged from on-site pyranometer by 42 W/m². Sensor calibration recommended before next clear-sky cycle.", timestamp: Date.now() - 7200000, isAcknowledged: true },
    { id: 4, plantId: "plant_college_200", type: "Grid Frequency", severity: "WARNING", message: "Area grid frequency deviation detected: 49.82 Hz (below 49.9 Hz threshold). Reserve dispatch window activated. Reduce ramp rate by 15%.", timestamp: Date.now() - 10800000, isAcknowledged: false },
    { id: 5, plantId: "plant_college_200", type: "Battery SOC", severity: "INFO", message: "Battery State of Charge at 87%. Optimal charging window 10:00–12:00 when ENN predicts 178–192 kW surplus over grid demand.", timestamp: Date.now() - 14400000, isAcknowledged: true },
];

// ─── Location Presets ─────────────────────────────────────────────────────────
export const SOLAR_PARK_PRESETS = [
    { name: "Campus 200kW Plant (Delhi)", latitude: 28.6139, longitude: 77.2090 },
    { name: "Bhadla Solar Park (Rajasthan)", latitude: 27.5330, longitude: 71.9000 },
    { name: "Kamuthi Solar (Tamil Nadu)", latitude: 9.3900, longitude: 78.3900 },
    { name: "Charanka Solar (Gujarat)", latitude: 23.9600, longitude: 71.1800 },
    { name: "Pavagada Solar (Karnataka)", latitude: 14.1115, longitude: 77.2900 },
];

// ─── Chat Role Presets ────────────────────────────────────────────────────────
export const CHATBOT_ROLES = [
    { id: "solar_engineer", title: "Solar Engineer", icon: "⚡", description: "PV system optimization, inverter dispatch, and maintenance scheduling expert." },
    { id: "grid_manager", title: "Grid Manager", icon: "🔌", description: "Grid stability, frequency balancing, and reserve dispatch specialist." },
    { id: "energy_trader", title: "Energy Trader", icon: "📈", description: "Day-ahead spot market bidding, hedge strategies, and intra-day liquidity." },
    { id: "ai_researcher", title: "AI Researcher", icon: "🔬", description: "CNN-LSTM + ENN architecture deep-dive, loss metrics, and spatiotemporal analysis." },
];
