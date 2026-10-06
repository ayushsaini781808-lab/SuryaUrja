import { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import {
    MapPin, RefreshCw, Sun, Zap, ThermometerSun, Cloud, Wind, Droplets,
    Home, IndianRupee, Leaf, Search, Globe, Calendar, TrendingUp, X,
    Navigation, Info, ChevronDown, ChevronUp, Shield, Award, Percent,
    ArrowRight, CheckCircle2, RotateCcw, CloudSun, Settings, Layers
} from 'lucide-react';
import {
    AreaChart, Area, BarChart, Bar, LineChart, Line,
    XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, ReferenceLine, Legend,
} from 'recharts';
import './SolarCalculator.css';

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

// ── Panel Types & Characteristics ─────────────────────────────────────────────
const PANEL_TYPES = [
    {
        id: 'mono_perc',
        name: 'Mono PERC',
        label: 'Mono PERC (Monocrystalline)',
        operatingEff: 76,
        tempCoeff: 0.0035,
        moduleEff: '20–22%',
        desc: 'Industry standard. High efficiency & low thermal degradation.',
    },
    {
        id: 'poly',
        name: 'Polycrystalline',
        label: 'Polycrystalline (Poly-Si)',
        operatingEff: 72,
        tempCoeff: 0.0040,
        moduleEff: '15–18%',
        desc: 'Traditional blue panels. Lower yield & higher heat sensitivity.',
    },
    {
        id: 'bifacial',
        name: 'Bifacial',
        label: 'Bifacial (Dual-Glass)',
        operatingEff: 80,
        tempCoeff: 0.0030,
        moduleEff: '21–23%',
        desc: 'Captures rear albedo reflection (+5–10% yield boost).',
    },
    {
        id: 'topcon',
        name: 'TOPCon / HJT',
        label: 'TOPCon / N-Type HJT',
        operatingEff: 78,
        tempCoeff: 0.0030,
        moduleEff: '22–24%',
        desc: 'Next-gen N-type technology with superior hot-climate generation.',
    },
];

// ── INDIAN STATES SOLAR & TARIFF DATA ────────────────────────────────────────
const STATE_DATA = {
    1:  { name: 'Jammu & Kashmir',            gen: 3.60, tariff: 2.27, sunHrs: 4.5, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    2:  { name: 'Himachal Pradesh',           gen: 3.76, tariff: 5.00, sunHrs: 4.7, nonSub: [43000, 42000, 38000, 45000], sub: [53000, 52000, 48000, 55000], govSub: [33000, 33000, 19800], commercial: 0 },
    3:  { name: 'Punjab',                     gen: 4.16, tariff: 5.00, sunHrs: 5.2, nonSub: [46300, 38000, 31000, 38000], sub: [56300, 48000, 41000, 48000], govSub: [30000, 30000, 18000], commercial: 0 },
    4:  { name: 'Chandigarh',                 gen: 4.24, tariff: 5.00, sunHrs: 5.3, nonSub: [46300, 38000, 31000, 38000], sub: [56300, 48000, 41000, 48000], govSub: [30000, 30000, 18000], commercial: 0 },
    5:  { name: 'Uttarakhand',                gen: 3.84, tariff: 5.00, sunHrs: 4.8, nonSub: [47000, 47000, 36500, 43500], sub: [57000, 57000, 46500, 53500], govSub: [30000, 30000, 18000], commercial: 0 },
    6:  { name: 'Haryana',                    gen: 4.32, tariff: 5.50, sunHrs: 5.4, nonSub: [43000, 42000, 38000, 45000], sub: [53000, 52000, 48000, 55000], govSub: [30000, 30000, 18000], commercial: 0 },
    7:  { name: 'Delhi (NCT)',                gen: 4.40, tariff: 4.50, sunHrs: 5.5, nonSub: [47000, 47000, 36500, 43500], sub: [57000, 57000, 46500, 53500], govSub: [30000, 30000, 18000], commercial: 30000 },
    8:  { name: 'Rajasthan',                  gen: 4.64, tariff: 6.00, sunHrs: 5.8, nonSub: [60000, 45500, 34000, 41000], sub: [70000, 55500, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    9:  { name: 'Uttar Pradesh',              gen: 4.24, tariff: 5.50, sunHrs: 5.3, nonSub: [43000, 42000, 38000, 45000], sub: [53000, 52000, 48000, 55000], govSub: [30000, 30000, 18000], commercial: 30000 },
    10: { name: 'Bihar',                      gen: 4.16, tariff: 6.50, sunHrs: 5.2, nonSub: [43000, 46500, 34000, 41000], sub: [53000, 56500, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    11: { name: 'Sikkim',                     gen: 3.36, tariff: 4.50, sunHrs: 4.2, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [30000, 30000, 18000], commercial: 0 },
    12: { name: 'Arunachal Pradesh',          gen: 3.52, tariff: 4.00, sunHrs: 4.4, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    13: { name: 'Nagaland',                   gen: 3.44, tariff: 5.00, sunHrs: 4.3, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    14: { name: 'Manipur',                    gen: 3.52, tariff: 5.00, sunHrs: 4.4, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    15: { name: 'Mizoram',                    gen: 3.44, tariff: 5.00, sunHrs: 4.3, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    16: { name: 'Tripura',                    gen: 3.52, tariff: 5.00, sunHrs: 4.4, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    17: { name: 'Meghalaya',                  gen: 3.36, tariff: 5.00, sunHrs: 4.2, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    18: { name: 'Assam',                      gen: 3.60, tariff: 5.00, sunHrs: 4.5, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    19: { name: 'West Bengal',                gen: 3.92, tariff: 5.75, sunHrs: 4.9, nonSub: [47000, 40000, 34000, 41000], sub: [57000, 50000, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    20: { name: 'Jharkhand',                  gen: 4.00, tariff: 6.50, sunHrs: 5.0, nonSub: [49000, 50000, 34000, 41000], sub: [59000, 60000, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    21: { name: 'Odisha',                     gen: 4.16, tariff: 5.50, sunHrs: 5.2, nonSub: [47000, 40000, 34000, 41000], sub: [57000, 50000, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    22: { name: 'Chhattisgarh',               gen: 4.32, tariff: 6.00, sunHrs: 5.4, nonSub: [45000, 45000, 34000, 41000], sub: [55000, 55000, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    23: { name: 'Madhya Pradesh',             gen: 4.48, tariff: 6.00, sunHrs: 5.6, nonSub: [45000, 45000, 34000, 41000], sub: [55000, 55000, 44000, 51000], govSub: [30000, 30000, 18000], commercial: 0 },
    24: { name: 'Gujarat',                    gen: 4.64, tariff: 5.00, sunHrs: 5.8, nonSub: [37500, 35000, 31000, 38000], sub: [47500, 45000, 41000, 48000], govSub: [30000, 30000, 18000], commercial: 0 },
    27: { name: 'Maharashtra',                gen: 4.40, tariff: 6.00, sunHrs: 5.5, nonSub: [46000, 45000, 37000, 44000], sub: [56000, 55000, 47000, 54000], govSub: [30000, 30000, 18000], commercial: 0 },
    28: { name: 'Andhra Pradesh',             gen: 4.56, tariff: 6.00, sunHrs: 5.7, nonSub: [50000, 50000, 40000, 47000], sub: [60000, 60000, 50000, 57000], govSub: [30000, 30000, 18000], commercial: 0 },
    29: { name: 'Karnataka',                  gen: 4.64, tariff: 6.00, sunHrs: 5.8, nonSub: [50000, 50000, 40000, 47000], sub: [60000, 60000, 50000, 57000], govSub: [30000, 30000, 18000], commercial: 0 },
    30: { name: 'Goa',                        gen: 4.40, tariff: 4.05, sunHrs: 5.5, nonSub: [46000, 45000, 37000, 44000], sub: [56000, 55000, 47000, 54000], govSub: [30000, 30000, 18000], commercial: 0 },
    31: { name: 'Lakshadweep',                gen: 4.00, tariff: 5.50, sunHrs: 5.0, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    32: { name: 'Kerala',                     gen: 4.16, tariff: 5.50, sunHrs: 5.2, nonSub: [50000, 50000, 40000, 47000], sub: [60000, 60000, 50000, 57000], govSub: [30000, 30000, 18000], commercial: 0 },
    33: { name: 'Tamil Nadu',                 gen: 4.48, tariff: 5.50, sunHrs: 5.6, nonSub: [50000, 50000, 40000, 47000], sub: [60000, 60000, 50000, 57000], govSub: [30000, 30000, 18000], commercial: 0 },
    34: { name: 'Puducherry',                 gen: 4.40, tariff: 5.50, sunHrs: 5.5, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [30000, 30000, 18000], commercial: 0 },
    35: { name: 'Andaman & Nicobar',          gen: 3.84, tariff: 6.00, sunHrs: 4.8, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
    36: { name: 'Telangana',                  gen: 4.56, tariff: 6.00, sunHrs: 5.7, nonSub: [50000, 50000, 40000, 47000], sub: [60000, 60000, 50000, 57000], govSub: [30000, 30000, 18000], commercial: 0 },
    37: { name: 'Ladakh',                     gen: 4.48, tariff: 6.00, sunHrs: 5.6, nonSub: [65000, 60000, 48000, 55000], sub: [75000, 70000, 58000, 65000], govSub: [33000, 33000, 19800], commercial: 0 },
};

function formatRupees(n) {
    if (!n || isNaN(n)) return '0';
    if (n >= 10000000) return (n / 10000000).toFixed(2) + ' Cr';
    if (n >= 100000) return (n / 100000).toFixed(2) + ' L';
    return Math.round(n).toLocaleString('en-IN');
}

// ── Custom Tooltip ────────────────────────────────────────────────────────────
function CustomTooltip({ active, payload, label }) {
    if (!active || !payload?.length) return null;
    return (
        <div style={{
            background: 'rgba(8,14,28,0.97)', border: '1px solid rgba(245,158,11,0.3)',
            borderRadius: 10, padding: '10px 14px', fontSize: 11, minWidth: 160,
        }}>
            <div style={{ fontWeight: 700, color: '#f59e0b', marginBottom: 6 }}>{label}</div>
            {payload.map(p => (
                <div key={p.name} style={{ color: p.color, display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 2 }}>
                    <span>{p.name}</span><span style={{ fontWeight: 700 }}>{p.value}</span>
                </div>
            ))}
        </div>
    );
}

export default function HomeSolarPage({ showToast }) {
    // ── STEP 1: Calculation Method ─────────────────────────────────
    const [calcMethod, setCalcMethod] = useState('bill'); // 'bill' | 'units' | 'area'
    const [billAmount, setBillAmount] = useState(5000);
    const [unitsAmount, setUnitsAmount] = useState(600);
    const [roofArea, setRoofArea] = useState(600);
    const [areaUnit, setAreaUnit] = useState('sqft'); // 'sqft' | 'sqm'
    const [roofUsagePct, setRoofUsagePct] = useState(70);

    // ── STEP 2: Location & Customer ────────────────────────────────
    const [selectedStateId, setSelectedStateId] = useState(7); // Default: Delhi
    const [customerType, setCustomerType] = useState('2'); // '2' = Residential, '1' = Commercial, '3' = Industrial
    const [subsidyType, setSubsidyType] = useState('1'); // '1' = With Subsidy (DCR)

    // ── STEP 3: Electricity Tariff ─────────────────────────────────
    const [tariff, setTariff] = useState(STATE_DATA[7].tariff);

    // Results & View state
    const [hasCalculated, setHasCalculated] = useState(true);
    const [activeTab, setActiveTab] = useState('calculator'); // 'calculator' | 'live_weather'
    const resultsRef = useRef(null);

    // ── PREVIOUS LIVE WEATHER & SIMULATOR FEATURE STATES ────────────
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [showSearchDrop, setShowSearchDrop] = useState(false);
    const [selectedLocation, setSelectedLocation] = useState({ name: 'Delhi, India', lat: 28.6139, lon: 77.2090 });
    const [isGpsActive, setIsGpsActive] = useState(false);

    // Custom Capacity Input via Keyboard (0.5 to 100 kW)
    const [capacityKw, setCapacityKw] = useState(3.5);
    const [capacityInput, setCapacityInput] = useState('3.5');

    const [panelTypeId, setPanelTypeId] = useState('mono_perc');
    const [operatingEffPct, setOperatingEffPct] = useState(76);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [todayResult, setTodayResult] = useState(null);
    const [weekData, setWeekData] = useState(null);
    const [selectedDayIdx, setSelectedDayIdx] = useState(0);
    const [lastFetchTime, setLastFetchTime] = useState(null);

    const searchRef = useRef(null);

    // Sync tariff when state changes
    const handleStateChange = (stateId) => {
        const id = parseInt(stateId);
        setSelectedStateId(id);
        if (STATE_DATA[id]) {
            setTariff(STATE_DATA[id].tariff);
        }
    };

    // ── INSTANT SOLAR CALCULATOR ENGINE ───────────────────────────
    const calculatorResults = useMemo(() => {
        const sd = STATE_DATA[selectedStateId] || STATE_DATA[7];
        const stateGen = sd.gen;

        let plantKW = 0;
        if (calcMethod === 'bill') {
            const safeBill = Math.max(100, Number(billAmount) || 100);
            const safeTariff = Math.max(1, Number(tariff) || 5);
            const monthlyUnits = safeBill / safeTariff;
            plantKW = (monthlyUnits / 30) / stateGen;
        } else if (calcMethod === 'units') {
            const safeUnits = Math.max(10, Number(unitsAmount) || 10);
            plantKW = (safeUnits / 30) / stateGen;
        } else if (calcMethod === 'area') {
            const rawArea = Math.max(50, Number(roofArea) || 50);
            const sqft = areaUnit === 'sqm' ? rawArea * 10.764 : rawArea;
            const usableArea = sqft * (Number(roofUsagePct) / 100);
            plantKW = usableArea / 70;
        }

        plantKW = Math.max(0.5, Math.round(plantKW * 10) / 10);

        const dailyGen = plantKW * stateGen;
        const monthlyGen = dailyGen * 30;
        const annualGen = dailyGen * 365 * 0.98;
        const lifetimeGen = annualGen * 30 * 0.93;

        const effectiveTariff = Math.max(1, Number(tariff) || 5);
        const monthlySavings = monthlyGen * effectiveTariff;
        const annualSavings = annualGen * effectiveTariff;
        const lifetimeSavings = annualSavings * 30;

        const isResidential = customerType === '2';
        const isEligibleSubsidy = isResidential && subsidyType === '1';

        const priceBracketIdx = plantKW < 3.5 ? 0 : plantKW < 5.3 ? 1 : plantKW < 8.1 ? 2 : 3;
        let pricePerKW = isEligibleSubsidy ? sd.sub[priceBracketIdx] : sd.nonSub[priceBracketIdx];
        if (!isResidential && sd.commercial > 0) {
            pricePerKW = sd.commercial;
        }

        const totalProjectCost = Math.round(plantKW * pricePerKW);

        let govtSubsidy = 0;
        if (isEligibleSubsidy) {
            const [r1, r2, r3] = sd.govSub;
            govtSubsidy += Math.min(plantKW, 1) * r1;
            if (plantKW > 1) govtSubsidy += Math.min(plantKW - 1, 1) * r2;
            if (plantKW > 2) govtSubsidy += Math.min(plantKW - 2, 1) * r3;
        }
        govtSubsidy = Math.round(govtSubsidy);
        const netInvestment = Math.max(0, totalProjectCost - govtSubsidy);

        const paybackYears = annualSavings > 0 ? (netInvestment / annualSavings).toFixed(1) : '4.2';
        const roiPercent = netInvestment > 0 ? ((annualSavings / netInvestment) * 100).toFixed(1) : '24.5';

        const roofAreaNeeded = Math.round(plantKW * 70);
        const co2AvoidedTons = Math.round((lifetimeGen * 0.82) / 1000);
        const treesEquivalent = Math.round((lifetimeGen * 0.82) / 625);

        return {
            stateName: sd.name,
            sunHrs: sd.sunHrs,
            plantKW,
            dailyGen: dailyGen.toFixed(1),
            monthlyGen: Math.round(monthlyGen),
            annualGen: Math.round(annualGen),
            monthlySavings: Math.round(monthlySavings),
            annualSavings: Math.round(annualSavings),
            lifetimeSavings: Math.round(lifetimeSavings),
            totalProjectCost,
            govtSubsidy,
            netInvestment,
            paybackYears,
            roiPercent,
            roofAreaNeeded,
            co2AvoidedTons,
            treesEquivalent,
            isEligibleSubsidy,
        };
    }, [calcMethod, billAmount, unitsAmount, roofArea, areaUnit, roofUsagePct, selectedStateId, customerType, subsidyType, tariff]);

    // ── LIVE OPEN-METEO WEATHER FETCH ENGINE (PREVIOUS FEATURE) ────
    const fetchAll = useCallback(async (lat, lon, cityName, kw = capacityKw, opEff = operatingEffPct, pTypeId = panelTypeId) => {
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
            const opEffFactor = opEff / 100;
            const activePanel = PANEL_TYPES.find(p => p.id === pTypeId) ?? PANEL_TYPES[0];
            const tempCoeff = activePanel.tempCoeff ?? 0.0035;

            const allWeek = d.time.map((dateStr, di) => {
                const dayStart = di * 24;
                const dayHours = Array.from({ length: 24 }, (_, hi) => {
                    const i = dayStart + hi;
                    const ghi = Math.max(0, Math.round(h.global_tilted_irradiance?.[i] ?? h.direct_radiation?.[i] ?? 0));
                    const temp = Math.round((h.temperature_2m?.[i] ?? 28) * 10) / 10;
                    const cloud = Math.round(h.cloudcover?.[i] ?? 20);
                    const wind = Math.round((h.windspeed_10m?.[i] ?? 3) * 10) / 10;
                    const hum = Math.round(h.relativehumidity_2m?.[i] ?? 55);
                    const tempDerate = 1 - Math.max(0, (temp - 25) * tempCoeff);
                    const kwOut = ghi > 0 ? Math.round((ghi / 1000) * kw * opEffFactor * tempDerate * 10) / 10 : 0;
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

                const dt = new Date(dateStr);
                const dayLabel = dt.toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' });

                return {
                    dateStr, dayLabel, hourly: dayHours,
                    totalKwh, peakKw, peakHour, maxTemp, minTemp, rain,
                    weatherCode, savings, co2Saved, ghiSum,
                };
            });

            const today = allWeek[0];
            const sunrise = d.sunrise?.[0]?.slice(11, 16) ?? '06:00';
            const sunset = d.sunset?.[0]?.slice(11, 16) ?? '18:00';
            const condition = wmoToCondition(today.weatherCode);
            const avgTemp = Math.round(today.hourly.reduce((s, p) => s + p.temp, 0) / 24 * 10) / 10;
            const avgCloud = Math.round(today.hourly.reduce((s, p) => s + p.cloud, 0) / 24);
            const totalGhiDay = today.hourly.reduce((s, p) => s + p.ghi, 0);
            const theoreticalKwh = (totalGhiDay / 1000) * kw;
            const perfRatio = theoreticalKwh > 0 ? Math.round((today.totalKwh / theoreticalKwh) * 1000) / 10 : Math.round(opEff * 10) / 10;

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
            showToast?.(`✅ Live weather data synced for ${cityName}`);
        } catch (e) {
            setError(e.message);
            showToast?.(`⚠️ ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, [capacityKw, operatingEffPct, panelTypeId, tariff, showToast]);

    // Initial weather fetch on mount
    useEffect(() => {
        fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name);
    }, []);

    // Geocoding Search
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

    // GPS Helper
    const handleGps = () => {
        if (!navigator.geolocation) { showToast?.('GPS not supported in browser'); return; }
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
                showToast?.('GPS permission denied. Please search your city.');
            },
            { timeout: 10000, maximumAge: 60000 }
        );
    };

    const handlePanelTypeChange = (id) => {
        setPanelTypeId(id);
        const pt = PANEL_TYPES.find(p => p.id === id);
        if (pt) setOperatingEffPct(pt.operatingEff);
        fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name, capacityKw, pt?.operatingEff ?? 76, id);
    };

    // KEYBOARD INPUT FOR SOLAR KW (0.5 to 100 kW)
    const handleCapacityInputText = (val) => {
        setCapacityInput(val);
        const num = parseFloat(val);
        if (!isNaN(num) && num >= 0.1 && num <= 1000) {
            setCapacityKw(num);
            fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name, num);
        }
    };

    const handleCapacitySliderChange = (kw) => {
        const num = parseFloat(kw);
        setCapacityKw(num);
        setCapacityInput(String(num));
        fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name, num);
    };

    const selectedDayData = weekData?.[selectedDayIdx];
    const chartHourly = (selectedDayData?.hourly ?? todayResult?.hourly ?? []).filter(p => p.hour >= 5 && p.hour <= 20);

    const handleCalculateClick = () => {
        setHasCalculated(true);
        showToast?.('Solar savings calculated successfully!');
        if (resultsRef.current) {
            resultsRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <div className="sc-container">
            {/* ── TOP NAV BAR TOGGLE FOR CALCULATOR vs LIVE WEATHER SIMULATOR ── */}
            <div style={{
                background: '#0d2818',
                padding: '12px 24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid rgba(255,255,255,0.1)',
                flexWrap: 'wrap',
                gap: 12,
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <Sun size={20} color="#22c55e" />
                    <span style={{ color: '#fff', fontWeight: 800, fontSize: 15, letterSpacing: '0.5px' }}>
                        SURYAURJA SOLAR INTELLIGENCE SUITE
                    </span>
                </div>

                <div style={{ display: 'flex', background: 'rgba(255,255,255,0.08)', borderRadius: 50, padding: 3, gap: 4 }}>
                    <button
                        type="button"
                        onClick={() => setActiveTab('calculator')}
                        style={{
                            background: activeTab === 'calculator' ? 'var(--sc-grad)' : 'transparent',
                            color: '#fff',
                            border: 'none',
                            borderRadius: 50,
                            padding: '6px 18px',
                            fontSize: 12,
                            fontWeight: 700,
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 6,
                        }}
                    >
                        <Zap size={14} /> Instant Solar Savings Calculator
                    </button>
                    <button
                        type="button"
                        onClick={() => setActiveTab('live_weather')}
                        style={{
                            background: activeTab === 'live_weather' ? 'var(--sc-grad)' : 'transparent',
                            color: '#fff',
                            border: 'none',
                            borderRadius: 50,
                            padding: '6px 18px',
                            fontSize: 12,
                            fontWeight: 700,
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 6,
                        }}
                    >
                        <CloudSun size={14} /> Live City Weather & PV Simulator
                    </button>
                </div>
            </div>

            {/* ════════════════════════════════════════════════════════════════
               TAB 1: SURYAURJA INSTANT SOLAR SAVINGS CALCULATOR LAYOUT
            ════════════════════════════════════════════════════════════════ */}
            {activeTab === 'calculator' && (
                <>
                    {/* ── HERO BANNER ── */}
                    <div className="sc-hero">
                        <div className="sc-hero-orb sc-hero-orb-1" />
                        <div className="sc-hero-orb sc-hero-orb-2" />

                        <div className="sc-hero-content">
                            <div className="sc-brand-badge">
                                <Sun size={15} color="#fff" />
                                <span>SURYAURJA · SOLAR POWER CALCULATOR</span>
                            </div>

                            <h1 className="sc-hero-title">
                                Go Solar, Save <span>Big</span>
                            </h1>

                            <p className="sc-hero-sub">
                                Calculate your personalised solar generation, financial savings, and PM Surya Ghar subsidy eligibility with SuryaUrja.
                            </p>

                            <div className="sc-hero-badges">
                                <div className="sc-hero-badge">
                                    <Shield size={14} /> 30 Year Warranty
                                </div>
                                <div className="sc-hero-badge">
                                    <Leaf size={14} /> Made in India
                                </div>
                                <div className="sc-hero-badge">
                                    <Percent size={14} /> Government Subsidy
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* ── MAIN CALCULATOR CARD ── */}
                    <div className="sc-main-wrapper">
                        <div className="sc-calc-card">
                            {/* STEP 1: Calculation Mode */}
                            <div className="sc-section">
                                <div className="sc-section-label">
                                    <div className="sc-step-num">1</div>
                                    <span>STEP ONE</span>
                                </div>
                                <div className="sc-section-title">
                                    How would you like to calculate?
                                </div>

                                <div className="sc-method-grid">
                                    <div
                                        className={`sc-method-card ${calcMethod === 'bill' ? 'active' : ''}`}
                                        onClick={() => setCalcMethod('bill')}
                                    >
                                        <div className="sc-method-icon">₹</div>
                                        <h3>Monthly Bill</h3>
                                        <p>Enter your current electricity bill amount</p>
                                    </div>

                                    <div
                                        className={`sc-method-card ${calcMethod === 'units' ? 'active' : ''}`}
                                        onClick={() => setCalcMethod('units')}
                                    >
                                        <div className="sc-method-icon">⚡</div>
                                        <h3>Monthly Units</h3>
                                        <p>Enter electricity consumption in kWh</p>
                                    </div>

                                    <div
                                        className={`sc-method-card ${calcMethod === 'area' ? 'active' : ''}`}
                                        onClick={() => setCalcMethod('area')}
                                    >
                                        <div className="sc-method-icon">🏠</div>
                                        <h3>Roof Area</h3>
                                        <p>Enter your available rooftop area</p>
                                    </div>
                                </div>

                                <div className="sc-input-box">
                                    {calcMethod === 'bill' && (
                                        <div>
                                            <label className="sc-input-label">
                                                <IndianRupee size={15} color="var(--sc-green)" /> Monthly Electricity Bill
                                            </label>
                                            <div className="sc-input-wrap">
                                                <div className="sc-addon-left">₹</div>
                                                <input
                                                    type="number"
                                                    min="100"
                                                    step="100"
                                                    value={billAmount}
                                                    onChange={(e) => setBillAmount(Number(e.target.value))}
                                                    placeholder="e.g. 5000"
                                                />
                                                <div className="sc-addon-right">/ month</div>
                                            </div>
                                            <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
                                                {[1500, 3000, 5000, 8000, 12000].map(val => (
                                                    <button
                                                        key={val}
                                                        type="button"
                                                        onClick={() => setBillAmount(val)}
                                                        style={{
                                                            background: billAmount === val ? '#dcfce7' : '#fff',
                                                            border: `1px solid ${billAmount === val ? '#16a34a' : '#d4ebd9'}`,
                                                            color: billAmount === val ? '#15803d' : '#537359',
                                                            padding: '4px 10px', borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: 'pointer',
                                                        }}
                                                    >
                                                        ₹{val.toLocaleString('en-IN')}
                                                    </button>
                                                ))}
                                            </div>
                                        </div>
                                    )}

                                    {calcMethod === 'units' && (
                                        <div>
                                            <label className="sc-input-label">
                                                <Zap size={15} color="var(--sc-green)" /> Monthly Electricity Consumption
                                            </label>
                                            <div className="sc-input-wrap">
                                                <div className="sc-addon-left">⚡</div>
                                                <input
                                                    type="number"
                                                    min="20"
                                                    step="10"
                                                    value={unitsAmount}
                                                    onChange={(e) => setUnitsAmount(Number(e.target.value))}
                                                    placeholder="e.g. 600"
                                                />
                                                <div className="sc-addon-right">kWh (Units) / month</div>
                                            </div>
                                        </div>
                                    )}

                                    {calcMethod === 'area' && (
                                        <div>
                                            <label className="sc-input-label">
                                                <Home size={15} color="var(--sc-green)" /> Available Rooftop Area
                                            </label>
                                            <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 12 }}>
                                                <div className="sc-input-wrap">
                                                    <div className="sc-addon-left">🏠</div>
                                                    <input
                                                        type="number"
                                                        min="50"
                                                        step="50"
                                                        value={roofArea}
                                                        onChange={(e) => setRoofArea(Number(e.target.value))}
                                                        placeholder="e.g. 600"
                                                    />
                                                </div>
                                                <div style={{ display: 'flex', border: '2px solid #d4ebd9', borderRadius: 14, overflow: 'hidden' }}>
                                                    <button type="button" onClick={() => setAreaUnit('sqft')} style={{ padding: '0 16px', background: areaUnit === 'sqft' ? 'var(--sc-grad)' : '#fff', color: areaUnit === 'sqft' ? '#fff' : '#537359', border: 'none', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Sq. Ft</button>
                                                    <button type="button" onClick={() => setAreaUnit('sqm')} style={{ padding: '0 16px', background: areaUnit === 'sqm' ? 'var(--sc-grad)' : '#fff', color: areaUnit === 'sqm' ? '#fff' : '#537359', border: 'none', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Sq. M</button>
                                                </div>
                                            </div>
                                            <div style={{ marginTop: 14 }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 700, color: 'var(--sc-text)', marginBottom: 6 }}>
                                                    <span>Roof Usage for Solar:</span>
                                                    <span style={{ color: 'var(--sc-green)' }}>{roofUsagePct}% Usable</span>
                                                </div>
                                                <input type="range" min="30" max="100" step="5" value={roofUsagePct} onChange={(e) => setRoofUsagePct(Number(e.target.value))} className="sc-slider" />
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* STEP 2: Location & Customer */}
                            <div className="sc-section">
                                <div className="sc-section-label">
                                    <div className="sc-step-num">2</div>
                                    <span>STEP TWO</span>
                                </div>
                                <div className="sc-section-title">
                                    Your Location & Customer Type
                                </div>

                                <div className="sc-form-grid">
                                    <div className="sc-form-group">
                                        <label><MapPin size={15} color="var(--sc-green)" /> State / Union Territory</label>
                                        <div className="sc-input-wrap">
                                            <select value={selectedStateId} onChange={(e) => handleStateChange(e.target.value)}>
                                                {Object.entries(STATE_DATA).map(([id, s]) => (
                                                    <option key={id} value={id}>{s.name}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>

                                    <div className="sc-form-group">
                                        <label><Award size={15} color="var(--sc-green)" /> Customer Category</label>
                                        <div className="sc-input-wrap">
                                            <select value={customerType} onChange={(e) => {
                                                const ct = e.target.value;
                                                setCustomerType(ct);
                                                if (ct !== '2') setSubsidyType('0');
                                                else setSubsidyType('1');
                                            }}>
                                                <option value="2">Residential (Eligible for Subsidy)</option>
                                                <option value="1">Commercial</option>
                                                <option value="3">Industrial</option>
                                            </select>
                                        </div>
                                    </div>
                                </div>

                                {customerType === '2' && (
                                    <div className="sc-subsidy-box">
                                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
                                            <label style={{ fontSize: 13, fontWeight: 700, color: 'var(--sc-text)', display: 'flex', alignItems: 'center', gap: 6 }}>
                                                <Percent size={15} color="var(--sc-green)" /> Subsidy Applicable?
                                            </label>
                                            <div className="sc-input-wrap" style={{ width: 220 }}>
                                                <select value={subsidyType} onChange={(e) => setSubsidyType(e.target.value)}>
                                                    <option value="1">With Subsidy (DCR)</option>
                                                    <option value="0">No Subsidy (Non-DCR)</option>
                                                </select>
                                            </div>
                                        </div>
                                        <div className="sc-subsidy-tip">
                                            <Info size={14} color="var(--sc-green)" style={{ flexShrink: 0, marginTop: 2 }} />
                                            <span>
                                                <strong>PM Surya Ghar Muft Bijli Yojana</strong> government subsidy applies to Made in India (DCR) solar panels for residential customers only. (Up to ₹78,000 for 3 kW system).
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* STEP 3: Tariff */}
                            <div className="sc-section">
                                <div className="sc-section-label">
                                    <div className="sc-step-num">3</div>
                                    <span>STEP THREE</span>
                                </div>
                                <div className="sc-section-title">
                                    Your Electricity Unit Cost
                                </div>

                                <div className="sc-tariff-box">
                                    <div className="sc-tariff-header">
                                        <div>
                                            <div className="sc-tariff-display">₹{Number(tariff).toFixed(2)}</div>
                                            <div className="sc-tariff-unit">per kWh (unit)</div>
                                        </div>
                                        <div className="sc-input-wrap" style={{ width: 140 }}>
                                            <div className="sc-addon-left">₹</div>
                                            <input type="number" min="1" max="30" step="0.25" value={tariff} onChange={(e) => setTariff(Math.min(30, Math.max(1, Number(e.target.value))))} style={{ textAlign: 'center', fontWeight: 800 }} />
                                        </div>
                                    </div>

                                    <input type="range" min="1" max="30" step="0.25" value={tariff} onChange={(e) => setTariff(Number(e.target.value))} className="sc-slider" />
                                    <div className="sc-slider-ticks"><span>₹1</span><span>₹8</span><span>₹15</span><span>₹22</span><span>₹30</span></div>
                                    <div className="sc-tariff-badge">
                                        <Info size={13} color="var(--sc-green)" />
                                        <span>Auto-filled with <strong>{STATE_DATA[selectedStateId]?.name}</strong>'s average tariff of <strong>₹{STATE_DATA[selectedStateId]?.tariff}/unit</strong>. Adjust if your bill rate differs.</span>
                                    </div>
                                </div>
                            </div>

                            {/* ACTION BUTTON */}
                            <div className="sc-action-section">
                                <button type="button" className="sc-btn-calculate" onClick={handleCalculateClick}>
                                    <Zap size={18} />
                                    <span>Calculate My Solar Savings</span>
                                    <ArrowRight size={18} />
                                </button>
                            </div>
                        </div>

                        {/* RESULTS REPORT */}
                        {hasCalculated && (
                            <div className="sc-results-card" ref={resultsRef}>
                                <div className="sc-results-header">
                                    <div className="sc-state-pill">
                                        <Sun size={14} /> Solar Potential for {calculatorResults.stateName}
                                    </div>
                                    <h2 className="sc-results-title">
                                        Your Personalised Solar Savings Report
                                    </h2>
                                    <p className="sc-results-sub">
                                        Based on {calculatorResults.sunHrs} peak sun hours/day in {calculatorResults.stateName} @ ₹{Number(tariff).toFixed(2)}/kWh
                                    </p>
                                </div>

                                <div className="sc-plant-hero">
                                    <div className="sc-plant-hero-left">
                                        <div className="sc-plant-label">Recommended System Capacity</div>
                                        <div className="sc-plant-value">
                                            {calculatorResults.plantKW} <span className="sc-plant-unit">kWp</span>
                                        </div>
                                        <div className="sc-plant-sub">
                                            Generates ~<strong>{calculatorResults.dailyGen} units</strong> of clean power daily
                                        </div>
                                    </div>
                                    <div className="sc-plant-stats-col">
                                        <div className="sc-plant-stat-chip">
                                            <div className="sc-plant-stat-lbl">Rooftop Area Required</div>
                                            <div className="sc-plant-stat-val">~{calculatorResults.roofAreaNeeded} sq. ft</div>
                                        </div>
                                        <div className="sc-plant-stat-chip">
                                            <div className="sc-plant-stat-lbl">Solar Panels Count</div>
                                            <div className="sc-plant-stat-val">≈ {Math.round(calculatorResults.plantKW / 0.54)} Panels (540W)</div>
                                        </div>
                                    </div>
                                </div>

                                <div className="sc-metric-grid">
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><Zap size={20} /></div>
                                        <div className="sc-metric-lbl">Daily Generation</div>
                                        <div className="sc-metric-val">{calculatorResults.dailyGen} Units</div>
                                        <div className="sc-metric-sub">kWh clean solar power</div>
                                    </div>
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><IndianRupee size={20} /></div>
                                        <div className="sc-metric-lbl">Monthly Savings</div>
                                        <div className="sc-metric-val" style={{ color: 'var(--sc-green)' }}>₹{formatRupees(calculatorResults.monthlySavings)}</div>
                                        <div className="sc-metric-sub">saved on electricity bills</div>
                                    </div>
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><Calendar size={20} /></div>
                                        <div className="sc-metric-lbl">Annual Savings</div>
                                        <div className="sc-metric-val" style={{ color: 'var(--sc-green)' }}>₹{formatRupees(calculatorResults.annualSavings)}</div>
                                        <div className="sc-metric-sub">estimated year 1 bill offset</div>
                                    </div>
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><TrendingUp size={20} /></div>
                                        <div className="sc-metric-lbl">30-Year Savings</div>
                                        <div className="sc-metric-val" style={{ color: 'var(--sc-dark)' }}>₹{formatRupees(calculatorResults.lifetimeSavings)}</div>
                                        <div className="sc-metric-sub">lifetime solar wealth creation</div>
                                    </div>
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><CheckCircle2 size={20} /></div>
                                        <div className="sc-metric-lbl">Payback Period</div>
                                        <div className="sc-metric-val">{calculatorResults.paybackYears} Yrs</div>
                                        <div className="sc-metric-sub">simple capital recovery</div>
                                    </div>
                                    <div className="sc-metric-card">
                                        <div className="sc-metric-icon"><Percent size={20} /></div>
                                        <div className="sc-metric-lbl">Annual ROI</div>
                                        <div className="sc-metric-val" style={{ color: 'var(--sc-green)' }}>{calculatorResults.roiPercent}%</div>
                                        <div className="sc-metric-sub">annual tax-free return</div>
                                    </div>
                                </div>

                                <div className="sc-cost-card">
                                    <div className="sc-cost-title"><IndianRupee size={18} color="var(--sc-green)" /> Project Cost & PM Surya Ghar Subsidy</div>
                                    <div className="sc-cost-row">
                                        <span>Estimated System Turnkey Cost</span>
                                        <span>₹{formatRupees(calculatorResults.totalProjectCost)}</span>
                                    </div>
                                    {calculatorResults.isEligibleSubsidy && calculatorResults.govtSubsidy > 0 && (
                                        <div className="sc-cost-row subsidy-highlight">
                                            <span>PM Surya Ghar Central Subsidy <span className="sc-subsidy-badge">Govt Benefit</span></span>
                                            <span>− ₹{formatRupees(calculatorResults.govtSubsidy)}</span>
                                        </div>
                                    )}
                                    <div className="sc-cost-row total-row">
                                        <span>Your Net Investment</span>
                                        <span>₹{formatRupees(calculatorResults.netInvestment)}</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </>
            )}

            {/* ════════════════════════════════════════════════════════════════
               TAB 2: PREVIOUS LIVE CITY SEARCH, GPS, NEXT 7-DAY OUTPUT & PV SIMULATOR
            ════════════════════════════════════════════════════════════════ */}
            {activeTab === 'live_weather' && (
                <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 24 }}>
                    {/* Header bar with Live badge & City Search */}
                    <div style={{
                        background: 'linear-gradient(135deg, rgba(245,158,11,0.15) 0%, rgba(6,182,212,0.08) 100%)',
                        border: '1px solid rgba(245,158,11,0.25)', borderRadius: 16, padding: '20px 24px',
                        display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap',
                    }}>
                        <div style={{
                            width: 48, height: 48, borderRadius: 14,
                            background: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                        }}>
                            <Home size={22} color="#000" />
                        </div>
                        <div style={{ flex: 1 }}>
                            <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--sc-text)' }}>
                                Live City Solar & Weather Forecasting Simulator
                            </div>
                            <div style={{ fontSize: 12, color: 'var(--sc-muted)' }}>
                                Search <strong>any city globally</strong> · Set custom capacity & electricity tariff rate · Get live 7-day power output & financial savings forecast
                            </div>
                        </div>
                        {lastFetchTime && (
                            <div style={{ fontSize: 11, color: '#16a34a', fontWeight: 700 }}>
                                ● LIVE SYNCHRONIZED ({lastFetchTime.toLocaleTimeString('en-IN', { hour12: false })})
                            </div>
                        )}
                    </div>

                    {/* Location Search Bar & GPS */}
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 12, position: 'relative' }} ref={searchRef}>
                        <div style={{ position: 'relative' }}>
                            <div className="sc-input-wrap">
                                <div className="sc-addon-left"><Search size={16} /></div>
                                <input
                                    type="text"
                                    value={searchQuery}
                                    onChange={(e) => handleSearchChange(e.target.value)}
                                    placeholder="Search any city globally (e.g. Mumbai, Delhi, London, Tokyo)..."
                                />
                            </div>
                            {showSearchDrop && searchResults.length > 0 && (
                                <div style={{
                                    position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
                                    background: '#fff', border: '1px solid #d4ebd9', borderRadius: 12,
                                    boxShadow: '0 8px 24px rgba(0,0,0,0.12)', marginTop: 4, overflow: 'hidden',
                                }}>
                                    {searchResults.map(res => (
                                        <div
                                            key={`${res.id}-${res.latitude}`}
                                            onClick={() => handleSelectSearchResult(res)}
                                            style={{ padding: '10px 14px', borderBottom: '1px solid #f0f4f1', cursor: 'pointer' }}
                                        >
                                            <div style={{ fontWeight: 700, fontSize: 13 }}>{res.name}</div>
                                            <div style={{ fontSize: 10, color: '#537359' }}>{[res.admin1, res.country].filter(Boolean).join(', ')}</div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <button
                            type="button"
                            onClick={handleGps}
                            style={{
                                display: 'flex', alignItems: 'center', gap: 6,
                                background: isGpsActive ? '#dcfce7' : '#fff',
                                border: `1px solid ${isGpsActive ? '#16a34a' : '#d4ebd9'}`,
                                color: isGpsActive ? '#15803d' : '#537359',
                                borderRadius: 14, padding: '0 16px', fontWeight: 700, cursor: 'pointer',
                            }}
                        >
                            <Navigation size={15} /> GPS
                        </button>

                        <button
                            type="button"
                            onClick={() => fetchAll(selectedLocation.lat, selectedLocation.lon, selectedLocation.name)}
                            style={{
                                display: 'flex', alignItems: 'center', gap: 6,
                                background: 'var(--sc-grad)', color: '#fff',
                                border: 'none', borderRadius: 14, padding: '0 20px', fontWeight: 700, cursor: 'pointer',
                            }}
                        >
                            <RefreshCw size={15} className={loading ? 'spin' : ''} /> Sync
                        </button>
                    </div>

                    {/* Solar PV System Configurator with CAPACITY (kW), PANEL TECH, and ELECTRICITY RATE TARIFF (₹/kWh) */}
                    <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 24 }}>
                        <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
                            <Settings size={18} color="var(--sc-green)" /> Custom PV Plant & Electricity Tariff Configurator
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 24 }}>
                            {/* 1. Solar kW Keyboard Input + Range Slider */}
                            <div>
                                <label style={{ fontSize: 12, fontWeight: 700, color: '#537359', display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                                    <span>🔆 Solar System Capacity (kWp)</span>
                                    <span style={{ color: 'var(--sc-green)' }}>Type 0.5 to 100 kW via keyboard</span>
                                </label>
                                <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                                    <input
                                        type="range" min={0.5} max={100} step={0.5}
                                        value={Math.min(100, Math.max(0.5, capacityKw))}
                                        onChange={(e) => handleCapacitySliderChange(e.target.value)}
                                        className="sc-slider" style={{ flex: 1 }}
                                    />
                                    <div className="sc-input-wrap" style={{ width: 110 }}>
                                        <input
                                            type="number"
                                            min={0.5}
                                            max={100}
                                            step={0.5}
                                            value={capacityInput}
                                            onChange={(e) => handleCapacityInputText(e.target.value)}
                                            style={{ textAlign: 'center', fontWeight: 900, color: 'var(--sc-dark)', fontSize: 16 }}
                                            placeholder="e.g. 5"
                                        />
                                        <div className="sc-addon-right" style={{ padding: '0 8px', fontSize: 12 }}>kW</div>
                                    </div>
                                </div>

                                {/* Quick Capacity Presets */}
                                <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
                                    {[1, 3, 5, 10, 25, 50, 100].map(kw => (
                                        <button
                                            key={kw}
                                            type="button"
                                            onClick={() => handleCapacitySliderChange(kw)}
                                            style={{
                                                background: capacityKw === kw ? '#dcfce7' : '#f8fbf9',
                                                border: `1px solid ${capacityKw === kw ? '#16a34a' : '#d4ebd9'}`,
                                                color: capacityKw === kw ? '#15803d' : '#537359',
                                                padding: '3px 10px',
                                                borderRadius: 8,
                                                fontSize: 11,
                                                fontWeight: 700,
                                                cursor: 'pointer',
                                            }}
                                        >
                                            {kw} kW
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* 2. ELECTRIC RATE / TARIFF SELECTOR (₹/kWh) */}
                            <div>
                                <label style={{ fontSize: 12, fontWeight: 700, color: '#537359', display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                                    <span>⚡ Electricity Rate / Tariff (₹/kWh)</span>
                                    <span style={{ color: 'var(--sc-green)' }}>Adjust to calculate financial savings</span>
                                </label>
                                <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                                    <input
                                        type="range" min={1} max={30} step={0.25}
                                        value={tariff}
                                        onChange={(e) => setTariff(Number(e.target.value))}
                                        className="sc-slider" style={{ flex: 1 }}
                                    />
                                    <div className="sc-input-wrap" style={{ width: 120 }}>
                                        <div className="sc-addon-left" style={{ padding: '0 8px', fontSize: 14 }}>₹</div>
                                        <input
                                            type="number"
                                            min={1}
                                            max={30}
                                            step={0.25}
                                            value={tariff}
                                            onChange={(e) => setTariff(Math.min(30, Math.max(1, Number(e.target.value))))}
                                            style={{ textAlign: 'center', fontWeight: 900, color: 'var(--sc-dark)', fontSize: 16 }}
                                        />
                                        <div className="sc-addon-right" style={{ padding: '0 6px', fontSize: 10 }}>/unit</div>
                                    </div>
                                </div>

                                {/* Quick Tariff Presets */}
                                <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
                                    {[4.5, 6.0, 8.0, 10.0, 12.0].map(tr => (
                                        <button
                                            key={tr}
                                            type="button"
                                            onClick={() => setTariff(tr)}
                                            style={{
                                                background: tariff === tr ? '#dcfce7' : '#f8fbf9',
                                                border: `1px solid ${tariff === tr ? '#16a34a' : '#d4ebd9'}`,
                                                color: tariff === tr ? '#15803d' : '#537359',
                                                padding: '3px 10px',
                                                borderRadius: 8,
                                                fontSize: 11,
                                                fontWeight: 700,
                                                cursor: 'pointer',
                                            }}
                                        >
                                            ₹{tr.toFixed(2)}/u
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* 3. Panel Tech Selection */}
                            <div>
                                <label style={{ fontSize: 12, fontWeight: 700, color: '#537359', display: 'block', marginBottom: 6 }}>
                                    🛡️ Panel Technology
                                </label>
                                <select
                                    value={panelTypeId}
                                    onChange={(e) => handlePanelTypeChange(e.target.value)}
                                    style={{ width: '100%', padding: '10px 14px', borderRadius: 12, border: '1px solid #d4ebd9', fontWeight: 700, outline: 'none' }}
                                >
                                    {PANEL_TYPES.map(p => (
                                        <option key={p.id} value={p.id}>{p.label}</option>
                                    ))}
                                </select>
                                <div style={{ fontSize: 11, color: '#537359', marginTop: 6 }}>
                                    {PANEL_TYPES.find(p => p.id === panelTypeId)?.desc}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* TODAY & FINANCIAL SAVINGS CARDS (TODAY SAVINGS, MONTHLY SAVINGS, ANNUAL SAVINGS) */}
                    {todayResult && (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
                            {/* Today Output */}
                            <div style={{ background: 'linear-gradient(135deg, #108e48, #16a34a)', borderRadius: 20, padding: 20, color: '#fff' }}>
                                <div style={{ fontSize: 11, textTransform: 'uppercase', opacity: 0.85, fontWeight: 700 }}>TODAY'S GENERATION</div>
                                <div style={{ fontSize: 30, fontWeight: 900, marginTop: 4 }}>
                                    {todayResult.totalKwh} <span style={{ fontSize: 14 }}>kWh</span>
                                </div>
                                <div style={{ fontSize: 11, opacity: 0.9, marginTop: 4 }}>
                                    {todayResult.condition.icon} {todayResult.condition.label} · Peak: {todayResult.peakKw} kW
                                </div>
                            </div>

                            {/* Today Savings */}
                            <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 20 }}>
                                <div style={{ fontSize: 11, textTransform: 'uppercase', color: '#537359', fontWeight: 700 }}>TODAY'S SAVINGS</div>
                                <div style={{ fontSize: 28, fontWeight: 900, color: '#16a34a', marginTop: 4 }}>
                                    ₹{formatRupees(todayResult.totalKwh * tariff)}
                                </div>
                                <div style={{ fontSize: 11, color: '#537359', marginTop: 4 }}>
                                    @ ₹{Number(tariff).toFixed(2)} / unit electricity rate
                                </div>
                            </div>

                            {/* Monthly Savings */}
                            <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 20 }}>
                                <div style={{ fontSize: 11, textTransform: 'uppercase', color: '#537359', fontWeight: 700 }}>ESTIMATED MONTHLY SAVINGS</div>
                                <div style={{ fontSize: 28, fontWeight: 900, color: '#16a34a', marginTop: 4 }}>
                                    ₹{formatRupees(todayResult.totalKwh * 30 * tariff)}
                                </div>
                                <div style={{ fontSize: 11, color: '#537359', marginTop: 4 }}>
                                    ~{Math.round(todayResult.totalKwh * 30)} kWh monthly bill offset
                                </div>
                            </div>

                            {/* Annual Savings */}
                            <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 20 }}>
                                <div style={{ fontSize: 11, textTransform: 'uppercase', color: '#537359', fontWeight: 700 }}>ANNUAL SAVINGS (YEAR 1)</div>
                                <div style={{ fontSize: 28, fontWeight: 900, color: '#0f5132', marginTop: 4 }}>
                                    ₹{formatRupees(todayResult.totalKwh * 365 * 0.98 * tariff)}
                                </div>
                                <div style={{ fontSize: 11, color: '#537359', marginTop: 4 }}>
                                    ~{Math.round(todayResult.totalKwh * 365 * 0.98)} kWh total yearly generation
                                </div>
                            </div>
                        </div>
                    )}

                    {/* NEXT 7 DAYS FORECAST CARDS */}
                    {weekData && weekData.length > 0 && (
                        <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 24 }}>
                            <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'space-between' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <Calendar size={18} color="var(--sc-green)" /> Next 7 Days Predicted Output & Weather
                                </div>
                                <span style={{ fontSize: 12, color: '#537359', fontWeight: 600 }}>Click any day to view detailed hourly curve</span>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(110px, 1fr))', gap: 12 }}>
                                {weekData.map((day, idx) => {
                                    const cond = wmoToCondition(day.weatherCode);
                                    const isSelected = selectedDayIdx === idx;
                                    const daySavings = Math.round(day.totalKwh * tariff);
                                    return (
                                        <div
                                            key={day.dateStr}
                                            onClick={() => setSelectedDayIdx(idx)}
                                            style={{
                                                background: isSelected ? 'linear-gradient(135deg, #108e48, #16a34a)' : '#f8fbf9',
                                                color: isSelected ? '#fff' : '#132a18',
                                                border: `2px solid ${isSelected ? '#16a34a' : '#d4ebd9'}`,
                                                borderRadius: 16,
                                                padding: '14px 10px',
                                                cursor: 'pointer',
                                                textAlign: 'center',
                                                transition: 'all 0.2s ease',
                                                boxShadow: isSelected ? '0 6px 18px rgba(16, 142, 72, 0.3)' : 'none',
                                            }}
                                        >
                                            <div style={{ fontSize: 11, fontWeight: 700, opacity: isSelected ? 0.9 : 0.7 }}>
                                                {idx === 0 ? 'Today' : day.dayLabel}
                                            </div>
                                            <div style={{ fontSize: 26, margin: '6px 0' }}>{cond.icon}</div>
                                            <div style={{ fontSize: 16, fontWeight: 900 }}>
                                                {day.totalKwh} <span style={{ fontSize: 10 }}>kWh</span>
                                            </div>
                                            <div style={{ fontSize: 11, fontWeight: 700, color: isSelected ? '#fff' : '#16a34a', marginTop: 2 }}>
                                                ₹{formatRupees(daySavings)}
                                            </div>
                                            <div style={{ fontSize: 10, opacity: 0.75, marginTop: 4 }}>
                                                {day.maxTemp}° / {day.minTemp}°
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* Hourly Output Curve for Selected Day */}
                    <div style={{ background: '#fff', border: '1px solid #d4ebd9', borderRadius: 20, padding: 24 }}>
                        <div style={{ fontWeight: 800, fontSize: 15, marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span>📈 Hourly Generation Profile for {selectedDayIdx === 0 ? 'Today' : selectedDayData?.dayLabel} ({capacityKw} kWp System @ ₹{Number(tariff).toFixed(2)}/kWh)</span>
                            <span style={{ fontSize: 12, color: 'var(--sc-green)', fontWeight: 700 }}>
                                {selectedDayData ? `${selectedDayData.totalKwh} kWh Total` : ''}
                            </span>
                        </div>
                        <div style={{ height: 290, width: '100%' }}>
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={chartHourly}>
                                    <defs>
                                        <linearGradient id="liveSolarGrad" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#16a34a" stopOpacity={0.4} />
                                            <stop offset="95%" stopColor="#16a34a" stopOpacity={0.0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#e2ece4" />
                                    <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                                    <YAxis tick={{ fontSize: 11 }} unit=" kW" />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Area type="monotone" dataKey="kw" stroke="#16a34a" strokeWidth={2.5} fill="url(#liveSolarGrad)" name="Generation (kW)" />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
