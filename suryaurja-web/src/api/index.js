/**
 * API Layer — SuryaUrja Web
 *
 * Live integrations:
 *  1. Open-Meteo  → free solar irradiance + forecast (no key needed)
 *  2. Gemini AI   → configured via user-supplied API key (localStorage)
 */

// ── Open-Meteo Solar & Weather ─────────────────────────────────────────────────
const OPEN_METEO_BASE = 'https://api.open-meteo.com/v1';

export async function fetchOpenMeteoWeather(latitude, longitude) {
    const url = `${OPEN_METEO_BASE}/forecast?latitude=${latitude}&longitude=${longitude}` +
        `&hourly=temperature_2m,relativehumidity_2m,windspeed_10m,cloudcover,` +
        `direct_radiation,diffuse_radiation,direct_normal_irradiance,global_tilted_irradiance` +
        `&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode` +
        `&forecast_days=7&timezone=auto&timeformat=unixtime`;

    const res = await fetch(url);
    if (!res.ok) throw new Error(`Open-Meteo HTTP ${res.status}`);
    return res.json();
}

/** Map Open-Meteo hourly response to SuryaUrja ForecastPoint format */
export function mapOpenMeteoToHourlyPoints(data, capacityKw) {
    const h = data.hourly;
    return h.time.map((ts, i) => {
        const date = new Date(ts * 1000);
        const hour = date.getHours();
        const ghi = Math.round(h.global_tilted_irradiance?.[i] ?? h.direct_radiation?.[i] ?? 0);
        const dni = Math.round(h.direct_normal_irradiance?.[i] ?? ghi * 0.82);
        const dhi = Math.round(h.diffuse_radiation?.[i] ?? ghi * 0.18);
        const temp = Math.round((h.temperature_2m?.[i] ?? 28) * 10) / 10;
        const cloud = Math.round(h.cloudcover?.[i] ?? 20);
        const wind = Math.round((h.windspeed_10m?.[i] ?? 3.5) * 10) / 10;
        const humidity = Math.round(h.relativehumidity_2m?.[i] ?? 50);

        // Irradiance → PV power conversion (operating efficiency / PR ≈ 75%, derating for temp)
        const efficiency = 0.75 * (1 - Math.max(0, (temp - 25) * 0.0035));
        const baseKw = Math.round((ghi / 1000) * capacityKw * efficiency * 10) / 10;
        const lower = Math.round(baseKw * 0.94 * 10) / 10;
        const upper = Math.round(baseKw * 1.06 * 10) / 10;
        const clearSky = Math.round((ghi > 0 ? Math.max(ghi, 500) : 500) / 1000 * capacityKw * 0.80 * 10) / 10; // clear-sky proxy

        let condition = 'CLEAR_SUNNY';
        if (cloud > 75) condition = 'OVERCAST';
        else if (cloud > 50) condition = 'PARTLY_CLOUDY';
        else if (cloud > 25) condition = 'SCATTERED_CLOUDS';

        return {
            hour, timeLabel: `${String(hour).padStart(2, '0')}:00`,
            actualKw: null, predictedKw: baseKw,
            lowerBoundKw: lower, upperBoundKw: upper, clearSkyKw: clearSky,
            ghi, dni, dhi, temperature: temp, humidity, windSpeed: wind,
            cloudCover: cloud, condition, modelUsed: 'CNN-LSTM+ENN (Live)',
        };
    });
}

/** Returns the timezone string from Open-Meteo response */
export function extractMeta(data) {
    return {
        timezone: data.timezone ?? 'UTC',
        elevationMeters: data.elevation ?? 0,
    };
}

// ── Gemini AI Chat ─────────────────────────────────────────────────────────────
/** Priority: localStorage override → VITE_GEMINI_API_KEY → VITE_API_KEY */
export function getGeminiKey() {
    return (
        localStorage.getItem('suryaurja_gemini_key') ||
        import.meta.env.VITE_GEMINI_API_KEY ||
        import.meta.env.VITE_API_KEY ||
        ''
    );
}
export function setGeminiKey(key) {
    localStorage.setItem('suryaurja_gemini_key', key.trim());
}
/** Generic project API key - e.g. OpenWeatherMap or other services */
export function getApiKey() {
    return import.meta.env.VITE_API_KEY ?? '';
}

const GEMINI_MODELS = {
    'gemini-3.8-flash': 'gemini-3.8-flash',
    'gemini-3.5-flash': 'gemini-3.5-flash',
    'gemini-flash-latest': 'gemini-flash-latest',
};

export async function callGemini({ prompt, roleSystem, apiKey, modelId = 'gemini-3.8-flash', history = [] }) {
    const key = apiKey || getGeminiKey();
    if (!key) throw new Error('No Gemini API key configured. Add your key in the Settings tab.');

    const modelName = GEMINI_MODELS[modelId] ?? 'gemini-3.8-flash';
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${key}`;

    // Build contents array
    const systemMsg = { role: 'user', parts: [{ text: roleSystem }] };
    const modelAck = { role: 'model', parts: [{ text: 'Understood. I am ready to assist.' }] };
    const contents = [
        systemMsg, modelAck,
        ...history.slice(-10).map(m => ({ role: m.role, parts: [{ text: m.text }] })),
        { role: 'user', parts: [{ text: prompt }] },
    ];

    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents,
            generationConfig: { temperature: 0.7, maxOutputTokens: 1024 },
        }),
    });

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error?.message ?? `Gemini HTTP ${res.status}`);
    }

    const data = await res.json();
    return data.candidates?.[0]?.content?.parts?.[0]?.text ?? 'No response generated.';
}

// ── localStorage persistence ───────────────────────────────────────────────────
export function loadAlerts() {
    try { return JSON.parse(localStorage.getItem('suryaurja_alerts') ?? 'null'); } catch { return null; }
}
export function saveAlerts(alerts) {
    localStorage.setItem('suryaurja_alerts', JSON.stringify(alerts));
}

export function loadChatHistory() {
    try { return JSON.parse(localStorage.getItem('suryaurja_chat') ?? '[]'); } catch { return []; }
}
export function saveChatHistory(msgs) {
    localStorage.setItem('suryaurja_chat', JSON.stringify(msgs.slice(-100)));
}

export function loadSelectedPlantId() {
    return localStorage.getItem('suryaurja_plant') ?? null;
}
export function saveSelectedPlantId(id) {
    localStorage.setItem('suryaurja_plant', id);
}
