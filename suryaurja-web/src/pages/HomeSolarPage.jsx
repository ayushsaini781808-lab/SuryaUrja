import { useState, useCallback, useEffect, useRef } from 'react';
import {
    MapPin, RefreshCw, Sun, Zap, ThermometerSun, Cloud, Wind, Droplets,
    Home, IndianRupee, Leaf, Search, Globe, Calendar, TrendingUp, X,
    Navigation, Info, ChevronDown, ChevronUp,
} from 'lucide-react';
import {
    AreaChart, Area, BarChart, Bar, LineChart, Line,
    XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, ReferenceLine, Legend,
} from 'recharts';

// ── WMO weather code → label + icon ──────────────────────────────────────────
function wmoToCondition(code) {
    if (code === 0) return { label: 'Clear Sky', icon: '☀️', severity: 0 };
    if (code <= 2) return { label: 'Mainly Clear', icon: '🌤️', severity: 1 };
    if (code <= 3) return { label: 'Overcast', icon: '☁️', severity: 3 };
    if (code <= 49) return { label: 'Foggy / Haze', icon: '🌫️', severity: 4 };
    if (code <= 69) return { label: 'Drizzle / Rain', icon: '🌧️', severity: 5 };
    if (code <= 79) return { label: 'Snow / Sleet', icon: '🌨️', severity: 5 };
    if (code <= 82) return { label: 'Rain Showers', icon: '🌦️', severity: 4 };
    if (code <= 99) return { label: 'Thunderstorm', icon: '⛈️', severity: 5 };
    return { label: 'Unknown', icon: '❓', severity: 2 };
}

// ── PV power from GHI + temp ─────────────────────────────────────────────────
function calcHourlyKw(ghi, temp, capacityKw) {
    if (ghi <= 0) return 0;
    const eta = 0.20 * (1 - Math.max(0, (temp - 25) * 0.004));
    return Math.round((ghi / 1000) * capacityKw * eta * 10) / 10;
}

// ── Custom Tooltip ────────────────────────────────────────────────────────────
function CustomTooltip({ active, payload, label }) {
    if (!active || !payload?.length) return null;
    return (
        <div style={{
            background: 'rgba(8,14,28,0.97)', border: '1px solid rgba(245,158,11,0.3)',
            borderRadius: 10, padding: '10px 14px', fontSize: 11, minWidth: 160,
        }}>
            <div style={{ fontWeight: 700, color: 'var(--amber)', marginBottom: 6 }}>{label}</div>
            {payload.map(p => (
                <div key={p.name} style={{ color: p.color, display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 2 }}>
                    <span>{p.name}</span><span style={{ fontWeight: 700 }}>{p.value}</span>
                </div>
            ))}
        </div>
    );
}

// ── Stat card ─────────────────────────────────────────────────────────────────
function StatCard({ icon: Icon, iconColor, label, value, unit, sub, gradient }) {
    return (
        <div style={{
            background: gradient || 'rgba(255,255,255,0.03)',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 14, padding: '16px 18px',
            display: 'flex', flexDirection: 'column', gap: 6,
        }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                <Icon size={15} color={iconColor} />
                <span style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{label}</span>
            </div>
            <div style={{ fontSize: 26, fontWeight: 800, lineHeight: 1, color: iconColor }}>
                {value}<span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-secondary)', marginLeft: 4 }}>{unit}</span>
            </div>
            {sub && <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 2 }}>{sub}</div>}
        </div>
    );
}

// ── Weather badge ─────────────────────────────────────────────────────────────
function WeatherBadge({ icon: Icon, label, value, color }) {
    return (
        <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
            background: 'rgba(255,255,255,0.03)', borderRadius: 10, padding: '10px 8px',
            border: '1px solid rgba(255,255,255,0.06)', flex: 1, minWidth: 60,
        }}>
            <Icon size={14} color={color} />
            <span style={{ fontSize: 14, fontWeight: 700, color }}>{value}</span>
            <span style={{ fontSize: 9, color: 'var(--text-muted)', textAlign: 'center', lineHeight: 1.3 }}>{label}</span>
        </div>
    );
}

// ── 7-Day mini forecast card ───────────────────────────────────────────────────
function DayCard({ day, isToday, capacityKw, tariff, onClick, isSelected }) {
    const cond = wmoToCondition(day.weathercode);
    const kwh = Math.round(day.totalKwh * 10) / 10;
    return (
        <div
            onClick={onClick}
            style={{
                background: isSelected
                    ? 'linear-gradient(135deg, rgba(245,158,11,0.18), rgba(245,158,11,0.06))'
                    : 'rgba(255,255,255,0.03)',
                border: `1px solid ${isSelected ? 'rgba(245,158,11,0.4)' : 'rgba(255,255,255,0.07)'}`,
                borderRadius: 12, padding: '12px 10px',
                cursor: 'pointer', transition: 'all 0.2s',
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
                minWidth: 80,
            }}
        >
            <div style={{ fontSize: 10, color: isToday ? 'var(--amber)' : 'var(--text-muted)', fontWeight: isToday ? 700 : 400 }}>
                {isToday ? 'Today' : day.dayLabel}
            </div>
            <div style={{ fontSize: 22 }}>{cond.icon}</div>
            <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--amber)' }}>{kwh}<span style={{ fontSize: 9, marginLeft: 2, color: 'var(--text-muted)' }}>kWh</span></div>
            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>{day.maxTemp}° / {day.minTemp}°</div>
        </div>
    );
}

// ── Location search result item ────────────────────────────────────────────────
function SearchResult({ result, onSelect }) {
    return (
        <div
            onClick={() => onSelect(result)}
            style={{
                padding: '10px 14px', cursor: 'pointer',
                borderBottom: '1px solid rgba(255,255,255,0.05)',
                transition: 'background 0.15s',
                display: 'flex', flexDirection: 'column', gap: 2,
            }}
            onMouseEnter={e => e.currentTarget.style.background = 'rgba(245,158,11,0.08)'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
        >
            <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
                {result.name}
            </div>
            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>
                {[result.admin1, result.country].filter(Boolean).join(', ')} · {result.latitude.toFixed(2)}°N, {result.longitude.toFixed(2)}°E
            </div>
        </div>
    );
}

// ── Main Component ─────────────────────────────────────────────────────────────
export default function HomeSolarPage({ showToast }) {
    // Location state
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [showSearchDrop, setShowSearchDrop] = useState(false);
    const [selectedLocation, setSelectedLocation] = useState({ name: 'Delhi, India', lat: 28.6139, lon: 77.2090 });
    const [isGpsActive, setIsGpsActive] = useState(false);

    // System config
    const [capacityKw, setCapacityKw] = useState(2);
    const [capacityInput, setCapacityInput] = useState('2');
    const [tariff, setTariff] = useState(8);
    const [panelEffPct, setPanelEffPct] = useState(20); // panel efficiency %

    // Data state
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [todayResult, setTodayResult] = useState(null);
    const [weekData, setWeekData] = useState(null);
    const [selectedDayIdx, setSelectedDayIdx] = useState(0);
    const [autoRefresh, setAutoRefresh] = useState(false);
    const [lastFetchTime, setLastFetchTime] = useState(null);

    // UI state
    const [showDetails, setShowDetails] = useState(false);
    const searchRef = useRef(null);
    const autoRefreshTimer = useRef(null);

    // ── Location search via Open-Meteo Geocoding API ──────────────────────────
    const searchLocations = useCallback(async (query) => {
        if (!query || query.length < 2) { setSearchResults([]); return; }
        setIsSearching(true);
        try {
            const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(query)}&count=8&language=en&format=json`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('Search failed');
            const data = await res.json();
            setSearchResults(data.results ?? []);
        } catch {
            setSearchResults([]);
        } finally {
            setIsSearching(false);
        }
    }, []);

    // Debounce search
    const searchDebounce = useRef(null);
    const handleSearchChange = (val) => {
        setSearchQuery(val);
        setShowSearchDrop(true);
        clearTimeout(searchDebounce.current);
        searchDebounce.current = setTimeout(() => searchLocations(val), 350);
    };

    const handleSelectSearchResult = (result) => {
        const loc = { name: `${result.name}${result.admin1 ? ', ' + result.admin1 : ''}${result.country ? ', ' + result.country : ''}`, lat: result.latitude, lon: result.longitude };
        setSelectedLocation(loc);
        setSearchQuery('');
        setSearchResults([]);
        setShowSearchDrop(false);
        setIsGpsActive(false);
        fetchAll(loc.lat, loc.lon, loc.name);
    };

    // ── GPS ───────────────────────────────────────────────────────────────────
    const handleGps = () => {
        if (!navigator.geolocation) { showToast?.('GPS not supported in this browser'); return; }
        setLoading(true);
        navigator.geolocation.getCurrentPosition(
            pos => {
                const loc = { name: 'My GPS Location', lat: pos.coords.latitude, lon: pos.coords.longitude };
                setSelectedLocation(loc);
                setIsGpsActive(true);
                fetchAll(loc.lat, loc.lon, loc.name);
            },
            () => {
                setLoading(false);
                showToast?.('Location access denied. Please allow GPS or search a city.');
            },
            { timeout: 10000, maximumAge: 60000 }
        );
    };

    // ── Fetch all data (today + 7-day) ────────────────────────────────────────
    const fetchAll = useCallback(async (lat, lon, cityName, kw = capacityKw, eff = panelEffPct) => {
        setLoading(true); setError(null);
        try {
            const url = `https://api.open-meteo.com/v1/forecast`
                + `?latitude=${lat}&longitude=${lon}`
                + `&hourly=temperature_2m,cloudcover,windspeed_10m,relativehumidity_2m,`
                + `direct_radiation,diffuse_radiation,direct_normal_irradiance,global_tilted_irradiance`
                + `&daily=temperature_2m_max,temperature_2m_min,weathercode,precipitation_sum,sunrise,sunset,shortwave_radiation_sum`
                + `&forecast_days=7&timezone=auto&timeformat=iso8601`;

            const res = await fetch(url);
            if (!res.ok) throw new Error(`Open-Meteo error: HTTP ${res.status}`);
            const data = await res.json();

            const h = data.hourly;
            const d = data.daily;
            const effFactor = eff / 100;

            // ── Build 7 days ──────────────────────────────────────────────────
            const allWeek = d.time.map((dateStr, di) => {
                const dayStart = di * 24;
                const dayHours = Array.from({ length: 24 }, (_, hi) => {
                    const i = dayStart + hi;
                    const ghi = Math.max(0, Math.round(h.global_tilted_irradiance?.[i] ?? h.direct_radiation?.[i] ?? 0));
                    const temp = Math.round((h.temperature_2m?.[i] ?? 28) * 10) / 10;
                    const cloud = Math.round(h.cloudcover?.[i] ?? 20);
                    const wind = Math.round((h.windspeed_10m?.[i] ?? 3) * 10) / 10;
                    const hum = Math.round(h.relativehumidity_2m?.[i] ?? 55);
                    const eta = effFactor * (1 - Math.max(0, (temp - 25) * 0.004));
                    const kwOut = ghi > 0 ? Math.round((ghi / 1000) * kw * eta * 10) / 10 : 0;
                    return { hour: hi, label: `${String(hi).padStart(2, '0')}:00`, ghi, temp, cloud, wind, hum, kw: kwOut };
                });

                const totalKwh = Math.round(dayHours.reduce((s, p) => s + p.kw, 0) * 10) / 10;
                const peakKw = Math.max(...dayHours.map(p => p.kw));
                const peakHour = dayHours.find(p => p.kw === peakKw)?.label ?? '--';
                const weatherCode = d.weathercode?.[di] ?? 0;
                const maxTemp = Math.round((d.temperature_2m_max?.[di] ?? 30) * 10) / 10;
                const minTemp = Math.round((d.temperature_2m_min?.[di] ?? 22) * 10) / 10;
                const rain = Math.round((d.precipitation_sum?.[di] ?? 0) * 10) / 10;
                const ghiSum = Math.round((d.shortwave_radiation_sum?.[di] ?? 0) * 10) / 10;
                const savings = Math.round(totalKwh * tariff);
                const co2Saved = Math.round(totalKwh * 0.82 * 10) / 10;

                // Parse date for label
                const dt = new Date(dateStr);
                const dayLabel = dt.toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' });

                return {
                    dateStr, dayLabel, hourly: dayHours,
                    totalKwh, peakKw, peakHour, maxTemp, minTemp, rain,
                    weatherCode, savings, co2Saved, ghiSum,
                };
            });

            // ── Today specifics ────────────────────────────────────────────────
            const today = allWeek[0];
            const sunrise = d.sunrise?.[0]?.slice(11, 16) ?? '06:00';
            const sunset = d.sunset?.[0]?.slice(11, 16) ?? '18:00';
            const condition = wmoToCondition(today.weatherCode);
            const avgTemp = Math.round(today.hourly.reduce((s, p) => s + p.temp, 0) / 24 * 10) / 10;
            const avgCloud = Math.round(today.hourly.reduce((s, p) => s + p.cloud, 0) / 24);
            const idealHrs = today.hourly.filter(p => p.ghi > 0).length || 8;
            const perfRatio = idealHrs > 0 ? Math.round((today.totalKwh / (kw * idealHrs)) * 1000) / 10 : 0;

            setTodayResult({
                cityName, lat, lon,
                hourly: today.hourly,
                totalKwh: today.totalKwh,
                peakKw: today.peakKw,
                peakHour: today.peakHour,
                avgTemp, avgCloud,
                savings: today.savings,
                co2Saved: today.co2Saved,
                sunrise, sunset,
                maxTemp: today.maxTemp,
                minTemp: today.minTemp,
                rain: today.rain,
                ghiSum: today.ghiSum,
                condition, perfRatio,
                timezone: data.timezone,
                fetchedAt: new Date().toLocaleTimeString('en-IN', { hour12: false }),
            });

            setWeekData(allWeek);
            setSelectedDayIdx(0);
            setLastFetchTime(new Date());
            showToast?.(`✅ Live data synced for ${cityName} · ${data.timezone}`);
        } catch (e) {
            setError(e.message);
            showToast?.(`⚠ ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, [capacityKw, panelEffPct, tariff, showToast]);

    // ── Capacity input helpers ────────────────────────────────────────────────
    const handleCapacityInput = (val) => {
        setCapacityInput(val);
        const num = parseFloat(val);
        if (!isNaN(num) && num > 0 && num <= 10000) setCapacityKw(num);
    };
    const handleCapacitySlider = (val) => {
        const num = parseFloat(val);
        setCapacityKw(num);
        setCapacityInput(String(num));
    };

    // ── Auto-refresh ──────────────────────────────────────────────────────────
    useEffect(() => {
        if (autoRefresh && selectedLocation) {
            autoRefreshTimer.current = setInterval(() => {
                fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name);
            }, 15 * 60 * 1000); // every 15 min
        } else {
            clearInterval(autoRefreshTimer.current);
        }
        return () => clearInterval(autoRefreshTimer.current);
    }, [autoRefresh, selectedLocation, fetchAll]);

    // ── Click outside to close search dropdown ────────────────────────────────
    useEffect(() => {
        const handler = (e) => {
            if (searchRef.current && !searchRef.current.contains(e.target)) {
                setShowSearchDrop(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    // ── Derived display data ───────────────────────────────────────────────────
    const efficiencyLabel = todayResult ? (
        todayResult.perfRatio >= 75 ? { label: 'Excellent', color: 'var(--emerald)' } :
            todayResult.perfRatio >= 55 ? { label: 'Good', color: 'var(--cyan)' } :
                todayResult.perfRatio >= 35 ? { label: 'Moderate', color: 'var(--amber)' } :
                    { label: 'Low (Cloudy/Rain)', color: 'var(--rose)' }
    ) : null;

    const selectedDayData = weekData?.[selectedDayIdx];
    const selectedDayCond = selectedDayData ? wmoToCondition(selectedDayData.weatherCode) : null;

    // Chart data for selected day
    const chartHourly = (selectedDayData?.hourly ?? todayResult?.hourly ?? [])
        .filter(p => p.hour >= 5 && p.hour <= 20);

    // 7-day bar chart data
    const weekChartData = weekData?.map((day, i) => ({
        label: i === 0 ? 'Today' : day.dateStr.slice(5),
        kWh: day.totalKwh,
        savings: day.savings,
        ghi: day.ghiSum,
        rain: day.rain,
    })) ?? [];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* ── Header ── */}
            <div style={{
                background: 'linear-gradient(135deg, rgba(245,158,11,0.15) 0%, rgba(6,182,212,0.08) 100%)',
                border: '1px solid rgba(245,158,11,0.25)', borderRadius: 16, padding: '20px 24px',
                display: 'flex', alignItems: 'center', gap: 16,
            }}>
                <div style={{
                    width: 48, height: 48, borderRadius: 14,
                    background: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                    boxShadow: '0 0 20px rgba(245,158,11,0.4)',
                }}>
                    <Home size={22} color="#000" />
                </div>
                <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1.2 }}>
                        Live Solar Energy Calculator
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 3, lineHeight: 1.5 }}>
                        Search <strong style={{ color: 'var(--amber)' }}>any city in the world</strong> · Set your system size · Get live weather-based solar output prediction
                    </div>
                </div>
                {lastFetchTime && (
                    <div style={{ fontSize: 10, color: 'var(--text-muted)', textAlign: 'right', flexShrink: 0 }}>
                        <div style={{ color: 'var(--emerald)', fontWeight: 700, fontSize: 11 }}>● LIVE</div>
                        <div>Updated {lastFetchTime.toLocaleTimeString('en-IN', { hour12: false })}</div>
                    </div>
                )}
            </div>

            {/* ── Configuration Panel ── */}
            <div className="card">
                <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Sun size={15} color="var(--amber)" /> System Configuration
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16, marginBottom: 16 }}>

                    {/* Capacity — slider + number input */}
                    <div>
                        <label style={{ fontSize: 11, color: 'var(--text-muted)', display: 'block', marginBottom: 6 }}>
                            🔆 System Capacity (kW)
                        </label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input type="range" min={0.5} max={500} step={0.5} value={Math.min(500, capacityKw)}
                                onChange={e => handleCapacitySlider(e.target.value)}
                                style={{ flex: 1 }} />
                            <input
                                type="number" min={0.1} max={100000} step={0.1}
                                value={capacityInput}
                                onChange={e => handleCapacityInput(e.target.value)}
                                style={{
                                    width: 80, padding: '4px 8px', borderRadius: 8, border: '1px solid rgba(245,158,11,0.4)',
                                    background: 'rgba(245,158,11,0.08)', color: 'var(--amber)',
                                    fontWeight: 800, fontSize: 15, textAlign: 'center', outline: 'none',
                                }}
                            />
                        </div>
                        <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>
                            ≈ {Math.round(capacityKw / 0.33)} panels @ 330W each
                        </div>
                    </div>

                    {/* Panel efficiency */}
                    <div>
                        <label style={{ fontSize: 11, color: 'var(--text-muted)', display: 'block', marginBottom: 6 }}>
                            ⚙️ Panel Efficiency (%)
                        </label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input type="range" min={10} max={25} step={0.5} value={panelEffPct} className="slider-cyan"
                                onChange={e => setPanelEffPct(parseFloat(e.target.value))}
                                style={{ flex: 1 }} />
                            <span style={{ fontWeight: 800, fontSize: 16, color: 'var(--cyan)', minWidth: 50 }}>
                                {panelEffPct}%
                            </span>
                        </div>
                        <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>
                            Mono PERC: 20–22% · Poly: 15–18% · Bifacial: 21–23%
                        </div>
                    </div>

                    {/* Tariff */}
                    <div>
                        <label style={{ fontSize: 11, color: 'var(--text-muted)', display: 'block', marginBottom: 6 }}>
                            💰 Electricity Tariff (₹/kWh)
                        </label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input type="range" min={1} max={20} step={0.5} value={tariff} className="slider-cyan"
                                onChange={e => setTariff(parseFloat(e.target.value))}
                                style={{ flex: 1 }} />
                            <span style={{ fontWeight: 800, fontSize: 16, color: 'var(--cyan)', minWidth: 50 }}>
                                ₹{tariff}
                            </span>
                        </div>
                        <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>
                            India residential: ₹6–₹10 · Commercial: ₹8–₹12
                        </div>
                    </div>
                </div>

                {/* Location Search */}
                <div style={{ marginBottom: 14 }}>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Globe size={11} /> Search any city, district, or location worldwide
                    </div>
                    <div style={{ position: 'relative' }} ref={searchRef}>
                        <div style={{ position: 'relative' }}>
                            <Search size={14} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
                            <input
                                type="text"
                                placeholder="Type city name… e.g. Mumbai, London, New York, Dubai"
                                value={searchQuery}
                                onChange={e => handleSearchChange(e.target.value)}
                                onFocus={() => searchQuery.length >= 2 && setShowSearchDrop(true)}
                                style={{
                                    width: '100%', boxSizing: 'border-box',
                                    padding: '10px 40px 10px 36px',
                                    borderRadius: 10, border: '1px solid rgba(245,158,11,0.3)',
                                    background: 'rgba(255,255,255,0.04)', color: 'var(--text-primary)',
                                    fontSize: 13, outline: 'none',
                                }}
                            />
                            {searchQuery && (
                                <button onClick={() => { setSearchQuery(''); setSearchResults([]); setShowSearchDrop(false); }}
                                    style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: 4 }}>
                                    <X size={13} />
                                </button>
                            )}
                        </div>

                        {/* Dropdown */}
                        {showSearchDrop && (searchResults.length > 0 || isSearching) && (
                            <div style={{
                                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
                                background: 'rgba(8,14,28,0.98)', border: '1px solid rgba(245,158,11,0.25)',
                                borderRadius: '0 0 10px 10px', maxHeight: 260, overflowY: 'auto',
                                boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
                            }}>
                                {isSearching && (
                                    <div style={{ padding: '12px 14px', fontSize: 12, color: 'var(--text-muted)' }}>
                                        <RefreshCw size={11} style={{ marginRight: 6, animation: 'spin 1s linear infinite', verticalAlign: 'middle' }} />
                                        Searching locations…
                                    </div>
                                )}
                                {searchResults.map((r, i) => (
                                    <SearchResult key={i} result={r} onSelect={handleSelectSearchResult} />
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Selected location chip */}
                    {selectedLocation && (
                        <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--text-secondary)' }}>
                            <MapPin size={11} color="var(--amber)" />
                            <span><strong style={{ color: 'var(--amber)' }}>{selectedLocation.name}</strong></span>
                            <span style={{ color: 'var(--text-muted)' }}>· {selectedLocation.lat.toFixed(4)}°N, {selectedLocation.lon.toFixed(4)}°E</span>
                            {isGpsActive && <span style={{ color: 'var(--emerald)', fontWeight: 600 }}>· GPS</span>}
                        </div>
                    )}
                </div>

                {/* Action buttons */}
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                    <button className="btn btn-amber" disabled={loading}
                        onClick={() => fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name)}
                        style={{ flex: 1, minWidth: 200 }}>
                        <RefreshCw size={13} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
                        {loading ? 'Fetching Live Weather…' : `⚡ Get Live Forecast · ${selectedLocation?.name?.split(',')[0]}`}
                    </button>
                    <button className="btn btn-outline" onClick={handleGps} disabled={loading}
                        style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Navigation size={13} /> Use GPS
                    </button>

                    {/* Auto-refresh toggle */}
                    <button
                        onClick={() => setAutoRefresh(v => !v)}
                        style={{
                            display: 'flex', alignItems: 'center', gap: 6,
                            padding: '8px 14px', borderRadius: 8, border: 'none', cursor: 'pointer',
                            background: autoRefresh ? 'rgba(16,185,129,0.15)' : 'rgba(255,255,255,0.05)',
                            color: autoRefresh ? 'var(--emerald)' : 'var(--text-muted)',
                            fontSize: 12, fontWeight: 600, transition: 'all 0.2s',
                        }}>
                        <div style={{
                            width: 8, height: 8, borderRadius: '50%',
                            background: autoRefresh ? 'var(--emerald)' : 'rgba(255,255,255,0.2)',
                            animation: autoRefresh ? 'pulse 2s infinite' : 'none',
                        }} />
                        {autoRefresh ? 'Auto-refresh ON (15m)' : 'Auto-refresh OFF'}
                    </button>
                </div>

                {error && (
                    <div style={{ marginTop: 12, fontSize: 11, color: 'var(--rose)', padding: '8px 12px', background: 'rgba(239,68,68,0.08)', borderRadius: 8 }}>
                        ⚠ {error}
                    </div>
                )}
            </div>

            {/* ── Results ── */}
            {todayResult && weekData && (
                <>
                    {/* Today Weather Summary */}
                    <div className="card" style={{ borderColor: 'rgba(245,158,11,0.2)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
                            <div>
                                <div style={{ fontSize: 16, fontWeight: 800, color: 'var(--text-primary)' }}>
                                    {todayResult.condition.icon} Today in {todayResult.cityName}
                                </div>
                                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                                    {todayResult.condition.label} · Fetched {todayResult.fetchedAt} · {todayResult.timezone}
                                </div>
                            </div>
                            <div style={{ textAlign: 'right' }}>
                                <div style={{ fontSize: 30, fontWeight: 900, color: 'var(--amber)' }}>
                                    {todayResult.maxTemp}°<span style={{ fontSize: 14, color: 'var(--text-muted)', fontWeight: 500 }}>C</span>
                                </div>
                                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Low {todayResult.minTemp}°C</div>
                            </div>
                        </div>

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <WeatherBadge icon={ThermometerSun} label="Avg Temp" value={`${todayResult.avgTemp}°C`} color="var(--amber)" />
                            <WeatherBadge icon={Cloud} label="Cloud Cover" value={`${todayResult.avgCloud}%`} color="var(--text-secondary)" />
                            <WeatherBadge icon={Wind} label="Wind" value={`${todayResult.hourly[12]?.wind ?? '--'}m/s`} color="var(--emerald)" />
                            <WeatherBadge icon={Droplets} label="Humidity" value={`${todayResult.hourly[12]?.hum ?? '--'}%`} color="var(--cyan)" />
                            <WeatherBadge icon={Sun} label="Sunrise" value={todayResult.sunrise} color="var(--amber)" />
                            <WeatherBadge icon={Sun} label="Sunset" value={todayResult.sunset} color="var(--rose)" />
                            <WeatherBadge icon={Zap} label="GHI Total" value={`${todayResult.ghiSum}`} color="var(--purple)" />
                            {todayResult.rain > 0 && (
                                <WeatherBadge icon={Droplets} label="Rain" value={`${todayResult.rain}mm`} color="var(--cyan)" />
                            )}
                        </div>
                    </div>

                    {/* Key Stats */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 12 }}>
                        <StatCard icon={Zap} iconColor="var(--amber)" label="Today's Output"
                            value={todayResult.totalKwh} unit="kWh"
                            sub={`From ${capacityKw}kW @ ${panelEffPct}% eff.`}
                            gradient="linear-gradient(135deg, rgba(245,158,11,0.12), rgba(245,158,11,0.03))" />
                        <StatCard icon={Sun} iconColor="var(--cyan)" label="Peak Power"
                            value={todayResult.peakKw} unit="kW"
                            sub={`At ${todayResult.peakHour}`}
                            gradient="linear-gradient(135deg, rgba(6,182,212,0.12), rgba(6,182,212,0.03))" />
                        <StatCard icon={IndianRupee} iconColor="var(--emerald)" label="Savings Today"
                            value={`₹${todayResult.savings}`} unit=""
                            sub={`@ ₹${tariff}/kWh`}
                            gradient="linear-gradient(135deg, rgba(16,185,129,0.12), rgba(16,185,129,0.03))" />
                        <StatCard icon={Leaf} iconColor="var(--purple)" label="CO₂ Avoided"
                            value={todayResult.co2Saved} unit="kg"
                            sub="vs. coal grid (0.82 kg/kWh)"
                            gradient="linear-gradient(135deg, rgba(139,92,246,0.12), rgba(139,92,246,0.03))" />
                        <StatCard icon={TrendingUp} iconColor={efficiencyLabel?.color} label="Performance Ratio"
                            value={`${todayResult.perfRatio}%`} unit=""
                            sub={efficiencyLabel?.label}
                            gradient={`linear-gradient(135deg, ${efficiencyLabel?.color}1a, ${efficiencyLabel?.color}08)`} />
                        <StatCard icon={Calendar} iconColor="var(--rose)" label="Monthly Est."
                            value={`₹${Math.round(todayResult.savings * 30)}`} unit=""
                            sub={`≈ ${Math.round(todayResult.totalKwh * 30)} kWh/month`}
                            gradient="linear-gradient(135deg, rgba(239,68,68,0.12), rgba(239,68,68,0.03))" />
                    </div>

                    {/* 7-Day Week View */}
                    <div className="card">
                        <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Calendar size={14} color="var(--cyan)" /> 7-Day Solar Forecast
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 14 }}>
                            Click a day to see its hourly breakdown ↓
                        </div>
                        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
                            {weekData.map((day, i) => (
                                <DayCard
                                    key={i} day={day} isToday={i === 0}
                                    capacityKw={capacityKw} tariff={tariff}
                                    isSelected={selectedDayIdx === i}
                                    onClick={() => setSelectedDayIdx(i)}
                                />
                            ))}
                        </div>
                    </div>

                    {/* Selected Day Hourly Chart */}
                    <div className="card">
                        <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 4 }}>
                            ⚡ Hourly Output — {selectedDayIdx === 0 ? 'Today' : selectedDayData?.dayLabel}
                            {selectedDayCond && <span style={{ marginLeft: 8, fontSize: 16 }}>{selectedDayCond.icon}</span>}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 14 }}>
                            Live Open-Meteo GHI · {capacityKw}kW system · {panelEffPct}% panel efficiency · temperature derating applied
                        </div>

                        {/* Day summary pills */}
                        {selectedDayData && (
                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
                                {[
                                    { label: 'Total Output', val: `${selectedDayData.totalKwh} kWh`, color: 'var(--amber)' },
                                    { label: 'Peak', val: `${selectedDayData.peakKw} kW @ ${selectedDayData.peakHour}`, color: 'var(--cyan)' },
                                    { label: 'Savings', val: `₹${selectedDayData.savings}`, color: 'var(--emerald)' },
                                    { label: 'CO₂ Saved', val: `${selectedDayData.co2Saved} kg`, color: 'var(--purple)' },
                                    { label: 'Rain', val: `${selectedDayData.rain}mm`, color: 'var(--cyan)' },
                                ].map(pill => (
                                    <div key={pill.label} style={{
                                        padding: '4px 10px', borderRadius: 20,
                                        background: 'rgba(255,255,255,0.05)',
                                        border: '1px solid rgba(255,255,255,0.08)',
                                        fontSize: 11,
                                    }}>
                                        <span style={{ color: 'var(--text-muted)' }}>{pill.label}: </span>
                                        <strong style={{ color: pill.color }}>{pill.val}</strong>
                                    </div>
                                ))}
                            </div>
                        )}

                        <ResponsiveContainer width="100%" height={230}>
                            <AreaChart data={chartHourly} margin={{ left: -10, right: 10, top: 5 }}>
                                <defs>
                                    <linearGradient id="kwGrad" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="var(--amber)" stopOpacity={0.4} />
                                        <stop offset="95%" stopColor="var(--amber)" stopOpacity={0.01} />
                                    </linearGradient>
                                    <linearGradient id="ghiGrad" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="var(--cyan)" stopOpacity={0.2} />
                                        <stop offset="95%" stopColor="var(--cyan)" stopOpacity={0.01} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                                <XAxis dataKey="label" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                                <YAxis yAxisId="kw" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false}
                                    label={{ value: 'kW', angle: -90, position: 'insideLeft', style: { fontSize: 9, fill: '#64748B' } }} />
                                <YAxis yAxisId="ghi" orientation="right" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false}
                                    label={{ value: 'W/m²', angle: 90, position: 'insideRight', style: { fontSize: 9, fill: '#64748B' } }} />
                                <Tooltip content={<CustomTooltip />} />
                                <ReferenceLine yAxisId="kw" y={capacityKw} stroke="rgba(245,158,11,0.2)" strokeDasharray="6 3"
                                    label={{ value: `${capacityKw}kW rated`, position: 'right', style: { fontSize: 9, fill: '#f59e0b' } }} />
                                <Area yAxisId="kw" dataKey="kw" name="Output (kW)" type="monotone"
                                    stroke="var(--amber)" strokeWidth={2.5} fill="url(#kwGrad)" dot={false} />
                                <Area yAxisId="ghi" dataKey="ghi" name="GHI (W/m²)" type="monotone"
                                    stroke="var(--cyan)" strokeWidth={1.5} fill="url(#ghiGrad)" dot={false} />
                                <Legend wrapperStyle={{ fontSize: 10, paddingTop: 8 }} />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>

                    {/* 7-Day kWh Bar Chart */}
                    <div className="card">
                        <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 4 }}>
                            📅 7-Day Energy & Savings Overview
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 14 }}>
                            Predicted daily solar output (kWh) and estimated savings (₹) based on live weather forecast
                        </div>
                        <ResponsiveContainer width="100%" height={200}>
                            <BarChart data={weekChartData} margin={{ left: -10, right: 10 }}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                                <XAxis dataKey="label" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                                <YAxis yAxisId="kwh" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                                <YAxis yAxisId="sav" orientation="right" tick={{ fontSize: 9, fill: '#64748B' }} axisLine={false} />
                                <Tooltip content={<CustomTooltip />} />
                                <Bar yAxisId="kwh" dataKey="kWh" name="Output (kWh)" fill="var(--amber)" radius={[4, 4, 0, 0]} opacity={0.85} />
                                <Bar yAxisId="sav" dataKey="savings" name="Savings (₹)" fill="var(--emerald)" radius={[4, 4, 0, 0]} opacity={0.6} />
                                <Legend wrapperStyle={{ fontSize: 10, paddingTop: 6 }} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>

                    {/* Hourly Breakdown Table — collapsible */}
                    <div className="card">
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: showDetails ? 14 : 0 }}>
                            <div style={{ fontWeight: 700, fontSize: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
                                📋 Hourly Breakdown
                                <span style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 400 }}>
                                    · {selectedDayIdx === 0 ? 'Today' : selectedDayData?.dayLabel}
                                </span>
                            </div>
                            <button onClick={() => setShowDetails(v => !v)}
                                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4, fontSize: 11 }}>
                                {showDetails ? <><ChevronUp size={13} /> Hide</> : <><ChevronDown size={13} /> Show Table</>}
                            </button>
                        </div>

                        {showDetails && (
                            <div style={{ overflowX: 'auto' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 11 }}>
                                    <thead>
                                        <tr style={{ color: 'var(--text-muted)', textTransform: 'uppercase', fontSize: 10, letterSpacing: '0.4px' }}>
                                            {['Time', 'GHI (W/m²)', 'Temp (°C)', 'Cloud (%)', 'Wind (m/s)', 'Output (kW)', 'Savings (₹)'].map(h => (
                                                <th key={h} style={{ textAlign: 'left', padding: '6px 12px', fontWeight: 600, borderBottom: '1px solid rgba(255,255,255,0.06)' }}>{h}</th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {(selectedDayData?.hourly ?? todayResult.hourly).filter(p => p.hour >= 5 && p.hour <= 20).map(p => (
                                            <tr key={p.hour}
                                                style={{ borderBottom: '1px solid rgba(255,255,255,0.04)', transition: 'background 0.15s' }}
                                                onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.03)'}
                                                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                                                <td style={{ padding: '7px 12px', fontWeight: 600, color: 'var(--text-secondary)' }}>{p.label}</td>
                                                <td style={{ padding: '7px 12px', color: 'var(--amber)' }}>{p.ghi}</td>
                                                <td style={{ padding: '7px 12px', color: 'var(--rose)' }}>{p.temp}</td>
                                                <td style={{ padding: '7px 12px', color: 'var(--text-secondary)' }}>{p.cloud}%</td>
                                                <td style={{ padding: '7px 12px', color: 'var(--emerald)' }}>{p.wind}</td>
                                                <td style={{ padding: '7px 12px', fontWeight: 700, color: p.kw > 0 ? 'var(--cyan)' : 'var(--text-muted)' }}>{p.kw}</td>
                                                <td style={{ padding: '7px 12px', color: 'var(--emerald)' }}>₹{Math.round(p.kw * tariff)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                    <tfoot>
                                        <tr style={{ background: 'rgba(245,158,11,0.07)', fontWeight: 700 }}>
                                            <td colSpan={5} style={{ padding: '8px 12px', color: 'var(--text-secondary)', fontSize: 11 }}>Total (Day)</td>
                                            <td style={{ padding: '8px 12px', color: 'var(--amber)' }}>{selectedDayData?.totalKwh ?? todayResult.totalKwh} kWh</td>
                                            <td style={{ padding: '8px 12px', color: 'var(--emerald)' }}>₹{selectedDayData?.savings ?? todayResult.savings}</td>
                                        </tr>
                                    </tfoot>
                                </table>
                            </div>
                        )}
                    </div>

                    {/* Smart Tips */}
                    <div className="card card-amber">
                        <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
                            <Info size={14} color="var(--amber)" /> Smart Tips for {todayResult.cityName.split(',')[0]} Today
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                            {todayResult.peakKw < capacityKw * 0.4 && (
                                <div>⚠️ <strong style={{ color: 'var(--amber)' }}>Low Output Day</strong> — Heavy cloud cover ({todayResult.avgCloud}%) is reducing irradiance significantly. Avoid running high-power appliances. Consider grid power during 18:00–22:00 peak tariff window.</div>
                            )}
                            {todayResult.peakKw >= capacityKw * 0.7 && (
                                <div>✅ <strong style={{ color: 'var(--emerald)' }}>Great Solar Day!</strong> — Good irradiance expected. Run washing machine, dishwasher, water heater around <strong>{todayResult.peakHour}</strong> (±2 hrs) for maximum self-consumption savings.</div>
                            )}
                            <div>📊 <strong>Monthly Estimate:</strong> ≈ {Math.round(todayResult.totalKwh * 30)} kWh/month → <strong style={{ color: 'var(--emerald)' }}>₹{Math.round(todayResult.savings * 30)}/month</strong> savings at ₹{tariff}/kWh tariff.</div>
                            <div>🌱 Annual impact: your {capacityKw}kW system avoids <strong style={{ color: 'var(--purple)' }}>{Math.round(todayResult.co2Saved * 365)} kg CO₂/year</strong> — equivalent to planting ~{Math.round(todayResult.co2Saved * 365 / 21)} trees.</div>
                            <div>🔆 <strong>Payback Estimate:</strong> At ₹{Math.round(todayResult.savings * 30)}/month savings, a {capacityKw}kW system (≈₹{Math.round(capacityKw * 55000).toLocaleString('en-IN')}) pays back in ~<strong style={{ color: 'var(--cyan)' }}>{Math.round((capacityKw * 55000) / (todayResult.savings * 12))} years</strong>.</div>
                            {todayResult.rain > 0 && (
                                <div>🌧️ <strong style={{ color: 'var(--cyan)' }}>Rain Bonus:</strong> ≈{todayResult.rain}mm rain expected today — naturally cleans your panels and may boost tomorrow's output by 2–5%!</div>
                            )}
                        </div>
                    </div>
                </>
            )}

            {/* ── Empty State ── */}
            {!todayResult && !loading && (
                <div style={{
                    textAlign: 'center', padding: '60px 24px',
                    background: 'rgba(255,255,255,0.02)', borderRadius: 16,
                    border: '1px dashed rgba(255,255,255,0.08)',
                }}>
                    <div style={{ fontSize: 52, marginBottom: 14 }}>🌍☀️</div>
                    <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>
                        How much solar will your system produce today?
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', maxWidth: 420, margin: '0 auto 24px', lineHeight: 1.7 }}>
                        Search <strong style={{ color: 'var(--amber)' }}>any city or location worldwide</strong>, set your system size (in kW), and get <strong>real-time weather-based power predictions</strong> for today and the next 7 days — completely free, no sign-up needed.
                    </div>
                    <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
                        <button className="btn btn-amber"
                            onClick={() => fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name)}>
                            <Sun size={14} /> Fetch Live Forecast for Delhi →
                        </button>
                        <button className="btn btn-outline" onClick={handleGps}>
                            <Navigation size={13} /> Use My Location
                        </button>
                    </div>
                </div>
            )}

            <style>{`
                @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
                @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
                input[type=number]::-webkit-outer-spin-button,
                input[type=number]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
                input[type=number] { -moz-appearance: textfield; }
            `}</style>
        </div>
    );
}
