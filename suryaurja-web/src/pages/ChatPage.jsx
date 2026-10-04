import { useState, useEffect, useRef } from 'react';
import { Send, Trash2, CloudUpload } from 'lucide-react';
import { callGemini, getGeminiKey, loadChatHistory, saveChatHistory } from '../api/index';
import { CHATBOT_ROLES } from '../data/solarData';

const GEMINI_MODELS = [
    { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash', desc: 'Latest · Fast & powerful' },
    { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', desc: 'Efficient multimodal' },
    { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash', desc: 'Balanced speed/quality' },
    { id: 'gemini-1.5-pro',   name: 'Gemini 1.5 Pro',   desc: 'Highest accuracy' },
];

function buildSystemPrompt(role, plant, hourPoint) {
    const plantCtx = plant ? `You are assisting with the ${plant.name} (${plant.capacityKw} kW ${plant.panelType} array in ${plant.location}).` : '';
    const forecastCtx = hourPoint ? `Current CNN-LSTM+ENN forecast for hour ${hourPoint.timeLabel}: ${hourPoint.predictedKw} kW (CI: [${hourPoint.lowerBoundKw}–${hourPoint.upperBoundKw} kW]). GHI: ${hourPoint.ghi} W/m², Temp: ${hourPoint.temperature}°C.` : '';
    const base = `You are SuryaUrja AI, an expert ${role.title} (${role.description}) for a solar PV forecasting platform powered by a hybrid CNN-LSTM + Ensemble Neural Network. ${plantCtx} ${forecastCtx} Respond concisely and with domain expertise. Use numbers where available. Reply in plain text without markdown formatting.`;
    return base;
}

export default function ChatPage({ selectedPlant, activeHourPoint, showToast }) {
    const [messages, setMessages] = useState(() => loadChatHistory());
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [selectedRole, setRole] = useState(CHATBOT_ROLES[0]);
    const [selectedModel, setModel] = useState(GEMINI_MODELS[0]);
    const [showRoles, setShowRoles] = useState(false);
    const [showModels, setShowModels] = useState(false);
    const [apiKeyInput, setApiKeyInput] = useState(getGeminiKey());
    const [showKeyInput, setShowKeyInput] = useState(!getGeminiKey());
    const bottomRef = useRef(null);

    useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages, isLoading]);
    useEffect(() => { saveChatHistory(messages); }, [messages]);

    const sendMessage = async (text) => {
        if (!text.trim() || isLoading) return;
        const userMsg = { id: Date.now(), role: 'user', text, timestamp: Date.now() };
        const history = messages.map(m => ({ role: m.role, text: m.text }));
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setIsLoading(true);

        try {
            const reply = await callGemini({
                prompt: text,
                roleSystem: buildSystemPrompt(selectedRole, selectedPlant, activeHourPoint),
                modelId: selectedModel.id,
                history,
            });
            setMessages(prev => [...prev, { id: Date.now() + 1, role: 'model', text: reply, timestamp: Date.now(), model: selectedModel.name }]);
        } catch (e) {
            setMessages(prev => [...prev, { id: Date.now() + 1, role: 'model', text: `⚠ ${e.message}`, timestamp: Date.now(), isError: true }]);
            if (e.message.includes('API key')) setShowKeyInput(true);
        } finally {
            setIsLoading(false);
        }
    };

    const clearHistory = () => {
        setMessages([]);
        saveChatHistory([]);
        showToast?.('Chat history cleared');
    };

    const saveToCloud = () => {
        saveChatHistory(messages);
        showToast?.('Conversation saved to localStorage (cloud sync via Firestore in production)');
    };

    const QUICK = [
        '⚡ Analyze today\'s forecast variance',
        '🔬 Explain CNN-LSTM + ENN layers',
        '📈 What\'s the optimal battery charging window?',
        '🌡 Impact of +5°C temperature on yield?',
        '🔌 Grid frequency deviation response?',
    ];

    return (
        <div className="chat-shell">
            {/* Control Ribbon */}
            <div className="chat-ribbon">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-12">
                        {/* Role chip */}
                        <div style={{ position: 'relative' }}>
                            <button className="chip active-amber" onClick={() => setShowRoles(v => !v)}>
                                {selectedRole.icon} {selectedRole.title} ▾
                            </button>
                            {showRoles && (
                                <div style={{ position: 'absolute', top: '110%', left: 0, zIndex: 200, background: '#0D1829', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, overflow: 'hidden', minWidth: 220, boxShadow: '0 16px 48px rgba(0,0,0,0.5)' }}>
                                    {CHATBOT_ROLES.map(r => (
                                        <div key={r.id} onClick={() => { setRole(r); setShowRoles(false); }}
                                            style={{ padding: '10px 14px', cursor: 'pointer', borderBottom: '1px solid rgba(255,255,255,0.05)', background: r.id === selectedRole.id ? 'rgba(245,158,11,0.1)' : 'transparent' }}>
                                            <div style={{ fontSize: 12, fontWeight: 600 }}>{r.icon} {r.title}</div>
                                            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>{r.description}</div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Model chip */}
                        <div style={{ position: 'relative' }}>
                            <button className="chip active-cyan" onClick={() => setShowModels(v => !v)}>
                                {selectedModel.name} ▾
                            </button>
                            {showModels && (
                                <div style={{ position: 'absolute', top: '110%', left: 0, zIndex: 200, background: '#0D1829', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, overflow: 'hidden', minWidth: 220, boxShadow: '0 16px 48px rgba(0,0,0,0.5)' }}>
                                    {GEMINI_MODELS.map(m => (
                                        <div key={m.id} onClick={() => { setModel(m); setShowModels(false); }}
                                            style={{ padding: '10px 14px', cursor: 'pointer', borderBottom: '1px solid rgba(255,255,255,0.05)', background: m.id === selectedModel.id ? 'rgba(34,211,238,0.08)' : 'transparent' }}>
                                            <div style={{ fontSize: 12, fontWeight: 600 }}>{m.name}</div>
                                            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>{m.desc}</div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="flex items-center gap-8">
                        <button className="btn btn-outline btn-icon" onClick={saveToCloud} title="Save to cloud"><CloudUpload size={14} /></button>
                        <button className="btn btn-outline btn-icon" onClick={clearHistory} title="Clear chat"><Trash2 size={14} /></button>
                    </div>
                </div>

                {/* Plant context banner */}
                {selectedPlant && (
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', padding: '4px 8px', background: 'rgba(245,158,11,0.05)', borderRadius: 8, border: '1px solid rgba(245,158,11,0.15)' }}>
                        🌡 Context: {selectedPlant.name} ({selectedPlant.capacityKw} kW)
                        {activeHourPoint && ` • ENN: ${activeHourPoint.predictedKw} kW @ ${activeHourPoint.timeLabel}`}
                    </div>
                )}

                {/* API key input */}
                {showKeyInput && (
                    <div style={{ display: 'flex', gap: 8 }}>
                        <input
                            className="text-input" placeholder="Enter Gemini API key (AIza…) — stored in browser only"
                            value={apiKeyInput} onChange={e => setApiKeyInput(e.target.value)}
                            type="password"
                        />
                        <button className="btn btn-amber" onClick={() => {
                            if (apiKeyInput) { localStorage.setItem('suryaurja_gemini_key', apiKeyInput); setShowKeyInput(false); showToast?.('Gemini API key saved'); }
                        }}>Save</button>
                    </div>
                )}
            </div>

            {/* Messages */}
            <div className="chat-messages">
                {messages.length === 0 && (
                    <div style={{ textAlign: 'center', margin: 'auto', padding: 40 }}>
                        <div style={{ fontSize: 32, marginBottom: 12 }}>{selectedRole.icon}</div>
                        <div style={{ fontWeight: 700, fontSize: 16, color: 'var(--text-primary)' }}>SuryaUrja {selectedRole.title}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 6, maxWidth: 360, margin: '8px auto 0' }}>{selectedRole.description}</div>
                    </div>
                )}

                {messages.map(msg => (
                    <div key={msg.id} className={`bubble-row ${msg.role === 'user' ? 'bubble-row-user' : 'bubble-row-ai'}`}>
                        {msg.role !== 'user' && (
                            <div style={{ fontSize: 10, color: 'var(--amber)', fontWeight: 600, marginBottom: 3 }}>
                                {selectedRole.icon} {msg.model || selectedModel.name}
                            </div>
                        )}
                        <div className={`bubble ${msg.role === 'user' ? 'bubble-user' : msg.isError ? 'bubble-ai bubble-error' : 'bubble-ai'}`}>
                            {msg.text}
                        </div>
                        <div className="bubble-time">{new Date(msg.timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</div>
                    </div>
                ))}

                {isLoading && (
                    <div className="bubble-row bubble-row-ai">
                        <div style={{ fontSize: 10, color: 'var(--amber)', fontWeight: 600, marginBottom: 3 }}>{selectedRole.icon} {selectedModel.name}</div>
                        <div className="bubble bubble-ai">
                            <div className="typing-dots"><span /><span /><span /></div>
                        </div>
                    </div>
                )}
                <div ref={bottomRef} />
            </div>

            {/* Quick suggestion chips */}
            <div style={{ padding: '6px 18px', display: 'flex', gap: 8, overflowX: 'auto', borderTop: '1px solid var(--border)', background: 'rgba(8,12,24,0.85)' }}>
                {QUICK.map(q => (
                    <button key={q} className="chip" style={{ whiteSpace: 'nowrap', flexShrink: 0 }} onClick={() => sendMessage(q)}>{q}</button>
                ))}
            </div>

            {/* Input row */}
            <div className="chat-input-row">
                <textarea
                    className="chat-input"
                    rows={1}
                    placeholder={`Ask ${selectedRole.title}…`}
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(input); } }}
                />
                <button className="send-btn" disabled={!input.trim() || isLoading} onClick={() => sendMessage(input)}>
                    <Send size={16} color="#000" />
                </button>
            </div>
        </div>
    );
}
