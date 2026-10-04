export default function Toast({ message }) {
    return (
        <div className="toast">
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                <span style={{ fontSize: 14, marginTop: 1 }}>⚡</span>
                <span style={{ color: 'var(--text-primary)', fontSize: 12, lineHeight: 1.5 }}>{message}</span>
            </div>
        </div>
    );
}
