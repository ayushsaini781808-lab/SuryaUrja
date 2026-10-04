import { LayoutDashboard, CalendarDays, BrainCircuit, Bell, Cloud, MessageSquare, User, Sun, Home, Zap } from 'lucide-react';

const NAV = [
    { id: 'dashboard',  icon: LayoutDashboard, label: 'Dashboard' },
    { id: 'homesolar',  icon: Home,             label: 'Solar Calculator' },
    { id: 'multiday',   icon: CalendarDays,     label: '7-Day Forecast' },
    { id: 'models',     icon: BrainCircuit,     label: 'Model Evaluation' },
    { id: 'alerts',     icon: Bell,             label: 'Grid Alerts' },
    { id: 'weather',    icon: Cloud,            label: 'Weather & Data' },
    { id: 'chat',       icon: MessageSquare,    label: 'AI Assistant' },
    { id: 'auth',       icon: User,             label: 'Account' },
];

export default function Sidebar({ activePage, onNavigate, alertCount }) {
    return (
        <nav className="sidebar">
            {/* Brand Logo */}
            <div
                className="sidebar-logo"
                title="SuryaUrja — Solar Intelligence Platform"
                style={{ cursor: 'pointer' }}
                onClick={() => onNavigate('dashboard')}
            >
                <Sun size={22} color="#fff" strokeWidth={2.5} />
            </div>

            {/* Brand text — vertical */}
            <div style={{
                writingMode: 'vertical-rl',
                transform: 'rotate(180deg)',
                fontSize: 8,
                fontWeight: 800,
                letterSpacing: '2px',
                color: 'rgba(255,255,255,0.25)',
                textTransform: 'uppercase',
                marginBottom: 12,
                marginTop: 4,
                userSelect: 'none',
            }}>
                SuryaUrja
            </div>

            {/* Nav Items */}
            {NAV.map(({ id, icon: Icon, label }) => (
                <button
                    key={id}
                    className={`sidebar-nav-item ${activePage === id ? 'active' : ''}`}
                    onClick={() => onNavigate(id)}
                    title={label}
                >
                    <Icon size={19} strokeWidth={activePage === id ? 2.5 : 1.8} />

                    {/* Alert badge */}
                    {id === 'alerts' && alertCount > 0 && (
                        <span style={{
                            position: 'absolute', top: 7, right: 7,
                            width: 15, height: 15, borderRadius: '50%',
                            background: '#EF4444', color: '#fff',
                            fontSize: 8, fontWeight: 800,
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            border: '1.5px solid #091840',
                        }}>{alertCount}</span>
                    )}

                    <span className="tooltip">{label}</span>
                </button>
            ))}

            {/* Bottom: live indicator */}
            <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, paddingTop: 12 }}>
                <Zap size={14} color="rgba(34,197,94,0.7)" />
                <div style={{
                    width: 7, height: 7, borderRadius: '50%',
                    background: '#22C55E', animation: 'pulse-dot 2s infinite',
                }} />
            </div>

            <style>{`
                @keyframes pulse-dot {
                    0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(34,197,94,0.5); }
                    50% { opacity: 0.7; box-shadow: 0 0 0 5px rgba(34,197,94,0); }
                }
            `}</style>
        </nav>
    );
}
