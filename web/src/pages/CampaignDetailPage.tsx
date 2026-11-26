import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getCampaign, retryFailures } from '../api'
import DeliveryChart from '../components/DeliveryChart'
import StatCard from '../components/StatCard'
import StatusBadge from '../components/StatusBadge'
import { useTenant } from '../TenantContext'

export default function CampaignDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { tenant } = useTenant()
  const queryClient = useQueryClient()

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['campaign', tenant.id, id],
    queryFn: () => getCampaign(tenant.token ?? '', id ?? ''),
    enabled: Boolean(tenant.token && id),
    refetchInterval: 3000,
  })

  const retry = useMutation({
    mutationFn: () => retryFailures(tenant.token ?? '', id ?? ''),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['campaign', tenant.id, id] }),
  })

  if (isLoading) return <div className="loading">Loading campaign…</div>
  if (isError || !data) {
    return <div className="error-box">{error instanceof Error ? error.message : 'Campaign not found.'}</div>
  }

  return (
    <>
      <Link className="back" to="/">
        ← Back to campaigns
      </Link>

      <div className="page-head">
        <div>
          <h1>{data.name}</h1>
          <p>
            {data.channel} · created {new Date(data.createdAt).toLocaleString()} · {data.id.slice(0, 8)}
          </p>
        </div>
        <div className="spacer" />
        <StatusBadge status={data.status} />
      </div>

      <div className="stats">
        <StatCard label="Recipients" value={data.totalRecipients} />
        <StatCard label="Sent" value={data.sent} />
        <StatCard label="Failed" value={data.failed} />
        <StatCard label="Skipped" value={data.skipped} />
        <StatCard label="Pending" value={data.pending} />
        <StatCard label="Retry queue" value={data.retryQueue} />
      </div>

      <div className="detail-grid">
        <div style={{ display: 'grid', gap: 18 }}>
          <section className="card panel">
            <h2>Delivery attempts per minute</h2>
            <DeliveryChart metrics={data.metrics} />
          </section>

          <section className="card panel">
            <h2>Error breakdown</h2>
            {data.errors.length === 0 ? (
              <div className="muted">No terminal failures.</div>
            ) : (
              <div className="errors">
                {data.errors.map((item) => (
                  <div className="error-row" key={item.error}>
                    <span>{item.error}</span>
                    <strong>{item.count}</strong>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        <aside style={{ display: 'grid', gap: 18 }}>
          <section className="card panel">
            <h2>Template</h2>
            <div className="template">{data.template}</div>
          </section>

          <section className="card panel">
            <h2>Schedule</h2>
            <div className="muted" style={{ fontSize: 13, lineHeight: 1.8 }}>
              <div>Scheduled: {new Date(data.scheduledAt).toLocaleString()}</div>
              {data.startedAt && <div>Started: {new Date(data.startedAt).toLocaleString()}</div>}
              {data.completedAt && <div>Finished: {new Date(data.completedAt).toLocaleString()}</div>}
              <div>Transactional: {data.transactional ? 'yes' : 'no'}</div>
            </div>
          </section>

          <section className="card panel">
            <h2>Recovery</h2>
            <p className="muted" style={{ fontSize: 13, lineHeight: 1.6, marginBottom: 12 }}>
              Requeue terminal failures after the provider recovers. Successful sends are never repeated.
            </p>
            <button
              className="button"
              disabled={data.failed === 0 || retry.isPending}
              onClick={() => retry.mutate()}
            >
              {retry.isPending ? 'Requeuing…' : `Retry ${data.failed} failed`}
            </button>
            {retry.data && (
              <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>
                Requeued {retry.data.requeued} jobs.
              </p>
            )}
            {retry.error && <div className="error-box" style={{ marginTop: 10 }}>{retry.error.message}</div>}
          </section>
        </aside>
      </div>
    </>
  )
}
