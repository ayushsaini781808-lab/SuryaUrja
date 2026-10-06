import { useState, useCallback } from 'react';
import './index.css';
import { PLANTS, BENCHMARKS, NEURAL_LAYERS, FEATURE_IMPORTANCES, DEFAULT_ALERTS, buildDaySummaries } from './data/solarData';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import DashboardPage from './pages/DashboardPage';
import HomeSolarPage from './pages/HomeSolarPage';
import MultiDayPage from './pages/MultiDayPage';
import ModelsPage from './pages/ModelsPage';
import AlertsPage from './pages/AlertsPage';
import WeatherPage from './pages/WeatherPage';
import ChatPage from './pages/ChatPage';
import AuthPage from './pages/AuthPage';
import Toast from './components/Toast';

export default function App() {
  const [page, setPage] = useState('homesolar');

  const [selectedPlant, setSelectedPlant] = useState(PLANTS[0]);
  const [selectedPersona, setSelectedPersona] = useState('PLANT_OPERATOR');
  const [selectedDayIndex, setSelectedDayIndex] = useState(1);
  const [selectedHour, setSelectedHour] = useState(null);
  const [daySummaries, setDaySummaries] = useState(() => buildDaySummaries(PLANTS[0].capacityKw));
  const [alerts, setAlerts] = useState(DEFAULT_ALERTS);
  const [toast, setToast] = useState(null);

  const [cloudDelta, setCloudDelta] = useState(0);
  const [tempDelta, setTempDelta] = useState(0);
  const [isSimulating, setIsSimulating] = useState(false);

  const [locationName, setLocationName] = useState('Campus 200kW Plant (Delhi)');
  const [latitude, setLatitude] = useState(28.6139);
  const [longitude, setLongitude] = useState(77.2090);
  const [isSynced, setIsSynced] = useState(false);
  const [syncTime, setSyncTime] = useState(null);

  const currentDay = daySummaries[selectedDayIndex] || daySummaries[0];
  const activeHourPoint = selectedHour || currentDay?.hourlyPoints?.[12] || null;
  const overview = {
    currentGenerationKw: currentDay?.hourlyPoints?.[13]?.predictedKw ?? 162.4,
    capacityUtilizationPercent: Math.round((currentDay?.hourlyPoints?.[13]?.predictedKw ?? 162.4) / selectedPlant.capacityKw * 1000) / 10,
    todayTotalKwh: currentDay?.totalEnergyKwh ?? 1184,
    nextDayPeakKw: daySummaries[1]?.peakPowerKw ?? 178.5,
    modelR2: 0.9991,
    dayAheadMaeKw: 1.579,
    activeAlertsCount: alerts.filter(a => !a.isAcknowledged).length,
  };

  const showToast = useCallback((msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3500);
  }, []);

  const handleUpdateCapacity = useCallback((newKw) => {
    const kw = Math.max(0.1, Number(newKw) || 200);
    setSelectedPlant(prev => ({ ...prev, capacityKw: kw }));
    setDaySummaries(buildDaySummaries(kw, cloudDelta, tempDelta));
    showToast(`System capacity updated to ${kw} kW`);
  }, [cloudDelta, tempDelta, showToast]);

  const handleSelectPlant = useCallback((plant) => {
    setSelectedPlant(plant);
    setDaySummaries(buildDaySummaries(plant.capacityKw));
    setSelectedHour(null);
    showToast(`Switched to ${plant.name}`);
  }, [showToast]);

  const handleSimulate = useCallback((cloud, temp) => {
    setCloudDelta(cloud);
    setTempDelta(temp);
    setIsSimulating(true);
    setTimeout(() => {
      setDaySummaries(buildDaySummaries(selectedPlant.capacityKw, cloud, temp));
      setIsSimulating(false);
      showToast(`CNN-LSTM+ENN re-inferred | Cloud Δ: ${cloud >= 0 ? '+' : ''}${Math.round(cloud)}% | Temp Δ: ${temp >= 0 ? '+' : ''}${temp.toFixed(1)}°C`);
    }, 800);
  }, [selectedPlant, showToast]);

  const handleResetSim = useCallback(() => {
    setCloudDelta(0);
    setTempDelta(0);
    setDaySummaries(buildDaySummaries(selectedPlant.capacityKw));
    showToast('Simulation reset to live weather data');
  }, [selectedPlant, showToast]);

  const handleSyncWeather = useCallback((lat, lon, name) => {
    setLatitude(lat);
    setLongitude(lon);
    setLocationName(name);
    setIsSynced(true);
    setSyncTime(new Date().toLocaleTimeString('en-IN', { hour12: false }));
  }, []);

  const handleAcknowledge = useCallback((alertId) => {
    setAlerts(prev => prev.map(a => a.id === alertId ? { ...a, isAcknowledged: true } : a));
    showToast('Alert acknowledged');
  }, [showToast]);

  const handleExportCsv = useCallback(() => {
    const pts = currentDay?.hourlyPoints ?? [];
    const header = 'Hour,Time,Predicted(kW),Actual(kW),LowerCI(kW),UpperCI(kW),GHI(W/m²),Temp(°C),Cloud(%)\n';
    const rows = pts.map(p =>
      `${p.hour},${p.timeLabel},${p.predictedKw},${p.actualKw ?? ''},${p.lowerBoundKw},${p.upperBoundKw},${p.ghi},${p.temperature},${p.cloudCover}`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `SuryaUrja_${selectedPlant.name.replace(/ /g, '_')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('CSV exported');
  }, [currentDay, selectedPlant, showToast]);

  const sharedProps = {
    page, setPage,
    plants: PLANTS, selectedPlant, onSelectPlant: handleSelectPlant, onUpdateCapacity: handleUpdateCapacity,
    selectedPersona, onSelectPersona: setSelectedPersona,
    daySummaries, selectedDayIndex, onSelectDay: setSelectedDayIndex,
    activeHourPoint, onSelectHour: setSelectedHour,
    overview, alerts, onAcknowledge: handleAcknowledge,
    benchmarks: BENCHMARKS, neuralLayers: NEURAL_LAYERS, featureImportances: FEATURE_IMPORTANCES,
    cloudDelta, tempDelta, isSimulating,
    onSimulate: handleSimulate, onResetSim: handleResetSim,
    locationName, latitude, longitude, isSynced, syncTime,
    onSyncWeather: handleSyncWeather,
    onExportCsv: handleExportCsv,
    showToast,
  };

  const PAGES_MAP = {
    dashboard: <DashboardPage {...sharedProps} />,
    homesolar: <HomeSolarPage showToast={showToast} />,
    multiday: <MultiDayPage  {...sharedProps} />,
    models: <ModelsPage    {...sharedProps} />,
    alerts: <AlertsPage    {...sharedProps} />,
    weather: <WeatherPage   {...sharedProps} />,
    chat: <ChatPage      {...sharedProps} />,
    auth: <AuthPage      {...sharedProps} />,
  };

  return (
    <div className="app-shell">
      <Sidebar activePage={page} onNavigate={setPage} alertCount={overview.activeAlertsCount} />
      <div className="main-content">
        <Topbar
          selectedPlant={selectedPlant}
          plants={PLANTS}
          onSelectPlant={handleSelectPlant}
          onUpdateCapacity={handleUpdateCapacity}
          overview={overview}
          isSynced={isSynced}
          locationName={locationName}
          page={page}
        />
        <div className="page">
          {PAGES_MAP[page] || PAGES_MAP.dashboard}
        </div>
      </div>
      {toast && <Toast message={toast} />}
    </div>
  );
}
