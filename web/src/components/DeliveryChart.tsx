import type { MinuteMetric } from '../types'

export default function DeliveryChart({ metrics }: { metrics: MinuteMetric[] }) {
  if (metrics.length === 0) {
    return <div className="empty">No delivery attempts yet.</div>
  }

  const max = Math.max(1, ...metrics.map((item) => item.sent + item.failed))

  return (
    <>
      <div className="chart">
        {metrics.map((item) => (
          <div
            className="bar"
            key={item.minute}
            title={`${new Date(item.minute).toLocaleTimeString()} — ${item.sent} sent, ${item.failed} failed`}
          >
            <span className="failed" style={{ height: `${(item.failed / max) * 100}%` }} />
            <span className="sent" style={{ height: `${(item.sent / max) * 100}%` }} />
          </div>
        ))}
      </div>
      <div className="legend">
        <span>
          <i style={{ background: 'var(--green)' }} />
          Sent
        </span>
        <span>
          <i style={{ background: 'var(--red)' }} />
          Failed attempt
        </span>
      </div>
    </>
  )
}
